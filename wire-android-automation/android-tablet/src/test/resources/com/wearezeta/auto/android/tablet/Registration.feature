Feature: Registration

  @C753 @regression @smoke @useSpecialEmail
  Scenario Outline: I want to be able to register with email
    When I tap Create an account button on Welcome page
    And I tap Create a Personal Account button
    And I select EMAIL login method
    And I enter registration name "<Name>"
    And I enter registration email "<Email>"
    And I enter registration password "<Password>"
    And I start listening for registration email
    And I submit the registration data
    Then I type and confirm email registration verification code
    And I tap Keep This One on Unique Username Takeover page as soon as it is visible
    And I see first time hint in Recent View

    Examples:
      | Email      | Password      | Name      |
      | user1Email | user1Password | user1Name |
