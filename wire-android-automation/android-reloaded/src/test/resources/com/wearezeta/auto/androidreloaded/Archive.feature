Feature: Archive

  @TC-4234 @regression @RC @archive
  Scenario Outline: Verify you can archive and unarchive a 1:1 conversation from user profile screen
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I tap show more options button on user profile screen
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    When I close the user profile through the close button
    And I tap back button
    Then I do not see conversation "<Member1>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<Member1>" in archive list
    When I tap on conversation name "<Member1>" in archive list
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I tap show more options button on user profile screen
    And I tap move out of archive button
    Then I see toast message "Conversation was unarchived" in user profile screen
    When I close the user profile through the close button
    And I tap back button
    And I open the main navigation menu
    And I tap on conversations menu entry
    Then I see conversation "<Member1>" in conversation list

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | Archive  | user2Name |

  @TC-4235 @regression @RC @archive
  Scenario Outline: Verify you can archive and unarchive a 1:1 conversation from conversation list screen
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    And I long tap on conversation name "<Member1>" in conversation list
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    And I do not see conversation "<Member1>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<Member1>" in archive list
    When I long tap on conversation name "<Member1>" in archive list
    And I tap move out of archive button
    Then I see "Conversation was unarchived" toast message on archive list
    And I do not see conversation "<Member1>" in archive list
    When I open the main navigation menu
    And I tap on conversations menu entry
    Then I see conversation "<Member1>" in conversation list

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | Archive  | user2Name |

  @TC-4236 @regression @RC @archive
  Scenario Outline: Verify you can archive and unarchive a group conversation from user profile screen
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    When I close the group conversation details through X icon
    And I tap back button
    Then I do not see conversation "<GroupConversation>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<GroupConversation>" in archive list
    When I tap on conversation name "<GroupConversation>" in archive list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    And I tap move out of archive button
    Then I see toast message "Conversation was unarchived" on group details page
    When I close the group conversation details through X icon
    And I tap back button
    Then I do not see conversation "<GroupConversation>" in archive list
    And I open the main navigation menu
    And I tap on conversations menu entry
    Then I see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation  |
      | user1Name | user2Name | user3Name | Archive  | MyTeam             |

  @TC-4237 @regression @RC @archive
  Scenario Outline: Verify you can archive and unarchive a group conversation from conversation list screen
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    And I do not see conversation "<GroupConversation>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<GroupConversation>" in archive list
    When I long tap on conversation name "<GroupConversation>" in archive list
    And I tap move out of archive button
    Then I see "Conversation was unarchived" toast message on archive list
    And I do not see conversation "<GroupConversation>" in archive list
    When I open the main navigation menu
    And I tap on conversations menu entry
    Then I see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation  |
      | user1Name | user2Name | user3Name | Archive   | MyTeam             |

  @TC-4238 @regression @RC @archive
  Scenario Outline: Verify you can interact with an archived conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I long tap on conversation name "<GroupConversation>" in conversation list
    And I tap move to archive button
    And I confirm archive conversation
    And I open the main navigation menu
    And I tap on archive menu entry
    And I see conversation "<GroupConversation>" in archive list
    And I tap on conversation name "<GroupConversation>" in archive list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation  | Message | Message2           |
      | user1Name | user2Name | user3Name | Archive   | MyTeam             | Hello!  | Hello to you, too! |

  @TC-4239 @regression @RC @archive
  Scenario Outline: Verify you can archive a conversation you have left
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    And I tap leave group button
    And I tap leave group confirm button
    And I see you left conversation toast message
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    And I do not see conversation "<GroupConversation>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<GroupConversation>" in archive list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation  |
      | user1Name | user2Name | user3Name | Archive   | MyTeam             |

  @TC-4240 @regression @archive
  Scenario Outline: I should not receive notifications for archived conversations
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <Member1> adds 1 device
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    And I see conversation "<Member1>" in conversation list
    When I long tap on conversation name "<Member1>" in conversation list
    And I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    Then I do not see conversation "<Member1>" in conversation list
    And I see conversation "<GroupConversation>" in conversation list
    When I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    Then I do not see conversation "<GroupConversation>" in conversation list
    When I minimise Wire
    And User <Member1> sends message "<Message1>" to group conversation <GroupConversation>
    And User <Member1> sends message "<Message2>" to User Myself
    And I open the notification center
    Then I do not see the message "<Message1>" from user <Member1> in group <GroupConversation> in the notification center
    And I do not see the message "<Message2>" from 1:1 conversation from user <Member1> in the notification center

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message1       | Message2         |
      | user1Name | Archive   | user2Name | ArchivedConvo     | Hello from 1:1 | Hello from group |

  @TC-4241 @regression @archive
  Scenario Outline: I should not receive notifications for self deleting messages in archived conversations
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <Member1> adds a new device Device1 with label Device1
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    And I see conversation "<Member1>" in conversation list
    When I long tap on conversation name "<Member1>" in conversation list
    And I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    Then I do not see conversation "<Member1>" in conversation list
    And I see conversation "<GroupConversation>" in conversation list
    When I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    Then I do not see conversation "<GroupConversation>" in conversation list
    When I minimise Wire
    And User <Member1> sends ephemeral message "<Message1>" with timer 10 seconds via device Device1 to conversation <GroupConversation>
    And User <Member1> sends ephemeral message "<Message2>" with timer 10 seconds via device Device1 to conversation <TeamOwner>
    And I open the notification center
    Then I do not see the message "<SelfDeletingNotification>" from user <Member1> in group <GroupConversation> in the notification center
    And I do not see the message "<SelfDeletingNotification>" from 1:1 conversation from user <Member1> in the notification center

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message1       | Message2         | SelfDeletingNotification       |
      | user1Name | Archive   | user2Name | ArchivedConvo     | Hello from 1:1 | Hello from group | Sent a self-deleting message   |

  @TC-4242 @regression @RC @archive @WPB-6127
  Scenario Outline: I should not receive notifications with app in foreground for a conversation which was archived before I logged in
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <Member1> adds 1 device
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    When User <TeamOwner> archives conversation "<Member1>"
    And User <TeamOwner> archives conversation "<GroupConversation>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I do not see conversation "<Member1>" in conversation list
    And I do not see conversation "<GroupConversation>" in conversation list
    When User <Member1> sends message "<Message1>" to group conversation <GroupConversation>
    And User <Member1> sends message "<Message2>" to User Myself
    And I open the notification center
    Then I do not see the message "<Message1>" from user <Member1> in group <GroupConversation> in the notification center
    And I do not see the message "<Message2>" from 1:1 conversation from user <Member1> in the notification center

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message1       | Message2 |
      | user1Name | Archive   | user2Name | ArchivedConvo     | Hello from 1:1 | Hello from group |
