Feature: EditMessage

  @TC-4330 @TC-4331 @TC-4332 @TC-4333 @regression @RC @editMessage @smoke
  Scenario Outline: I want to edit a sent message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> remembers the recent message from user Myself via device Device1
    When I long tap on the message "<Message>" in current conversation
    And I tap on edit option
    And I edit my message to "<Message2>"
    And I tap send button for my edit message
    And I hide the keyboard
    Then I see the message "<Message2>" in current conversation
    And I do not see the message "<Message>" in current conversation
    # TC-4331 - I want to see edited label with timestamp when I edited a message in a 1on1 conversation
    And I see the edited label for the message "<Message2>"
    And I see the time and date when the message "<Message2>" was edited
    And User <Member1> sees the recent message from user Myself via device Device1 is changed in 15 seconds
    # TC-4332 - I want to receive an edited message in a 1on1 conversation
    When User <Member1> sends message "<Message3>" to User Myself
    Then I see the message "<Message3>" in current conversation
    When User <Member1> edits the recent message to "<Message4>" from user Myself via device Device1
    Then I see the message "<Message4>" in current conversation
    And I do not see the message "<Message3>" in current conversation
    # TC-4333 - I want to see edited label with timestamp when I received an edited message in a 1on1 conversation
    And I see the edited label for the message "<Message4>"
    And I see the time and date when the message "<Message4>" was edited

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Message2      | Message3 | Message4      |
      | user1Name | Messaging | user2Name | Hello!  | Good Morning! | Hi!      | Good Evening! |

  @TC-4334 @TC-4335 @TC-4336 @TC-4337 @regression @RC @editMessage @smoke
  Scenario Outline: I want to edit a sent message in a group conversation
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
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> remembers the recent message from group conversation <GroupConversation> via device Device1
    When I long tap on the message "<Message>" in current conversation
    And I tap on edit option
    And I edit my message to "<Message2>"
    And I tap send button for my edit message
    And I hide the keyboard
    Then I see the message "<Message2>" in current conversation
    And I do not see the message "<Message>" in current conversation
    # TC-4335 - I want to see edited label with timestamp when I edited a message in a group conversation
    And I see the edited label for the message "<Message2>"
    And I see the time and date when the message "<Message2>" was edited
    And User <Member1> sees the recent message from group conversation <GroupConversation> via device Device1 is changed in 15 seconds
    # TC-4336 - I want to receive an edited message in a group conversation
    When User <Member1> sends message "<Message3>" to group conversation <GroupConversation>
    Then I see the message "<Message3>" in current conversation
    When User <Member1> edits the recent message to "<Message4>" from group conversation <GroupConversation> via device Device1
    Then I see the message "<Message4>" in current conversation
    And I do not see the message "<Message3>" in current conversation
    # TC-4337 - I want to see edited label with timestamp when I received an edited message in a group conversation
    And I see the edited label for the message "<Message4>"
    And I see the time and date when the message "<Message4>" was edited

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message | Message2      | Message3 | Message4      |
      | user1Name | Messaging | user2Name | EditMe            | Hello!  | Good Morning! | Hi!      | Good Evening! |

  @TC-4338 @regression @RC @editMessage @WPB-3525
  Scenario Outline: I want the edit option to be still visible after I scrolled through a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <Member1> sends 20 default message to conversation <GroupConversation>
    And I type the message "<Message2>" into text input field
    And I tap send button
    And I see the message "<Message2>" in current conversation
    And I scroll to the top of conversation view
    And I scroll to the bottom of conversation view
    And I long tap on the message "<Message2>" in current conversation
    Then I see edit option

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message | Message2                  |
      | user1Name | Messaging | user2Name | EditMe            | Hello!  | That is a lot of messages |

  @TC-8151 @regression @RC @editMessage @WPB-10773
  Scenario Outline: I want the receive all the edited messages, if user edits their message more than once
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
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    When User <Member1> edits the recent message to "<Message2>" from group conversation <GroupConversation> via device Device1
    And I see the message "<Message2>" in current conversation
    And User <Member1> edits the recent message to "<Message3>" from group conversation <GroupConversation> via device Device1
    And I see the message "<Message3>" in current conversation
    And User <Member1> edits the recent message to "<Message4>" from group conversation <GroupConversation> via device Device1
    And I see the message "<Message4>" in current conversation
    And User <Member1> edits the recent message to "<Message5>" from group conversation <GroupConversation> via device Device1
    Then I see the message "<Message5>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message | Message2     | Message3 | Message4      | Message5      |
      | user1Name | Messaging | user2Name | EditMe            | Hello!  | Good Morning | Good Day | Good Evening! | Good Night!   |