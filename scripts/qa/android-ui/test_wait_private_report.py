import json
import itertools
import subprocess
import unittest
from unittest.mock import patch

import wait_private_report as waiter

READY = {'id': 123, 'status': 'completed', 'conclusion': 'success'}
PENDING = {'id': 123, 'status': 'in_progress'}


class PublicationWaitTest(unittest.TestCase):
    def run_wait(self, responses, timeout=60):
        clock = [0]

        def sleep(seconds):
            clock[0] += seconds

        def request(*args, **kwargs):
            result = next(responses)
            if isinstance(result, Exception):
                raise result
            return subprocess.CompletedProcess(args, 0, stdout=json.dumps(result))
        with patch.object(waiter.subprocess, "run", side_effect=request), \
                patch.object(waiter.time, "sleep", side_effect=sleep), \
                patch.object(waiter.time, "monotonic", side_effect=lambda: clock[0]):
            waiter.wait_for_report("wireapp/reports", "123", timeout=timeout)
        return clock[0]

    def test_waits_for_api_visibility(self):
        self.run_wait(iter([PENDING, PENDING, READY]))

    def test_retries_temporary_api_failures(self):
        self.run_wait(iter([subprocess.CalledProcessError(1, "gh"),
                            subprocess.TimeoutExpired("gh", 30), READY]))

    def test_retries_invalid_json(self):
        self.run_wait(iter([json.JSONDecodeError("bad", "", 0), READY]))

    def test_healthy_publisher_can_take_longer_than_five_minutes(self):
        elapsed = self.run_wait(iter([PENDING] * 40 + [READY]), timeout=300)
        self.assertGreater(elapsed, 300)

    def test_successful_status_resets_api_outage_deadline(self):
        error = subprocess.CalledProcessError(1, "gh")
        self.run_wait(iter([error, error, PENDING, error, error, READY]), timeout=20)

    def test_pending_status_clears_previous_api_outage(self):
        error = subprocess.CalledProcessError(1, "gh")
        self.run_wait(iter([error] + [PENDING] * 40 + [READY]), timeout=20)

    def test_continuous_api_outage_times_out_instead_of_claiming_success(self):
        with self.assertRaises(TimeoutError):
            self.run_wait(itertools.repeat(subprocess.CalledProcessError(1, "gh")), timeout=20)

    def test_failed_or_cancelled_publisher_is_not_success(self):
        for conclusion in ("failure", "cancelled", "timed_out"):
            with self.subTest(conclusion=conclusion), self.assertRaises(RuntimeError):
                self.run_wait(iter([dict(READY, conclusion=conclusion)]))

    def test_rejects_different_run(self):
        with self.assertRaises(ValueError):
            self.run_wait(iter([dict(READY, id=456)]))

if __name__ == "__main__":
    unittest.main()
