import json
import subprocess
import unittest
from unittest.mock import patch

import wait_private_report as waiter

READY = {'id': 123, 'status': 'completed', 'conclusion': 'success'}
PENDING = {'id': 123, 'status': 'in_progress'}


class PublicationWaitTest(unittest.TestCase):
    def run_wait(self, responses, timeout=60):
        def request(*args, **kwargs):
            result = next(responses)
            if isinstance(result, Exception):
                raise result
            return subprocess.CompletedProcess(args, 0, stdout=json.dumps(result))
        with patch.object(waiter.subprocess, "run", side_effect=request), \
                patch.object(waiter.time, "sleep"), \
                patch.object(waiter.time, "monotonic", side_effect=range(1000)):
            waiter.wait_for_report("wireapp/reports", "123", timeout=timeout)

    def test_waits_for_api_visibility(self):
        self.run_wait(iter([PENDING, PENDING, READY]))

    def test_retries_temporary_api_failures(self):
        self.run_wait(iter([subprocess.CalledProcessError(1, "gh"),
                            subprocess.TimeoutExpired("gh", 30), READY]))

    def test_retries_invalid_json(self):
        self.run_wait(iter([json.JSONDecodeError("bad", "", 0), READY]))

    def test_times_out_instead_of_claiming_success(self):
        with self.assertRaises(TimeoutError):
            self.run_wait(iter([PENDING] * 20), timeout=5)

    def test_failed_or_cancelled_publisher_is_not_success(self):
        for conclusion in ("failure", "cancelled", "timed_out"):
            with self.subTest(conclusion=conclusion), self.assertRaises(RuntimeError):
                self.run_wait(iter([dict(READY, conclusion=conclusion)]))

    def test_rejects_different_run(self):
        with self.assertRaises(ValueError):
            self.run_wait(iter([dict(READY, id=456)]))

if __name__ == "__main__":
    unittest.main()
