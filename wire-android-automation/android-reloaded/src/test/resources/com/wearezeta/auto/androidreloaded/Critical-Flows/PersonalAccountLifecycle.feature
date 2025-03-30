Feature: Personal Account Lifecycle

  @TC-8609 @CriticalFlows
  Scenario Outline: I want to successfully create a personal account and also delete the account
    Given User registration and team creation is enabled on the build
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
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
    And I hide the keyboard
    And I tap show password icon on Create a Personal Account Page
    And I see my password "<PasswordClearText>" in cleartext
    And I tap hide password icon on Create a Personal Account Page
    And I tap continue button on Create a Personal Account Page
    And I type the verification code for user <Name> on Create a Personal Account Page
    Then I see Personal Account Created Success Page
    And I see Get Started button on Personal Account Created Success Page
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
    When I tap on start a new conversation button
    And I tap on search people field
    And I type unique user name "<Uniqueusername>" in search field
    And I see user name "<TeamOwnerB>" in Search result list
    And I tap on user name <TeamOwnerB> found on search page
    Then I see username "<TeamOwnerB>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    And I tap close button on unconnected user profile page
    And I tap the back arrow inside the search people field
    And I close the search page through X icon
    And I see conversation "<TeamOwnerB>" is having pending status in conversation list
    And User <TeamOwnerB> accepts all requests
    And I wait for 1 seconds
    Then I see conversation "<TeamOwnerB>" in conversation list
    When I tap on conversation name "<TeamOwnerB>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <TeamOwnerB> sends message "<Message2>" to User <Name>
    And I hide the keyboard
    Then I see the message "<Message2>" in current conversation
    When I open conversation details for 1:1 conversation with "<TeamOwnerB>"
    And I tap show more options button on user profile screen
    And I tap on Block option
    And I tap Block button on alert
    Then I see toast message "<TeamOwnerB> blocked" in user profile screen
    And I see Blocked label
    And I see Unblock User button
    When I close the user profile through the close button
    And I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    Then I see my profile name "<Name>" is displayed
    And I see my username "@<UserName>" is displayed
    And I see my email address "<Name1Email>" is displayed
    And I see my domain "<domain>" is displayed
    And I see delete account button
    When I tap delete account button
    And I see delete account confirmation alert
    And I tap continue button on delete account alert
    Then I do not see delete account alert confirmation

    Examples:
      | Email      | UserName            | TeamOwnerB | Name       | LastName     | TeamNameB  | Uniqueusername      | Password       | ConfirmPassword | PasswordClearText  | Message | Message2           | domain            | Name1Email | SuccessText                                                                                           | InfoTextUserName                                                               |
      | user1Email | user1UniqueUsername | user2Name  | user1Name  | user1Name    | ChatFriend | user2UniqueUsername | user1Password  | user1Password   | user1Password      | Hello!  | Hello to you, too! | staging.zinfra.io | User1Email | You have successfully created your personal account. Start communicating securely – wherever you are! | Enter your username. It helps others to find you in Wire and connect with you. |
