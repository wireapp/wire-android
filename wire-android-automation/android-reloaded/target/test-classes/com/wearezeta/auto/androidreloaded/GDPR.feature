Feature: GDPR

  @TC-8132 @regression @RC @gdpr
  Scenario Outline: I want to accept sending anonymous data as a team user
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
    When I accept share data alert
    And I see conversation list
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I tap Privacy Settings menu
    Then I see send anonymous usage data switch is turned on
    When I tap back button
    And I open the debug menu
    Then I see analytics initialized is set to true
    And I see my Analytics tracking identifier

    Examples:
      | TeamOwner | TeamName | Member1   |
      | user1Name | GDPR     | user2Name |

  @TC-8134 @regression @RC @gdpr
  Scenario Outline: I want to accept sending anonymous data as a personal user
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    When I accept share data alert
    And I see conversation list
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I tap Privacy Settings menu
    Then I see send anonymous usage data switch is turned on
    When I tap back button
    And I open the debug menu
    Then I see analytics initialized is set to true
    And I see my Analytics tracking identifier

    Examples:
      | User      |
      | user1Name |

  @TC-8133 @regression @RC @gdpr
  Scenario Outline: I want to decline sending anonymous data as a team user
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
    When I decline share data alert
    And I see conversation list
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I tap Privacy Settings menu
    Then I see send anonymous usage data switch is turned off
    When I tap back button
    And I open the debug menu
    Then I see analytics initialized is set to false
    And I see my Analytics tracking identifier

    Examples:
      | TeamOwner | TeamName | Member1   |
      | user1Name | GDPR     | user2Name |

  @TC-8135 @regression @RC @gdpr
  Scenario Outline: I want to decline sending anonymous data as a personal user
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    When I decline share data alert
    And I see conversation list
    And I open the main navigation menu
    And I tap on Settings menu entry
    And I tap Privacy Settings menu
    Then I see send anonymous usage data switch is turned off
    When I tap back button
    And I open the debug menu
    Then I see analytics initialized is set to false
    And I see my Analytics tracking identifier

    Examples:
      | User      |
      | user1Name |
