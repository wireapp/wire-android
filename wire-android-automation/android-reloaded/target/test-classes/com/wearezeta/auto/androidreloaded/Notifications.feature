Feature: Notifications

  ######################
  # 1:1
  ######################

  @TC-4461 @TC-4467 @regression @RC @notifications
  Scenario Outline: I want to receive push notifications for a messages in a 1:1 conversation with the app in foreground
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
    When User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message>" in current conversation
    # TC-4467 - I want to verify that push notifications disappear once I have read a message in a 1:1 conversation
    When I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName      | Message |
      | user1Name | user2Name | Notifications | Hello!  |

  @TC-4463 @regression @notifications
  Scenario Outline: I want to receive push notifications for messages in a 1:1 conversation with the app in background
    Given Notifications when the app is in the background are enabled on the build
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
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
    When I minimise Wire
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    # TC-4468 - I want to be able to open a 1:1 conversation when tapping on a push notification
    When I tap on the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    Then I see conversation view with "<TeamOwner>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName      | Message |
      | user1Name | user2Name | Notifications | Hello!  |

  @TC-4465 @regression @notifications
  Scenario Outline: I want to receive push notifications for messages in a 1:1 conversation when the app was terminated
    Given Notifications when the app is in the background are enabled on the build
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
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
    When I swipe the app away from background
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I tap on the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    Then I see conversation view with "<TeamOwner>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName      | Message |
      | user1Name | user2Name | Notifications | Hello!  |

  # Runs only on Graphene OS phone
  # This test is needed for Akamaya, who is using devices without playservices
  @TC-4466 @regression @RC @websocket @resource=25181JEGR05249
  Scenario Outline: I want to receive notifications in a 1:1 conversation when the app was terminated on websocket only device
    When Test runs only on node "Google-bluejay-Pixel6a-25181JEGR05249"
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
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
    And I wait until the notification popup disappears
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    When I tap Network Settings menu
    Then I see that there is no option to enable or disable my websocket
    When I swipe the app away from background
    And I open the notification center
    And I see the message that my Websocket connecting is running
    And I close the notification center
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I tap on the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    Then I see conversation view with "<TeamOwner>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | TeamName      | Message |
      | user1Name | user2Name | Notifications | Hello!  |

  ######################
  # Groups
  ######################

  @TC-4462 @TC-4470 @regression @RC @notifications
  Scenario Outline: I want to receive push notifications for messages in a group conversation with the app in foreground
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<GroupChatName>" in conversation list
    Then I see the message "<Message>" in current conversation
    # TC-4470 - I want to verify that push notifications disappear once I have read a message in a group conversation
    When I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupChatName      | Message |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup | Hello!  |

  @TC-4464 @regression @notifications
  Scenario Outline: I want to receive push notifications for messages in a group conversation with the app in background
    Given Notifications when the app is in the background are enabled on the build
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I minimise Wire
    And User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center
    # TC-4469 - I want to be able to open a group conversation when tapping on a push notification
    When I tap on the message "<Message>" from group conversation from user <Member2> in the notification center
    Then I see group conversation "<GroupChatName>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupChatName      | Message |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup | Hello!  |

  @TC-4460 @regression @RC @notifications @connect
  Scenario Outline: I want to receive a notification when I receive a connection request
    Given There are personal users <User1>, <User2>
    And User <User2> sets their unique username
    And User <User1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <User2> sends connection request to me
    And I open the notification center
    Then I see the message "<NotificationMessage>" from 1:1 conversation from user <User2> in the notification center

    Examples:
      | User1     | User2     | NotificationMessage |
      | user1Name | user2Name | Wants to connect    |

  @TC-4471 @regression @RC @notifications @mentions
  Scenario Outline: I want to receive notifications about mentions
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <TeamOwner> sends mention "@<Member1>" to group conversation <GroupChatName>
    And I minimise Wire
    And I open the notification center
    Then I see the mention "@<Member1>" from user <TeamOwner> in group <GroupChatName> in the notification center

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupChatName      |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup |

  @TC-4078 @regression @notification @calling
    Scenario Outline: I want to receive a notification for an incoming 1on1 call
    Given Notifications when the app is in the background are enabled on the build
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I minimise Wire
    When User <Member1> calls me
    And I open the notification center
    Then I see the message "<NotificationMessage>" from 1:1 conversation from user <Member1> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName      |  NotificationMessage |
      | user1Name | user2Name | Notifications |  Calling…            |

  @TC-4079 @regression @RC @notifications @calling
    Scenario Outline: I should receive a notification for a missed call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <Member1> calls <GroupConversation>
    And I see incoming group call from group <GroupConversation>
    And I decline the call
    And <Member1> stops calling <GroupConversation>
    And I open the notification center
    Then I see the message "Missed call" from user <Member1> in group <GroupConversation> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName    | GroupConversation |
      | user1Name | user2Name | WeLikeCalls | WantToCall        |

   @TC-4080 @regression @RC @notifications @calling
     Scenario Outline: I should see a notification that I am in an active call when the app is in the background
     Given There is a team owner "<TeamOwner>" with team "<TeamName>"
     And I wait for 3 seconds
     And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
     And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
     And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
     And User <TeamOwner> is me
     And <Member1> starts instance using chrome
     And I see Welcome Page
     And I open staging backend deep link
     And I tap proceed button on custom backend alert
     And I tap login button on Welcome Page
     And I sign in using my email
     And I tap login button on email Login Page
     And I wait until I am fully logged in
     And I decline share data alert
     When User <Member1> calls <GroupConversation>
     And I see incoming group call from group <GroupConversation>
     And I accept the call
     Then I see ongoing group call
     When I minimise Wire
     And I open the notification center
     Then I see ongoing call in group "<GroupConversation>" in the notification center

     Examples:
       | TeamOwner | Member1   | TeamName      | GroupConversation |
       | user1Name | user2Name | Notifications | WantToCall        |

  ######################
  # Status Notifications
  ######################

  @TC-4552 @regression @RC @notifications
  Scenario Outline: I want to receive push notifications for messages in a 1:1 conversation when my status is set to available
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
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to available on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName      | Message | StatusText                                                                                                                                                                    |
      | user1Name | user2Name | Notifications | Hello!  | You will appear as Available to other people. You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation. |

  @TC-4553 @regression @RC @notifications
  Scenario Outline: I should not receive push notifications for messages in a 1:1 conversation when my status is set to busy
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
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to busy on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <TeamOwner> sends message "<Message>" to User Myself
    And I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName      | Message | StatusText                                                                                                                                         |
      | user1Name | user2Name | Notifications | Hello!  | You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations that are not muted. |

  @TC-4554 @regression @RC @notifications
  Scenario Outline: I should not receive push notifications for messages in a 1:1 conversation when my status is set to away
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
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to away on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <TeamOwner> sends message "<Message>" to User Myself
    And I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<TeamOwner>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center

    Examples:
      | TeamOwner | Member1   | TeamName      | Message | StatusText                                                                                                                                         |
      | user1Name | user2Name | Notifications | Hello!  | You will appear as Away to other people. You will not receive notifications about any incoming calls or messages. |

  ######################
  # Groups
  ######################

  @TC-4555 @regression @RC @notifications
  Scenario Outline: I want to receive push notifications for messages in a group conversation when my status is set to available
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
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
    And I see change status options
    When I change my status to available on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<GroupChatName>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupChatName      | Message | StatusText                                                                                                                                                                    |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup | Hello!  | You will appear as Available to other people. You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation. |

  @TC-4556 @regression @RC @notifications
  Scenario Outline: I should not receive push notifications for messages in a group conversation when my status is set to busy
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
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
    And I see change status options
    When I change my status to busy on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    And I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<GroupChatName>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupChatName      | Message | StatusText                                                                                                                                         |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup | Hello!  | You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations that are not muted. |

  @TC-4557 @regression @RC @notifications
  Scenario Outline: I should not receive push notifications for messages in a group conversation when my status is set to away
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
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
    And I see change status options
    When I change my status to away on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I tap close button on User Profile Page
    And I see conversation list
    When User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    And I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center
    When I close the notification center
    And I tap on unread conversation name "<GroupChatName>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I open the notification center
    Then I do not see the message "<Message>" from user <Member2> in group <GroupChatName> in the notification center

    Examples:
      | TeamOwner | Member1   | Member2   |  TeamName     | GroupChatName      | Message | StatusText                                                                                                                                         |
      | user1Name | user2Name | user3Name | Notifications | NotificationsGroup | Hello!  | You will appear as Away to other people. You will not receive notifications about any incoming calls or messages. |

