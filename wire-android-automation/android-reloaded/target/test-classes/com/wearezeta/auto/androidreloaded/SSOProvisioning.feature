Feature: SSO Provisioning

  @TC-4550 @regression @RC @settings @SSO
  Scenario Outline: I should not see an option to reset my password when I am logged in with SSO
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    And I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    Then I do not see reset password button

    Examples:
      | TeamOwner | OktaMember1 | UserName            |  TeamName      |
      | user1Name | user2Name   | user2UniqueUsername |  ResetPassword |

  @TC-4551 @regression @RC @settings @SSO
  Scenario Outline: I should not be able to change my profile name when I am logged in with SSO and am managed by SCIM
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta and SCIM
    And SSO user <OktaMember1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    And I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    And I see my profile name "<OktaMember1>" is displayed
    When I tap on my profile name "<OktaMember1>" in Account Details
    Then I do not see edit profile name page

    Examples:
      | TeamOwner | OktaMember1 | TeamName       |
      | user1Name | user2Name   | ChangeUserName |