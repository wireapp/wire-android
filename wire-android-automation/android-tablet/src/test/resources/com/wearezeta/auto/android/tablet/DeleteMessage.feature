Feature: Delete Message

  @C164770 @regression @smoke
  Scenario Outline: Verify deleting received text message
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Waiting for sync to be done in the background
    Given I wait for 5 seconds
    Given User <Contact> sends message "<Message>" to user Myself
    Given I see Recent View with conversations
    Given I see the subtitle "<Message>" of conversation <Contact>
    Given I tap on conversation name "<Contact>"
    And  I see the message "<Message>" in the conversation view
    When I long tap the Text message "<Message>" in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see the message "<Message>" in the conversation view

    Examples:
      | Name      | Contact   | Message           |
      | user1Name | user2Name | DeleteTextMessage |
