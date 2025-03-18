Feature: E2EE

  @C457858 @regression
  Scenario Outline: Verify audio call degradation when other user adds a new device
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User <Contact1> sets the unique username
    Given <Contact1> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given <Contact1> accepts next incoming call automatically
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact1>"
    Given I tap conversation name from top toolbar
    Given I switch tab to "Devices" in Single connected user details page
    Given I verify 1st device on Device list page
    Given I tap back button
    When User adds the following device: {"<Contact1>": [{"label": "C1"}]}
    And I tap Audio Call button from top toolbar
    And I wait for 2 seconds
    Then I see Call degradation overlay
    When I tap OK button on Call degradation overlay
    Then I do not see Call degradation overlay
    And I do not see outgoing call
    When I see conversation view
    When I tap conversation name from top toolbar
    And I switch tab to "Devices" in Single connected user details page
    And I verify 2nd device on Device list page
    And I tap Back button
    And I tap Audio Call button from top toolbar
    Then I do not see Call degradation overlay
    And <Contact1> verifies that waiting instance status is changed to active in 30 seconds

    Examples:
      | Name      | Contact1  | CallBackend |
      | user1Name | user2Name | chrome      |

  @C661266 @regression
  Scenario Outline: Verify sending text message on degraded conversation after call was trying to establish
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User <Contact1> sets the unique username
    Given <Contact1> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given <Contact1> accepts next incoming call automatically
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact1>"
    Given I tap conversation name from top toolbar
    Given I switch tab to "Devices" in Single connected user details page
    Given I verify 1st device on Device list page
    Given I tap Back button
    When User adds the following device: {"<Contact1>": [{"label": "C1"}]}
    And I tap Audio Call button from top toolbar
    Then I see Call degradation overlay
    When I tap OK button on Call degradation overlay
    And I tap on text input
    And I type the message "<Message>" and send it by cursor Send button without hiding keyboard
    And I tap SEND ANYWAY button on degradation overlay page
    Then I do not see degradation overlay page
      And I see the message "<Message>" in the conversation view

    Examples:
      | Name      | Contact1  | CallBackend | Message |
      | user1Name | user2Name | chrome      | Hello   |
