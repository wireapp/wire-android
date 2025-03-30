Feature: Federation Offline

  ######################
  # Login
  ######################

  @TC-4155 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to login when I have a group conversation with a user who’s backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    When Federator for backend column-offline-android is turned off
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4154 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to login when I have a 1:1 conversation with a user who’s backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has 1:1 conversation with <TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    When Federator for backend column-offline-android is turned off
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user1Email | user2Name              | Team Column1     | Team Offline          |

  @TC-4156 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to login when I have an outgoing connection request with a user who is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> sends connection request to <TeamOwnerColumnOffline>
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    When Federator for backend column-offline-android is turned off
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" is having pending status in conversation list

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user1Email | user3Name              | Team Column1     | Team Offline          |

  @TC-4157 @federation @federationOffline @WPB-4814 @resource=column-offline @col1
  Scenario Outline: I want to login when I have an incoming connection request from a user who is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumnOffline> sends connection request to <TeamOwnerColumn1>
    And I open column-1 backend deep link
    And I see alert informing me that I am about to switch to column-1 backend
    And I tap proceed button on custom backend alert
    When Federator for backend column-offline-android is turned off
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" is having pending status in conversation list
    # ToDo: Add steps below once https://wearezeta.atlassian.net/browse/WPB-4814 is fixed
