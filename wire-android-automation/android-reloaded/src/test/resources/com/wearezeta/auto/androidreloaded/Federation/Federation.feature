Feature: Federation
  # Most tests are currently disabled because a/b/c now have MLS.
  # Either MLS needs to be enabled for all the tests, if backend will be used for MLS cloud testing,
  # Or tests need to be deleted and replaced by tests for MLS cloud

  ######################
  # Search
  ######################

#  @TC-4085 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to search for Wire users from same BE
#    Given There is a team owner "<TeamOwner>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamNameBella>" with role Member
#    And User <TeamOwner> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type user name "<Member1>" in search field
#    Then I see user name "<Member1>" in Search result list
#    And I do not see federated guest label for user name "<Member1>" in Search result list
#    And I do not see domain <FederatedBackendName> in subtitle for <Member1>
#
#    Examples:
#      | TeamOwner | Member1   | Member2   | TeamNameBella | FederatedBackendName  |
#      | user1Name | user2Name | user3Name | Banana        | @bella.wire.link      |
#
#  @TC-4086 @regression @RC @federation @federationSearch
#  Scenario Outline: I should be able to find a user from a same BE when I know his fully qualified User name
#    Given There is a team owner "<TeamOwner>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamNameBella>" with role Member
#    And User <TeamOwner> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
#    Then I see user name "<Member1>" in Search result list
#    And I do not see federated guest label for user name "<Member1>" in Search result list
#    And I do not see domain <FederatedBackendName> in subtitle for <Member1>
#
#    Examples:
#      | TeamOwner | Member1   | Member2   | TeamNameBella | Uniqueusername      | FederatedBackendName |
#      | user1Name | user2Name | user3Name | Banana        | user2UniqueUsername | @bella.wire.link     |
#
#  @TC-4087 @TC-4089 @TC-4088 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to search for Wire users from different BE
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1>,<Member2> to team "<TeamNameChala>" with role Member
#    And User <Member1> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
#    Then I see user name "<Member1>" in Search result list
#    # @TC-4089 - I should see federated guest icon in search results
#    And I see federated guest label for user name "<Member1>" in Search result list
#    # @TC-4088 - I want to see the domain displayed in the user entry subtitle when I search for a User from a different BE
#    And I see domain <FederatedBackendName> in subtitle for <Member1>
#
#    Examples:
#      | TeamOwnerBella | Member1   | Member2   | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendName |
#      | user1Name      | user2Name | user3Name | user4Name      | Banana        | Cactus        | user2UniqueUsername | @chala.wire.link     |
#
#  @TC-4090 @TC-4091 @regression @RC @federation @federationSearch @WPB-6257
#  Scenario Outline: I should only get search result when I know the fully qualified User name for User from different BE
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1>,<Member2> to team "<TeamNameChala>" with role Member
#    And User <Member1> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    Then I see search hint "<SearchHint>" on search page
#    And I see Learn More link on empty search page
#    When I type unique user name "<Uniqueusername>" in search field
#    And I do not see user name "<Member1>" in Search result list
#    #Bug - WPB-6257 - search hint is displayed wrongly enable step again after bug is fixed
#    # @TC-4091 - I want to see an empty search result screen when no user can be found
#    # Then I see search hint "<NoResult>" on search page
#    When I clear the input field on Search page
#    And I type unique user name "<FederatedBackendName>" in search field
#    Then I do not see user name "<Member1>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<Member1>" in search field
#    Then I do not see user name "<Member1>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername><FalseFederatedBackendName>" in search field
#    Then I do not see user name "<Member1>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
#    Then I see user name "<Member1>" in Search result list
#
#    Examples:
#      | TeamOwnerBella | Member1   | Member2   | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendName | FalseFederatedBackendName | NoResult                                     | SearchHint                                           |
#      | user1Name      | user2Name | user3Name | user4Name      | Banana        | Cactus        | user2UniqueUsername | @chala.wire.link     | @andromeda.wire.link      | No results could be found. Please try again. | Search for people by their profile name or @username |
#
#  @TC-4092 @TC-4109 @TC-4094 @TC-4093 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to have a conversation with users from different BE
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1> to team "<TeamNameChala>" with role Member
#    And User <Member1> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
#    Then I see user name "<Member1>" in Search result list
#    When I tap on user name <Member1> found on search page
#    # TC-4093 - I should see the fully qualified username on connection page when I want to connect to a user from different backend
#    Then I see fully qualified username "@<Uniqueusername><FederatedBackendName>" on unconnected user profile page
#    And I see username "<Member1>" on unconnected user profile page
#    # TC-4094 - I should see the federated icon on connection page when I want to connect to a user from different backend
#    And I see federated guest icon for user "<Member1>" on unconnected user profile page
#    # TC-4109 - I want to be able to connect to a user from different backend
#    When I tap connect button on unconnected user profile page
#    And I tap close button on unconnected user profile page
#    And I close the search page through X icon
#    Then I see conversation "<Member1>" is having pending status in conversation list
#    And User <Member1> accepts all requests
#    Then I see conversation "<Member1>" in conversation list
#    And I tap on conversation name "<Member1>" in conversation list
#    When I type the message "<Message>" into text input field
#    And I tap send button
#    Then I see the message "<Message>" in current conversation
#
#    Examples:
#      | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendName | Message |
#      | user1Name      | user2Name | user3Name      | Banana        | Cactus        | user2UniqueUsername | @chala.wire.link     | Hello   |
#
#  @TC-4095 @TC-4096 @regression @RC @federation @federationSearch
#  Scenario Outline: I should not be able to find any user on backend with disabled search
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> sets their unique username
#    And User <TeamOwnerChala> sets their unique username
#    And User <TeamOwnerAnta> is me
#    And I see Welcome Page
#    And I open anta backend deep link
#    And I see alert informing me that I am about to switch to anta backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername2><FederatedBackendNameChala>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername2>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<FederatedBackendNameChala>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<TeamOwnerChala>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    # TC-4096 - I want to find users with their fully qualified handle on different backends if my backend disabled search
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername1><FederatedBackendNameBella>" in search field
#    Then I see user name "<TeamOwnerBella>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername1>" in search field
#    Then I do not see user name "<TeamOwnerBella>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<FederatedBackendNameBella>" in search field
#    Then I do not see user name "<TeamOwnerBella>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<TeamOwnerBella>" in search field
#    Then I do not see user name "<TeamOwnerBella>" in Search result list
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | Uniqueusername1     | Uniqueusername2     | FederatedBackendNameBella | FederatedBackendNameChala |
#      | user1Name     | user2Name      | user3Name      | Avocado      | Banana        | Cactus        | user2UniqueUsername | user3UniqueUsername | @bella.wire.link          | @chala.wire.link          |
#
#  @TC-4097 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to find a user only by fully qualified handle if their backend enabled exact search
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendNameChala>" in search field
#    Then I see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<FederatedBackendNameChala>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<TeamOwnerChala>" in search field
#    Then I do not see user name "<TeamOwnerChala>" in Search result list
#
#    Examples:
#      | TeamOwnerBella | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendNameChala |
#      | user1Name      | user2Name      | Banana        | Cactus        | user2UniqueUsername | @chala.wire.link          |
#
#  @TC-4098 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to find a user with display name and handle if their backend enabled full search
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerAnta> sets their unique username
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendNameAnta>" in search field
#    Then I see user name "<TeamOwnerAnta>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername>" in search field
#    Then I do not see user name "<TeamOwnerAnta>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<TeamOwnerAnta><FederatedBackendNameAnta>" in search field
#    Then I see user name "<TeamOwnerAnta>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<TeamOwnerAnta>" in search field
#    Then I do not see user name "<TeamOwnerAnta>" in Search result list
#    When I clear the input field on Search page
#    And I type user name "<FederatedBackendNameAnta>" in search field
#    Then I do not see user name "<TeamOwnerAnta>" in Search result list
#    When I clear the input field on Search page
#    And I type unique user name "<Uniqueusername><FalseFederatedBackendNameAnta>" in search field
#    Then I do not see user name "<TeamOwnerAnta>" in Search result list
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerChala | TeamNameAnta | TeamNameChala | Uniqueusername      | FederatedBackendNameAnta | FalseFederatedBackendNameAnta |
#      | user1Name     | user2Name      | Avocado      | Cactus        | user1UniqueUsername | @anta.wire.link          | @andromeda.wire.link          |

  ######################
  # Inbound/Outbound search settings
  ######################

  # For Details, see here
  # https://wearezeta.atlassian.net/wiki/spaces/ENGINEERIN/pages/566035910/Searching+for+users#Searching-users-on-the-same-backend

  @TC-4099 @regression @RC @federation @federationSearch
  Scenario Outline: I want to be able to find a user from another team through his exact handle or full text search, if his team has SearchableByAllTeams enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameAntaA>" on anta backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameAntaA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameAntaB>" on anta backend
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open anta backend deep link
    And I see alert informing me that I am about to switch to anta backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerB>" sets the search behaviour for SearchVisibilityInbound to SearchableByAllTeams for team <TeamNameAntaB>
    When I tap on search people field
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type the first 5 chars of user name "<TeamOwnerB>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameAntaA | TeamNameAntaB     | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers     | SearchEnabled     | user2UniqueUsername      |

  @TC-4100 @TC-4101 @regression @RC @federation @federationSearch
  Scenario Outline: I should not be able to find a user from another team through full text search, if they have SearchableByOwnTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameAntaA>" on anta backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameAntaA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameAntaB>" on anta backend
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open anta backend deep link
    And I see alert informing me that I am about to switch to anta backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerB>" sets the search behaviour for SearchVisibilityInbound to SearchableByOwnTeam for team <TeamNameAntaB>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type the first 5 chars of user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    # TC-4101 - I want to be able to find a user from another team trough exact handle, if they have SearchableByOwnTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameAntaA | TeamNameAntaB      | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers     | SearchDisabled     | user2UniqueUsername      |

  @TC-4102 @TC-4103 @regression @RC @federation @federationSearch
  Scenario Outline: I should not be able to find a user from another team by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameAntaA>" on anta backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameAntaA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameAntaB>" on anta backend
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open anta backend deep link
    And I see alert informing me that I am about to switch to anta backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerA>" enables the search behaviour for TeamSearchVisibility for team <TeamNameAntaA>
    And TeamOwner "<TeamOwnerA>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamNameAntaA>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    # TC-4103 - I want to find a user from another team by his exact handle, if my team has SearchVisibilityNoNameOutsideTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameAntaA | TeamNameAntaB | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers     | ToSearch      | user2UniqueUsername      |

  @TC-4104 @TC-4105 @regression @RC @federation @federationSearch
  Scenario Outline: I should not be able to find a personal user by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwner>" with team "<TeamNameAnta>" on anta backend
    And User <TeamOwner> adds users <Member1> to team "<TeamNameAnta>" with role Member
    And There are users <Name> on anta backend
    And User <Member1> is me
    And User <Name> sets their unique username
    And Personal user <Name> sets profile image
    And I see Welcome Page
    And I open anta backend deep link
    And I see alert informing me that I am about to switch to anta backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwner>" enables the search behaviour for TeamSearchVisibility for team <TeamNameAnta>
    And TeamOwner "<TeamOwner>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamNameAnta>
    When I tap on search people field
    And I type user name "<Name>" in search field
    Then I do not see user name "<Name>" in Search result list
    # TC-4105 - I want to find a personal user by his exact handle, if my team has SearchVisibilityNoNameOutsideTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<NameUniqueUserName>" in search field
    Then I see user name "<Name>" in Search result list

    Examples:
      | TeamOwner | Member1   | Name      | TeamNameAnta | NameUniqueUserName  |
      | user1Name | user2Name | user3Name | Searchers    | user3UniqueUsername |

