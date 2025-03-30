Feature: E2EE Device Management

  @TC-4676 @E2EEDeviceManagement @SF.Provisioning @TSFI.RESTfulAPI @S0.1 @S2 @col1 @col3
  Scenario Outline: I want to be able to remove a device when I already have 7 devices registered and login successfully
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds 6 2FA devices
    # Adding the 7th device with a little delay, so that it's for sure the first device on the list
    And I wait for 1 second
    And User <Member1> adds a new 2FA device Device7 with label Device7
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    # This delay is needed to workaround backend rate limiting
    And I wait for 300 seconds
    And I sign in using my email
    And I now start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    When I select first device <Device> on device removal page
    Then I see remove device alert
    And I see device name <Device> on the alert
    And I see device ID on the alert
    And I see date of device creation on the alert
    When I enter my password <Password> on remove device alert
    And I tap Remove button on remove device alert
    Then I see conversation list
    And I see conversation "<TeamOwner>" in conversation list

    Examples:
      | TeamOwner | Member1   | Email      |  TeamName         | Device  | Password      |
      | user1Name | user2Name | user2Email | DeviceManagement | Device7 | user1Password |

  @TC-4677 @E2EEDeviceManagement @SF.Provisioning @TSFI.RESTfulAPI @S0.1 @S2 @col1 @col3
  Scenario Outline: I should not be able to remove a device with wrong password
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds 6 2FA devices
    # Adding the 7th device with a little delay, so that it's for sure the first device on the list
    And I wait for 1 second
    And User <Member1> adds a new 2FA device Device7 with label Device7
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    # This delay is needed to workaround backend rate limiting
    And I wait for 300 seconds
    And I sign in using my email
    And I now start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    When I select first device <Device> on device removal page
    Then I see remove device alert
    And I see device name <Device> on the alert
    And I see device ID on the alert
    And I see date of device creation on the alert
    When I enter my password <Password> on remove device alert
    And I tap Remove button on remove device alert
    Then I see invalid password error
    And I do not see conversation list

    Examples:
      | TeamOwner | Member1   | Email      | TeamName         | Device  | Password        |
      | user1Name | user2Name | user2Email | DeviceManagement | Device7 | InvalidPassword |