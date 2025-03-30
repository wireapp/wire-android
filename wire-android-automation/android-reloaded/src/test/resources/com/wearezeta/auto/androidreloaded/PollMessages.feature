Feature: PollMessages

  @TC-4472 @TC-4473 @regression @RC @polls
  Scenario Outline: I want to receive a poll message in a group conversation
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
    When User <Member1> sends poll message "<PollMessageText>" with title "<PollMessageTitle>" and buttons "<Button1>,<Button2>" to conversation <GroupConversation>
    Then I see the poll for "<PollMessageText>" is displayed in the conversation
    # TC-4473 - I want to be able to vote in a poll in a group conversation
    And I see the button "<Button1>" in the poll
    And I see the button "<Button2>" in the poll
    When I tap the button "<Button1>" in the poll
    And User <Member1> sends button action confirmation to user <TeamOwner> on the latest poll in conversation <GroupConversation> with button "<Button1>"
    Then I see the button "<Button1>" is selected

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | PollMessageTitle | PollMessageText              | Button1 | Button2 |
      | user1Name | Polling  | user2Name | Polls             | Question         | What is your favorite animal?| Cat     | Dog     |

  @TC-4474 @regression @RC @polls
  Scenario Outline: I want to change my vote after I voted already in a poll in a group conversation
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
    When User <Member1> sends poll message "<PollMessageText>" with title "<PollMessageTitle>" and buttons "<Button1>,<Button2>" to conversation <GroupConversation>
    Then I see the poll for "<PollMessageText>" is displayed in the conversation
    # TC-4473 - I want to be able to vote in a poll in a group conversation
    And I see the button "<Button1>" in the poll
    And I see the button "<Button2>" in the poll
    When I tap the button "<Button2>" in the poll
    And User <Member1> sends button action confirmation to user <TeamOwner> on the latest poll in conversation <GroupConversation> with button "<Button2>"
    Then I see the button "<Button2>" is selected
    When I tap the button "<Button1>" in the poll
    And User <Member1> sends button action confirmation to user <TeamOwner> on the latest poll in conversation <GroupConversation> with button "<Button1>"
    Then I see the button "<Button1>" is selected

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | PollMessageTitle | PollMessageText              | Button1 | Button2 |
      | user1Name | Polling  | user2Name | Polls             | Question         | What is your favorite animal?| Cat     | Dog     |