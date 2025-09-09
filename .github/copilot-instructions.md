# Project stack context
We use Gradle as our build system, with toml for configuration files.
We use Android, kotlin for our android app development.
We have a dependency on a git submodule called "Kalium"
In Kalium, we use kotlin multiplatform to share code between android and other platforms.

# Instructions 
When a change is made to the Kalium git submodule reference, ask for a linked PR in Kalium repo or some description of the changes made in Kalium.
Our team uses Jira for tracking items of work, verify that the PR title contains a Jira ticket reference.
Focus on code quality, performance, security, and best practices.
Ensure the code adheres to idiomatic Kotlin and Android development practices.
Ensure the code adheres to idiomatic Gradle and toml configuration practices.
Ensure the code adheres to idiomatic Kotlin Multiplatform practices.
Ensure that changes have appropriate test coverage.
If the PR includes UI changes to Android Composables ensure that accessibility best practices are followed, like content descriptions for images, proper contrast ratios, and support for screen readers.
If the PR includes changes to Android Manifest or Gradle configuration files, ensure that they follow best practices for security.
