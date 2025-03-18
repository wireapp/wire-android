@E2EI
Feature: E2EI

  @TC-8328
  Scenario Outline: I should not get a certificate if ACME server rejects identifier because user does not match
    Given There is a team owner "<Owner>" with team "MLS"
    And User <Owner> adds users <Member1>,<Member2> to team "MLS" with role Member
    And User <Owner> adds users <Member1>,<Member2> to keycloak for E2EI
    And User <Owner> configures MLS for team "MLS"
    And Admin user <Owner> enables E2EI with ACME server for team "MLS"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    When I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I wait for 10 seconds
    When I enter email <Member2Email> on keycloak login page
    And I enter password <Member2Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see error that certificate could not be updated

    Examples:
      | Owner     | Member1   | Member1Email | Member1Password | Member2   | Member2Email | Member2Password |
      | user1Name | user2Name | user2Email   | user2Password   | user3Name | user3Email   | user3Password   |

  @TC-8329
  Scenario Outline: I should not get a certificate if TLS certificate of ACME is invalid
    Given There is a team owner "<Owner>" with team "MLS"
    And User <Owner> adds users <Member1> to team "MLS" with role Member
    And User <Owner> adds users <Member1> to keycloak for E2EI
    And User <Owner> configures MLS for team "MLS"
    And Admin user <Owner> enables E2EI with insecure ACME server for team "MLS"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    When I see get certificate alert
    And I tap get certificate alert
    Then I see error that certificate could not be updated
    When I click retry button on error that certificate could not be updated
    Then I see error that certificate could not be updated
    When I click cancel button on error that certificate could not be updated
    Then I see get certificate alert
    When I tap get certificate alert
    Then I see error that certificate could not be updated

    Examples:
      | Owner     | Member1   | Member1Email |
      | user1Name | user2Name | user2Email   |

  @TC-8330
  Scenario Outline: I want to enrol my certificate after login
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    When I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | Member1Password |
      | user1Name | user2Name | E2EI     | user2Email   | user2Password   |

  @TC-8331
  Scenario Outline: I want to enrol my certificate after E2EI is enabled for the team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I do not see get certificate alert
    And I see conversation list
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And I see get certificate alert
    And I clear cache of system browser
    When I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I do not see get certificate alert

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | Member1Password |
      | user1Name | user2Name | E2EI     | user2Email   | user2Password   |

  @TC-8346 @TC-8349 @TC-8350
  Scenario Outline: I want to be able to see my Certificate details from device details
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    When I tap Certificate Details button on E2EI certificate updated alert
    Then I see certificate details screen
    And I tap back button
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I open manage your devices menu
    And I tap on my current device listed under devices
    And I see E2EI Certificate device status is "<Status>" on device details
    When I tap on Show Certificate Details button on device details
    Then I see certificate details screen
    And I tap show more options button on certificate details screen
    # TC-8349 I want to be able to download my certificate
    When I tap download option on certificate details screen
    Then I see certificate downloaded alert
    And I tap show more options button on certificate details screen
    # TC-8350 I want to be able to copy my certificate to clipboard
    When I tap copy to clipboard option
    Then I see certificate copied alert

    Examples:
      | TeamOwner | Member1   | TeamName  | Member1Email | Member1Password | Status |
      | user1Name | user2Name | E2EI      | user2Email   | user2Password   | Valid  |

  @TC-8316 @TC-8333
  Scenario Outline: I want delay generating my certificate while grace period is still active
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And I see get certificate alert
    When I tap remind me later alert
    And I tap OK button on E2EI alert
    And I do not see get certificate alert
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I open manage your devices menu
    And I tap on my current device listed under devices
    Then I see E2EI Certificate device status is "<Status>" on device details
    # TC-8333 - I want to generate my certificate from devices page after I delayed generating it
    And I clear cache of system browser
    And I see Get Certificate button on device details
    When I tap on Get Certificate button on device details
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I do not see get certificate alert
    And I see E2EI Certificate device status is "<UpdatedStatus>" on device details

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | Member1Password | Status        | UpdatedStatus |
      | user1Name | user2Name | E2EI     | user2Email   | user2Password   | Not activated | Valid         |

  @TC-8335
  Scenario Outline: I want to update my certificate from my devices page
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    When I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I do not see get certificate alert
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I open manage your devices menu
    And I tap on my current device listed under devices
    And I see E2EI Certificate device status is "<Status>" on device details
    And I tap on Show Certificate Details button on device details
    And I tap back button
    And I see Update Certificate button on device details
    When I tap on Update Certificate button on device details
    And I tap use without an account button if visible
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    Then I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I do not see get certificate alert

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | Member1Password | Status |
      | user1Name | user2Name | E2EI     | user2Email   | user2Password   | Valid  |

  @TC-8315 @TC-8348
  Scenario Outline: I want to create MLS group and add/remove users which has e2ei enabled
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> adds user <Member1>,<Member2> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member2> adds 1 2FA device
    And User <Member1> adds 1 2FA device
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I do not see get certificate alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <Member2> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member2Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member2Email> on keycloak login page
    And I enter password <Member2Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap on search people field
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user <TeamOwner> in search suggestions list
    And I select users <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member1>" in search field
    And I see user <Member1> in search suggestions list
    And I select users <Member1> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    And I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member1> in participants list
    And I tap on user <Member1> in participants list
    And I see connected user <Member1> profile
    And I see remove from group button
    When I tap remove from group button
    And I see alert asking me if I want to remove user <Member1> from group
    And I tap remove button on alert
    And I do not see remove from group button
    And I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member1> from the conversation" in conversation view
    And I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I do not see user <Member1> in participants list
    And I close the group conversation details through X icon
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    And I tap on group conversation title "<ConversationName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I see user <Member1> in search suggestions list
    When I select user <Member1> in search suggestions list
    And I tap Continue button on add participants page
    And I see user <Member1> in participants list
    And I close the group conversation details through X icon
    Then I see system message "You added <Member1> to the conversation" in conversation view
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation
    # TC-8348 - I want to verify that I can exchange messages in an E2EI group after a member was removed from the team
    When User <TeamOwner> removes user <Member1> from team <TeamName>
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message2>" to group conversation <ConversationName>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Member1Email | Member2Email | Member1Password | Member2Password | ConversationName | Message | Message2     |
      | user1Name | user2Name | user3Name | E2EI     | user2Email   | user3Email   | user2Password   |  user3Password  | E2EI Group       | Hello!   | Hello back! |

  @TC-8352
  Scenario Outline: I want to see a system message in the 1:1 conversation when both users are verified
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds users <Member1>,<TeamOwner> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I do not see get certificate alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <TeamOwnerEmail>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <TeamOwnerEmail> on keycloak login page
    And I enter password <TeamOwnerPassword> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I tap on user name "<Member1>" in Search result list
    When I tap start conversation button on connected user profile page
    And I see conversation view with "<Member1>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    Then I see "All devices are verified (end-to-end identity)" system message in conversation
    And I see e2ei verified icon for user "<Member1>" in the 1:1 conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | TeamOwnerEmail | Member1Password | TeamOwnerPassword | Message |
      | user1Name | user2Name | E2EI     | user2Email   | user1Email     | user2Password   |  user1Password    | Hello!  |

  @TC-8377
  Scenario Outline: I want to see a system message in the group conversation when everyone in the conversation is verified
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds users <Member1>,<TeamOwner> to keycloak for E2EI
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And Admin user <TeamOwner> enables E2EI with ACME server for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I do not see get certificate alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <TeamOwnerEmail>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <TeamOwnerEmail> on keycloak login page
    And I enter password <TeamOwnerPassword> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I see certificate updated alert
    And I tap OK button on E2EI certificate updated alert
    And I see conversation list
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap on search people field
    And I tap create new group button
    And I type user name "<Member1>" in search field
    And I see user <Member1> in search suggestions list
    And I select users <Member1> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    When I see group conversation "<ConversationName>" is in foreground
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    Then I see "All devices are verified (end-to-end identity)" system message in conversation
    And I see e2ei verified icon for "<ConversationName>" group conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | Member1Email | TeamOwnerEmail | Member1Password | TeamOwnerPassword | Message | ConversationName |
      | user1Name | user2Name | E2EI     | user2Email   | user1Email     | user2Password   |  user1Password    | Hello!  | E2EI Group       |
