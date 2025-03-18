Feature: Search

  @TC-4505 @regression @RC @search @smoke
  Scenario Outline: I want to search for Wire users by username
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    When I tap on search people field
    And I type user name "<Member1>" in search field
    Then I see user name "<Member1>" in Search result list

    Examples:
      | TeamOwner | Member1   | TeamName |
      | user1Name | user2Name | Search   |

  @TC-4504 @regression @RC @search
  Scenario Outline: I should not to be able to search for a team member by email
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on start a new conversation button
    And I tap on search people field
    And I type user email "<Member1Email>" in search field
    Then I do not see user name "<Member1>" in Search result list

    Examples:
      | TeamOwner  | Member1   | TeamName | Member1Email |
      | user1Name  | user2Name | Search   | user2Email   |

  @TC-4503 @regression @RC @search
  Scenario Outline: I want to be able to search for a team user by unique username
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <Member1> sets their unique username
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on start a new conversation button
    And I tap on search people field
    And I type unique user name "<Member1UniqueUsername>" on search field in conversation list page
    Then I see user name "<Member1>" in Search result list

    Examples:
      | TeamOwner | Member1   | TeamName | Member1UniqueUsername |
      | user1Name | user2Name | Search   | user2UniqueUsername   |

  @TC-4502 @regression @RC @search
  Scenario Outline: I want to be able to search for a contact by unique username
    Given There are 2 users where <Name> is me
    And User Myself is connected to <Contact2>
    And User <Contact2> sets their unique username
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact2>" in conversation list
    When I tap on start a new conversation button
    And I tap on search people field
    And I type unique user name "<Contact2UniqueUsername>" on search field in conversation list page
    Then I see user name "<Contact2>" in Search result list

    Examples:
      | Name      | Contact2  | Contact2UniqueUsername |
      | user1Name | user2Name | user2UniqueUsername    |

  @TC-4515 @regression @search @RC @WPB-3261
  Scenario Outline: I want to see other members of my team without searching for them, when I have an existing group conversation with them
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member1> in participants list
    And I see user <Member2> in participants list
    And I see user <Member3> in participants list
    And I close the group conversation details through X icon
    And I close the conversation view through the back arrow
    When I tap on start a new conversation button
    Then I see user <Member1> in search suggestions list
    Then I see user <Member2> in search suggestions list
    Then I see user <Member3> in search suggestions list

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | AddGroup | MyTeam            |

  ######################
  # Inbound/Outbound search settings
  ######################

  # For Details, see here
  # https://wearezeta.atlassian.net/wiki/spaces/ENGINEERIN/pages/566035910/Searching+for+users#Searching-users-on-the-same-backend

  @TC-4506 @regression @RC @search
  Scenario Outline: I want to be able to find a user from another team through his exact handle or full text search, if his team has SearchableByAllTeams enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerB>" sets the search behaviour for SearchVisibilityInbound to SearchableByAllTeams for team <TeamNameB>
    When I tap on search people field
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type <TeamOwnerBUniqueUserName> in search field in search only partially
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameA | TeamNameB     | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers | SearchEnabled | user2UniqueUsername      |

  @TC-4507 @TC-4508 @regression @RC @search
  Scenario Outline: I should not be able to find a user from another team through full text search, if they have SearchableByOwnTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And TeamOwner "<TeamOwnerB>" sets the search behaviour for SearchVisibilityInbound to SearchableByOwnTeam for team <TeamNameB>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    When I clear the input field on Search page
    And I type the first 5 chars of user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    # TC-4508 - I want to be able to find a user from another team trough exact handle, if they have SearchableByOwnTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameA | TeamNameB      | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers | SearchDisabled | user2UniqueUsername      |

  @TC-4509 @TC-4510 @regression @RC @search
  Scenario Outline: I should not be able to find a user from another team by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <Member1> is me
    And User <TeamOwnerB> sets their unique username
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwnerA>" enables the search behaviour for TeamSearchVisibility for team <TeamNameA>
    And TeamOwner "<TeamOwnerA>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamNameA>
    When I tap on search people field
    And I type user name "<TeamOwnerB>" in search field
    Then I do not see user name "<TeamOwnerB>" in Search result list
    # TC-4510 - I want to find a user from another team by his exact handle, if my team has SearchVisibilityNoNameOutsideTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<TeamOwnerBUniqueUserName>" in search field
    Then I see user name "<TeamOwnerB>" in Search result list

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | TeamNameA | TeamNameB | TeamOwnerBUniqueUserName |
      | user1Name  | user2Name  | user3Name | Searchers | ToSearch  | user2UniqueUsername      |

  @TC-4511 @TC-4512 @regression @RC @search
  Scenario Outline: I should not be able to find a personal user by full text search, if my team has SearchVisibilityNoNameOutsideTeam enabled
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And There is a personal user <Name>
    And User <Member1> is me
    And User <Name> sets their unique username
    And Personal user <Name> sets profile image
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    And TeamOwner "<TeamOwner>" enables the search behaviour for TeamSearchVisibility for team <TeamName>
    And TeamOwner "<TeamOwner>" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team <TeamName>
    When I tap on search people field
    And I type user name "<Name>" in search field
    Then I do not see user name "<Name>" in Search result list
    # TC-4512 - I want to find a personal user by his exact handle, if my team has SearchVisibilityNoNameOutsideTeam enabled
    When I clear the input field on Search page
    And I type unique user name "<NameUniqueUserName>" in search field
    Then I see user name "<Name>" in Search result list

    Examples:
      | TeamOwner | Member1   | Name      | TeamName  | NameUniqueUserName  |
      | user1Name | user2Name | user3Name | Searchers | user3UniqueUsername |

  @TC-4513 @TC-4514 @regression @RC @search
  Scenario Outline: I want to be able to find a team user through exact handle as a personal user
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a personal user <Name>
    And User <TeamOwner> sets their unique username
    And User <Name> sets their unique username
    And Personal user <Name> sets profile image
    And User <Name> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<TeamOwnerUniqueUserName>" in search field
    Then I see user name "<TeamOwner>" in Search result list
    # TC-4107 - I should not be able to find a user from another team through full text search as a personal User
    When I clear the input field on Search page
    And I type the first 5 chars of user name "<TeamOwner>" in search field
    Then I do not see user name "<TeamOwner>" in Search result list
    And I clear the input field on Search page
    When I type user name "<TeamOwner>" in search field
    Then I do not see user name "<TeamOwner>" in Search result list

    Examples:
      | TeamOwner | TeamOwnerUniqueUserName | Name      | TeamName  |
      | user1Name | user1UniqueUsername     | user2Name | Searchers |