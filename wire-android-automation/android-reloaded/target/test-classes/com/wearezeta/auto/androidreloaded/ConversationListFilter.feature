Feature: ConversationListFilter

  @TC-4299 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a contact by full name
    Given There are 2 users where <Member> is me
    And User <Contact1> is connected to <Member>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact1>" in conversation list
    When I tap on search conversation field
    And I type conversation name "<Contact1>" in search field
    Then I see conversation name "<Contact1>" in Search result list

    Examples:
      | Member    | Contact1  |
      | user1Name | user2Name |

  @TC-4300 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a conversation by full name
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I tap on search conversation field
    And I type conversation name "<GroupChatName>" in search field
    Then I see conversation name "<GroupChatName>" in Search result list

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupChatName    |
      | user1Name | Messaging | user2Name | Search GroupChat |

  @TC-4302 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a conversation by partial name
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I tap on search conversation field
    And I type first 3 chars of group name "<GroupChatName>" in search field
    Then I see conversation name "<GroupChatName>" in Search result list

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupChatName    |
      | user1Name | Messaging | user2Name | Search GroupChat |

  @TC-4303 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a conversation in lower case
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I tap on search conversation field
    And I type conversation name "<GroupChatName>" in search field in lower case
    Then I see conversation name "<GroupChatName>" in Search result list

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupChatName   |
      | user1Name | Messaging | user2Name | Search GroupChat |

  @TC-4304 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a conversation in upper case
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I tap on search conversation field
    And I type conversation name "<GroupChatName>" in search field in upper case
    Then I see conversation name "<GroupChatName>" in Search result list

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupChatName    |
      | user1Name | Messaging | user2Name | Search GroupChat |

  @TC-4301 @regression @RC @conversationListFilter @search
  Scenario Outline: I want to be able to search for a contact by partial user name
    Given There are 2 users where <Member> is me
    And User <Contact1> is connected to <Member>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact1>" in conversation list
    When I tap on search conversation field
    And I type first <Size> chars of user name "<Contact1>" in search field
    Then I see conversation name "<Contact1>" in Search result list

    Examples:
      | Member    | Contact1  | Size |
      | user1Name | user2Name | 3    |

  @TC-4305 @regression @RC @conversationListFilter @search
  Scenario Outline: I should not get a search result when my search does not match any existing conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupChatName>" in conversation list
    When I tap on search conversation field
    And I type a random conversation name in search field
    Then I do not see conversation name "<GroupChatName>" in Search result list

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupChatName    |
      | user1Name | Messaging | user2Name | Search Groupchat |
