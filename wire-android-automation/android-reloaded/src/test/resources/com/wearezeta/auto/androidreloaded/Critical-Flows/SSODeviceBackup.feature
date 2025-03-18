Feature: SSO Device Backup

  @TC-8604 @CriticalFlows
  Scenario Outline: Setting Up a New Device with Backup Restoration via SSO
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    When I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I submit my Username <UniqueUsername> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I tap on start a new conversation button
    When I tap on search people field
    And I type conversation name "<TeamOwner>" in search field
    Then I see conversation name "<TeamOwner>" in Search result list
    When I tap on user name "<TeamOwner>" in Search result list
    When I see start conversation button on connected user profile page
    And I tap start conversation button on connected user profile page
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see a message is displayed in the conversation view
    And I close the conversation view through the back arrow
    When I close the user profile through the close button
    And I tap the back arrow inside the search people field
    And I click close button on New Conversation screen to go back conversation details
    Then I see conversation list
    And I tap on menu button on conversation list
    When I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I see Backup Page Heading
    And I tap on Create a Backup button
    And I tap on Back Up Now button
    And I wait until I see the message "Conversations successfully saved" in backup alert
    And I tap on Save File button in backup alert
    And I tap on Save button in DocumentsUI
    Then I see Backup Page
    When I tap back button
    And I open the main navigation menu
    And I tap on conversations menu entry
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "Delete all your personal information and conversations on this device" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    Then I see Welcome Page
    When I tap login button on Welcome Page
    And I tap on SSO login tab
    And I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    Then I do not see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I tap on Restore from Backup button
    And I tap on Choose Backup button
    And I select backup file with name containing "WBX-<UniqueUsername>" in DocumentsUI
    Then I wait until I see the message "Conversations have been restored" in backup alert
    And I tap OK button on the alert
    And I see conversation list
    When I see conversation "<TeamOwner>" in conversation list
    Then I tap on conversation name "<TeamOwner>" in conversation list
    And I see conversation view with "<TeamOwner>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | OktaMember1| UniqueUsername        | TeamName   | Message                              |
      | user1Name | SSO       | user2Name  | user2UniqueUsername   | Messaging  | Testing of the backup functionality  |
