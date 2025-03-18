Feature: Account Management

  @TC-8610 @CriticalFlows
  Scenario Outline: As a member, I want to enable logging, applock, change my email and reset my password
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the debug menu
    And I tap the logging toggle
    Then I see enable logging toggle is off
    When I tap the logging toggle
    Then I see enable logging toggle is on
    When I tap back button
    And I see lock with passcode toggle is turned off
    And I tap on lock with passcode toggle
    And I see set up app lock page
    And I see description text that app will lock after 1 minute of inactivity
    And I enter my passcode "<Passcode>" for app lock
    And I tap set passcode button
    Then I see lock with passcode toggle is turned on
    When I open my account details menu
    And I see my email address "<Member1Email>" is displayed
    And I see my domain "<domain>" is displayed
    And I tap my email address "<Member1Email>" that is displayed
    And I start activation email monitoring on mailbox <NewEmail>
    And I change email address to <NewEmail> on Settings page
    And I tap save button on email change view
    Then I see the notification about email change containing the "<NewEmail>" is displayed
    And I verify email address <NewEmail> for Myself
    When I tap back button
    Then I see my email address "<NewEmail>" is displayed
    And I verify user's Myself email on the backend is equal to <NewEmail>
    And I see reset password button
    When I tap reset password button
    And I see the Wire app is not in foreground
    Then I see webpage with "<URL>" is in foreground

    Examples:
      | TeamOwner | TeamName  | Member1   | Passcode     | Member1Email | NewEmail   | GroupConversation | domain            | URL                            |
      | user1Name | Messaging | user2Name | Qwertz12345! | user2Email   | user3Email | MyTeam            | staging.zinfra.io | wire-account-staging.zinfra.io |