#    When I open the notification center
#    Then I do not see the message "Wants to connect" from 1:1 conversation from user <TeamOwnerColumnOffline> in the notification center

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user1Email | user2Name              | Team Column1     | Team Offline          |

  @TC-4158 @federation @federationOffline @resource=column-offline @col1
    # Bund-offline Backends do not have 2FA set up
  Scenario Outline: I want to login to my backend when my federator is turned off
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumnOffline> is me
    And I see Welcome Page
    And I open column-offline-android backend deep link
    And I see alert informing me that I am about to switch to column-offline-android backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    Then I see conversation list

    Examples:
      | TeamOwnerColumnOffline | TeamNameColumnOffline |
      | user1Name              | Team Offline          |

  ######################
  # Connection requests
  ######################

  @TC-4160 @TC-4162 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I should not be able to accept a connection request from a user of a currently unreachable backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumnOffline> sends connection request to <TeamOwnerColumn1>
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
    And I see unread conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerColumnOffline>" in conversation list
    And Federator for backend column-offline-android is turned off
    And I tap on unread conversation name "<TeamOwnerColumnOffline>" in conversation list
    When I tap accept button on unconnected user profile page
    Then I see toast message "Connection request could not be accepted" in user profile screen
    # TC-4162 - I want to ignore a connection request from a user of a currently unreachable backend
    When I tap ignore button on unconnected user profile page
    Then I see toast message containing "You ignored" on conversation list
    And I see conversation list

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline | Subtitle         |
      | user1Name        | user1Email | user2Name              | Team Column1     | Team Offline          | Wants to connect |

  @TC-4163 @federation @federationOffline @resource=column-offline @col1 @WPB-6247
  Scenario Outline: I want to be able to accept a connection request from a user whos backend was unreachable and is now again reachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumnOffline> sends connection request to <TeamOwnerColumn1>
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
    And I see unread conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerColumnOffline>" in conversation list
    And Federator for backend column-offline-android is turned off
    And I tap on unread conversation name "<TeamOwnerColumnOffline>" in conversation list
    When I tap accept button on unconnected user profile page
    Then I see toast message "Connection request could not be accepted" in user profile screen
    When I terminate Wire
    And Federator for backend column-offline-android is turned on
    And I restart Wire
    And I wait until the notification popup disappears
    And I see unread conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerColumnOffline>" in conversation list
    And I tap on unread conversation name "<TeamOwnerColumnOffline>" in conversation list
    And I tap accept button on unconnected user profile page
    Then I see toast message "Connection request accepted" in user profile screen

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline | Subtitle         |
      | user1Name        | user1Email | user2Name              | Team Column1     | Team Offline          | Wants to connect |

  @TC-4161 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see my outgoing connection request sent to a user who became unavailable after sending the connection request
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> sends connection request to <TeamOwnerColumnOffline>
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
    And I see conversation "<TeamOwnerColumnOffline>" is having pending status in conversation list
    When Federator for backend column-offline-android is turned off
    And I swipe the app away from background
    And I restart Wire
    Then I see conversation "<TeamOwnerColumnOffline>" is having pending status in conversation list

    Examples:
      | TeamOwnerColumn1 | Email      |  TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user1Email | user3Name               | Team Column1     | Team Offline          |

  @TC-4159 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I should not be able to send a connection request to a user that was cached in search but on unreachable backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
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
    When I tap on search people field
    And I search user <TeamOwnerColumnOffline> by handle and domain in Search UI input field
    Then I see user name "<TeamOwnerColumnOffline>" in Search result list
    And I tap on user name <TeamOwnerColumnOffline> found on search page
    And I see username "<TeamOwnerColumnOffline>" on unconnected user profile page
    When Federator for backend column-offline-android is turned off
    And I tap connect button on unconnected user profile page
    Then I see toast message "Connection request could not be sent" in user profile screen

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumnOffline | Email      | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user2Name              | user1Email | Team Column1     | Team Offline          |

  ######################
  # Block User
  ######################

  @TC-4164 @TC-4165 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I should no be able to unblock a user from another backend when his backend is not reachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumnOffline>
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
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I tap on conversation name "<TeamOwnerColumnOffline>" in conversation list
    And I open conversation details for 1:1 conversation with "<TeamOwnerColumnOffline>"
    And I tap show more options button on user profile screen
    And I tap on Block option
    And I tap Block button on alert
    And I see toast message "<TeamOwnerColumnOffline> blocked" in user profile screen
    And I see Blocked label
    When Federator for backend column-offline-android is turned off
    And I tap show more options button on user profile screen
    And I tap on Unblock option
    And I tap Unblock button alert
    Then I see toast message "User could not be unblocked" in user profile screen
    And I see Blocked label
    And I see Unblock User button
    # TC-4165 - I want to unblock a user who was unreachable but is reachable again
    When Federator for backend column-offline-android is turned on
    And I tap show more options button on user profile screen
    And I tap on Unblock option
    And I tap Unblock button alert
    And I do not see Blocked label
    And I do not see Unblock User button

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumnOffline | Email      | TeamNameColumn1  | TeamNameColumnOffline |
      | user1Name        | user2Name              | user1Email | Team Column1     | Team Offline          |

  ######################
  # Creating Conversations
  ######################

  @TC-4166 @TC-4167 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see a system message explaining the user could not be added when I create a group conversation with a user who's backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
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
    And I see user <Member1> in search suggestions list
    And I see user <TeamOwnerColumn3> in search suggestions list
    And I see user <TeamOwnerColumnOffline> in search suggestions list
    When Federator for backend column-offline-android is turned off
    And I select users <Member1>,<TeamOwnerColumn3>,<TeamOwnerColumnOffline>  in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    Then I see system message "<TeamOwnerColumnOffline> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view
    # TC-4167 - I want see reachable members added when I create a group conversation with combination of reachable and unreachable users
    When I tap on group conversation title "<ConversationName>" to open group details
    And I tap on Participants tab
    Then I see user <Member1> in participants list
    And I see user <TeamOwnerColumn3> in participants list

    Examples:
      | TeamOwnerColumn1 | Member1   | TeamOwnerColumn3 | TeamOwnerColumnOffline | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | ConversationName |
      | user1Name        | user2Name | user3Name        | user4Name              | user1Email | Team Column1     | Team Column3    | Team Offline          | FederatedGroup   |

  @TC-4169 @TC-4171 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see system message explaining users could not be added when I create a group conversation with only offline users
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> adds users <Member1> to team "<TeamNameColumnOffline>" with role Member
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumnOffline>,<Member1>
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
    And I see user <Member1> in search suggestions list
    And I see user <TeamOwnerColumnOffline> in search suggestions list
    When Federator for backend column-offline-android is turned off
    And I select users <Member1>,<TeamOwnerColumnOffline>  in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    Then I see system message "2 participants could not be added to the group." in conversation view
    When I tap Show All button
    Then I see system message "<Member1> and <TeamOwnerColumnOffline> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view
    #  TC-4171	I want to see information page open when I tap Learn More on system message when the backend of an added participant is unreachable
    When I tap Learn more link in conversation view
    #ToDo: Add check for URL once correct page is added -> currently we see zendesk login page
    Then I see the Wire app is not in foreground

    Examples:
      | TeamOwnerColumn1 | Member1   | TeamOwnerColumnOffline | Email      | TeamNameColumn1  | TeamNameColumnOffline | ConversationName |
      | user1Name        | user2Name | user4Name              | user1Email | Team Column1     | Team Offline          | FederatedGroup   |

  @TC-4170 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I should not see unreachable users in group creation flow when selecting users after fresh install
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> adds users <Member1> to team "<TeamNameColumnOffline>" with role Member
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>,<Member1>
    When Federator for backend column-offline-android is turned off
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
    And I see user <TeamOwnerColumn3> in search suggestions list
    And I do not see user <Member1> in search suggestions list
    And I do not see user <TeamOwnerColumnOffline> in search suggestions list

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerColumnOffline | Member1   | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline |
      | user1Name        | user2Name        | user3Name              | user4Name | user1Email | Team Column1     | Team Column3    | Team Offline          |

