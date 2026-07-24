#!/usr/bin/env python3
"""Unit tests for least-privilege Android E2E secret selection."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from fetch_secrets_json import BACKEND_FIELDS, PASSWORD_FIELD, allowed_fields_for_title, filter_item_fields, sanitize


class FetchSecretsJsonTest(unittest.TestCase):
    def test_selects_only_known_device_and_host_sections(self) -> None:
        self.assertEqual(BACKEND_FIELDS, allowed_fields_for_title("BackendConnection staging"))
        self.assertEqual(PASSWORD_FIELD, allowed_fields_for_title("KEYCLOAK_QA_AUTOMATION"))
        self.assertEqual(PASSWORD_FIELD, allowed_fields_for_title("TESTINY_API_KEY_ANDROID"))
        self.assertIsNone(allowed_fields_for_title("Unrelated production credential"))

    def test_filters_unneeded_fields_from_selected_item(self) -> None:
        item = {
            "id": "item-id",
            "title": "TESTINY_API_KEY_ANDROID",
            "fields": [
                {"label": "password", "type": "CONCEALED", "value": "required"},
                {"label": "notesPlain", "type": "STRING", "value": "must-not-be-copied"},
            ],
        }

        filtered = filter_item_fields(item, PASSWORD_FIELD)

        self.assertEqual(["password"], list(filtered["fields"]))
        self.assertNotIn("notesPlain", filtered["fields"])

    def test_sanitize_matches_build_config_key_format(self) -> None:
        self.assertEqual("BACKENDCONNECTION_STAGING_COMPAT", sanitize("BackendConnection staging-compat"))


if __name__ == "__main__":
    unittest.main()
