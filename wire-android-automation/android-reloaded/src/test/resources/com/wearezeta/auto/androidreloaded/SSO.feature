Feature: SSO

  @TC-4547 @regression @RC @login @SSO
  Scenario Outline: I want to successfully login with SSO code
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    #FIXME: Figure out why this method is not working
    #And User Myself has 1:1 conversation with <TeamOwner> in team <TeamName>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    When I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list

    Examples:
      | TeamOwner | TeamName | OktaMember1 | UserName            |
      | user1Name | SSO      | user2Name   | user2UniqueUsername |

  @TC-4548 @regression @RC @login @SSO
  Scenario Outline: I want to see an error when logging in with SSO with wrong credentials
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    When I type an invalid SSO code <InvalidSSOCode> on SSO Login Tab
    And I tap login button on Login Page
    Then I see error message "<Message>" underneath SSO code input field
    And I clear the SSO Code input field
    And I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    When I sign in with invalid email "<InvalidEmail>" credentials on Okta Page
    And I sign in with invalid password "<InvalidPassword>" credentials on Okta Page
    And I tap login button on Okta Page
    And I see error message telling me that I am unable to sign in on Okta Page

    Examples:
      | TeamOwner | TeamName | OktaMember1 | InvalidSSOCode | Message                | InvalidEmail                 | InvalidPassword         |
      | user1Name | SSO      | user2Name   | thisIsNotValid | Enter a valid SSO code | smoketester+invalid@wire.com | thisIsAnInvalidPassword |

  @TC-4549 @regression @login @SSO
    #This test works only with staging builds
  Scenario Outline: I want to login on a registered custom backend with fixed SSO(Orange)
    Given There is a QA-Fixed-SSO team owner <TeamOwner> with email smoketester+volkman764991@wire.com and password Aqa123456!
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    And I see Welcome Page
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    When I type email to redirect to QA-Fixed-SSO backend on Enterprise Login popup
    And I tap login button on Login Page
    And I tap proceed button on custom backend alert
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    Then I see conversation list

    Examples:
      | TeamOwner       | OktaMember1 | UserName            |
      | Mr. Quinn Sipes | user2Name   | user2UniqueUsername |
