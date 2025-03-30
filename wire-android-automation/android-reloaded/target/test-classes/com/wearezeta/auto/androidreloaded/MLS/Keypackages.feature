@keyPackages
Feature: Keypackages

  @TC-8298 @MLS
  Scenario Outline: I should not be able to add users to a group when they have no remaining key packages
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
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And Users <Member1> claims 100 key packages
    And User <Member1> verifies to have 0 remaining key packages
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select user <Member1> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I tap create new group button
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    And I see system message "<Member1> could not be added to the group." in conversation view

    Examples:
      | TeamOwner | Email      | Member1   | Member2   |  TeamName | ConversationName |
      | user1Name | user1Email | user2Name | user3Name | MLS       | MLS              |

  @TC-8114 @@MLS
  Scenario Outline: I should not be able to create a group conversation with a user who ran out of keypackages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> adds 1 2FA device
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And Users <Member1> claims 100 key packages
    And User <Member1> verifies to have 0 remaining key packages
    When I tap on start a new conversation button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I select user <Member1> in search suggestions list
    And I tap create new group button
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    And I see system message "<Member1> could not be added to the group." in conversation view

    Examples:
      | TeamOwner | Email      | Member1   | TeamName | ConversationName |
      | user1Name | user1Email | user2Name |MLS       | MLS              |

# ToDo Test commented because of issue with the key packages claiming
#  @TC-8296 @TC-8295 @MLS
#  Scenario Outline: I want to renew my key packages when I have less than 50 and I'm online and added to a group
#    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
#    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
#    And User <TeamOwner> configures MLS for team "<TeamName>"
#    And User <TeamOwner> adds 1 2FA device
#    And User <Member1> adds 1 2FA device
#    And User <TeamOwner> is me
#    And I see Welcome Page
#    And I open column-1 backend deep link
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I start 2FA verification email monitoring for <Email>
#    And I tap login button on email Login Page
#    And I type 2FA verification code from email
#    And I wait until I am fully logged in
#    And Users <TeamOwner> claims 51 key packages
#    And User <TeamOwner> verifies to have 49 remaining key packages
#    And User <Member1> verifies to have 100 remaining key packages
#    When I tap on start a new conversation button
#    And I tap create new group button
#    And I type user name "<Member1>" in search field
#    And I see user name "<Member1>" in Search result list
#    And I select user <Member1> in search suggestions list
#    And I tap Continue button on add participants page
#    And I see create new group details page
#    And I type new group name "<ConversationName>"
#    And I hide the keyboard
#    And I see the protocol used for creating conversation is MLS
#    And I tap continue button on create new group details page
#    And I see create new group settings page
#    And I tap continue button on create new group settings page
#    And I see group conversation "<ConversationName>" is in foreground
#        # -> 49 keypackages left here
#    Then User <TeamOwner> verifies to have 100 remaining key packages
#    # TC-8295 - I want to consume a key package when I'm added to a group
#    And User <Member1> verifies to have 99 remaining key packages
#
#    Examples:
#      | TeamOwner | Email      | Member1   | TeamName | ConversationName |
#      | user1Name | user1Email | user2Name | MLS      | MLS              |

  @TC-8297 @MLS
  Scenario Outline: I want to renew my key packages when I have less than 50 and login
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <TeamOwner> is me
    And Users <TeamOwner> claims 51 key packages
    And User <TeamOwner> verifies to have 49 remaining key packages
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And User <TeamOwner> verifies to have 100 remaining key packages

    Examples:
      | TeamOwner | Email      | TeamName |
      | user1Name | user1Email | MLS      |

  @TC-8299 @MLS
  Scenario Outline: I want to create a group when I have no remaining key packages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> adds 1 2FA device
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And Users <TeamOwner> claims 100 key packages
    And User <TeamOwner> verifies to have 0 remaining key packages
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
    And I see group conversation "<ConversationName>" is in foreground
    Then User <TeamOwner> verifies to have 0 remaining key packages

    Examples:
      | TeamOwner | Email      |  Member1  | TeamName | ConversationName |
      | user1Name | user1Email | user2Name | MLS      | MLS              |

  @TC-8113 @MLS
  Scenario Outline: I should not be able to create a 1on1 conversation with a user who ran out of keypackages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> adds 1 2FA device
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And Users <Member1> claims 100 key packages
    And User <Member1> verifies to have 0 remaining key packages
    When I tap on start a new conversation button
    And I see user <Member1> in search suggestions list
    And I tap on user name <Member1> found on search page
    And I see connected user <Member1> profile
    And I tap start conversation button on connected user profile page
    Then I see unable to start conversation alert
    And I see subtext "You can’t start the conversation with <Member1> right now. <Member1> needs to open Wire or log in again first. Please try again later." in unable to start conversation alert
    When I tap OK button on the alert
    Then I do not see conversation view with "<Member1>" is in foreground

    Examples:
      | TeamOwner | Email      |  Member1  | TeamName |
      | user1Name | user1Email | user2Name | MLS      |