#  ToDO: TC-4168 - I want to see “username unavailable” when trying to add offline user to the group ?	No	Medium

  ######################
  # Adding / Removing participants
  ######################

  @TC-4172 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see an error system message when I add a user to a group conversation who's backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    When Federator for backend column-offline-android is turned off
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I see user <TeamOwnerColumnOffline> in search suggestions list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
    And I tap Continue button on add participants page
    Then I do not see user <TeamOwnerColumnOffline> in participants list
    When I close the group conversation details through X icon
    Then I see system message "<TeamOwnerColumnOffline> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email |  user2Name       | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4173 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to add users who are reachable to a group conversation when I select multiple users of which one has an unreachable backend
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <Member1> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    When Federator for backend column-offline-android is turned off
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I see user <TeamOwnerColumnOffline> in search suggestions list
    And I select user <TeamOwnerColumnOffline> in search suggestions list
    And I see user <TeamOwnerColumn3> in search suggestions list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <TeamOwnerColumn3> in participants list
    And I do not see user <TeamOwnerColumnOffline> in participants list
    When I close the group conversation details through X icon
    Then I see system message "<TeamOwnerColumnOffline> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | Member1   | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name | user3Name        | user4Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4174 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to remove a user from a conversation whos backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <TeamOwnerColumnOffline> in participants list
    When I tap on user <TeamOwnerColumnOffline> in participants list
    And I see connected user <TeamOwnerColumnOffline> profile
    And Federator for backend column-offline-android is turned off
    And I scroll to the bottom of user profile page
    And I see remove from group button
    And I tap remove from group button
    And I see alert asking me if I want to remove user <TeamOwnerColumnOffline> from group
    And I tap remove button on alert
    And I do not see remove from group button
    And I close the user profile through the close button
    And I do not see user <TeamOwnerColumnOffline> in participants list
    And I close the group conversation details through X icon
    Then I see system message "You removed <TeamOwnerColumnOffline> from the conversation" in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email |  user2Name       | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4177 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to add users who are reachable to a group conversation when I also select users from unreachable backend and one user from unreachable backend is already in the group
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> adds users <MemberOffline> to team "<TeamNameColumnOffline>" with role Member
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>,<MemberOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <Member1>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see conversation "<MemberOffline>" in conversation list
    When Federator for backend column-offline-android is turned off
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I see user <MemberOffline> in search suggestions list
    And I select user <MemberOffline> in search suggestions list
    And I see user <TeamOwnerColumn3> in search suggestions list
    And I select user <TeamOwnerColumn3> in search suggestions list
    And I tap Continue button on add participants page
    Then I do not see user <TeamOwnerColumn3> in participants list
    And I do not see user <MemberOffline> in participants list
    When I close the group conversation details through X icon
    Then I see system message "2 participants could not be added to the group." in conversation view
    And I see Show All button
    When I tap Show All button
    Then I see system message "<MemberOffline> and <TeamOwnerColumn3> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | Member1   | TeamOwnerColumn3 | TeamOwnerColumnOffline | MemberOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name | user3Name        | user4Name              | user5Name     | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4175 @TC-4176 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to receive any messages up to the point that I was removed from a conversation if my backend was unreachable while I was being removed
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> adds 1 2FA device
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And User <TeamOwnerColumnOffline> is me
    And I see Welcome Page
    And I open column-offline-android backend deep link
    And I see alert informing me that I am about to switch to column-offline-android backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation list
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    And User <TeamOwnerColumn1> sends message "<Message2>" to group conversation <Group>
    And User <TeamOwnerColumn3> sends message "<Message3>" to group conversation <Group>
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumn1> sends message "<Message4>" to group conversation <Group>
    And User <TeamOwnerColumn3> sends message "<Message5>" to group conversation <Group>
    And User <TeamOwnerColumn1> removes user <TeamOwnerColumnOffline> from group conversation "<Group>"
    And User <TeamOwnerColumn1> sends message "<Message6>" to group conversation <Group>
    And User <TeamOwnerColumn3> sends message "<Message7>" to group conversation <Group>
    And Federator for backend column-offline-android is turned on
    And I wait for 12 seconds
    And I see the message "<Message4>" in current conversation
    And I see the message "<Message5>" in current conversation
    And I see system message "<TeamOwnerColumn1> removed you from the conversation" in conversation view
    # TC-4176 - I should not see group conversation messages sent after I was removed from the conversation while my backend was unreachable
    And I do not see the message "<Message6>" in current conversation
    And I do not see the message "<Message7>" in current conversation

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2            | Message3            | Message4                          | Message5                          | Message6                                   | Message7                                   |
      | user1Name        | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | Hello from column1! | Hello from column3! | Hello from column1 while offline! | Hello from column3 while offline! | Hello from column1 after user was removed! | Hello from column3 after user was removed! |

  ######################
  # Messaging
  ######################

  @TC-4179 @TC-4182 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to receive messages in a group conversation of which one user is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And User <TeamOwnerColumn1> adds users <Member1> to team "<TeamNameColumn1>" with role Member
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> adds 1 2FA device
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <Member1>,<TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And User <Member1> is me
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    And User <TeamOwnerColumn1> sends message "<Message2>" to group conversation <Group>
    And User <TeamOwnerColumn3> sends message "<Message3>" to group conversation <Group>
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumn1> sends message "<Message4>" to group conversation <Group>
    And User <TeamOwnerColumn3> sends message "<Message5>" to group conversation <Group>
    And I see the message "<Message4>" in current conversation
    And I see the message "<Message5>" in current conversation
    # TC-4182 - I want to send messages in a group conversation of which one user is unreachable
    When I type the message "<Message6>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message6>" in current conversation
    And I see participants will not receive your message error in conversation view
    And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | Member1       | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2            | Message3            | Message4                          | Message5                          | Message6                              |
      | user1Name        | user2Name     | user2Email |  user3Name        | user4Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | Hello from column1! | Hello from column3! | Hello from column1 while offline! | Hello from column3 while offline! | I am sending a message while offline! |

  @TC-4178 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see ephemeral messages disappear after the timer expired when sending backend has become unreachable after sending
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And User <TeamOwnerColumnOffline> adds a new device Device1 with label Device1
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn3>" in conversation list
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <TeamOwnerColumnOffline> sends ephemeral message "<Message2>" with timer 10 seconds via device Device1 to conversation <Group>
    And Federator for backend column-offline-android is turned off
    Then I see the message "<Message2>" in current conversation
    When I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2              |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | This is self deleting |

  @TC-4184 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see ephemeral messages disappear before the timer expired when the message was removed while sending backend was unreachable and became reachable again
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And User <TeamOwnerColumnOffline> adds a new device Device1 with label Device1
    And User <TeamOwnerColumnOffline> is connected to <TeamOwnerColumn1>,<TeamOwnerColumn3>
    And User <TeamOwnerColumnOffline> has group conversation <Group> with <TeamOwnerColumn1>,<TeamOwnerColumn3> in team "<TeamNameColumnOffline>"
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <TeamOwnerColumnOffline> sends ephemeral message "<Message2>" with timer 5 minutes via device Device1 to conversation <Group>
    And Federator for backend column-offline-android is turned off
    And I see the message "<Message2>" in current conversation
    And User <TeamOwnerColumnOffline> deletes the recent message everywhere from group conversation <Group> via device Device1
    And I see the message "<Message2>" in current conversation
    And Federator for backend column-offline-android is turned on
    Then I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2              |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | This is self deleting |

  @TC-4180 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to delete a message in a conversation while one of the participants is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When Federator for backend column-offline-android is turned off
    And I long tap on the message "<Message>" in current conversation
    And I tap delete button
    And I tap delete for everyone button
    Then I see deleted label
    And I do not see the message "<Message>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  |

  @TC-4181 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see conversation hosted on A, and User on backend B deletes message for everyone while backend is unreachable to backend A
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> adds a new device Device1 with label Device1
    And User <TeamOwnerColumnOffline> is connected to <TeamOwnerColumn1>,<TeamOwnerColumn3>
    And User <TeamOwnerColumnOffline> has group conversation <Group> with <TeamOwnerColumn1>,<TeamOwnerColumn3> in team "<TeamNameColumnOffline>"
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    When User <TeamOwnerColumnOffline> sends message "<Message>" to group conversation <Group>
    And I see the message "<Message>" in current conversation
    And Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumnOffline> deletes the recent message everywhere from group conversation <Group> via device Device1
    And I see the message "<Message>" in current conversation
    And Federator for backend column-offline-android is turned on
    Then I do not see the message "<Message>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  |

  @TC-4183 @TC-4185 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to see a retry button on message that failed to send because the owning backend is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumnOffline> adds a new device Device1 with label Device1
    And User <TeamOwnerColumn1> is me
    And I see Welcome Page
    And User <TeamOwnerColumnOffline> is connected to <TeamOwnerColumn1>,<TeamOwnerColumn3>
    And User <TeamOwnerColumnOffline> has group conversation <Group> with <TeamOwnerColumn1>,<TeamOwnerColumn3> in team "<TeamNameColumnOffline>"
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And User <TeamOwnerColumnOffline> sends message "<Message>" to group conversation <Group>
    And I see the message "<Message>" in current conversation
    When Federator for backend column-offline-android is turned off
    And I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see Retry button in current conversation
    And I see message could not be sent due to backends not reachable error in conversation view
    And I see Learn more link is displayed in conversation view
    # TC-4185 - I want to retry sending a message which failed to send because the owning backend is unreachable
    When Federator for backend column-offline-android is turned on
    And I wait for 5 seconds
    And I tap on Retry button in current conversation
    Then I see the message "<Message2>" in current conversation
    And I do not see system message "Message could not be sent, as the backend of" in conversation view
    And I do not see system message "could not be reached." in conversation view

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2           |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | Hello to you, too! |

  ######################
  # Calling
  ######################

  @TC-4187 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to start a group call, when I am in a group with 2 other users and only one of them is unreachable
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And <TeamOwnerColumn3> starts 2FA instance using chrome
    And User <TeamOwnerColumn1> is me
    And <TeamOwnerColumn3> accept next incoming call automatically
    And I see Welcome Page
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    When Federator for backend column-offline-android is turned off
    And I tap start call button
    Then I see ongoing group call
    And <TeamOwnerColumn3> verifies that waiting instance status is changed to active in 60 seconds
    And I see users <TeamOwnerColumn3> in ongoing group call
    And I do not see user <TeamOwnerColumnOffline> in ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4188 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to accept an incoming group call from a reachable user in a group which contains an unreachable user
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And <TeamOwnerColumn3> starts 2FA instance using chrome
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    #And <TeamOwnerColumnOffline> starts instance using chrome
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    When Federator for backend column-offline-android is turned off
    And User <TeamOwnerColumn3> calls <Group>
    And I see incoming group call from group <Group>
    And I accept the call
    Then I see ongoing group call
    And I see users <TeamOwnerColumn3> in ongoing group call
    And I do not see user <TeamOwnerColumnOffline> in ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

  @TC-4190 @notWorkingYet @resource=column-offline
    #Test not working, because bund-offline can not accept calls
  Scenario Outline: I want to remain in a group call of which one participant became unreachable during the call
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And <TeamOwnerColumn3> starts 2FA instance using chrome
    And <TeamOwnerColumnOffline> starts instance using chrome
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And User <TeamOwnerColumn1> is me
    And <TeamOwnerColumn3>,<TeamOwnerColumnOffline> accept next incoming call automatically
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
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap start call button
    And I see ongoing group call
    And <TeamOwnerColumn3>,<TeamOwnerColumnOffline> verify that waiting instance status is changed to active in 90 seconds
    And I see users <TeamOwnerColumn3> in ongoing group call
    And I see user <TeamOwnerColumnOffline> in ongoing group call
    When Federator for backend column-offline-android is turned off
    And I wait for 10 seconds
    Then I do not see user <TeamOwnerColumnOffline> in ongoing group call
    But I see ongoing group call

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      |
      | user1Name        | user1Email | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad |

