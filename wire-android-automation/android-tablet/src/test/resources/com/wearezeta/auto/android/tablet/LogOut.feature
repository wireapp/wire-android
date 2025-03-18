Feature: Log Out

  @C745 @regression
  Scenario Outline: Sign out from Wire in portrait mode
    Given There is 1 user where <Name> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversations
    When I open Self profile
    And I tap Settings button on Self profile page
    And I select "Account" settings menu item
    And I select "Log out" settings menu item
    And I confirm sign out
    Then I see welcome page

    Examples:
      | Name      |
      | user1Name |
