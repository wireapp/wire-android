Feature: Upgrade

  @TC-8607 @CriticalFlows
  Scenario Outline: I want to be able to update from previous wire version to the new wire version without losing my history as a team user
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation
    When I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message2>" in current conversation
    When I tap the back arrow to go back to conversation list
    And User <Member2> sends message "<Message>" to User <Member1>
    And I wait until the notification popup disappears
    Then I see conversation "<Member2>" is having 1 unread messages in conversation list
    When I minimise Wire
    And I upgrade Wire to the recent version
    And I wait until I am fully logged in
    Then I see conversation "<Member2>" is having 1 unread messages in conversation list
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message2>" in current conversation
    And I see the message "<Message>" in current conversation
    When I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message | Message2       | Message3                 |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  | Hello as well! | Migration was a success! |
