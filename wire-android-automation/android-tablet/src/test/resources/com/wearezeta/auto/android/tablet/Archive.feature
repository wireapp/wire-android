Feature: Archive

  @C775 @regression
  Scenario Outline: Verify you can archive and unarchive
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User Myself is connected to <Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    When I long tap on conversation name "<Contact1>"
    And I tap Archive button on Single conversation options menu
    Then I do not see conversation "<Contact1>" in Recent View
    When I open Archive
    Then I see conversation "<Contact1>" in Archive View
    When I long tap on conversation name "<Contact1>"
    And I tap Unarchive button on Single conversation options menu
    And I tap back button
    Then I see conversation "<Contact1>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  |
      | user1Name | user2Name | user3Name |
