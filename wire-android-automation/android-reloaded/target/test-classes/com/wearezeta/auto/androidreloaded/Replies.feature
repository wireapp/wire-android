Feature: Replies

  @TC-4495 @regression @RC @replies
  Scenario Outline: I want to reply to a message in a group conversation
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    When User <TeamOwner> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And I long tap on the message "<Message>" in current conversation
    Then I see reply option
    When I tap reply option
    Then I see the message "<Message>" as preview in message input field
    When I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message2>" as a reply to message "<Message>" in conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | GroupConversation | Message                      | Message2 |
      | user1Name | user2Name | Reply    | Replies           | What is your favorite pizza? | Tuna!    |

  @TC-4496 @TC-4497 @regression @RC @replies
  Scenario Outline: I want to see a reply to my own message in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device <Device> with label <Device>
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" as reply to last message of conversation <GroupConversation> via device <Device>
    Then I see the message "<Message2>" as a reply to message "<Message>" in conversation view
    # TC-4497 - I want to reply to a reply in a group conversation
    When I long tap on the message "<Message2>" in current conversation
    And I tap reply option
    And I see the message "<Message2>" as preview in message input field
    And I type the message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message3>" as a reply to message "<Message2>" in conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | GroupConversation | Message                      | Message2 | Message3 | Device  |
      | user1Name | user2Name | Reply    | Replies           | What is your favorite pizza? | Hawaii!  | Wow!     | Device1 |

  @TC-4498 @regression @replies @RC
  Scenario Outline: I want to reply to a message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When User <TeamOwner> sends message "<Message>" to User <Member1>
    And I see the message "<Message>" in current conversation
    And I long tap on the message "<Message>" in current conversation
    Then I see reply option
    When I tap reply option
    Then I see the message "<Message>" as preview in message input field
    When I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message2>" as a reply to message "<Message>" in conversation view

    Examples:
      | TeamOwner | TeamName | Member1   | Message | Message2 |
      | user1Name | Reply    | user2Name | Hello   | Bye      |

  @TC-4499 @regression @replies @RC
  Scenario Outline: I want to see a reply to my own message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device <Device> with label <Device>
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" as reply to last message of conversation <Member1> via device <Device>
    Then I see the message "<Message2>" as a reply to message "<Message>" in conversation view

    Examples:
      | TeamOwner | TeamName | Member1   | Message        | Message2 | Device  |
      | user1Name | Reply    | user2Name | How you doing? | Fine     | Device1 |

  @TC-4500 @regression @replies @RC
  Scenario Outline: I want to reply to a reply in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device <Device> with label <Device>
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" as reply to last message of conversation <Member1> via device <Device>
    When I long tap on the message "<Message2>" in current conversation
    And I tap reply option
    And I see the message "<Message2>" as preview in message input field
    And I type the message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message3>" as a reply to message "<Message2>" in conversation view

    Examples:
      | TeamOwner | TeamName | Member1   | Message       | Message2 | Message3  | Device  |
      | user1Name | Reply    | user2Name | VacationPlans | London   | Vancouver | Device1 |

  @TC-4501 @regression @RC @replies @WPB-3525
  Scenario Outline: I want to see that I replied to the correct message after I scrolled through the conversation
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
    And I see the message "<Message>" in current conversation
    And I scroll to the top of conversation view
    And I scroll to the bottom of conversation view
    When I long tap on the message "<Message>" in current conversation
    And I tap reply option
    And I see the message "<Message>" as preview in message input field
    And I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" as a reply to message "<Message>" in conversation view

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                   | Message2 |
      | user1Name | Reply    | user2Name | Replies           | That is a lot of messages | Yes!     |