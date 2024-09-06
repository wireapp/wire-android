# 4. Deeplink handling refactor

Date: 2024-09-06

## Status

Accepted

## Context

The existing implementation of deeplink handling within our ViewModel is characterized by complex,
bulky logic that makes maintenance and scalability challenging. Current handling involves redundant
checks for each deeplink type and duplicated error logging. Additionally, the logic to handle toast
errors for deeplinks requiring authorization and the management of user session state during calls
(to prevent account switching) are not centralized, leading to repeated code and scattered handling
across components.

## Decision

We have decided to refactor the existing handleDeepLink function in the ViewModel by consolidating
the URI deeplink processing logic into a dedicated class, DeepLinkProcessor. This class will now
centralize the interpretation and validation of deeplinks, and manage user session states more
effectively, including checks for ongoing calls before allowing account switches. The
DeepLinkProcessor will handle:

- Duplicate checks and error logging by centralizing deeplink type verification.
- Displaying toast errors when deeplink operations require user authorization and preventing
  operations during calls.
- Secure and condition-based account switching.

## Consequences

1. Reduced Duplication: Centralizing deeplink handling reduces duplicated logic across the
   application, especially for checking deeplink types and logging errors, ensuring a cleaner
   codebase.

2. Centralized Authorization Management: Toast messages to inform users about the need for
   authorization if they attempt to access resources that require authentication are managed
   centrally, enhancing user experience and security.

3. Enhanced Call State Management: The new implementation prevents account switching during active
   calls by checking the user's call status directly within DeepLinkProcessor. This is vital for
   maintaining session integrity and user experience.

4. Improved Error Handling: By centralizing error handling and response mechanisms, the application
   can more effectively manage and respond to deeplink processing errors, providing a more robust
   and fault-tolerant system.

5. Streamlined Testing and Maintenance: With deeplink logic isolated in a single class, testing
   becomes more focused and efficient, allowing for targeted testing strategies and easier
   maintenance.

This decision involves careful re-engineering of the deeplink processing mechanisms to ensure
compatibility and functionality across all current and potential future deeplink scenarios,
requiring comprehensive testing and validation to ensure seamless integration.
