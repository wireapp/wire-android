import os
import subprocess
import tempfile
import unittest

import classify


def diff(path, *lines):
    body = "\n".join(lines)
    return f"diff --git a/{path} b/{path}\n--- a/{path}\n+++ b/{path}\n@@ -1 +1 @@\n{body}\n"


def tier(text, author=""):
    return classify.classify(classify.parse_diff(text), author)


class ClassifyTest(unittest.TestCase):
    def test_bot_author_is_skipped(self):
        self.assertEqual(0, tier(diff("app/A.kt", "+x"), "renovate[bot]")["tier"])

    def test_docs_only_is_skipped(self):
        text = diff("docs/a.md", "+x") + diff("app/src/main/res/values-de/strings.xml", "+x")
        self.assertEqual(0, tier(text)["tier"])

    def test_tests_only_is_skipped(self):
        self.assertEqual(0, tier(diff("app/src/test/kotlin/ATest.kt", "+x"))["tier"])

    def test_generated_profiles_are_skipped(self):
        self.assertEqual(0, tier(diff("app/src/main/baseline-prof.txt", "+x"))["tier"])

    def test_kalium_pointer_is_deep(self):
        text = diff("kalium", "-Subproject commit a", "+Subproject commit b")
        self.assertEqual(2, tier(text)["tier"])

    def test_di_path_is_deep(self):
        text = diff("app/src/main/kotlin/com/wire/android/di/AppModule.kt", "+x")
        self.assertEqual(2, tier(text)["tier"])

    def test_test_file_under_navigation_is_not_a_trigger(self):
        text = diff("app/src/test/kotlin/com/wire/android/navigation/NavTest.kt", "+x") + diff(
            "app/src/main/kotlin/com/wire/android/ui/A.kt", "+x"
        )
        self.assertEqual(1, tier(text)["tier"])

    def test_removed_public_declaration_in_shared_module_is_deep(self):
        text = diff("core/ui-common/src/main/kotlin/A.kt", "-fun greet(name: String)", "+fun greet()")
        self.assertEqual(2, tier(text)["tier"])

    def test_override_and_private_are_not_surface(self):
        text = diff(
            "core/ui-common/src/main/kotlin/A.kt",
            "-    override fun run()",
            "-    private fun helper()",
            "+    internal fun other()",
        ) + diff("core/ui-common/src/test/kotlin/ATest.kt", "+x")
        self.assertEqual(1, tier(text)["tier"])

    def test_declaration_in_app_module_is_not_surface(self):
        text = diff("app/src/main/kotlin/A.kt", "-fun greet()", "+fun hello()") + diff(
            "app/src/test/kotlin/ATest.kt", "+x"
        )
        self.assertEqual(1, tier(text)["tier"])

    def test_changed_local_variable_is_not_surface(self):
        text = diff("features/cells/src/main/kotlin/A.kt", "-        val x = 1", "+        val x = 2") + diff(
            "features/cells/src/test/kotlin/ATest.kt", "+x"
        )
        self.assertEqual(1, tier(text)["tier"])

    def test_untyped_local_of_top_level_function_is_not_surface(self):
        text = diff("features/cells/src/main/kotlin/A.kt", "-    val x = 1", "+    val x = 2") + diff(
            "features/cells/src/test/kotlin/ATest.kt", "+x"
        )
        self.assertEqual(1, tier(text)["tier"])

    def test_typed_member_property_is_surface(self):
        text = diff("features/cells/src/main/kotlin/A.kt", "-    val x: Int = 1", "+    val x: Long = 1")
        self.assertEqual(2, tier(text)["tier"])

    def test_modifiers_and_annotations_are_surface(self):
        for line in (
            "-expect fun platform(): String",
            "-actual class Platform",
            "-const val LIMIT: Int = 3",
            "-operator fun plus(other: A): A",
            "-annotation class Marker",
            "-@Composable fun Screen()",
        ):
            text = diff("core/ui-common/src/main/kotlin/A.kt", line)
            self.assertEqual(2, tier(text)["tier"], line)

    def test_workflow_change_is_deep(self):
        self.assertEqual(2, tier(diff(".github/workflows/a.yml", "+x"))["tier"])

    def test_review_prompt_change_is_deep(self):
        self.assertEqual(2, tier(diff(".github/review/deep.md", "+x"))["tier"])

    def test_removed_line_starting_with_dashes_is_counted(self):
        files = classify.parse_diff(diff("docs/a.md", "--- rule"))
        self.assertEqual(1, files["docs/a.md"]["removed"])

    def test_small_change_with_tests_is_light(self):
        text = diff("app/src/main/kotlin/A.kt", "+x") + diff("app/src/test/kotlin/ATest.kt", "+x")
        self.assertEqual(1, tier(text)["tier"])

    def test_missing_tests_plus_build_file_is_deep(self):
        text = diff("app/src/main/kotlin/A.kt", "+x") + diff("gradle/libs.versions.toml", "+x")
        self.assertEqual(2, tier(text)["tier"])

    def test_empty_diff_is_skipped(self):
        self.assertEqual(0, tier("")["tier"])


class KaliumBumpTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = self.tmp.name
        self.git("init", "-q")
        self.write("logic/api/android/logic.api", "public final class A {\n}\n")
        self.write("data/src/main/kotlin/Id.kt", "class Id\n")
        self.base = self.commit("base")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        out = subprocess.run(
            ["git", "-c", "user.name=t", "-c", "user.email=t@t", "-C", self.repo, *args],
            capture_output=True, text=True, check=True,
        )
        return out.stdout.strip()

    def write(self, path, content):
        full = os.path.join(self.repo, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "w") as handle:
            handle.write(content)

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-q", "-m", message)
        return self.git("rev-parse", "HEAD")

    def bump(self, new, author="dev"):
        pointer = diff("kalium", f"-Subproject commit {self.base}", f"+Subproject commit {new}")
        pointer = pointer.replace("@@ -1 +1 @@", f"@@ -1 +1 @@").replace(
            "--- a/kalium", f"index {self.base[:11]}..{new[:11]} 160000\n--- a/kalium"
        )
        old, newer = classify.kalium_range(pointer)
        info = classify.inspect_kalium(self.repo, old, newer)
        return classify.classify(classify.parse_diff(pointer), author, info)

    def test_additive_dump_change_is_light(self):
        self.write("logic/api/android/logic.api", "public final class A {\n}\npublic final class B {\n}\n")
        self.assertEqual(1, self.bump(self.commit("add"))["tier"])

    def test_removed_dump_line_is_deep(self):
        self.write("logic/api/android/logic.api", "public final class C {\n}\n")
        result = self.bump(self.commit("break"))
        self.assertEqual(2, result["tier"])
        self.assertIn("ABI dump", result["reasons"][0])

    def test_changed_changelog_fragment_is_deep(self):
        self.write("changelog.d/1-x.changed.md", "Changed.\n")
        self.assertEqual(2, self.bump(self.commit("changed"))["tier"])

    def test_fixed_changelog_fragment_is_light(self):
        self.write("changelog.d/1-x.fixed.md", "Fixed.\n")
        self.assertEqual(1, self.bump(self.commit("fixed"))["tier"])

    def test_persistence_change_is_deep(self):
        self.write("data/persistence/src/main/sqldelight/migrations/5.sqm", "ALTER TABLE a;\n")
        self.assertEqual(2, self.bump(self.commit("migration"))["tier"])

    def test_removed_public_declaration_outside_logic_is_deep(self):
        self.write("data/src/main/kotlin/Id.kt", "class Other\n")
        self.assertEqual(2, self.bump(self.commit("rename"))["tier"])

    def test_private_change_outside_logic_is_light(self):
        self.write("data/src/main/kotlin/Id.kt", "class Id\nprivate fun helper()\n")
        self.assertEqual(1, self.bump(self.commit("private"))["tier"])

    def test_unknown_commit_is_deep(self):
        result = self.bump("f" * 40)
        self.assertEqual(2, result["tier"])
        self.assertIn("cannot be inspected", result["reasons"][0])

    def test_non_hex_commit_is_not_passed_to_git(self):
        self.assertEqual({"unavailable": True}, classify.inspect_kalium(self.repo, "--output=x", "abcdef1"))

    def test_without_a_checkout_every_bump_is_deep(self):
        text = diff("kalium", "-Subproject commit a", "+Subproject commit b")
        self.assertEqual(2, tier(text)["tier"])


if __name__ == "__main__":
    unittest.main()
