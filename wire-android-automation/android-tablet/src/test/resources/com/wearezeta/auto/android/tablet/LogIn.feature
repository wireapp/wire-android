Feature: Log In

  @C743 @regression @smoke
  Scenario Outline: Sign in to Wire in portrait mode
    Given There is 1 user where <Name> is me
    Given I see welcome page
    When I tap Log in button on Welcome page
    And I enter login MyEmail on Login page
    And I enter password MyPassword on Login page
    And I tap Log in button
    And I accept First Time overlay
    Then I see first time hint in Recent View

    Examples:
      | Name      |
      | user1Name |

  @C750 @regression
  Scenario Outline: Negative case for sign in portrait mode
    Given I see welcome page
    Given I see Log in button on Welcome page
    Given I tap Log in button on Welcome page
    When I enter login <Login> on Login page
    And I enter password <Password> on Login page
    Then I do not see Log in button on Welcome page

    Examples:
      | Login | Password  |
      | aaa   | aaabbbccc |

  @C781 @regression
  Scenario Outline: Verify reset password button works from sign-in page
    Given I see welcome page
    Given I tap Log in button on Welcome page
    When I tap Forgot Password button on Login page
    Then I see URL <URL> in browser

    Examples:
      | URL                       |
      | account-staging.zinfra.io |

  @C480 @regression
  Scenario Outline: Verify Sign In progress behaviour while there are problems with internet connectivity
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to Myself
    Given I see welcome page
    Given I tap Log in button on Welcome page
    When I disable Wi-Fi on the device
    And I enter login MyEmail on Login page
    And I enter password MyPassword on Login page
    And I tap Log in button
    Then I see alert message containing "<ErrMessage>" in the body
    When I enable Wi-Fi on the device
    And I do not see No Internet bar in 15 seconds
    And I accept the error message
    And I tap Log in button
    And I accept First Time overlay
    Then I see Recent View with conversations

    Examples:
      | Name      | Contact   | ErrMessage                                           |
      | user1Name | user2Name | Please check your Internet connection and try again  |

  @C162658 @regression @SF.Provisioning @TSFI.RESTfulAPI @S0.1 @S2
  Scenario Outline: Verify you can remove extra devices and log in successfully if too many devices are registered for your account
    Given There is 1 user where <Name> is me
    Given Users add the following devices: {"Myself": [{"name": "Device1", "label": "Device1"}, {"name": "<DeviceToRemove>", "label": "<DeviceToRemove>"}, {"name": "<DeviceToRemoveWithPassword>", "label": "<DeviceToRemoveWithPassword>"}, {"name": "<OtherDevice>", "label": "<OtherDevice>"}, {"name": "Device5", "label": "Device5"}, {"name": "Device6", "label": "Device6"}, {"name": "Device7", "label": "Device7"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay before removing a device
    When I see Manage Devices overlay
    And I tap Manage Devices button on Manage Devices overlay
    And I select "<DeviceToRemove>" settings menu item
    And I select "Remove device" settings menu item
    And I see "<DeviceToRemoveWithPassword>" settings menu item
    And I do not see "<DeviceToRemove>" settings menu item
    # C145960
    And I select "<DeviceToRemoveWithPassword>" settings menu item
    And I select "Remove device" settings menu item
    And I enter <Password> into the device removal password confirmation dialog
    And I tap OK button on the password confirmation dialog
    And I see "<OtherDevice>" settings menu item
    And I do not see "<DeviceToRemoveWithPassword>" settings menu item
    And I tap Back button 1 times
    When I do not see Manage Devices overlay
    Then I see Recent View with no conversations

    Examples:
      | Name      | DeviceToRemoveWithPassword | DeviceToRemove | OtherDevice | Password      |
      | user1Name | Device2                    | Device3        | Device4     | user1Password |
    