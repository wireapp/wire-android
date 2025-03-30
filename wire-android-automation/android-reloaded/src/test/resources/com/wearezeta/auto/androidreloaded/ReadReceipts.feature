Feature: Read Receipts

  @TC-4488 @regression @RC @readReceipts
  Scenario Outline: I want to receive read receipts for messages in 1on1 conversations
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device <Device> with label <Device>
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <Member1> sends read receipt on last message in conversation <TeamOwner> via device Device1
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see 1 read receipts in read receipts tab
    And I see user <Member1> in the list of users that read my message

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Device  |
      | user1Name | Reactions | user2Name | Hello!  | Device1 |

  @TC-4489 @regression @RC @readReceipts
  Scenario Outline: I want to send read receipts for messages in 1on1 conversations
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
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <Member1> account
    And I see conversation list
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    When I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <TeamOwner> account
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see user <Member1> in the list of users that read my message

    Examples:
      | TeamOwner | TeamName     | Member1   | Message  |
      | user1Name | ReadReceipts | user2Name | Read me! |

  @TC-4492 @regression @RC @readReceipts
  Scenario Outline: I should not send read receipts in 1on1 conversations when I turned them off for my account
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
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I tap Privacy Settings menu
    And I see read receipts are turned on
    And I tap read receipts toggle
    Then I see read receipts are turned off
    And I tap back button 2 times
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <Member1> account
    And I see conversation list
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    When I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <TeamOwner> account
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I do not see user <Member1> in the list of users that read my message

    Examples:
      | TeamOwner | TeamName     | Member1   | Message  |
      | user1Name | ReadReceipts | user2Name | Read me! |

  @TC-4490 @regression @RC @readReceipts
  Scenario Outline: I want to receive read receipts for messages in group conversations
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
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
    When User <Member1> sends read receipt on last message in conversation <GroupConversation> via device Device1
    When User <Member2> sends read receipt on last message in conversation <GroupConversation> via device Device2
    When User <Member3> sends read receipt on last message in conversation <GroupConversation> via device Device3
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see 3 read receipts in read receipts tab
    And I see user <Member1> in the list of users that read my message
    And I see user <Member2> in the list of users that read my message
    And I see user <Member3> in the list of users that read my message

    Examples:
      | TeamOwner | TeamName     | GroupConversation | Member1   | Member2   | Member3   | Message  |
      | user1Name | ReadReceipts | Reading is fun    | user2Name | user3Name | user4Name | Read me! |

  @TC-4491 @regression @RC @readReceipts
  Scenario Outline: I want to send read receipts for messages in group conversations
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <TeamOwner> sets read receipt option to true for conversation <GroupConversation>
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <Member2> sends read receipt on last message in conversation <GroupConversation> via device Device2
    When User <Member3> sends read receipt on last message in conversation <GroupConversation> via device Device3
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see 2 read receipts in read receipts tab
    And I see user <Member2> in the list of users that read my message
    And I see user <Member3> in the list of users that read my message
    And I tap back button
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <Member1> account
    And I see conversation list
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    When I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I switch to <TeamOwner> account
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see 3 read receipts in read receipts tab
    And I see user <Member1> in the list of users that read my message
    And I see user <Member2> in the list of users that read my message
    And I see user <Member3> in the list of users that read my message

    Examples:
      | TeamOwner | TeamName     | GroupConversation | Member1   | Member2   | Member3   | Message  |
      | user1Name | ReadReceipts | Reading is fun    | user2Name | user3Name | user4Name | Read me! |
