Feature: Multiple Accounts

  @TC-4702 @TC-4703 @multipleAccounts @col1 @col3
  Scenario Outline: I want to see the logout working as expected when I am logged in with 2 accounts
    And There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerB> adds users <Member2> to team "<TeamNameB>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email1>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    When I tap New Team or Account button
    Then I see Welcome Page
    And I do not see Create a Team button on Welcome Page
    And I do not see Create a Personal Account link on Welcome Page
    And User <Member2> is me
    When I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email2>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member2> as my currently active account
    And I see my other account <Member1> is listed under other logged in accounts
    # TC-4703 - I should not be able to login with a 3rd account
    When I tap New Team or Account button
    Then I see alert informing me that I can not login with another account
    And I tap OK button on the alert
    When I tap log out button on User Profile Page
    And I tap log out button on clear data alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I don't see any other accounts logged in
    When I tap log out button on User Profile Page
    And I tap log out button on clear data alert
    Then I see Welcome Page

    Examples:
      | TeamOwnerA | TeamOwnerB  | Member1   | Email1     | Member2   | Email2     | TeamNameA | TeamNameB |
      | user1Name  | user2Name   | user3Name | user3Email | user4Name | user4Email | TeamA     | TeamB     |