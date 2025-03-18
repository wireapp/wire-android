Feature: Mentions

  @TC-4448 @TC-4449 @regression @RC @mentions
  Scenario Outline: I want to be able to mention a team user in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    When I type the mention "@<Member2FirstName>" into text input field
    And I see user "<Member1>" in mention list
    And I select user "<Member1>" from mention list
    And I tap send button
    Then I see the last mention is "@<Member1>" in current conversation
    And I tap back button 2 times
    And I see conversation list
    # TC-4449 - I want to be able to receive mentions from team users in a group conversation
    When User <Member2> sends mention "@<TeamOwner>" to group conversation <GroupConversation>
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    Then I see the last mention is "@<TeamOwner>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2FirstName | Member2   | TeamName     | GroupConversation |
      | user1Name | user2Name | user2FirstName   | user3Name | Notification | MyTeam            |