#  ToDO: TC-4186	I want to be able to attempt a 1:1 call with a user who’s backend is unreachable	No	Low
#  ToDO: TC-4189	I want to remain in a call with a user whom their backend got unreachable during the call, until it degrades	No	Low

  ######################
  # Getting online after being offline
  ######################

#  TC-4191	I want to receive messages that I missed while I was unreachable when I am reachable again in a 1on1 conversation	No	Low
#  TC-4192	I want to be able to send and receive new messages after I was unreachable when I am reachable again in a 1on1 conversation	No	Medium
#  TC-4193	I want to receive messages that I missed while I was unreachable when I am reachable again in a group conversation	No	Medium
#  TC-4194	I want to be able to send and receive new messages after I was unreachable when I am online again in a group conversation	No	Medium
#  TC-4195	I want to see username for a 1on1 conversation if the user from that conversation was unreachable and is reachable again	No	Medium
#  TC-4196	I want to see username displayed in participants list of a group conversation if the user was unreachable and is reachable again	No	Medium
#  TC-4197	I want to see user information being updated if I open the user profile of a user who was unreachable and is now reachable again and changes their user details while they were unreachable	No	Medium
#  TC-4198	I want to see a system message of users being removed from a group conversation while I was unreachable when I am reachable again	No	Medium
#  TC-4199	I want to see a system message of users being added to a group conversation while I was unreachable when I am reachable again	No	Medium
#  TC-4200	I should not see a pending connection request when I had an outgoing connection request which was accepted while I was offline when I am online again	No	Medium
#  TC-4201	I want to see missed calls which I received while I was offline when I am online again	No	Low
#  TC-4202	I want to see messages being removed that were removed while I could not reach the backend	No	Medium
#  TC-4203	I want to see updated user information if I open the user profile of a user who changed their user details while I was unreachable 	No	Medium

  ######################
  # invisible messages
  ######################

