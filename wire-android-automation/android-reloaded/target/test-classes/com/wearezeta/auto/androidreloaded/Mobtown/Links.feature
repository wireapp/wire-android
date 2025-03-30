@mobtown
Feature: Links

  @TC-4669
  Scenario Outline: I should not see url of other ingress instance when reset my password
    Given There is a team owner "<TeamOwner>" with team "My Team"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open mobtown-ernie backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    When I tap reset password button
    Then I see webpage with "wire.systems" is in foreground
    And I do not see webpage with "wire.link" is in foreground

    Examples:
      | TeamOwner |
      | user1Name |

  @TC-4670
  Scenario Outline:  I should not see url of other ingress instance on support links
    Given There is a team owner "<TeamOwner>" with team "My Team"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open mobtown-ernie backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I tap on menu button on conversation list
    When I tap on Support menu entry
    Then I see webpage with "support.wire.com" is in foreground
    And I do not see webpage with "wire.link" is in foreground
    And I close the page through the X icon
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    When I tap Support in Settings
    Then I see webpage with "support.wire.com" is in foreground
    And I do not see webpage with "wire.link" is in foreground

    Examples:
      | TeamOwner |
      | user1Name |