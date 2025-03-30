Feature: E2EE Verification

  @TC-8115 @TC-8118 @regression @RC @E2EEVerification
  Scenario Outline: I want to verify another users devices and see a 1on1 conversation as verified
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> adds a new device Device2 with label Device2
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
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
    Then I see that the conversation is verified
    And I see system message "All fingerprints are verified (Proteus)" in conversation view
    When I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" in current conversation
    # TC-8118 - I should see degradation alert when I try to send a message in a degraded 1on1 conversation
    When User <Member1> adds a new device Device3 with label Device3
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I close the user profile through the close button
    Then I see system message "This conversation is no longer verified, as at least one participant started using a new device or has an invalid certificate." in conversation view
    And I see that the conversation is not verified
    When I type the message "<Message3>" into text input field
    And I tap send button
    And I see conversation no longer verified alert
    And I tap cancel button on degradation alert
    Then I do not see the message "<Message3>" in current conversation
    When I type the message "<Message3>" into text input field
    And I tap send button
    And I see conversation no longer verified alert
    And I tap send anyway button on degradation alert
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName            | Message | Message2            | Message3                           |
      | user1Name | user2Name | ProteusVerification | Hello!  | Verfication worked! | New device, conversation degraded! |

  @TC-8116 @TC-8117 @TC-8119 @regression @RC @E2EEVerification
  Scenario Outline: I want to verify other users devices and see a group conversation as verified
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> adds a new device Device2 with label Device2
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
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
    # TC-8117 - I should see degradation alert when I try to send a message in a degraded group conversation
    When User <Member1> adds a new device Device3 with label Device3
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
    # TC-8119 - I should see degradation alert when I try to send a file to a degraded group conversation
    When I push 1KB sized file with name "textfile.txt" to file storage
    And I tap file sharing button
    And I tap on Attach File option
    And I select file with name containing "textfile.txt" in DocumentsUI
    And I see file "textfile.txt" on preview page
    And I tap send button on preview page
    And I see conversation no longer verified alert
    And I tap send anyway button on degradation alert
    And I tap file sharing button
    And I scroll to the bottom of conversation view
    Then I see a file with name "textfile.txt" in the conversation view

    Examples:
      | TeamOwner | Member1   | TeamName            | GroupConversation | Message | Message2            | Message3                           |
      | user1Name | user2Name | ProteusVerification | Verification      | Hello!  | Verfication worked! | New device, conversation degraded! |
