@MLS
Feature: General

  ######################
  # 1:1 Conversation Creation
  ######################

  @TC-8289
  Scenario Outline: I want to see a newly created 1:1 conversation is using MLS protocol after MLS being enabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds 1 2FA device
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I tap on user name "<Member1>" in Search result list
    When I tap start conversation button on connected user profile page
    And I see conversation view with "<Member1>" is in foreground
    Then Conversation <Member1> from user <TeamOwner> uses mls protocol

    Examples:
      | TeamOwner | Email      | Member1   | TeamName |
      | user1Name | user1Email | user2Name | MLS      |

  ######################
  # Group Creation
  ######################

  @TC-8259 @TC-8266 @TC-8260 @TC-8273 @TC-8292
  Scenario Outline: I want to create an MLS group and exchange messages in that group if MLS is enabled for me
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    # TC-8259 - I want to create an MLS group if MLS is enabled for me
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    # TC-8266 - I want to see a protocol dropdown during group creation as MLS enabled member
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    When I tap on group conversation title "MLS" to open group details
    Then I see group details page
    And I scroll to the bottom of group details page
    # TC-8292 - I want to see MLS protocol on group details page
    And I see the conversation is using MLS protocol
    And I see Cipher Suite entry in group details page
    #Todo: Figure out what to do with those entries. They are visible in all builds, expect for prod (Bund uses prod)
