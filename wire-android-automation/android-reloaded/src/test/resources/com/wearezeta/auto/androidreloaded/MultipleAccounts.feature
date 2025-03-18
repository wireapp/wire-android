Feature: Multiple Accounts

  @TC-4457 @regression @RC @login @multipleAccounts
  Scenario Outline: I want to be able to login with 2 accounts
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    When I tap New Team or Account button
    Then I see Welcome Page
    And User <Member2> is me
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member2> as my currently active account

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName     |
      | user1Name | user2Name | user3Name | MultiAccount |

  @TC-4458 @regression @RC @multipleAccounts
  Scenario Outline: I want to be able to login with 2 accounts and send and receive messages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I tap User Profile Button
    And I see User Profile Page
    When I tap New Team or Account button
    Then I see Welcome Page
    And User <Member2> is me
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    And I see conversation "<Member1>" in conversation list
    When I tap on conversation name "<Member1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And I wait until the notification popup disappears
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I see my other account <Member1> is listed under other logged in accounts
    When I switch to <Member1> account
    And I wait until the notification popup disappears
    And I see conversation list
    And I see conversation "<Member2>" is having 1 unread messages in conversation list
    And I tap on unread conversation name "<Member2>" in conversation list
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName     | Message |
      | user1Name | user2Name | user3Name | MultiAccount | Hello!  |

  @TC-4459 @regression @RC @logout @multipleAccounts
  Scenario Outline: I want to see the logout working as expected when I am logged in with multiple accounts
    Given Login with 3 accounts is enabled on the build
    And There is a team owner "<TeamOwnerStaging>" with team "<TeamNameStaging>"
    And User <TeamOwnerStaging> adds users <MemberStaging> to team "<TeamNameStaging>" with role Member
    And There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
    And User <TeamOwnerAnta> adds users <MemberAnta> to team "<TeamNameAnta>" with role Member
    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
    And User <TeamOwnerBella> adds users <MemberBella> to team "<TeamNameBella>" with role Member
    And User <MemberStaging> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberStaging> as my currently active account
    When I tap New Team or Account button
    Then I see Welcome Page
    And User <MemberAnta> is me
    And I open anta backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberAnta> as my currently active account
    When I tap New Team or Account button
    Then I see Welcome Page
    And User <MemberBella> is me
    And I open bella backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberBella> as my currently active account
    And I see my other account <MemberAnta> is listed under other logged in accounts
    And I see my other account <MemberStaging> is listed under other logged in accounts
    When I tap log out button on User Profile Page
    And I tap log out button on clear data alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberStaging> as my currently active account
    And I see my other account <MemberAnta> is listed under other logged in accounts
    When I tap log out button on User Profile Page
    And I tap log out button on clear data alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberAnta> as my currently active account
    And I don't see any other accounts logged in
    When I tap log out button on User Profile Page
    And I tap log out button on clear data alert
    Then I see Welcome Page

    Examples:
      | TeamOwnerStaging | TeamOwnerAnta   | TeamOwnerBella   | MemberStaging | MemberAnta | MemberBella | TeamNameStaging | TeamNameAnta | TeamNameBella |
      | user1Name        | user2Name       | user3Name        | user4Name     | user5Name  | user6Name   | TeamStaging     | TeamAnta     | TeamBella     |