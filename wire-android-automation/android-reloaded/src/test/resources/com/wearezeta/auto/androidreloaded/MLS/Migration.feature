@MLS @MLSMigration
Feature: Migration

  ######################
  # Messaging
  ######################

  @TC-8300 @TC-8308 @TC-8309 @TC-8306
  Scenario Outline: I want to see my message history from proteus after migrating group to MLS
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1> in team "<TeamName>"
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I scroll to the bottom of group details page
    And I do not see the conversation is using MLS protocol
    And I close the group conversation details through X icon
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    # Add step below again and remove instance step once https://wearezeta.atlassian.net/browse/WPB-9143 is fixed
    # And User <TeamOwner> adds 1 2FA device
    And <TeamOwner> starts 2FA instance using chrome
    # TC-8308 - I want to see a system message that migration to MLS has started
    Then I see system message "<MigrationStart>" in conversation view
    # TC-8309 - I want to see a system message that migration to MLS has ended
    And I see system message "<MigrationEnd>" in conversation view
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I scroll to the bottom of group details page
    Then I see the conversation is using MLS protocol
    And I see Cipher Suite entry in group details page
    #Todo: Figure out what to do with those entries. They are visible in all builds, expect for prod (Bund uses prod)
#    And I see Last Key Material Update entry in group details page
#    And I see Group State entry in group details page
#    And I see Group State status is "ESTABLISHED" in group details page
    And I close the group conversation details through X icon
    # TC-8306 - I want to continue sending and receiving messages after migration to MLS
    When User <TeamOwner> sends message "<Message3>" to group conversation <ConversationName>
    Then I see the message "<Message3>" in current conversation
    And I type the message "<Message4>" into text input field
    And I tap send button
    And I see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | Message3              | Message4          | MigrationStart                                                                                 | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email | MLS      | Migration        | Hello!  | Hello to you, too! | Hello after Migration | It was a success! | Migration of encryption protocol has started. Make sure you all your Wire clients are updated. | This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  @TC-8301 @TC-8302 @TC-8303
  Scenario Outline: I want to see replies and edited messages displayed correctly from proteus conversation after migration to MLS
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1> in team "<TeamName>"
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" as reply to last message of conversation <ConversationName> via device Device1
    And I see the message "<Message2>" as a reply to message "<Message>" in conversation view
    And User <TeamOwner> edits the recent message to "<Message3>" from group conversation <ConversationName> via device Device1
    And I see the message "<Message3>" as a reply to message "<Message>" in conversation view
    And I type the message "<Message4>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message4>" in current conversation
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    # Add step below again and remove instance step once https://wearezeta.atlassian.net/browse/WPB-9143 is fixed
    # And User <TeamOwner> adds a new 2FA device Device2 with label Device2
    And <TeamOwner> starts 2FA instance using chrome
    Then I see system message "<MigrationEnd>" in conversation view
    And I scroll to the top of conversation view
    And I see the message "<Message3>" as a reply to message "<Message>" in conversation view
    # TC-8303 - I want to edit a message that was sent before MLS migration
    When I long tap on the message "<Message4>" in current conversation
    And I tap on edit option
    And I edit my message to "<Message5>"
    And I tap send button for my edit message
    And I hide the keyboard
    Then I see the message "<Message5>" in current conversation
    And I do not see the message "<Message4>" in current conversation
    And I scroll to the bottom of conversation view
    # TC-8302 - I want to reply to a message that was sent before MLS migration
    When I long tap on the message "<Message3>" in current conversation
    And I tap reply option
    And I see the message "<Message3>" as preview in message input field
    And I type the message "<Message6>" into text input field
    And I tap send button
    Then I see the message "<Message6>" as a reply to message "<Message3>" in conversation view

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | Message3 | Message4                | Message5               | Message6                                           | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email | MLS      | Migration        | Hello!  | Hello to you, too! | Good Day | Let us start migration. | Edited after migration | This is a reply to a message from before migration.| This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  @TC-8304
  Scenario Outline: I want to see self deleting message sent before migration delete in the pre-defined time after migration
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1> in team "<TeamName>"
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    When I tap on self deleting messages button
    And I tap on 10 seconds timer button
    And I see self deleting message label in text input field
    And I type the self deleting message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    And User <TeamOwner> sends ephemeral message "<Message4>" with timer 10 seconds via device Device1 to conversation <ConversationName>
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    # Add step below again and remove instance step once https://wearezeta.atlassian.net/browse/WPB-9143 is fixed
    # And User <TeamOwner> adds 1 2FA device
    And <TeamOwner> starts 2FA instance using chrome
    And I see system message "<MigrationEnd>" in conversation view
    And I hide the keyboard
    And I wait for 2 seconds
    Then I do not see the message "<Message3>" in current conversation
    And I do not see the message "<Message4>" in current conversation
    But I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | Message3                        | Message4                                | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email | MLS      | Migration        | Hello!  | Hello to you, too! | This will delete in 10 seconds. | This will delete in 10 seconds as well. | This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  @TC-8305
  Scenario Outline: I want to see received migrated messages when I was offline and migration happened before I got back online
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1>,<Member2> in team "<TeamName>"
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I tap log out button on clear data alert
    And I see Welcome Page
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    # Add steps below again and remove instance step once https://wearezeta.atlassian.net/browse/WPB-9143 is fixed
    # And User <TeamOwner> adds 1 2FA device
    # And User <Member2> adds 1 2FA device
    And <TeamOwner> starts 2FA instance using chrome
    And <Member2> starts 2FA instance using chrome
    And User <TeamOwner> sends message "<Message3>" to group conversation <ConversationName>
    And User <Member2> sends message "<Message4>" to group conversation <ConversationName>
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I wait until the notification popup disappears
    And I see unread conversation "<ConversationName>" in conversation list
    And I tap on unread conversation name "<ConversationName>" in conversation list
    Then I see system message "<MigrationStart>" in conversation view
    # ToDo: System message for END not visible, but group migrated successfully. Why?
    # And I see system message "<MigrationEnd>" in conversation view
    And I see the message "<Message3>" in current conversation
    And I see the message "<Message4>" in current conversation
    When User <TeamOwner> sends message "<Message5>" to group conversation <ConversationName>
    Then I see the message "<Message5>" in current conversation
    And I type the message "<Message6>" into text input field
    And I tap send button
    And I see the message "<Message6>" in current conversation
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I scroll to the bottom of group details page
    Then I see the conversation is using MLS protocol
    And I see Cipher Suite entry in group details page
    #Todo: Figure out what to do with those entries. They are visible in all builds, expect for prod (Bund uses prod)
