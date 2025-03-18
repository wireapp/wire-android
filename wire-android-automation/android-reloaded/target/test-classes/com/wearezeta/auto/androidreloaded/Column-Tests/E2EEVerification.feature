Feature: E2EE Verification

  @TC-8131 @col1 @col3
  Scenario Outline: I want to see conversation degrades with warning when sending files to unverified devices
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member1> adds a new 2FA device Device2 with label Device2
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    # To receive Device Key Fingerprint for verifying devices, we need to send a message from a device first
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open manage your devices menu
    And I tap on my device "Device1" listed under devices
    And I see the device is not verified
    When I tap on verify device button
    Then I see the device is verified
    And I close the devices details screen through the back arrow
    And I close the devices screen through the back arrow
    And I open the main navigation menu
    And I tap on conversations menu entry
    And I see conversation list
    When I tap on conversation name "<Member1>" in conversation list
    And I see conversation view with "<Member1>" is in foreground
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I see connected user <Member1> profile
    And I tap on devices tab in connected user profile
    And I see "Desktop" is listed under devices in connected user profile
    And I tap device "Desktop" listed under devices in connected user profile
    And I see the device is not verified
    When I tap on verify device button
    Then I see the device is verified
    And I see the verified shield for other users device
    And I close the devices details screen through the back arrow
    When I close the user profile through the close button
    And I close the conversation view through the back arrow
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see that the conversation is verified
    And I see system message "All fingerprints are verified (Proteus)" in conversation view
    When I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    When User <Member1> adds a new 2FA device Device3 with label Device3
    And I tap on conversation name "<Member1>" in conversation list
    And I see conversation view with "<Member1>" is in foreground
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I see connected user <Member1> profile
    And I close the user profile through the close button
    And I close the conversation view through the back arrow
    And I tap on conversation name "<GroupConversation>" in conversation list
    Then I see system message "This conversation is no longer verified, as at least one participant started using a new device or has an invalid certificate." in conversation view
    When I type the message "<Message3>" into text input field
    And I tap send button
    And I see conversation no longer verified alert
    And I tap cancel button on degradation alert
    Then I do not see the message "<Message3>" in current conversation
    When I push 1KB sized file with name "textfile.txt" to file storage
    And I tap file sharing button
    And I tap on Attach File option
    And I select file with name containing "textfile.txt" in DocumentsUI
    And I see conversation no longer verified alert
    And I tap send anyway button on degradation alert
    And I tap file sharing button
    And I scroll to the bottom of conversation view
    Then I see a file with name "textfile.txt" in the conversation view

    Examples:
      | TeamOwner | Email      |  Member1   | TeamName            | GroupConversation | Message | Message2            | Message3                           |
      | user1Name | user1Email | user2Name | ProteusVerification | Verification      | Hello!  | Verfication worked! | New device, conversation degraded! |
