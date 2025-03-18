Feature: App lock

  @TC-8143 @regression @RC @applock
  Scenario Outline: I want to set up app lock for my app and verify that app is locked after 1 minute in the background
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I see lock with passcode toggle is turned off
    When I tap on lock with passcode toggle
    Then I see set up app lock page
    And I see description text that app will lock after 1 minute of inactivity
    When I enter my passcode "<Passcode>" for app lock
    And I tap set passcode button
    Then I see lock with passcode toggle is turned on
    When I tap back button
    And I see conversation list
    And I minimise Wire
    And I wait for 60 seconds
    And I restart Wire
    And I see app lock page
    And I enter my passcode "<Passcode>" for app lock
    And I tap unlock button on app lock page
    Then I see conversation list

    Examples:
      | TeamOwner | TeamName | Member1   | Passcode     |
      | user1Name | AppLock  | user2Name | Qwertz12345! |

  @TC-8144 @regression @RC @applock
  Scenario Outline: I should not be able to unlock my app if I have app lock set up and enter the wrong passphrase
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I see lock with passcode toggle is turned off
    When I tap on lock with passcode toggle
    Then I see set up app lock page
    And I see description text that app will lock after 1 minute of inactivity
    When I enter my passcode "<Passcode>" for app lock
    And I tap set passcode button
    Then I see lock with passcode toggle is turned on
    When I tap back button
    And I see conversation list
    And I minimise Wire
    And I wait for 60 seconds
    And I restart Wire
    And I see app lock page
    And I enter my passcode "<WrongPasscode>" for app lock
    And I tap unlock button on app lock page
    Then I see error message on app lock page
    And I do not see conversation list
    When I clear the password field on app lock page
    And I enter my passcode "<Passcode>" for app lock
    And I tap unlock button on app lock page
    Then I see conversation list

    Examples:
      | TeamOwner | TeamName | Member1   | Passcode     | WrongPasscode  |
      | user1Name | AppLock  | user2Name | Qwertz12345! | Password12345! |

  @TC-8145 @TC-8146 @regression @RC @applock
  Scenario Outline: I want to see app lock setup modal on login after app lock has been enforced for the team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <TeamOwner> enables force app lock feature for team <TeamName> with timeout of 30 seconds
    Then I see alert informing me that my Team settings have changed
    And I see subtext "<Subtext>" in the Team settings change alert
    And I tap OK button on the alert
    Then I see set up app lock page
    When I enter my passcode "<Passcode>" for app lock
    And I tap set passcode button
    And I see conversation list
    # TC-8146 - I should not be able to switch off app lock if it is enforced for the team
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    Then I see lock with passcode toggle is turned on
    And I see lock with passcode toggle can not be changed

    Examples:
      | TeamOwner | TeamName | Member1   | Passcode     | Subtext                                                                             |
      | user1Name | AppLock  | user2Name | Qwertz12345! | App lock is now mandatory. Wire will lock itself after a certain time of inactivity |