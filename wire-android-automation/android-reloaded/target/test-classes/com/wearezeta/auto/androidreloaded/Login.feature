Feature: Login

  @TC-4435 @TC-4438 @TC-4439 @regression @RC @login
  Scenario Outline: I want to successfully login into the app
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    # TC-4438 - I want to see my password in cleartext when I use the eye icon
    When I tap show password icon
    Then I see my password "<Password>" in cleartext
    And I tap hide password icon
    And I tap login button on email Login Page
    # TC-4439 - I want to see a welcome message when I login for the first time
    When I wait until I am fully logged in
    And I decline share data alert
    Then I see welcome message
    And I see introduction message "<Introduction Message>"

  Examples:
  | User      | Password      | Introduction Message                                              |
  | user1Name | user1Password | Connect with others or create a new group to start collaborating! |

  @TC-4442 @regression @RC @login @SF.Channel @TSFI.UserInterface @S0.1 @S2
  Scenario Outline: I should not be able to login via email with wrong email credentials
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    When I sign in using my invalid email "<invalidEmailFormat>" and password
    And I tap login button on email Login Page
    Then I see an error informing me that "<ErrorInvalidEmailFormat>" on login page
    And I clear the email input field
    And I clear the password input field
    When I sign in using my invalid email "<invalidEmail>" and password
    And I tap login button on email Login Page
    Then I see invalid information alert
    And I see text "<textIncorrectCredentials>" informing me that I used incorrect credentials
    And I do not see conversation list

    Examples:
      | User      | invalidEmailFormat       | ErrorInvalidEmailFormat                                         |  invalidEmail                | textIncorrectCredentials                                                           |
      | user1Name | smoketester+invalid@wire | This email or username is invalid. Please verify and try again. | smoketester+invalid@wire.com | These account credentials are incorrect. Please verify your details and try again. |

  @TC-4436 @regression @RC @login @SF.Channel @TSFI.UserInterface @S0.1 @S2
  Scenario Outline: I should not be able to login via email with wrong password credentials
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    When I sign in using my email and invalid password "<invalidPassword>"
    And I tap login button on email Login Page
    Then I see invalid information alert
    And I see text "<textIncorrectCredentials>" informing me that I used incorrect credentials
    And I tap OK button on the alert on Login Page
    And I do not see conversation list

    Examples:
      | User      | invalidPassword         | textIncorrectCredentials                                                           |
      | user1Name | thisIsAnInvalidPassword | These account credentials are incorrect. Please verify your details and try again. |

  @TC-4440 @regression @RC @login
  Scenario Outline: I want to successfully login with my username into the app
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my username
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    And I see welcome message
    And I see introduction message "<Introduction Message>"

    Examples:
      | User      | Introduction Message                                              |
      | user1Name | Connect with others or create a new group to start collaborating! |

  @TC-4441 @regression @RC @login @sessionExpiration
  Scenario Outline: I want to be able to login successfully again into the app after my device was removed
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
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list

    Examples:
      | TeamOwner | Member1   | TeamName          | Subtext                                              |
      | user1Name | user2Name | SessionExpiration | You were logged out because your device was removed. |

  @TC-4443 @regression @RC @login
  Scenario Outline: I want to be redirected to the webpage when I click on forgot password link
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    When I tap on Forgot Password Link
    Then I see the Wire app is not in foreground
    And I see webpage with "<URL>" is in foreground
    And I close the page through the X icon

    Examples:
      | TeamOwner | Member1   | TeamName       | URL                            |
      | user1Name | user2Name | ForgotPassword | wire-account-staging.zinfra.io |