#    And I see Last Key Material Update entry in group details page
#    And I see Group State entry in group details page
#    And I see Group State status is "ESTABLISHED" in group details page

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | ConversationName | Message | Message2           | Message3              | Message4          | Message5 | Message6   | MigrationStart                                                                                 | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email | user3Name | MLS      | Migration        | Hello!  | Hello to you, too! | Hello after Migration | It was a success! | Yes      | I am back! | Migration of encryption protocol has started. Make sure you all your Wire clients are updated. | This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  ######################
  # Calling
  ######################

  @TC-8307
  Scenario Outline: I want to start a call in a group after migration to MLS
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1> in team "<TeamName>"
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I hide the keyboard
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    And <TeamOwner> starts 2FA instance using chrome
    And <TeamOwner> accepts next incoming call automatically
    And I see system message "<MigrationStart>" in conversation view
    And I see system message "<MigrationEnd>" in conversation view
    And I tap start call button
    And <TeamOwner> verifies that waiting instance status is changed to active in 60 seconds
    Then I see ongoing group call
    And User <TeamOwner> verifies to send and receive audio

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | MigrationStart                                                                                 | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email |  MLS      | Migration        | Hello!  | Hello to you, too! | Migration of encryption protocol has started. Make sure you all your Wire clients are updated. | This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  @TC-8310
  Scenario Outline: I should not be dropped from a call that started before migration while migration to MLS is happening
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <ConversationName> with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
    And User <TeamOwner> adds 1 2FA device
    And <TeamOwner> accepts next incoming call automatically
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
    And I see conversation "<ConversationName>" in conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I hide the keyboard
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    When I tap start call button
    And <TeamOwner> verifies that waiting instance status is changed to active in 60 seconds
    Then I see ongoing group call
    And Users <TeamOwner> unmutes their microphone
    And User <TeamOwner> verifies to send and receive audio
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> now migrates conversations of team <TeamName> to MLS via backdoor
    # Add step below again and remove instance step once https://wearezeta.atlassian.net/browse/WPB-9143 is fixed
    # And User <TeamOwner> adds 1 2FA device
    And <TeamOwner> starts 2FA instance using chrome
    Then I see ongoing group call
    And User <TeamOwner> verifies to send and receive audio
    When I tap hang up button
    Then I do not see ongoing group call
    And I see system message "<MigrationStart>" in conversation view
    And I see system message "<MigrationEnd>" in conversation view

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | ConversationName | Message | Message2           | MigrationStart                                                                                 | MigrationEnd                                                                                                                                                  |
      | user1Name | user2Name | user2Email | MLS      | Migration        | Hello!  | Hello to you, too! | Migration of encryption protocol has started. Make sure you all your Wire clients are updated. | This conversation now uses the new Messaging Layer Security (MLS) protocol. To communicate seamlessly, always use the latest version of Wire on your devices. |

  ######################
  # Misc
  ######################

#  TC-8311	I want to send and receive messages when my team has migrated to MLS and I am in a group conversation with a team that is on proteus	No	Medium

#  TC-8312	I want to log in on a new client after my conversations were migrated to MLS and exchange messages	No	Medium
#  TC-8313	I want to migrate to MLS on a client that has been inactive for >28 days	No	Medium
#  TC-8314	I want to import a backup created before MLS migration