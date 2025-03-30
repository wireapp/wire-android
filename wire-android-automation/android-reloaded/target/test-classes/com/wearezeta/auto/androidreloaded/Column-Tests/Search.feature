Feature: Search

  @TC-4841 @SF.Usersearch @TSFI.UserInterface @S0.1 @col1
  Scenario Outline: Local: I should not find a user from another team through full text search, if they have SearchableByOwnTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerB>" sets the search behaviour for SearchVisibilityInbound to SearchableByOwnTeam for team <TeamNameB>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type the first 5 chars of user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type unique user name "@<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB      | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | SearchDisabled | user2UniqueUsername      |

  @TC-4842 @SF.Usersearch @TSFI.UserInterface @S0.1 @col1
  Scenario Outline: Local: I should not find a user from another team by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerA>" enables the search behaviour for TeamSearchVisibility for team <TeamNameA>
    And TeamOwner "<TeamOwnerA>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamNameA>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type unique user name "@<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | ToSearch  | user2UniqueUsername      |

  @TC-4843 @SF.Usersearch @TSFI.UserInterface @S0.1 @col1
  Scenario Outline: Local: I should not find a user from another team by email
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
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
    And I tap on start a new conversation button
    When I tap on search people field
    And I search user "<TeamOwnerB>" by email in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | ToSearch  |

  @TC-4844 @SF.Usersearch @TSFI.UserInterface @S0.1 @S7 @col1
  Scenario Outline: Remote: I should not find a user from another team on another backend by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-3 backend
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
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerA>" enables the search behaviour for TeamSearchVisibility for team <TeamNameA>
    And TeamOwner "<TeamOwnerA>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamNameA>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type unique user name "@<TeamOwnerBUniqueUserName>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I search user <TeamOwnerB> by exact handle and domain in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | ToSearch  | user2UniqueUsername      |

  @TC-4845 @SF.Usersearch @TSFI.UserInterface @S0.1 @S7 @col1
  Scenario Outline: Remote: I should not find a user from another backend by email
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-3 backend
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
    And I tap on start a new conversation button
    When I tap on search people field
    And I search user "<TeamOwnerB>" by email in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | ToSearch  |

  #@TC-4846 @SF.Usersearch @TSFI.UserInterface @S0.1 @S7 @col1
  #Scenario Outline: Remote: I should not find a user on another backend by full text if their FederatedUserSearchPolicy is exact_handle_search

  @TC-4847 @SF.Usersearch @TSFI.UserInterface @S0.1 @S7 @col1
  Scenario Outline: Remote: I should not find a user on another backend by full text or handle if their FederatedUserSearchPolicy is no_search
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-2 backend
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
    And The search policy is no_search with no team level restriction from column-1 backend to column-2 backend
    And User <Member1> is me
    And I see Welcome Page
    And I open column-2 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    When I tap on search people field
    And I search user <TeamOwnerB> by exact handle and domain in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type unique user name "@<TeamOwnerBUniqueUserName>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member1Email | TeamNameA | TeamNameB | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | user3Email   | Searchers | ToSearch  | user2UniqueUsername      |