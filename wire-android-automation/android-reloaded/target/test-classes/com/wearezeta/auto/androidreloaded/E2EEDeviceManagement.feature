Feature: E2EEDeviceManagement

  @TC-4322 @regression @RC @E2EEDeviceManagement @smoke
  Scenario Outline: I want to be able to remove a device when I already have 7 devices registered and login successfully
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds 6 devices
    # Adding the 7th device with a little delay, so that it's for sure the first device on the list
    And I wait for 1 second
    And User <Member1> adds a new device Device7 with label Device7
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    When I select first device <Device> on device removal page
    Then I see remove device alert
    And I see device name <Device> on the alert
    And I see device ID on the alert
    And I see date of device creation on the alert
    When I enter my password <Password> on remove device alert
    And I tap Remove button on remove device alert
    And I decline share data alert
    Then I see conversation list
    And I see conversation "<TeamOwner>" in conversation list

    Examples:
      | TeamOwner | Member1   | TeamName         | Device  | Password      |
      | user1Name | user2Name | DeviceManagement | Device7 | user2Password |

  @TC-4326 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I should not be able to remove a device with wrong password
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds 6 devices
    # Adding the 7th device with a little delay, so that it's for sure the first device on the list
    And I wait for 1 second
    And User <Member1> adds a new device Device7 with label Device7
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    When I select first device <Device> on device removal page
    Then I see remove device alert
    And I see device name <Device> on the alert
    And I see device ID on the alert
    And I see date of device creation on the alert
    When I enter my invalid password <Password> on remove device alert
    And I tap Remove button on remove device alert
    Then I see invalid password error
    And I do not see conversation list

    Examples:
      | TeamOwner | Member1   | TeamName         | Device  | Password        |
      | user1Name | user2Name | DeviceManagement | Device7 | InvalidPassword |

  @TC-4323 @TC-4324 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I want to verify that I see my current and previously added devices on the devices screen
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds 2 devices
    And User <TeamOwner> adds 1 device
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on menu button on conversation list
    And I tap on Settings menu entry
    When I open manage your devices menu
    Then I see my current device is listed under devices
    And I see my other device "Device1" is listed under other devices section
    And I see my other device "Device2" is listed under other devices section
    And I close the devices screen through the back arrow
    And I open the main navigation menu
    And I tap on conversations menu entry
    And I see conversation list
    And I see conversation "<TeamOwner>" in conversation list
    # TC-4324 - I want to see other users devices displayed in their user profile
    When I tap on conversation name "<TeamOwner>" in conversation list
    And I open conversation details for 1:1 conversation with "<TeamOwner>"
    And I tap on devices tab in connected user profile
    Then I see "Desktop" is listed under devices in connected user profile

    Examples:
      | TeamOwner | Member1   | TeamName         |
      | user1Name | user2Name | DeviceManagement |

  @TC-4325 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I want to see ID and date added of my current device
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
    When I open manage your devices menu
    And I see my current device is listed under devices
    Then I see my current device has ID and date added displayed

    Examples:
      | TeamOwner | Member1   | TeamName         |
      | user1Name | user2Name | DeviceManagement |

  @TC-4327 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I want to see my device list updated when a device was removed from another client
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> adds 2 devices
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
    When I open manage your devices menu
    And I see my current device is listed under devices
    And I see my other device "Device1" is listed under other devices section
    And I see my other device "Device2" is listed under other devices section
    And I close the devices screen through the back arrow
    When User <Member1> removes OTR client with device name Device1
    And I open manage your devices menu
    Then I do not see my other device "Device1" is listed under other devices section
    And I see my current device is listed under devices
    And I see my other device "Device2" is listed under other devices section

    Examples:
      | TeamOwner | Member1   | TeamName         |
      | user1Name | user2Name | DeviceManagement |

  @TC-4328 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I want to see an alert when my account was used on another device
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
    When User <Member1> adds 1 device
    Then I see alert informing me that my account was used on another device
    And I see subtext "<Subtext>" in the added device alert
    When I tap Manage Devices button
    Then I see Manage Devices Page
    And I close the devices screen through the back arrow
    And I see conversation list
    And I do not see the connecting banner displayed

    Examples:
      | TeamOwner | Member1   | TeamName         | Subtext                                    |
      | user1Name | user2Name | DeviceManagement | remove the device and reset your password. |

  @TC-4329 @regression @RC @E2EEDeviceManagement
  Scenario Outline: I want to see an alert when my second logged in account was used on another device
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
    And I tap User Profile Button
    And I see User Profile Page
    And I see User Profile Page for account <Member1> as my currently active account
    And I tap New Team or Account button
    And I see Welcome Page
    And User <TeamOwner> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    When User <Member1> adds 1 device
    Then I see alert informing me that my second account "<Member1>" was used on another device
    And I see subtext "<Subtext>" in the added device alert
    When I tap Switch Account button
    Then I see Manage Devices Page
    And I close the devices screen through the back arrow
    And I see conversation list
    And I do not see the connecting banner displayed
    When I tap User Profile Button
    And I see User Profile Page
    Then I see User Profile Page for account <Member1> as my currently active account

    Examples:
      | TeamOwner | Member1   | TeamName         | Subtext                                                                                                            |
      | user1Name | user2Name | DeviceManagement | remove the device (navigate to “Manage Devices” in the Settings section of this account), and reset your password. |
