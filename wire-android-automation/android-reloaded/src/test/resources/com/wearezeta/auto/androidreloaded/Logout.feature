Feature: Logout

  @TC-4445 @regression @RC @logout
  Scenario Outline: I want to successfully login and log out of the app
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> is me
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
    When I tap log out button on User Profile Page
    Then I see alert informing me that I am about to clear my data when I log out
    When I tap log out button on clear data alert
    Then I see Welcome Page

    Examples:
      | TeamOwner | TeamName |
      | user1Name | Logout   |

  @TC-4447 @regression @RC @logout
  Scenario Outline: I want to successfully login and log out of the app without clearing my data
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
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
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to User Myself
    And I see the message "<Message2>" in current conversation
    And I hide the keyboard
    And I tap back button
    And I tap User Profile Button
    And I see User Profile Page
    When I tap log out button on User Profile Page
    Then I see alert informing me that I am about to clear my data when I log out
    When I tap log out button on clear data alert
    Then I see Welcome Page
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    Then I see conversation list
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2           |
      | user1Name | user2Name | user3Name | Logout   | Hello!  | Hello to you, too! |

  @TC-4446 @regression @RC @logout
  Scenario Outline: I want to successfully login and log out of the app and clear my data
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
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
    And I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" to User Myself
    And I see the message "<Message2>" in current conversation
    And I hide the keyboard
    And I tap back button
    And I tap User Profile Button
    And I see User Profile Page
    When I tap log out button on User Profile Page
    Then I see alert informing me that I am about to clear my data when I log out
    And I see option to "<clearData>" when I will log out
    And I select checkbox to clear my data
    When I tap log out button on clear data alert
    Then I see Welcome Page
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation "<TeamOwner>" in conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    And I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2           | clearData                                                             |
      | user1Name | user2Name | user3Name | Logout   | Hello!  | Hello to you, too! | Delete all your personal information and conversations on this device |