#  @TC-4106 @TC-4107 @regression @RC @federation @federationSearch
#  Scenario Outline: I want to be able to find a team user through exact handle as a personal user
#    Given There is a team owner "<TeamOwner>" with team "<TeamNameAnta>" on anta backend
#    And There are users <Name> on anta backend
#    And User <TeamOwner> sets their unique username
#    And User <Name> sets their unique username
#    And User <Name> is me
#    And I see Welcome Page
#    And I open anta backend deep link
#    And I see alert informing me that I am about to switch to anta backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<TeamOwnerUniqueUserName>" in search field
#    Then I see user name "<TeamOwner>" in Search result list
#    # TC-4107 - I should not be able to find a user from another team through full text search as a personal User
#    When I clear the input field on Search page
#    And I type the first 5 chars of user name "<TeamOwner>" in search field
#    Then I do not see user name "<TeamOwner>" in Search result list
#    And I clear the input field on Search page
#    When I type user name "<TeamOwner>" in search field
#    Then I do not see user name "<TeamOwner>" in Search result list
#
#    Examples:
#      | TeamOwner | TeamOwnerUniqueUserName | Name      | TeamNameAnta  |
#      | user1Name | user1UniqueUsername     | user2Name | Searchers     |

  ######################
  # Connect
  ######################

  @TC-4110 @regression @RC @federation @federationConnect
  Scenario Outline: I want to be able to cancel a connection request from a different backend
    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
    And User <TeamOwnerChala> adds users <Member1> to team "<TeamNameChala>" with role Member
    And User <TeamOwnerBella> configures MLS for team "<TeamNameBella>"
    And User <TeamOwnerChala> configures MLS for team "<TeamNameChala>"
    And User <Member1> sets their unique username
    And User <Member1> adds 1 device
    And User <TeamOwnerBella> is me
    And I see Welcome Page
    And I open bella backend deep link
    And I see alert informing me that I am about to switch to bella backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
    And I see user name "<Member1>" in Search result list
    And I tap on user name <Member1> found on search page
    Then I see username "<Member1>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    And I wait until cancel connection request button on unconnected user profile page is visible
    And I tap cancel connection request button on unconnected user profile page
    And I tap close button on unconnected user profile page
    Then I do not see conversation "<Member1>" in conversation list

    Examples:
      | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendName |
      | user1Name      | user2Name | user3Name      | Banana        | Mango         | user2UniqueUsername | @chala.wire.link     |