#  TC-4204	I want to see read receipts sync while backend is unreachable ?	No	Medium
#  TC-4205	I want to see the timer of ephemeral messages sync between clients while conversation owning backend is unreachable 	No	Medium

  ######################
  # Backup
  ######################

  @TC-4207 @federation @federationOffline @resource=column-offline @col1
  Scenario Outline: I want to import a backup that I exported when I had 1:1 conversations to backend that is now offline
    Given Federator for backend column-offline-android is turned on
    And I wait until the federator pod on column-offline-android is available
    And There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerColumnOffline>" with team "<TeamNameColumnOffline>" on column-offline-android backend
    And User <TeamOwnerColumn3> adds 1 2FA device
    And User <TeamOwnerColumnOffline> adds a new device Device1 with label Device1
    And User <TeamOwnerColumn1> is connected to <TeamOwnerColumn3>,<TeamOwnerColumnOffline>
    And User <TeamOwnerColumn1> has group conversation <Group> with <TeamOwnerColumn3>,<TeamOwnerColumnOffline> in team "<TeamNameColumn1>"
    And User <TeamOwnerColumn1> is me
    And User <TeamOwnerColumn1> sets their unique username
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
    And I see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And User <TeamOwnerColumnOffline> sends message "<Message>" to group conversation <Group>
    And I see the message "<Message>" in current conversation
    And User <TeamOwnerColumn3> sends message "<Message2>" to group conversation <Group>
    And I see the message "<Message2>" in current conversation
    And I type the message "<Message3>" into text input field
    And I tap send button
    And I see the message "<Message3>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on conversation name "<TeamOwnerColumnOffline>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <TeamOwnerColumnOffline> sends message "<Message2>" to User Myself
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I see Backup Page
    And I tap on Create a Backup button
    And I tap on Back Up Now button
    And I wait until I see the message "Conversations successfully saved" in backup alert
    And I tap on Save File button in backup alert
    And I tap on Save button in DocumentsUI
    And I see Backup Page
    And I tap back button 2 times
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "Delete all your personal information and conversations on this device" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    And I see Welcome Page
    When Federator for backend column-offline-android is turned off
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<Group>" in conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I do not see the message "<Message>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open the Back up & Restore Conversations menu
    And I tap on Restore from Backup button
    And I tap on Choose Backup button
    And I select backup file with name containing "WBX-<UniqueUsername>" in DocumentsUI
    Then I wait until I see the message "Conversations have been restored" in backup alert
    And I tap OK button on the alert
    And I see conversation list
    And I do not see conversation "<TeamOwnerColumnOffline>" in conversation list
    And I see conversation "<Group>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwnerColumn1 | Email      | UniqueUsername      | TeamOwnerColumn3 | TeamOwnerColumnOffline | TeamNameColumn1  | TeamNameColumn3 | TeamNameColumnOffline | Group      | Message | Message2           | Message3  |
      | user1Name        | user1Email | user1UniqueUsername | user2Name        | user3Name              | Team Column1     | Team Column3    | Team Offline          | FruitSalad | Hello!  | Hello to you, too! | Good Day. |

