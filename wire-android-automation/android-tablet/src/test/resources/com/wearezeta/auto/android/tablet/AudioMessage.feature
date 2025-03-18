Feature: Audio Message

  @C162660 @regression
  Scenario Outline: Verify sending voice message by long tap > release the thumb > tap on icon
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I long tap Audio message button <TapDuration> seconds from cursor toolbar
    And I tap audio recording Send button
    Then I see cursor toolbar
    And I see Audio Message container in the conversation view

    Examples:
      | Name      | Contact   | TapDuration |
      | user1Name | user2Name | 5           |

  @C162661 @regression
  Scenario Outline: Verify cancelling sending voice message
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I long tap Audio message button <TapDuration> seconds from cursor toolbar
    And I tap audio recording Cancel button
    Then I see cursor toolbar
    And I do not see Audio Message container in the conversation view

    Examples:
      | Name      | Contact   | TapDuration |
      | user1Name | user2Name | 5           |

  @C162659 @regression
  Scenario Outline: Verify sending voice message by long tap > swipe up
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I long tap Audio message button <TapDuration> seconds from cursor toolbar
    And I tap audio recording Send button
    Then I see cursor toolbar
    And I see Audio Message container in the conversation view

    Examples:
      | Name      | Contact   | TapDuration |
      | user1Name | user2Name | 5           |
