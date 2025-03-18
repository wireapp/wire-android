Feature: Conversation Notifications

  ######################
  # Groups
  ######################

  @TC-4312 @TC-4313 @TC-4307 @regression @RC @groups @conversationNotifications
  Scenario Outline: I should not receive push notifications for messages in a group conversation when conversation status is set to mention and replies
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from user <Member2> in group <GroupConversation> in the notification center
    And I close the notification center
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    And I see the message "<Message>" in current conversation
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap notifications button on group details
    # TC-4313 - I want to see conversation notification for group conversation is set to Everything in default
    Then I see default notification is Everything
    # TC-4307 - I want to receive push notifications for messages in a group conversation when conversation status is set to Everything
    When I tap notification status of "<Notification>" in group details page
    And I see notification status of "<Notification>" in group details page
    And I tap back button 3 times
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I open the notification center
    Then I do not see the message "<Message2>" from user <Member2> in group <GroupConversation> in the notification center
    When I close the notification center
    And I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName     | Message | Notification                | GroupConversation | Message2        |
      | user1Name | user2Name | user3Name | Notification | Hello!  | Calls, mentions and replies | MyTeam            | No notification |

  @TC-4306 @regression @RC @groups @conversationNotifications
  Scenario Outline: I should not receive push notifications for messages in a group conversation when conversation status is set to Nothing
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And I wait until the notification popup disappears
    And I open the notification center
    And I see the message "<Message>" from user <Member2> in group <GroupConversation> in the notification center
    And I close the notification center
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    And I see the message "<Message>" in current conversation
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap notifications button on group details
    And I tap notification status of "<Notification>" in group details page
    And I see notification status of "<Notification>" in group details page
    And I tap back button 3 times
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I open the notification center
    Then I do not see the message "<Message2>" from user <Member2> in group <GroupConversation> in the notification center
    When I close the notification center
    And I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName     | Message | Notification | GroupConversation | Message2        |
      | user1Name | user2Name | user3Name | Notification | Hello!  | Nothing      | MyTeam            | No notification |

  ######################
  # 1:1
  ######################

  @TC-4308 @TC-4309 @TC-4310 @regression @RC @conversationNotifications
  Scenario Outline: I should not receive push notifications for messages in a 1:1 conversation when conversation status is set to mention and replies
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
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
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    And I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    And I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    And I see the message "<Message>" in current conversation
    And I open conversation details for 1:1 conversation with "<TeamOwner>"
    And I tap show more options button on user profile screen
    # TC-4309 - I want to see conversation notification for 1:1 conversation is set to Everything in default
    When I tap notifications button on user profile screen
    Then I see default notification is Everything on user profile screen
    # TC-4310 - I want to receive push notifications for messages in a 1:1 conversation when conversation status is set to Everything
    When I tap notification status of "<Notification>" on user profile screen
    And I see notification status of "<Notification>" on user profile screen
    And I tap back button 3 times
    And User <TeamOwner> sends message "<Message2>" to User Myself
    And I open the notification center
    And I do not see the message "<Message2>" from 1:1 conversation from user <TeamOwner> in the notification center
    And I close the notification center
    And I tap on conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName     | Message | Notification                | Message2        |
      | user1Name | user2Name | Notification | Hello!  | Calls, mentions and replies | No notification |

  @TC-4311 @regression @RC @conversationNotifications
  Scenario Outline: I should not receive push notifications for messages in a 1:1 conversation when conversation status is set to Nothing
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
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
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    And I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    And I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    And I see the message "<Message>" in current conversation
    And I open conversation details for 1:1 conversation with "<TeamOwner>"
    And I tap show more options button on user profile screen
    When I tap notifications button on user profile screen
    # TC-4309 - I want to see conversation notification for 1:1 conversation is set to Everything in default
    Then I see default notification is Everything on user profile screen
    When I tap notification status of "<Notification>" on user profile screen
    And I see notification status of "<Notification>" on user profile screen
    And I tap back button 3 times
    And User <TeamOwner> sends message "<Message2>" to User Myself
    And I open the notification center
    And I do not see the message "<Message2>" from 1:1 conversation from user <TeamOwner> in the notification center
    And I close the notification center
    And I tap on conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName     | Message | Notification | Message2        |
      | user1Name | user2Name | Notification | Hello!  | Nothing      | No notification |

