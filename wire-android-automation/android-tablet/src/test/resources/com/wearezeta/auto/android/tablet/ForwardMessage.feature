Feature: Forward Message

  @C162655 @regression
  Scenario Outline: Text message forwarding into other conversation
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given User <Contact> sends message "<Message>" to user Myself
    Given I wait for 3 seconds
    Given I see Recent View with conversations
    Given I see the subtitle "<Message>" of conversation <Contact>
    Given I tap on conversation name "<Contact>"
    And I see the message "<Message>" in the conversation view
    When I long tap the Text message "<Message>" in the conversation view
    And I tap Share button on the message bottom menu
    Then I see the Wire app is not in foreground

    Examples:
      | Name      | Contact   | Message |
      | user1Name | user2Name | Wassap  |