#ToDO:
#  TC-4206	I want to import a backup that I exported when I had connection requests to backend that is now offline	Yes	Medium
#  TC-4208	I want to import a backup that I exported when I had group conversations to backend that is now offline	Yes	Medium
#  TC-4209	I want to import a backup while I have a connection requests to backend that is now offline, but was online when I exported the backup	No	Medium
#  TC-4210	I want to import a backup while I have a 1:1 conversation with a backend that is now offline, but was online when I exported the backup	No	Medium
#  TC-4211	I want to import a backup while I have a group conversation with a backend that is now offline, but was online when I exported the backup	No	Medium
#  TC-4212	I want to import a backup while I have a connection requests to backend that is now offline, and was offline when I exported the backup	Yes	Medium
#  TC-4213	I want to import a backup while I have a 1:1 conversation with a backend that is now offline, and was offline when I exported the backup	Yes	Medium
#  TC-4214	I want to import a backup while I have a group conversation with a backend that is now offline, and was offline when I exported the backup	Yes	Medium
#  TC-4215	I want to import a backup while I have a 1:1 conversation with a backend that is now online, but was offline when I exported the backup	Yes	Medium
#  TC-4216	I want to import a backup while I have a group conversation with a backend that is now online, but was offline when I exported the backup	Yes	Medium
#  TC-4217	I want to import a backup while I have a connection requests to backend that is now online, but was offline when I exported the backup	Yes	Medium
#  TC-4218	I want to import a backup that I exported when I had blocked connected user to backend that is now offline	No	Medium
#  TC-4219	I want to send message in 1:1 conversation to backend which is online after importing	No	Medium
#  TC-4220	I want to send message in group conversation to backend which is online after importing	No	Medium
#  TC-4221	I want to initiate a audio/video call in 1:1 conversation to backend which is online after importing	No	Medium
#  TC-4222	I want to initiate a audio/video call in group conversation to backend which is online after importing	No	Medium
