Feature: Share Location

  @C162657 @regression
  Scenario Outline: Verify you can share Location from conversation view
    Given There is 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Contact> sends message "<Message>" to user Myself
    Given I wait for 3 seconds
    Given I see the subtitle "<Message>" of conversation <Contact>
    Given I tap on conversation name "<Contact>"
    When I tap Share location button from cursor toolbar
    # Let it to find the location
      And I wait for 5 seconds
      And I tap OK Button on Share Location popup
      And I tap Send button on Share Location page
    Then I see Share Location container in the conversation view

    Examples:
      | Name      | Contact   | Message |
      | user1Name | user2Name | Morgen  |
