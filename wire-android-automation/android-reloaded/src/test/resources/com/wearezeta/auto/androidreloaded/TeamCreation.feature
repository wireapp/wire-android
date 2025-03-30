Feature: Team Creation

  @TC-4558 @regression @RC @teamCreation @smoke
  Scenario Outline: I want to be able to create a team successfully
    Given User registration and team creation is enabled on the build
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap Create a Team button on Welcome Page
    Then I see Create a Team Page
    And I see link "<LearnMoreLink>" on Create a Team Page
    And I tap continue button on Create a Team Page
    And I see text "<EmailCreationText>" on Create a Team Page
    When I enter my email <Email> on Create a Team Page
    And I tap continue button on Create a Team Page
    Then I see Terms of Use alert on Create a Team Page
    When I tap continue button on Terms of Use alert on Create a Team Page
    And I enter my first name <Name> on Create a Team Page
    And I enter my last name <LastName> on Create a Team Page
    And I enter my team name <TeamName> on Create a Team Page
    And I hide the keyboard
    And I enter my password <Password> on Create a Team Page
    And I scroll to the bottom of team creation page
    And I enter my confirm password <ConfirmPassword> on Create a Team Page
    When I tap show password icon on Create a Team Page
    Then I see my password "<PasswordClearText>" in cleartext
    And I tap hide password icon on Create a Team Page
    And I tap continue button on Create a Team Page
    And I type the verification code for user <Name> on Create a Team Page
    Then I see Team Created success Page
    And I see text "<SuccessText>" on Team Created Success Page
    When I tap Get Started button on Team Created Page
    And I see Username Page
    And I see text "<InfoTextUserName>" on Username Page
    And I see help text underneath Username input field
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    And I verify team member <Name> has role Owner

  Examples:
  | Email      | UserName            | LearnMoreLink                      | EmailCreationText                     | Name      | LastName  | TeamName  | Password      | ConfirmPassword | PasswordClearText | SuccessText                                                                                       | InfoTextUserName                                                               |
  | user1Email | user1UniqueUsername | Learn more about plans and pricing | Enter your email to create your team: | user1Name | user1Name | SuperTeam | user1Password | user1Password   | user1Password     | You have successfully created your team account. Start communicating securely – wherever you are! | Enter your username. It helps others to find you in Wire and connect with you. |

  @TC-4559 @TC-4560 @TC-4561 @regression @RC @teamCreation
  Scenario Outline: I should see errors when I try to create a team with invalid credentials
    Given User registration and team creation is enabled on the build
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap Create a Team button on Welcome Page
    Then I see Create a Team Page
    And I tap continue button on Create a Team Page
    # TC-4559 - I want to see an error when I try to register with an invalid email
    When I enter my invalid email <InvalidEmail> on Create a Team Page
    And I tap continue button on Create a Team Page
    Then I see an error informing me that "<ErrorInvalidEmail>" on Create a Team Page
    And I clear the email input field on Create a Team Page
    When I enter my email <Email> on Create a Team Page
    And I tap continue button on Create a Team Page
    And I see Terms of Use alert on Create a Team Page
    And I tap continue button on Terms of Use alert on Create a Team Page
    And I enter my first name <FirstName> on Create a Team Page
    And I enter my last name <LastName> on Create a Team Page
    And I enter my team name <TeamName> on Create a Team Page
    # TC-4560 - I want to see an error when I use an invalid password
    When I enter my invalid password <InvalidPassword> on Create a Team Page
    And I hide the keyboard
    And I enter my invalid confirm password <InvalidPassword> on Create a Team Page
    And I tap continue button on Create a Team Page
    Then I see an error informing me that "<InvalidPasswordError>" on Create a Team Page
    # TC-4561 - I want to see an error when my passwords do not match
    When I clear the password field on Create a Team Page
    And I enter my password <Password> on Create a Team Page
    And I clear the confirm password field on Create a Team Page
    And I enter my invalid confirm password <InvalidPassword> on Create a Team Page
    And I hide the keyboard
    And I tap continue button on Create a Team Page
    Then I see an error informing me that "<MismatchPasswordError>" on Create a Team Page

    Examples:
      | Email      | InvalidEmail           | ErrorInvalidEmail                           | FirstName  | LastName  | TeamName  | InvalidPassword | Password      | InvalidPasswordError                                                                                         | MismatchPasswordError  |
      | user1Email | smokester+invalid@wire | Please enter a valid format for your email. | user1Name  | user1Name | SuperTeam | invalidPassword | user1Password | Use at least 8 characters, with one lowercase letter, one capital letter, a number, and a special character. | Passwords do not match |

  @TC-4563 @TC-4564 @regression @RC @teamCreation
  Scenario Outline: I should see an error when I add a wrong verification code while registering a team
    Given User registration and team creation is enabled on the build
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap Create a Team button on Welcome Page
    Then I see Create a Team Page
    And I tap continue button on Create a Team Page
    When I enter my email <Email> on Create a Team Page
    And I tap continue button on Create a Team Page
    Then I see Terms of Use alert on Create a Team Page
    When I tap continue button on Terms of Use alert on Create a Team Page
    And I enter my first name <FirstName> on Create a Team Page
    And I enter my last name <LastName> on Create a Team Page
    And I enter my team name <TeamName> on Create a Team Page
    And I enter my password <Password> on Create a Team Page
    And I hide the keyboard
    And I enter my confirm password <ConfirmPassword> on Create a Team Page
    And I tap continue button on Create a Team Page
    When I type the invalid verification code 123456 on Create a Team Page
    Then I see an error informing me that "<InvalidOTPError>" on login page
    # TC-4564 - I want to resend my verification code while registering a team
    When I tap on resend code link
    And I clear the code input field
    And I type the verification code for user <FirstName> on Create a Team Page
    Then I see Team Created success Page

    Examples:
      | Email      | FirstName  | LastName  | TeamName  | Password      | ConfirmPassword | InvalidOTPError |
      | user1Email | user1Name  | user1Name | SuperTeam | user1Password | user1Password   | Invalid code, or maximum attempts exceeded. Please retry, or request another code |

  @TC-4562 @regression @RC @teamCreation
  Scenario: I want to reach login page on submit email page while registering a team
    Given User registration and team creation is enabled on the build
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap Create a Team button on Welcome Page
    And I see Create a Team Page
    And I tap continue button on Create a Team Page
    When I tap on Login Link on Create a Team Page
    Then I see Login Page