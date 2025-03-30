@col1
Feature: Federation

  @TC-4731
  Scenario Outline: I want to create MLS conversation and add users on federated backend
    Given There is a team owner "<TeamOwnerCol1>" with team "<TeamNameCol1>" on column-1 backend
    And User <TeamOwnerCol1> adds users <Member1> to team "<TeamNameCol1>" with role Member
    Given There is a team owner "<TeamOwnerCol3>" with team "<TeamNameCol3>" on column-3 backend
    And User <TeamOwnerCol1> configures MLS for team "<TeamNameCol1>"
    And User <TeamOwnerCol3> configures MLS for team "<TeamNameCol3>"
    And User <TeamOwnerCol3> adds 1 2FA device
    And User <Member1> adds 1 2FA device
    And User <TeamOwnerCol1> is connected to <TeamOwnerCol3>
    And User <TeamOwnerCol1> is me
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
    Then I see group conversation "<ConversationName>" is in foreground
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I type user name "<TeamOwnerCol3>" in search field
    And I see user name "<TeamOwnerCol3>" in Search result list
    And I select user <TeamOwnerCol3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <TeamOwnerCol3> in participants list
    And I close the group conversation details through X icon
    And I see system message "You added <TeamOwnerCol3> to the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <TeamOwnerCol3> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwnerCol1 | Email      | Member1   | TeamNameCol1 | TeamOwnerCol3| TeamNameCol3 | ConversationName | Message | Message2           |
      | user1Name     | user1Email | user2Name | MLSCol1      | user3Name    | MLSCol3      | MLSFederated     | Hello!  | Hello to you, too! |

  @TC-4732 @TC-4760
  Scenario Outline: I want to create MLS conversation and remove users on federated backend
    Given There is a team owner "<TeamOwnerCol1>" with team "<TeamNameCol1>" on column-1 backend
    And User <TeamOwnerCol1> adds users <Member1> to team "<TeamNameCol1>" with role Member
    And There is a team owner "<TeamOwnerCol3>" with team "<TeamNameCol3>" on column-3 backend
    And User <TeamOwnerCol1> configures MLS for team "<TeamNameCol1>"
    And User <TeamOwnerCol3> configures MLS for team "<TeamNameCol3>"
    And User <TeamOwnerCol3> adds 1 2FA device
    And User <Member1> adds 1 2FA device
    And User <TeamOwnerCol1> is connected to <TeamOwnerCol3>
    And User <TeamOwnerCol1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    # TC-4760- I want to create MLS conversation with federated users
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select user <Member1> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerCol3>" in search field
    And I see user name "<TeamOwnerCol3>" in Search result list
    And I select user <TeamOwnerCol3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <TeamOwnerCol3> in participants list
    And I tap on user <TeamOwnerCol3> in participants list
    And I see connected user <TeamOwnerCol3> profile
    And I see remove from group button
    And I tap remove from group button
    And I see alert asking me if I want to remove user <TeamOwnerCol3> from group
    And I tap remove button on alert
    Then I do not see remove from group button
    And I close the user profile through the close button
    And I close the group conversation details through X icon
    And I see system message "You removed <TeamOwnerCol3> from the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwnerCol1 | Email      | Member1   | TeamNameCol1 | TeamOwnerCol3| TeamNameCol3 | ConversationName | Message | Message2           |
      | user1Name     | user1Email | user2Name | MLSCol1      | user3Name    | MLSCol13     | MLSFederated     | Hello!  | Hello to you, too! |

  ######################
  # Federation Offline
  ######################

  @TC-4722 @federationOfflineMLS @resource=column-offline
  Scenario Outline: I want to see queued messages when my backend comes back online in a Federated MLS conversation
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumnOffline> adds 1 device
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumnOffline>,<TeamOwnerColumn3>
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwnerColumnOffline>" in search field
    And I see user name "<TeamOwnerColumnOffline>" in Search result list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerColumn3>" in search field
    And I see user name "<TeamOwnerColumn3>" in Search result list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I tap New Team or Account button
    And User <TeamOwnerColumnOffline> is me
    And User <TeamOwnerColumn1> adds 1 2FA device
    And I tap OK button on Account was used on another device alert
    And I see Welcome Page
    And I open column-offline-android backend deep link
    And I see alert informing me that I am about to switch to column-offline-android backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on conversation name "<ConversationName>" in conversation list
    And I see group conversation "<ConversationName>" is in foreground
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    And User <TeamOwnerColumn1> sends message "<Message2>" to group conversation <ConversationName>
    And User <TeamOwnerColumn3> sends message "<Message3>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumn1> sends message "<Message4>" to group conversation <ConversationName>
    And User <TeamOwnerColumn3> sends message "<Message5>" to group conversation <ConversationName>
    And Federator for backend column-offline-android is turned on
    And I wait for 12 seconds
    Then I see the message "<Message4>" in current conversation
    And I see the message "<Message5>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      |  TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName | Message | Message2            | Message3            | Message4                          | Message5                          | Message6                                   | Message7                                   |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad       | Hello!  | Hello from column1! | Hello from column3! | Hello from column1 while offline! | Hello from column3 while offline! | Hello from column1 after user was removed! | Hello from column3 after user was removed! |


  @TC-4726 @TC-4733 @TC-4734 @federationOfflineMLS @resource=column-offline @WPB-3694
  Scenario Outline: I want to create a conversation with members from a reachable and an unreachable backend and see the members from unreachable backend could not be added
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And User <Member1> adds 1 2FA device
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumnOffline> adds 1 device
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on start a new conversation button
    And I tap create new group button
    And I see user <TeamOwnerColumn3> in search suggestions list
    And I see user <TeamOwnerColumnOffline> in search suggestions list
    And I select users <TeamOwnerColumn3>,<TeamOwnerColumnOffline>  in search suggestions list
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select users <Member1> in search suggestions list
    When Federator for backend column-offline-android is turned off
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    # TC-4733 - I want to see a system message explaining the user could not be added when I create an MLS group conversation with a user who's backend is unreachable
    # Bug: Group can't be created -> https://wearezeta.atlassian.net/browse/WPB-3694
    And I see group conversation "<ConversationName>" is in foreground
    Then I see system message "<TeamOwnerColumnOffline> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view
    # TC-4734 - I want see reachable members added when I create a group conversation with combination of reachable and unreachable users
    When I tap on group conversation title "<ConversationName>" to open group details
    And I tap on Participants tab
    Then I see user <Member1> in participants list
    And I see user <TeamOwnerColumn3> in participants list

    Examples:
      | TeamOwnerColumn1 | Member1   | TeamOwnerColumn3 | TeamOwnerColumnOffline | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName |
      | user1Name        | user2Name | user3Name        | user4Name              | user1Email | Team Column1     | Team Column3    | Team Offline          | FederatedGroup   |

  @TC-4723 @federationOfflineMLS @resource=column-offline @WPB-10721
  Scenario Outline: I want to continue sending and receiving messages on hosting backend in federated group with offline backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumnOffline> adds 1 device
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwnerColumn3>" in search field
    And I see user name "<TeamOwnerColumn3>" in Search result list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerColumnOffline>" in search field
    And I see user name "<TeamOwnerColumnOffline>" in Search result list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
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
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    And User <TeamOwnerColumnOffline> sends message "<Message2>" to group conversation <ConversationName>
    And User <TeamOwnerColumn3> sends message "<Message3>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumn3> sends message "<Message4>" to group conversation <ConversationName>
    And I see the message "<Message4>" in current conversation
    When I type the message "<Message5>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message5>" in current conversation
    #ToDo: Add steps below again after https://wearezeta.atlassian.net/browse/WPB-10721 is fixed
    #And I see participants will not receive your message error in conversation view
    #And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName | Message | Message2                   | Message3            | Message4                          | Message5                          |
      | user1Name        | user1Email |  user3Name       | user4Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad       | Hello!  | Hello from column offline! | Hello from column3! | Hello from column3 while offline! | Hello from column1 while offline! |

  ######################
  # Add/Remove User
  ######################

  @TC-4724 @TC-4725 @federationOfflineMLS @resource=column-offline
  Scenario Outline: I want to add members to a conversation containing members from an unreachable backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And User <Member1> adds 1 2FA device
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumnOffline> adds 1 device
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> is me
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
    And I type user name "<TeamOwnerColumn3>" in search field
    And I see user name "<TeamOwnerColumn3>" in Search result list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerColumnOffline>" in search field
    And I see user name "<TeamOwnerColumnOffline>" in Search result list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    When Federator for backend column-offline-android is turned off
    And I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select user <Member1> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member1> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You added <Member1> to the conversation" in conversation view
    When I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member1> in participants list
    # TC-4725 - I want to remove members from a conversation containing members from an unreachable backend
    When I tap on user <Member1> in participants list
    Then I see connected user <Member1> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member1> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member1> from the conversation" in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | Member1   | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName | Message | Message2                   | Message3            | Message4                          | Message5                          |
      | user1Name        | user1Email | user2Name |  user3Name       | user4Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad       | Hello!  | Hello from column offline! | Hello from column3! | Hello from column3 while offline! | Hello from column1 while offline! |

  ######################
  # Connection requests
  ######################

  @TC-4728 @TC-4729 @federationOfflineMLS @resource=column-offline
  Scenario Outline: I should not be able to accept a connection request from a user who's backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And User <TeamOwnerColumnOffline> adds 1 device
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumnOffline> sends connection request to <TeamOwnerColumn1>
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I see unread conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerColumnOffline>" in conversation list
    When Federator for backend column-offline-android is turned off
    And I tap on unread conversation name "<TeamOwnerColumnOffline>" in conversation list
    And I see unconnected user <TeamOwnerColumnOffline> profile
    And I tap accept button on unconnected user profile page
    Then I see toast message "Connection request could not be accepted" in user profile screen
    And I tap close button on unconnected user profile page
    # TC-4729 - I want to accept a connection request from a user who was unreachable and is available again
    When Federator for backend column-offline-android is turned on
    And I tap on unread conversation name "<TeamOwnerColumnOffline>" in conversation list
    And I see unconnected user <TeamOwnerColumnOffline> profile
    # Waiting for app to refresh the connection request
    And I wait for 3 seconds
    And I tap accept button on unconnected user profile page
    Then I see toast message "Connection request accepted" in user profile screen

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline | Subtitle         |
      | user1Name        | user1Email | user2Name              | Team Column1     | Team Offline          | Wants to connect |

  ######################
  # Non Fully Connected Graph
  ######################

  @TC-4730 @federationOfflineMLS @resource=column-offline @WPB-7143
  Scenario Outline: I should not be able to create a conversation with 2 other domains that do not federate with each other
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerExternal> configures MLS for team "<TeamNameExternal>"
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> is me
    And User <TeamOwnerColumn1> adds 1 2FA device
    And User <TeamOwnerExternal> adds 1 device
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwnerColumn1>" in search field
    And I see user name "<TeamOwnerColumn1>" in Search result list
    And I select user <TeamOwnerColumn1> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerExternal>" in search field
    And I see user name "<TeamOwnerExternal>" in Search result list
    And I select user <TeamOwnerExternal> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<Group>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see alert informing me that group can not be created
    And I see Learn more link in the can not create group alert
    And I see Edit Participants List button
    And I see Discard Group Creation button

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group          |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup |

  ######################
  # Calling
  ######################

  @TC-4727 @federationOfflineMLS @resource=column-offline
  Scenario Outline: I want to initiate a call in a conversation which contains members from an unreachable backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> configures MLS for team "<TeamNameColumnOffline>"
    And <TeamOwnerColumn3> starts 2FA instance using chrome
    And <TeamOwnerColumnOffline> starts instance using chrome
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> is me
    And <TeamOwnerColumn3> accept next incoming call automatically
    And I see Welcome Page
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwnerColumn3>" in search field
    And I see user name "<TeamOwnerColumn3>" in Search result list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<TeamOwnerColumnOffline>" in search field
    And I see user name "<TeamOwnerColumnOffline>" in Search result list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    When Federator for backend column-offline-android is turned off
    And I tap start call button
    Then I see ongoing group call
    And <TeamOwnerColumn3> verifies that waiting instance status is changed to active in 60 seconds
    And I see users <TeamOwnerColumn3> in ongoing group call
    And I do not see user <TeamOwnerColumnOffline> in ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad       |

  @TC-4713
  Scenario Outline: I want to see VS-NFD banner during MLS call on classified backend
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And User <TeamOwnerColumn1> is me
    And <Member1> starts 2FA instance using chrome
    And <Member1> accepts next incoming call automatically
    And I see Welcome Page
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
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
    And I see classified domain label with text "<textClassified>" in the conversation view
    When I tap start call button
    Then I see ongoing group call
    And I see classified domain label with text "<textClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I see users <Member1> in ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | Member1   | TeamNameColumn1 | ConversationName | textClassified         | textNonClassified            |
      | user1Name        | user1Email | user2Name | Column1         | MLS              | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4714
  Scenario Outline: I want to see security level: unclassified during MLS call with unclassified backend
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> configures MLS for team "<TeamNameColumn1>"
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And User <TeamOwnerColumn3> configures MLS for team "<TeamNameColumn3>"
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>
    And User <TeamOwnerColumn1> is me
    And <TeamOwnerColumn3> starts 2FA instance using chrome
    And <TeamOwnerColumn3> accepts next incoming call automatically
    And I see Welcome Page
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwnerColumn3>" in search field
    And I see user name "<TeamOwnerColumn3>" in Search result list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I see classified domain label with text "<textNonClassified>" in the conversation view
    When I tap start call button
    Then I see ongoing group call
    And I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see users <TeamOwnerColumn3> in ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamNameColumn1 | TeamNameColumn3 | ConversationName | textClassified         | textNonClassified            |
      | user1Name        | user1Email | user2Name        | Column1         | Column3         | MLS              | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |