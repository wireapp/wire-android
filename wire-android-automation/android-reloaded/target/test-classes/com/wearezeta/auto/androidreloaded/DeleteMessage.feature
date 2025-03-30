Feature: Delete Message

  @TC-4316 @regression @RC @conversationView @deleteMessage
  Scenario Outline: I want to be able to delete a message in a 1:1 conversation for everyone
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds a new device <ContactDevice> with label <ContactDevice>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And User <Member1> sends message "<Message>" via device <ContactDevice> to User Myself
    And I wait until the notification popup disappears
    And I tap on unread conversation name "<Member1>" in conversation list
    And I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message2>" in current conversation
    And User <Member1> sees message "<Message2>" in conversation <TeamOwner> via device <ContactDevice>
    When I long tap on the message "<Message2>" in current conversation
    And I tap delete button
    And I see delete options
    And I tap delete for everyone button
    Then I see deleted label
    And I do not see the message "<Message2>" in current conversation
    And User <Member1> does not see message "<Message2>" in conversation <TeamOwner> via device <ContactDevice> anymore
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Message2           | ContactDevice |
      | user1Name | Messaging | user2Name | Hello!  | Hello to you, too! | Device1       |

  @TC-4317 @regression @RC @conversationView @deleteMessage
  Scenario Outline: I want to be able to delete a message in a 1:1 conversation for myself
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds a new device <ContactDevice> with label <ContactDevice>
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And User <Member1> sends message "<Message2>" via device <ContactDevice> to User Myself
    And I hide the keyboard
    And I see the message "<Message2>" in current conversation
    And User <Member1> sees message "<Message2>" in conversation <TeamOwner> via device <ContactDevice>
    When I long tap on the message "<Message2>" in current conversation
    And I tap delete button
    And I see delete for me text
    And I tap delete for me confirm button
    Then I see deleted label
    And I do not see the message "<Message2>" in current conversation
    And I see the message "<Message>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I tap delete button
    And I tap delete for me button
    And I see delete for me text
    And I tap delete for me confirm button
    Then I see deleted label
    And I do not see the message "<Message>" in current conversation
    And User <Member1> sees message "<Message2>" in conversation <TeamOwner> via device <ContactDevice>

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Message2           | ContactDevice |
      | user1Name | Messaging | user2Name | Hello!  | Hello to you, too! | Device1       |

  @TC-4318 @regression @RC @groups @deleteMessage
  Scenario Outline: I want to be able to delete a message in a group conversation for everyone
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <ConversationName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member2> adds a new device <ContactDevice> with label <ContactDevice>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I see group conversation "<ConversationName>" is in foreground
    And User <Member2> sends message "<Message>" via device <ContactDevice> to group conversation <ConversationName>
    And I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message2>" in current conversation
    And User <Member2> sees message "<Message2>" in conversation <ConversationName> via device <ContactDevice>
    When I long tap on the message "<Message2>" in current conversation
    And I tap delete button
    And I see delete options
    And I tap delete for everyone button
    Then I see deleted label
    And I do not see the message "<Message2>" in current conversation
    And User <Member2> does not see message "<Message2>" in conversation <ConversationName> via device <ContactDevice> anymore
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName        | ConversationName | Message | Message2           | ContactDevice |
      | user1Name | user2Name | user3Name | MessageDeleting | MyTeam           | Hello!  | Hello to you, too! | Device1       |

  @TC-4319 @regression @RC @groups @deleteMessage
  Scenario Outline: I want to be able to delete a message in a group conversation for myself
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <ConversationName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member2> adds a new device <ContactDevice> with label <ContactDevice>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And User <Member2> sends message "<Message2>" via device <ContactDevice> to group conversation <ConversationName>
    And I hide the keyboard
    And I see the message "<Message2>" in current conversation
    And User <Member2> sees message "<Message2>" in conversation <ConversationName> via device <ContactDevice>
    When I long tap on the message "<Message2>" in current conversation
    And I tap delete button
    And I see delete for me text
    And I tap delete for me confirm button
    Then I see deleted label
    And I do not see the message "<Message2>" in current conversation
    And I see the message "<Message>" in current conversation
    And User <Member2> sees message "<Message2>" in conversation <ConversationName> via device <ContactDevice>
    When I long tap on the message "<Message>" in current conversation
    And I tap delete button
    And I see delete options
    And I tap delete for me button
    And I see delete for me text
    And I tap delete for me confirm button
    Then I see deleted label
    And I do not see the message "<Message>" in current conversation
    And User <Member2> sees message "<Message>" in conversation <ConversationName> via device <ContactDevice>

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName        | ConversationName | Message | Message2           | ContactDevice |
      | user1Name | user2Name | user3Name | MessageDeleting | MyTeam           | Hello!  | Hello to you, too! | Device1       |