#  @TC-4108 @regression @RC @federation @federationConnect
#  Scenario Outline: I want to be able to accept a connection request from a different backend
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1> to team "<TeamNameChala>" with role Member
#    And User <Member1> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    When User <Member1> sends connection request to me
#    Then I see unread conversation "<Member1>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<Member1>" in conversation list
#    And I tap on unread conversation name "<Member1>" in conversation list
#    And I see fully qualified username "@<Uniqueusername><FederatedBackendName>" on unconnected user profile page
#    And I see username "<Member1>" on unconnected user profile page
#    And I see federated guest icon for user "<Member1>" on unconnected user profile page
#    And I see accept button on unconnected user profile page
#    And I see ignore button on unconnected user profile page
#    And I see text "<ConnectionText>" on unconnected user profile page
#    When I tap accept button on unconnected user profile page
#    And I tap start conversation button on connected user profile page
#    Then I see conversation view with "<Member1>" is in foreground
#
#    Examples:
#      | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameBella | TeamNameChala | Subtitle          | Uniqueusername      | FederatedBackendName | ConnectionText                       |
#      | user1Name      | user2Name | user3Name      | Banana        | Mango         | Wants to connect  | user2UniqueUsername | @chala.wire.link     | This user wants to connect with you. |
#
#  @TC-4111 @regression @RC @blockUser @federationBlock
#  Scenario Outline: I want to be able to block a federated user
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1> to team "<TeamNameChala>" with role Member
#    And User <Member1> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    And I tap on search people field
#    And I type unique user name "<Uniqueusername><FederatedBackendName>" in search field
#    And I see user name "<Member1>" in Search result list
#    And I tap on user name <Member1> found on search page
#    And I tap connect button on unconnected user profile page
#    And I tap close button on unconnected user profile page
#    And User <Member1> accepts all requests
#    And I close the search page through X icon
#    And I see conversation "<Member1>" in conversation list
#    And I tap on conversation name "<Member1>" in conversation list
#    And I open conversation details for 1:1 conversation with "<Member1>"
#    And I tap show more options button on user profile screen
#    When I tap on Block option
#    And I tap Block button on alert
#    Then I see toast message "<Member1> blocked" in user profile screen
#    And I see Blocked label
#    And I see Unblock User button
#
#    Examples:
#      | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameBella | TeamNameChala | Uniqueusername      | FederatedBackendName |
#      | user1Name      | user2Name | user3Name      | Banana        | Cactus        | user2UniqueUsername | @chala.wire.link     |

  ######################
  # Conversation View
  ######################

#  @TC-4113 @TC-4114 @TC-4112 @regression @RC @federation @membershipIdentifiers
#  Scenario Outline: I want to see a federated banner if federated users are present in a group conversation
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1> to team "<TeamNameChala>" with role Member
#    And User <TeamOwnerChala> is connected to <TeamOwnerBella>
#    And User <TeamOwnerChala> has group conversation <GroupConversation> with <TeamOwnerBella>,<Member1> in team "<TeamNameChala>"
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    When I tap on conversation name "<GroupConversation>" in conversation list
#    And I see group conversation "<GroupConversation>" is in foreground
#    Then I see a banner informing me that "Federated users are present" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4112 - I want to see a federated identifier on conversation list for 1:1 conversations with a federated user
#    When I see conversation "<TeamOwnerBella>" in conversation list
#    Then I see <TeamOwnerBella> has "Federated" identifier next to his name in conversation list
#    # TC-4114 - I should not see a federated banner if federated users are present in a 1:1 conversation
#    When I tap on conversation name "<TeamOwnerBella>" in conversation list
#    Then I do not see a banner informing me that "Federated users are present" in the conversation view
#
#    Examples:
#      | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameBella | TeamNameChala | GroupConversation     |
#      | user1Name      | user2Name | user3Name      | Banana        | Cactus        | FederatedConversation |