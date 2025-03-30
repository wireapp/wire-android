Feature: Session Expiration

  @TC-4537 @regression @RC @sessionExpiration
  Scenario Outline: I want to see the appropriate device is signed out if it was removed
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <Member1> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    And I see subtext "<Subtext>" in the removed device alert
    When I tap OK button on the alert
    Then I see Welcome Page

    Examples:
      | TeamOwner | Member1   | TeamName          | Subtext                                              |
      | user1Name | user2Name | SessionExpiration | You were logged out because your device was removed. |

  @TC-4538 @regression @RC @sessionExpiration @multipleAccounts
  Scenario Outline: I want to see the logout working as expected when I am logged in with multiple accounts when a client of mine is deleted
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
    When User <MemberBella> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    When I tap OK button on the alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberStaging> as my currently active account
    And I see my other account <MemberAnta> is listed under other logged in accounts
    When User <MemberStaging> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    When I tap OK button on the alert
    Then I see conversation list
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <MemberAnta> as my currently active account
    And I don't see any other accounts logged in
    When User <MemberAnta> removes all their registered OTR clients
    Then I see alert informing me that my device was removed
    When I tap OK button on the alert
    Then I see Welcome Page

    Examples:
      | TeamOwnerStaging | TeamOwnerAnta   | TeamOwnerBella   | MemberStaging | MemberAnta | MemberBella | TeamNameStaging | TeamNameAnta | TeamNameBella |
      | user1Name        | user2Name       | user3Name        | user4Name     | user5Name  | user6Name   | TeamStaging     | TeamAnta     | TeamBella     |

  @TC-4539 @regression @RC @sessionExpiration
  Scenario Outline: I want to get automatically logged out if my account was removed from the team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <TeamOwner> removes user <Member1> from team <TeamName>
    Then I see alert informing me that my account was deleted
    And I see subtext "<Subtext>" in the deleted account alert
    When I tap OK button on the alert
    Then I see Welcome Page

    Examples:
      | TeamOwner | Member1   | TeamName          | Subtext                                               |
      | user1Name | user2Name | SessionExpiration | You were logged out because your account was deleted. |
