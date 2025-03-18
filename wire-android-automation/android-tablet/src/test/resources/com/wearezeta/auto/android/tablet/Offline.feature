Feature: Offline Mode

  @C778 @regression
  Scenario Outline: Receive updated content when changing from offline to online
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    Given User <Contact> sends message "<Message1>" to user Myself
    Given I see the message "<Message1>" in the conversation view
    Given I disable Wi-Fi on the device
    When User <Contact> sends image "<Picture>" to single user conversation <Name>
    And I wait for 3 seconds
    Then I do not see any pictures in the conversation view
    When User <Contact> sends message "<Message2>" to user Myself
    And I wait for 3 seconds
    Then I do not see the message "<Message2>" in the conversation view
    When I enable Wi-Fi on the device
    # To let the content load properly after offline mode
    And I wait for 10 seconds
    Then I see the message "<Message2>" in the conversation view
    And I see a picture in the conversation view

    Examples:
      | Name      | Contact   | Message1 | Message2  | Picture     |
      | user1Name | user2Name | FirstMsg | SecondMsg | testing.jpg |

  @C780 @regression @SQCORE-577
  Scenario Outline: I want to see an unsent indicator when I send message or image during offline
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I disable Wi-Fi on the device
    And I tap on text input
    And I type the message "<Message>" and send it by cursor Send button
    Then I see the message "<Message>" in the conversation view
    And I wait until i see the Message status contains "Waiting for connection"
    When I hide keyboard
    And I tap Add picture button from cursor toolbar
    And I tap Take Photo button on Take Picture view
    And I tap Confirm button on Take Picture view
    Then I see a picture in the conversation view
    And I scroll to the bottom of conversation view
    And I wait until i see the Message status contains "Waiting for connection"

    Examples:
      | Name      | Contact   | Message    |
      | user1Name | user2Name | My message |