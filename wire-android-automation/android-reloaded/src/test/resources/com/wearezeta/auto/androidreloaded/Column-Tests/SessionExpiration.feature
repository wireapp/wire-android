Feature: Session Expiration

  @TC-4848 @sessionExpiration @SF.Provisioning @TSFI.RESTfulAPI @S0.1 @S2 @col1 @col3
  Scenario Outline: I want to see the appropriate device is signed out if it was removed
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <Member1> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    And I see subtext "<Subtext>" in the removed device alert
    When I tap OK button on the alert
    Then I see Welcome Page

    Examples:
      | TeamOwner | Member1   | Email      | TeamName          | Subtext                                              |
      | user1Name | user2Name | user2Email | SessionExpiration | You were logged out because your device was removed. |

  @TC-4850 @sessionExpiration @multipleAccounts @col1 @col3
  Scenario Outline: I want to see the logout working as expected when I am logged in with multiple accounts when a client of mine is deleted
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
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
    When User <Member2> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    When I tap OK button on the alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I don't see any other accounts logged in
    When User <Member1> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    When I tap OK button on the alert
    Then I see Welcome Page

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Email1     | Member2   | Email2     | TeamNameA | TeamNameB |
      | user1Name  | user2Name  | user3Name | user3Email | user4Name | user4Email | TeamA     | TeamB     |

  @TC-4849 @SF.Provisioning @TSFI.UserInterface @S0.1 @S2 @col1 @col3
  Scenario Outline: I want to get automatically logged out and not be able to login if my account was removed from the team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <TeamOwner> removes user <Member1> from team <TeamName>
    Then I see alert informing me that my account was deleted
    And I see subtext "<Subtext>" in the deleted account alert
    When I tap OK button on the alert
    Then I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    When I tap login button on email Login Page
    Then I see invalid information alert
    And I see text "<textIncorrectCredentials>" informing me that I used incorrect credentials
    And I tap OK button on the alert on Login Page
    And I do not see conversation list

    Examples:
      | TeamOwner | Member1   | Email      | TeamName          | Subtext                                               | textIncorrectCredentials                                                          |
      | user1Name | user2Name | user2Email | SessionExpiration | You were logged out because your account was deleted. | These account credentials are incorrect. Please verify your details and try again.|


