"""Fixtures for the private handoff's file boundary and log compression."""
import gzip
from pathlib import Path
import tempfile
import unittest

from prepare_private_report import copy_evidence


class PrivateEvidenceTests(unittest.TestCase):
    def test_compresses_log_data_without_changing_its_content(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "logs"
            source.mkdir()
            original = b"phone-id original instrumentation output\n"
            (source / "attempt.log").write_bytes(original)
            self.assertEqual(1, copy_evidence(source, root / "private", compress=True))
            self.assertEqual(original, gzip.decompress((root / "private" / "attempt.log.gz").read_bytes()))
            self.assertFalse((root / "private" / "attempt.log").exists())

    def test_refuses_to_follow_symlinks_to_files_outside_the_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "results"
            source.mkdir()
            secret = root / "secrets.json"
            secret.write_text("not test evidence")
            (source / "attachment").symlink_to(secret)
            with self.assertRaisesRegex(ValueError, "symbolic links"):
                copy_evidence(source, root / "private")
            self.assertFalse((root / "private" / "attachment").exists())


if __name__ == "__main__":
    unittest.main()
