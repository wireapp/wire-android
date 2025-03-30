Feature: Registration

  @TC-4493 @regression @RC @registration
  Scenario Outline: I want to be able to create a personal account successfully
    Given User registration and team creation is enabled on the build
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap Create a Personal Account link on Welcome Page
    Then I see Create a Personal Account Page
    And I tap continue button on Create a Personal Account Page
    When I enter my email <Email> on Create a Personal Account Page
    And I tap continue button on Create a Personal Account Page
    Then I see Terms of Use alert on Create a Personal Account Page
    When I tap continue button on Terms of Use alert on Create a Personal Account Page
    And I enter my first name <Name> on Create a Personal Account Page
    And I enter my last name <LastName> on Create a Personal Account Page
    And I enter my password <Password> on Create a Personal Account Page
    And I hide the keyboard
    And I enter my confirm password <ConfirmPassword> on Create a Personal Account Page
    And I tap show password icon on Create a Personal Account Page
    And I see my password "<PasswordClearText>" in cleartext
    And I tap hide password icon on Create a Personal Account Page
    And I tap continue button on Create a Personal Account Page
    And I type the verification code for user <Name> on Create a Personal Account Page
    Then I see Personal Account Created Success Page
    And I see text "<SuccessText>" on Personal Account Created Success Page
    When I tap Get Started button on Personal Account Created Success Page
    And I see Username Page
    And I see text "<InfoTextUserName>" on Username Page
    And I see help text underneath Username input field
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list

  Examples:
  | Email      | UserName            | Name      | LastName  | Password      | ConfirmPassword | PasswordClearText | SuccessText                                                                                           | InfoTextUserName                                                               |
  | user1Email | user1UniqueUsername | user1Name | user1Name | user1Password | user1Password   | user1Password     | You have successfully created your personal account. Start communicating securely – wherever you are! | Enter your username. It helps others to find you in Wire and connect with you. |