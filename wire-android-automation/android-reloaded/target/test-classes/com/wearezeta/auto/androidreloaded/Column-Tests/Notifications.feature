Feature: Notifications

  @TC-4859 @col1
  Scenario Outline: I want to receive notifications in a 1:1 conversation when the app was terminated on websocket only device
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I tap on user name "<TeamOwner>" in Search result list
    And I tap start conversation button on connected user profile page
    And I see conversation view with "<TeamOwner>" is in foreground
    And I close the conversation view through the back arrow
    And I close the user profile through the close button
    And I close the search page through X icon
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    When I tap Network Settings menu
    Then I see that there is no option to enable or disable my websocket
    When I swipe the app away from background
    And I open the notification center
    And I see the message that my Websocket connecting is running
    And I close the notification center
    And User <TeamOwner> sends message "<Message>" to User Myself
    And I wait until the notification popup disappears
    And I open the notification center
    Then I see the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    When I tap on the message "<Message>" from 1:1 conversation from user <TeamOwner> in the notification center
    Then I see conversation view with "<TeamOwner>" is in foreground
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | TeamName      | Message |
      | user1Name | user2Name | user2Email | Notifications | Hello!  |
