Feature: Federation Non Fully Connected Graph

  @TC-4223 @TC-4233 @TC-4227 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I should not be able to create a group with both column 1 and bund-external as a column 3 user
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
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
    And I see user <TeamOwnerColumn1> in search suggestions list
    And I see user <TeamOwnerExternal> in search suggestions list
    And I select users <TeamOwnerColumn1>,<TeamOwnerExternal> in search suggestions list
    And I tap Continue button on add participants page
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<Group>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see alert informing me that group can not be created
    # TC-4227 - I want to see an alert when I try to create a group with users from column 1 and bund-external as a column 3 user
    And I see explanation that users can not join the same conversation in can not create group alert
    And I see Edit Participants List button
    And I see Discard Group Creation button
    And I see Learn more link in the can not create group alert
    #  TC-4233	I want to see information page open when tapping learn more on group creation alert when group could not be created because not all participants federate with each other
    When I tap on Learn more link on group creation page alert
    Then I see the Wire app is not in foreground

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group          |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup |

  @TC-4224 @TC-4232 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I should not be able to add a column 1 user to a group with bund-external users on column 3
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> has group conversation <Group> with <TeamOwnerExternal> in team "<TeamNameColumn3>"
    And User <TeamOwnerColumn3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn1>" in conversation list
    And I see conversation "<TeamOwnerExternal>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I see user <TeamOwnerColumn1> in search suggestions list
    And I select user <TeamOwnerColumn1> in search suggestions list
    And I tap Continue button on add participants page
    Then I do not see user <TeamOwnerColumn1> in participants list
    When I close the group conversation details through X icon
    Then I see system message "<TeamOwnerColumn1> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view
    # TC-4232 - I want to see information page open when tapping learn more on system message showing user could not be added	Medium
    When I tap Learn more link in conversation view
    #ToDo: Add check for URL once correct page is added -> currently we see zendesk login page
    Then I see the Wire app is not in foreground

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group            |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup   |

  @TC-4225 @TC-4228 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I should not be able to add a bund-external user to a group with column 1 users on column 3
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> has group conversation <Group> with <TeamOwnerColumn1> in team "<TeamNameColumn3>"
    And User <TeamOwnerColumn3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation list
    And I see conversation "<Group>" in conversation list
    And I see conversation "<TeamOwnerColumn1>" in conversation list
    And I see conversation "<TeamOwnerExternal>" in conversation list
    And I tap on conversation name "<Group>" in conversation list
    And I see group conversation "<Group>" is in foreground
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I see user <TeamOwnerExternal> in search suggestions list
    And I select user <TeamOwnerExternal> in search suggestions list
    And I tap Continue button on add participants page
    Then I do not see user <TeamOwnerExternal> in participants list
    # TC-4228 - I want to see a system message when I try to add bund-external user to a group with users from column 1 as a column 3 user
    When I close the group conversation details through X icon
    Then I see system message "<TeamOwnerExternal> could not be added to the group." in conversation view
    And I see Learn more link is displayed in conversation view

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group            |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup   |

  @TC-4226 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I should not be able to find bund-external user if I am a user on column 1
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
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
    And I search user <TeamOwnerExternal> by handle and domain in Search UI input field
    Then I do not see user name "<TeamOwnerExternal>" in Search result list

    Examples:
      | TeamOwnerColumn1 | Email      | TeamOwnerExternal | TeamNameColumn1  | TeamNameExternal |
      | user1Name        | user1Email | user3Name         | Team Column1     | Team External    |

  @TC-4231 @federation @federationNFCG @col1
  Scenario Outline: I should not be able to find column 1 user if I am a user on bund-external
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerExternal> is me
    And I see Welcome Page
    And I open external backend deep link
    And I see alert informing me that I am about to switch to external backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation list
    And I tap on start a new conversation button
    When I tap on search people field
    And I search user <TeamOwnerExternal> by handle and domain in Search UI input field
    Then I do not see user name "<TeamOwnerColumn1>" in Search result list

    Examples:
      | TeamOwnerColumn1 | TeamOwnerExternal | TeamNameColumn1  | TeamNameExternal |
      | user1Name        | user3Name         | Team Column1     | Team External    |

  @TC-4229 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I want to add a bund-external user to a conversation that previously had a column 1 user participating
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> has group conversation <Group> with <TeamOwnerColumn1> in team "<TeamNameColumn3>"
    And User <TeamOwnerColumn3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
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
    When User <TeamOwnerColumn3> removes user <TeamOwnerColumn1> from group conversation "<Group>"
    And I see system message "You removed <TeamOwnerColumn1> from the conversation" in conversation view
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I see user <TeamOwnerExternal> in search suggestions list
    And I select user <TeamOwnerExternal> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <TeamOwnerExternal> in participants list

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group            |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup   |

  @TC-4230 @federation @federationNFCG @resource=column-offline @col1
  Scenario Outline: I want to add a column 1 user to a conversation that previously had a bund-external user participating
    Given There is a team owner "<TeamOwnerColumn1>" with team "<TeamNameColumn1>" on column-1 backend
    And There is a team owner "<TeamOwnerColumn3>" with team "<TeamNameColumn3>" on column-3 backend
    And There is a team owner "<TeamOwnerExternal>" with team "<TeamNameExternal>" on external backend
    And User <TeamOwnerColumn3> is connected to <TeamOwnerColumn1>,<TeamOwnerExternal>
    And User <TeamOwnerColumn3> has group conversation <Group> with <TeamOwnerExternal> in team "<TeamNameColumn3>"
    And User <TeamOwnerColumn3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I see alert informing me that I am about to switch to column-3 backend
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
    When User <TeamOwnerColumn3> removes user <TeamOwnerExternal> from group conversation "<Group>"
    And I see system message "You removed <TeamOwnerExternal> from the conversation" in conversation view
    And I tap on group conversation title "<Group>" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    And I see user <TeamOwnerColumn1> in search suggestions list
    And I select user <TeamOwnerColumn1> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <TeamOwnerColumn1> in participants list

    Examples:
      | TeamOwnerColumn1 | TeamOwnerColumn3 | TeamOwnerExternal | Email      | TeamNameColumn1  | TeamNameColumn3 | TeamNameExternal | Group            |
      | user1Name        | user2Name        | user3Name         | user2Email | Team Column1     | Team Column3    | Team External    | FederatedGroup   |
