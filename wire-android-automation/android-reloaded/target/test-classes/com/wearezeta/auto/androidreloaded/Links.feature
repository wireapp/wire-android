Feature: Links

  @TC-4433 @regression @RC @links
  Scenario Outline: I want to be forwarded to my browser when receiving a link
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
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
    When User <Member1> sends message "<Link>" via device Device1 to group conversation <GroupConversation>
    And I see the message "<Link>" in current conversation
    And I tap on the link "<Link>" in current conversation
    Then I see an alert informing me that I will be forwarded to "<Link>" in my browser
    When I tap open button on the link alert
    Then I see the Wire app is not in foreground
    And I see webpage with "<URL>" is in foreground

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Link           | URL    |
      | user1Name | Linking  | user2Name | WeLikeLinks       | www.github.com | github |

  @TC-4434 @regression @RC @links @WPB-3518
  Scenario Outline: I want to be forwarded to my browser when receiving a link after I scrolled through the conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
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
    When User <Member1> sends message "<Link>" via device Device1 to group conversation <GroupConversation>
    And I see the message "<Link>" in current conversation
    And I tap on the link "<Link>" in current conversation
    Then I see an alert informing me that I will be forwarded to "<Link>" in my browser
    When I tap open button on the link alert
    And I see the Wire app is not in foreground
    And I restart Wire
    And I see the Wire app is in foreground
    And User <Member1> sends 20 default message to conversation <GroupConversation>
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" via device Device1 to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I scroll to the top of conversation view
    And I scroll to the bottom of conversation view
    And I scroll to the top of conversation view
    And I tap on the link "<Link>" in current conversation
    Then I see an alert informing me that I will be forwarded to "<Link>" in my browser
    When I tap open button on the link alert
    Then I see the Wire app is not in foreground
    And I see webpage with "<URL>" is in foreground

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Link           | Message                   | Message2 | URL    |
      | user1Name | Linking  | user2Name | WeLikeLinks       | www.github.com | That is a lot of messages | Yes!     | github |