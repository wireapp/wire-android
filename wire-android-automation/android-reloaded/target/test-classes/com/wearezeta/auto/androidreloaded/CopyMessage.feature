Feature: CopyMessage

  @TC-4315 @regression @RC @copyMessage
  Scenario Outline: I want to be able to copy a message
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
    And User <Member1> sends message "<Message>" via device Device1 to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I tap on copy option
    And I see "Message copied" toast message in conversation view
    Then I verify that Android clipboard content equals to "<Message>"
    When I tap on the text input field
    And I paste the copied text into the text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message   |
      | user1Name | CopyCats | user2Name | CopyMe            | Good day! |

  @TC-4314 @regression @RC @copyMessage @WPB-3525
  Scenario Outline: I want to see that I copied to the correct message after I scrolled through the conversation
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
    And User <Member1> sends 20 default message to conversation <GroupConversation>
    And User <Member1> sends message "<Message>" via device Device1 to group conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see the message "<Message>" in current conversation
    And I scroll to the top of conversation view
    And I scroll to the bottom of conversation view
    When I long tap on the message "<Message>" in current conversation
    And I tap on copy option
    And I see "Message copied" toast message in conversation view
    Then I verify that Android clipboard content equals to "<Message>"
    When I tap on the text input field
    And I paste the copied text into the text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                   |
      | user1Name | CopyCats | user2Name | CopyMe            | That is a lot of messages |