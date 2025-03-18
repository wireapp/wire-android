Feature: Ephemeral message

  @C399840 @regression
  Scenario Outline: Verify sending all types of messages after I enable ephemeral mode
    Given I am on Android with Google Location Service
    Given There is 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    Given I tap Ephemeral button from cursor toolbar
    Given I set timeout to <EphemeralTimeout> on Extended cursor ephemeral overlay
    Given I tap on text input
    # Video
    And I tap Video message button from cursor toolbar
    And I scroll to the bottom of conversation view
    Then I see Video Message container in the conversation view
    When I wait for <EphemeralTimeout>
    Then I see Video Message Placeholder container in the conversation view
    # Picture
    When I tap File button from cursor toolbar
    And I see file chooser popup in the testing gallery
    And I tap Image button on file chooser popup in the testing gallery
    And I scroll to the bottom of conversation view
    Then I see a picture in the conversation view
    When I wait for <EphemeralTimeout>
    Then I see Image Placeholder container in the conversation view
    # Audio message
    When I long tap Audio message button 2 seconds from cursor toolbar
    And I tap audio recording Send button
    And I scroll to the bottom of conversation view
    Then I see Audio Message container in the conversation view
    When I wait for <EphemeralTimeout>
    Then I see Audio Message Placeholder container in the conversation view
    # Ping
    When I tap Ping button from cursor toolbar
    And I scroll to the bottom of conversation view
    Then I see Ping message "<PingMsg>" in the conversation view
    When I wait for <EphemeralTimeout>
    Then I do not see Ping message "YOU PINGED" in the conversation view
    # File
    And I tap File button from cursor toolbar
    And I see file chooser popup in the testing gallery
    And I tap Textfile button on file chooser popup in the testing gallery
    And I scroll to the bottom of conversation view
    Then I see File Upload container in the conversation view
    When I wait for <EphemeralTimeout>
    Then I see File Upload Placeholder container in the conversation view
    # Location
    When I tap Share location button from cursor toolbar
    And I tap OK Button on Share Location popup
    And I tap Send button on Share Location page
    And I scroll to the bottom of conversation view
    Then I see Share Location container in the conversation view
    When I wait for <EphemeralTimeout>
    Then I see Share Location Placeholder container in the conversation view
    # Link Preview
    When I type the message "<Link>" and send it by cursor Send button
    And I scroll to the bottom of conversation view
    Then I see Link Preview container in the conversation view
    And I do not see Message status with expected text "<MessageStatus>" in conversation view
    When I wait for <EphemeralTimeout>
    Then I see Link Preview Placeholder container in the conversation view

    Examples:
      | Name      | Contact   | EphemeralTimeout | Link               | MessageStatus | PingMsg    | FileSize |
      | user1Name | user2Name | 10 seconds       | https://github.com | Sending       | YOU PINGED | 1.00MB   |
