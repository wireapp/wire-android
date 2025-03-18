Feature: Backup

  @TC-4243 @regression @RC @backup
  Scenario Outline: I want to create a backup without password and import it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I see Backup Page
    And I tap on Create a Backup button
    And I tap on Back Up Now button
    And I wait until I see the message "Conversations successfully saved" in backup alert
    And I tap on Save File button in backup alert
    And I tap on Save button in DocumentsUI
    And I see Backup Page
    And I tap back button 2 times
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "Delete all your personal information and conversations on this device" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    And I see Welcome Page
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I do not see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I tap on Restore from Backup button
    And I tap on Choose Backup button
    And I select backup file with name containing "WBX-<UniqueUsername>" in DocumentsUI
    Then I wait until I see the message "Conversations have been restored" in backup alert
    And I tap OK button on the alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | UniqueUsername      | GroupConversation | Message | Message2           |
      | user1Name | Messaging | user2Name | user1UniqueUsername | BackingUp         | Hello!  | Hello to you, too! |

  @TC-4244 @regression @RC @backup
  Scenario Outline: I want to create a backup with password and import it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I see Backup Page
    And I tap on Create a Backup button
    And I type my password <Password> to create my backup
    And I tap on Back Up Now button
    And I wait until I see the message "Conversations successfully saved" in backup alert
    And I tap on Save File button in backup alert
    And I tap on Save button in DocumentsUI
    And I see Backup Page
    And I tap back button 2 times
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "Delete all your personal information and conversations on this device" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    And I see Welcome Page
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I do not see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I tap on Restore from Backup button
    And I tap on Choose Backup button
    And I select backup file with name containing "WBX-<UniqueUsername>" in DocumentsUI
    And I type my password <Password> to restore my backup
    And I tap continue button on restore backup page
    Then I wait until I see the message "Conversations have been restored" in backup alert
    And I tap OK button on the alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | UniqueUsername      | Password     | GroupConversation | Message | Message2           |
      | user1Name | Messaging | user2Name | user1UniqueUsername | Qwertz12345! | BackingUp         | Hello!  | Hello to you, too! |

  @TC-4245 @regression @RC @backup
  Scenario Outline: I want to import a backup that I created on a previous version
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I see Backup Page
    And I tap on Create a Backup button
    And I type my password <Password> to create my backup
    And I tap on Back Up Now button
    And I wait until I see the message "Conversations successfully saved" in backup alert
    And I tap on Save File button in backup alert
    And I tap on Save button in DocumentsUI
    And I see Backup Page
    And I tap back button 2 times
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "Delete all your personal information and conversations on this device" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    When I upgrade Wire to the recent version
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
    And I see group conversation "<GroupConversation>" is in foreground
    And I do not see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I tap on Restore from Backup button
    And I tap on Choose Backup button
    And I select backup file with name containing "WBX-<UniqueUsername>" in DocumentsUI
    And I type my password <Password> to restore my backup
    And I tap continue button on restore backup page
    Then I wait until I see the message "Conversations have been restored" in backup alert
    And I tap OK button on the alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | UniqueUsername      | Password     | GroupConversation | Message | Message2           |
      | user1Name | Messaging | user2Name | user1UniqueUsername | Qwertz12345! | BackingUp         | Hello!  | Hello to you, too! |