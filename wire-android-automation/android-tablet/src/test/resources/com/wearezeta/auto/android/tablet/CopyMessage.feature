Feature: Copy Message

  @C162656 @regression @smoke
  Scenario Outline: I want to verify long tap on the message shows menu 'Copy, Delete'
    Given There are 2 users where <Name> is me
    And User Myself is connected to <Contact>
    And User adds the following device: {"<Contact>": [{}]}
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I see Recent View with conversations
    And User <Contact> sends message "<Message>" to user Myself
    And I wait for 3 seconds
    And I see the subtitle "<Message>" of conversation <Contact>
    And I tap on conversation name "<Contact>"
    And I see the message "<Message>" in the conversation view
    When I long tap the Text message "<Message>" in the conversation view
    Then I see Copy button on the message bottom menu
    And I see Delete button on the message bottom menu

    Examples:
      | Name      | Contact   | Message |
      | user1Name | user2Name | Wassap  |