#    And I see Last Key Material Update entry in group details page
#    And I see Group State entry in group details page
#    And I see Group State status is "ESTABLISHED" in group details page
    And I close the group conversation details through X icon
    # TC-8260 - I want to send and receive messages in the MLS conversation
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation
    # TC-8273 - I want to be able to leave an MLS group and keep conversation history
    And I tap on group conversation title "<ConversationName>" to open group details
    And I tap show more options button
    When I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation list
    And I see conversation "<ConversationName>" in conversation list
    When I tap on conversation name "<ConversationName>" in conversation list
    Then I see system message "<LeftConversationText>" in conversation view

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | LeftConversationText      |
      | user1Name | user2Name | user2Email | MLS      | MLS              | Hello!  | Hello to you, too! | You left the conversation |

  @TC-8261
  Scenario Outline: I want to be able to send a message in an MLS group if the group was created before I logged in
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email1>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I close the conversation view through the back arrow
    And I see conversation list
    And I tap User Profile Button
    When I tap New Team or Account button
    And I see Welcome Page
    And User <Member2> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email2>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<ConversationName>" in conversation list
    When I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I see conversation list
    And I tap User Profile Button
    And I see my other account <Member1> is listed under other logged in accounts
    When I switch to <Member1> account
    And I see conversation list
    And I wait until the notification popup disappears
    And I see unread conversation "<ConversationName>" in conversation list
    And I tap on unread conversation name "<ConversationName>" in conversation list
    Then I see the message "<Message>" in current conversation
    When I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email1     | Member2   | Email2     | Member3   | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | user3Email | user4Name | MLS      | MLS              | Hello!  | Hello to you, too! |

  @TC-8363 @TC-8258
  Scenario Outline: I want to create an MLS group when MLS was enabled after I logged in
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    # TC-8258 - I should not see protocol dropdown during group creation when MLS is not enabled for my team
    When I tap on start a new conversation button
    And I tap create new group button
    And I see create new group details page
    Then I do not see the protocol dropdown
    And I close the group creation page through the back arrow
    And I close the search page through X icon
    And I see conversation list
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              | Hello!  | Hello to you, too! |

  ######################
  # Group deletion
  ######################

  @TC-8288
  Scenario Outline: I want to delete a conversation for all conversation members as the group creator of a MLS conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "<ConversationName>" to open group details
    And I tap show more options button
    When I tap delete group button
    And I tap remove group button
    Then I see conversation list
    And I do not see conversation "<ConversationName>" in conversation list

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | MLS      | MLS              |

  ######################
  # General Conversation Behaviours
  ######################

  @TC-8270 @TC-8285
  Scenario Outline: I want to share assets in a MLS conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member2> adds a new 2FA device Device2 with label Device2
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    # Receiving
    When User <TeamOwner> sends 1.00MB file having name "<FileName><FileExtension>" and MIME type "text/plain" via device Device1 to group conversation "<ConversationName>"
    Then I see a file with name "<FileName><FileExtension>" in the conversation view
    When I tap on the file with name "<FileName><FileExtension>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    And I wait up 15 seconds until file having name "<FileName><FileExtension>" is downloaded to the device
    And I remove the file "<FileName><FileExtension>" from device's sdcard
    # Sending
    And I push 1KB sized file with name "textfile.txt" to file storage
    When I tap file sharing button
    And I tap on Attach File option
    And I select file with name containing "textfile.txt" in DocumentsUI
    And I see file "textfile.txt" on preview page
    And I tap send button on preview page
    Then I see a file with name "textfile.txt" in the conversation view
    And I push image with QR code containing "Image" to file storage
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    And I tap back button
    Then I see an image with QR code "Image" in the conversation view
    And I tap file sharing button
    And I hide the keyboard
    # TC-8285 - I want to remove assets for everyone in a MLS conversation
    When I longtap on the file with name "textfile.txt" in the conversation view
    And I see delete option
    And I tap delete option
    And I tap delete for everyone button
    And I see conversation view with "<ConversationName>" is in foreground
    Then I do not see a file with name "textfile.txt" in the conversation view
    But I see a file with name "<FileName><FileExtension>" in the conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | ConversationName | FileName  | FileExtension |
      | user1Name | user2Name | user2Email | user3Name | MLS      | MLS              | qa_random | .txt          |

  @TC-8257 @TC-8278 @TC-8279 @TC-8280 @TC-8281 @TC-8267
  Scenario Outline: I want to send likes, mentions, replies, pings and read receipts in MLS group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member2> adds a new 2FA device Device2 with label Device2
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    # TC-8280 - I want to send and receive pings in MLS group conversation
    When I tap file sharing button
    And I tap on ping button
    Then I see system message "You pinged" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    # TC-8281 - I want to send and receive read receipts in MLS group conversation
    When User <TeamOwner> sends read receipt on last message in conversation <ConversationName> via device Device1
    And I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    And I tap on read receipts tab in message details
    Then I see 1 read receipts in read receipts tab
    And I see user <TeamOwner> in the list of users that read my message
    And I tap back button
    # TC-8267 - I want to send and receive reactions in MLS group conversation
    When User <Member2> likes the recent message from group conversation <ConversationName> via device Device2
    Then I see a "<ReactionHeart>" from 1 user as reaction to my message
    When User <Member2> sends message "<Message2>" via device Device2 to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I long tap on the message "<Message2>" in current conversation
    And I see reactions options
    And I tap on <ReactionHeart> icon
    Then I see a "<ReactionHeart>" from 1 user as reaction to user <Member2> message
    # TC-8278 - I want to send and receive mentions in MLS group conversation
    When I type the mention "@<TeamOwnerFirstName>" into text input field
    And I see user "<TeamOwner>" in mention list
    And I select user "<TeamOwner>" from mention list
    And I tap send button
    Then I see the last mention is "@<TeamOwner>" in current conversation
    # TC-8279 - I want to send and receive replies in MLS group conversation
    When User <TeamOwner> sends message "<Message3>" as reply to last message of conversation <ConversationName> via device Device1
    Then I see the message "<Message3>" as a reply to my mention "@<TeamOwner>" in conversation view
    When I long tap on the message "<Message3>" in current conversation
    And I see reply option
    And I tap reply option
    And I see the message "<Message3>" as preview in message input field
    And I type the message "<Message4>" into text input field
    And I tap send button
    Then I see the message "<Message4>" as a reply to message "<Message3>" in conversation view

    Examples:
      | TeamOwner | TeamOwnerFirstName | Member1   | Email      | Member2   | TeamName | ConversationName | Message             | Message2 | Message3   | Message4 | ReactionHeart |
      | user1Name | user1FirstName     | user2Name | user2Email | user3Name | MLS      | MLS              | Who else likes MLS? | I do!    | Me as well | Great!   | ❤️            |

  ######################
  # Self Deleting Messages
  ######################

  #ToDo: Make tests faster by waiting 10 seconds only once (teamowner reads already when I wait)

  @TC-8379
  Scenario Outline: I want make sure self deleting messages are working in MLS conversations (self user timer)
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message1>" into text input field
    And I tap send button
    And I see the message "<Message1>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When I tap on self deleting messages button
    And I tap on 10 seconds timer button
    And I see self deleting message label in text input field
    And I type the self deleting message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message3>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message3>" in current conversation
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> reads the recent message from group conversation <ConversationName> via device Device1
    Then I do not see the message "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> sends message "<Message4>" via device Device1 to group conversation <ConversationName>
    Then I see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | Email      | ConversationName | Message1                            | Message2 | Message3                           | Message4                          |
      | user1Name | MLS      | user2Name | user2Email | SelfDeletingMLS  | Let us test self deleting messages! | OK!      | This will delete after 10 seconds. | I do not see the message anymore. |

  @TC-8276
  Scenario Outline: I want make sure self deleting messages are working in MLS conversations (conversation timer)
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message1>" into text input field
    And I tap send button
    And I see the message "<Message1>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see Self Deleting messages option is in OFF state
    And I tap on Self Deleting messages option for group conversation
    And I tap on Self Deleting messages toggle
    And I tap on 10 seconds timer button for changing the timer for group conversation
    And I tap on Apply button
    And I close the group conversation details through X icon
    Then I see system message "You set self-deleting messages to 10 seconds for everyone" in conversation view
    When I type the self deleting message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message3>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message3>" in current conversation
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> reads the recent message from group conversation <ConversationName> via device Device1
    Then I do not see the message "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> sends ephemeral message "<Message4>" with timer 10 seconds via device Device1 to conversation <ConversationName>
    Then I see the message "<Message4>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | Email      | ConversationName | Message1                            | Message2 | Message3                           | Message4                          |
      | user1Name | MLS      | user2Name | user2Email | SelfDeletingMLS  | Let us test self deleting messages! | OK!      | This will delete after 10 seconds. | I do not see the message anymore. |

  @TC-8277 @WPB-5301
  Scenario Outline: I want make sure self deleting messages are working in MLS conversations (global timer)
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message1>" into text input field
    And I tap send button
    And I see the message "<Message1>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I hide the keyboard
    When Team Owner <TeamOwner> enables forced Self deleting messages for team <TeamName> with timeout of 10 seconds
    And I tap OK button on the alert
    And I see self deleting message label in text input field
    And I type the self deleting message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message3>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message3>" in current conversation
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> reads the recent message from group conversation <ConversationName> via device Device1
    Then I do not see the message "After one particxipant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    #Bug - self deleting messages are sent as normal message from kalium -> WPB-5301
    When User <TeamOwner> sends ephemeral message "<Message4>" with timer 10 seconds via device Device1 to conversation <ConversationName>
    Then I see the message "<Message4>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | Email      | ConversationName | Message1                            | Message2 | Message3                           | Message4                          |
      | user1Name | MLS      | user2Name | user2Email | SelfDeletingMLS  | Let us test self deleting messages! | OK!      | This will delete after 10 seconds. | I do not see the message anymore. |

  ######################
  # Leave Group
  ######################

  @TC-8274 @leaveGroup
  Scenario Outline: I want to verify that I can still exchange messages after a member left an MLS group
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    When <Member2> leaves group conversation <ConversationName>
    Then I see system message "<Member2> left the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | MLS      | MLS              | Hello!  | Hello to you, too! |

  @TC-8284 @leaveGroup
  Scenario Outline: I want to be able to leave an MLS group and clear conversation history
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And User <Member2> sends message "<Message>" to group conversation <ConversationName>
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When I tap on group conversation title "<ConversationName>" to open group details
    And I tap show more options button
    And I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation list
    And I see conversation "<ConversationName>" in conversation list
    When I tap on conversation name "<ConversationName>" in conversation list
    Then I see system message "<LeftConversationText>" in conversation view
    And I see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    When I long tap on conversation name "<ConversationName>" in conversation list
    And I tap clear content button on conversation list
    And I tap clear content confirm button on conversation list
    Then I see "Group content was deleted" toast message on conversation list
    When I tap on conversation name "<ConversationName>" in conversation list
    And I see conversation view with "<ConversationName>" is in foreground
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | ConversationName | Message | Message2           | LeftConversationText      |
      | user1Name | user2Name | user2Email | user3Name | MLS      | MLS              | Hello!  | Hello to you, too! | You left the conversation |

  ######################
  # Remove User
  ######################

  @TC-8265 @TC-8269 @removeGroup
  Scenario Outline: I want to remove users from MLS group
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member3> in participants list
    # TC-8269 - I want to remove users from a MLS group conversation as a group admin
    When I tap on user <Member3> in participants list
    Then I see connected user <Member3> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member3> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member3> from the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              | Hello!  | Hello to you, too! |

  @TC-8262 @removeGroup @addGroup
  Scenario Outline: I want to remove a user from an MLS group and add him again to the same group
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member3> in participants list
    When I tap on user <Member3> in participants list
    Then I see connected user <Member3> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member3> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member3> from the conversation" in conversation view
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    Then I see user <Member3> in search suggestions list
    When I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member3> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You added <Member3> to the conversation" in conversation view
    When I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member3> in participants list
    When I tap on user <Member3> in participants list
    Then I see connected user <Member3> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member3> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member3> from the conversation" in conversation view
    When I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    Then I see user <Member3> in search suggestions list
    When I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member3> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You added <Member3> to the conversation" in conversation view
    When I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member3> in participants list
    When I tap on user <Member3> in participants list
    Then I see connected user <Member3> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member3> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member3> from the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              |

  ######################
  # Delete User
  ######################

  @TC-8275 @deletedUser
  Scenario Outline: I want to verify that I can still exchange messages in an MLS group after a member was removed from the team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And User <TeamOwner> sends message "<Message>" to group conversation <ConversationName>
    And I see the message "<Message>" in current conversation
    When User <TeamOwner> removes user <Member2>  from team <TeamName>
    Then I see system message "<Member2> was removed from the team" in conversation view
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation
    When I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName | Message | Message2            | Message3 |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              | Hello!  | Hello after delete! | Works!   |

  ######################
  # Add User
  ######################

  @TC-8264 @TC-8368 @addGroup
  Scenario Outline: I want to add users to MLS group
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member3> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member3> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You added <Member3> to the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              | Hello!  | Hello to you, too! |

  @TC-8272 @TC-8283 @addGroup
  Scenario Outline: I should not be able to add a member without a client to an MLS group
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    Then I do not see user <Member3> in participants list
    # TC-8283 - I want to see an alert explaining that I cannot add a user to MLS conversation when this User does not have a MLS client
    When I close the group conversation details through X icon
    Then I see system message "<Member3> could not be added to the group." in conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLS              |

  ######################
  # Delete Message
  ######################

  @TC-8286
  Scenario Outline: I want to remove a message for everyone in a MLS conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I tap delete button
    And I see delete options
    And I tap delete for everyone button
    Then I see deleted label
    And I do not see the message "<Message>" in current conversation
    And User <TeamOwner> does not see message "<Message>" in conversation <ConversationName> via device Device1 anymore
    But I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           |
      | user1Name | user2Name | user2Email | MLS      | MLS              | Hello!  | Hello to you, too! |

  ######################
  # Promoting Members
  ######################

  @TC-8287 @TC-8290 @TC-8291
  Scenario Outline: I want to promote group conversation members on same backend to admin role in a MLS conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email1>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select user <Member1> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member1> in participants list
    When I tap on user <Member1> in participants list
    And I see connected user <Member1> profile
    And I tap on edit button to change user role
    And I change the user role to admin
    Then I see new role for user is admin
    And I close the user profile through the close button
    And I close the group conversation details through X icon
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I tap New Team or Account button
    And User <Member1> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email2>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on conversation name "<ConversationName>" in conversation list
    And I tap on group conversation title "<ConversationName>" to open group details
    # TC-8290 - I want to be able to add a user to MLS conversation after I have been promoted to Admin
    When I tap on Participants tab
    And I tap on Add Participants button
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    When I select user <Member2> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member2> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You added <Member2> to the conversation" in conversation view
    # TC-8291 - I want to be able to remove a user to MLS conversation after I have been promoted to Admin
    When I tap on group conversation title "MLS" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member2> in participants list
    When I tap on user <Member2> in participants list
    Then I see connected user <Member2> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member2> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member2> from the conversation" in conversation view

    Examples:
      | TeamOwner | Email1     | Member1   | Email2     | Member2   | TeamName | ConversationName |
      | user1Name | user1Email | user2Name | user2Email | user3Name | MLS      | MLS              |

  ######################
  # Device Details
  ######################

  @TC-8293 @TC-8294
  Scenario Outline: I want to see MLS Thumbprint on my devices page
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    When I open manage your devices menu
    Then I see my current device is listed under devices
    When I tap on my current device listed under devices
    Then I see my MLS thumbprint is displayed
    # TC-8294 - I want to see last active label on my devices page
    And I see last active entry on devices page
    And I see that my device was used Less than a week ago on last active entry on devices page

    Examples:
      | TeamOwner | Member1   | Email      | TeamName |
      | user1Name | user2Name | user2Email | MLS      |
