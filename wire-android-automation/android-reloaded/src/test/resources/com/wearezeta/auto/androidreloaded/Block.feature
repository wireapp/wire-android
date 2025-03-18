Feature: Block

  @TC-4246 @regression @RC @blockUser
  Scenario Outline: I should not be able to block a team user from group details
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And I open conversation details for 1:1 conversation with "<Member1>"
    When I tap show more options button on user profile screen
    Then I do not see Block option

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | Blocking  | user2Name |

  @TC-4247 @TC-4248 @regression @RC @blockUser @unblockUser
  Scenario Outline: I want to be able to block a guest user from group details
    Given There are 2 users where <Member> is me
    And User <Contact1> is connected to <Member>
    And User <Contact1> sets their unique username
    And Personal user <Contact1> sets profile image
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact1>" in conversation list
    And I tap on conversation name "<Contact1>" in conversation list
    And I see conversation view with "<Contact1>" is in foreground
    And I open conversation details for 1:1 conversation with "<Contact1>"
    When I tap show more options button on user profile screen
    And I tap on Block option
    And I tap Block button on alert
    Then I see toast message "<Contact1> blocked" in user profile screen
    And I see Blocked label
    And I see Unblock User button
    # TC-4248 I want to be able to unblock a guest user from group details through the unblock button
    When I tap Unblock User button
    And I tap Unblock button alert
    Then I do not see Blocked label
    And I do not see Unblock User button

    Examples:
      | Member    | Contact1  |
      | user1Name | user2Name |

  @TC-4253 @TC-4252 @regression @RC @blockUser @unblockUser
  Scenario Outline: I want to be able to unblock a guest user from group details through the unblock option in the context menu
    Given There are 2 users where <Member> is me
    And User <Contact1> is connected to <Member>
    And User <Contact1> sets their unique username
    And Personal user <Contact1> sets profile image
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact1>" in conversation list
    And I tap on conversation name "<Contact1>" in conversation list
    And I see conversation view with "<Contact1>" is in foreground
    And I open conversation details for 1:1 conversation with "<Contact1>"
    When I tap show more options button on user profile screen
    And I tap on Block option
    And I tap Block button on alert
    Then I see toast message "<Contact1> blocked" in user profile screen
    # TC-4252 - I want to see a blocked label on conversation list for a blocked user
    And I see Blocked label
    When I tap show more options button on user profile screen
    And I tap on Unblock option
    And I tap Unblock button alert
    Then I do not see Blocked label
    And I do not see Unblock User button

    Examples:
      | Member    | Contact1  |
      | user1Name | user2Name |

  @TC-4249 @regression @RC @blockUser
  Scenario Outline: I should not be able to block a team user from conversation list
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    When I long tap on conversation name "<Member1>" in conversation list
    Then I do not see block option on conversation list

    Examples:
      | TeamOwner | TeamName | Member1   |
      | user1Name | Blocking | user2Name |

  @TC-4250 @TC-4251 @regression @RC @blockUser @unblockUser
  Scenario Outline: I want to be able to block a guest user from conversation list
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
    When I long tap on conversation name "<Contact1>" in conversation list
    And I tap block option on conversation list
    And I tap block confirm button on conversation list
    Then I see "<Contact1> blocked" toast message on conversation list
    And I see user <Contact1> is having the Blocked label on conversation list
    # TC-4251 I want to be able to unblock a guest user from conversation list
    When I long tap on conversation name "<Contact1>" in conversation list
    And I tap unblock option on conversation list
    And I tap unblock confirm button on conversation list
    And I tap back button
    Then I do not see user <Contact1> is having the Blocked label on conversation list

    Examples:
      | Member    | Contact1  |
      | user1Name | user2Name |

  @TC-4254 @TC-4255 @regression @RC @blockUser @unblockUser
  Scenario Outline: I want to be able to block a team user from another team from group details
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerA> is connected to <TeamOwnerB>
    And User <TeamOwnerA> has 1:1 conversation with <TeamOwnerB> in team "<TeamNameA>"
    And User <TeamOwnerA> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwnerB>" in conversation list
    And I tap on conversation name "<TeamOwnerB>" in conversation list
    And I open conversation details for 1:1 conversation with "<TeamOwnerB>"
    When I tap show more options button on user profile screen
    And I tap on Block option
    And I tap Block button on alert
    Then I see toast message "<TeamOwnerB> blocked" in user profile screen
    And I see Blocked label
    And I see Unblock User button
    # TC-4255 I want to be able to unblock a guest user from group details through the unblock button
    When I tap Unblock User button
    And I tap Unblock button alert
    Then I do not see Blocked label
    And I do not see Unblock User button

    Examples:
      | TeamOwnerA | TeamOwnerB  | TeamNameA | TeamNameB   |
      | user1Name  | user2Name   | Blocking  | ToBeBlocked |

  @TC-4256 @TC-4257 @regression @RC @blockUser @unblockUser
  Scenario Outline: I want to be able to block a team user from another team from conversation list
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerA> is connected to <TeamOwnerB>
    And User <TeamOwnerA> has 1:1 conversation with <TeamOwnerB> in team "<TeamNameA>"
    And User <TeamOwnerA> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwnerB>" in conversation list
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    And I tap block option on conversation list
    And I tap block confirm button on conversation list
    Then I see "<TeamOwnerB> blocked" toast message on conversation list
    And I see user <TeamOwnerB> is having the Blocked label on conversation list
    # TC-4257 I want to be able to unblock a guest user from conversation list
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    And I tap unblock option on conversation list
    And I tap unblock confirm button on conversation list
    And I tap back button
    Then I do not see user <TeamOwnerB> is having the Blocked label on conversation list

    Examples:
      | TeamOwnerA | TeamOwnerB | TeamNameA | TeamNameB   |
      | user1Name  | user2Name  | Blocking  | ToBeBlocked |