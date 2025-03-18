Feature: Settings

  ######################
  # Account Details
  ######################

  @TC-4545 @regression @RC @settings
  Scenario Outline: I want to see my user details in My Account section
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <Member1> sets their unique username
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
    When I open my account details menu
    Then I see my profile name "<Member1>" is displayed
    And I see my username "@<UniqueUsername>" is displayed
    And I see my email address "<Member1Email>" is displayed
    And I see my team name "<TeamName>" is displayed
    And I see my domain "<domain>" is displayed

    Examples:
      | TeamOwner | Member1   | TeamName      | UniqueUsername      | Member1Email | domain            |
      | user1Name | user2Name | MyAmazingTeam | user2UniqueUsername | user2Email   | staging.zinfra.io |

  @TC-4543 @regression @RC @settings
  Scenario Outline: I want to see an option to reset my password in the settings as a team user
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    Then I see reset password button
    When I tap reset password button
    Then I see the Wire app is not in foreground
    And I see webpage with "<URL>" is in foreground

    Examples:
      | TeamOwner | Member1   | TeamName      | URL                            |
      | user1Name | user2Name | ResetPassword | wire-account-staging.zinfra.io |

  @TC-4544 @regression @RC @settings
  Scenario Outline: I want to see an option to reset my password in the settings as a personal user
    Given There is a personal user <User>
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open my account details menu
    Then I see reset password button
    When I tap reset password button
    Then I see the Wire app is not in foreground
    And I see webpage with "<URL>" is in foreground

    Examples:
      | User      | URL                            |
      | user1Name | wire-account-staging.zinfra.io |

  @TC-4546 @regression @RC @settings
  Scenario Outline: I want to successfully change my profile name
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
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
    And I open my account details menu
    And I see my profile name "<Member1>" is displayed
    When I tap on my profile name "<Member1>" in Account Details
    Then I see edit profile name page
    When I edit my profile name to "<Member1NewName>" in Account Details
    And I tap save button
    Then I see toast message "<Success>" in Account Details
    And I see my profile name "<Member1NewName>" is displayed


    Examples:
      | TeamOwner | Member1   | TeamName       | Member1NewName  | Success                   |
      | user1Name | user2Name | ChangeUserName | ThisIsMyNewName | Your profile name changed |

  ######################
  # Other Settings
  ######################

  @TC-4540 @regression @RC @settings
  Scenario Outline: I want to be able to share a bug report
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
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
    When I open report a bug menu
    Then I see the app drawer opens where I can share my bug report
    And I tap back button

    Examples:
      | TeamOwner | Member1   | TeamName      |
      | user1Name | user2Name | ResetPassword |

  @TC-4541 @TC-4542 @regression @RC @settings
  Scenario Outline: I want to turn network settings switch on and off
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
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
    When I tap Network Settings menu
    Then I see Websocket switch is at "OFF" state
    When I tap Websocket Connection button
    Then I see Websocket switch is at "ON" state
    When I minimise Wire
    And I open the notification center
    Then I see the message that my Websocket connecting is running
    And I close the notification center
    And I restart Wire
    When I tap Websocket Connection button
    Then I see Websocket switch is at "OFF" state

    Examples:
      | TeamOwner | Member1   | TeamName        |
      | user1Name | user2Name | NetworkSettings |
