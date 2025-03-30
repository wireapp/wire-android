@mobtown
Feature: Text Messages

  @TC-4671
  Scenario Outline: I want to send and receive text messages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds a new device <ContactDevice> with label <ContactDevice>
    And I see Welcome Page
    And I open <Backend> backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation "<Member1>" in conversation list
    And User <Member1> sends message "<Message1>" via device <ContactDevice> to User Myself
    And I wait until the notification popup disappears
    And I tap on unread conversation name "<Member1>" in conversation list
    And I see the message "<Message1>" in current conversation
    And I type the message "<Message2>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message2>" in current conversation

    Examples:
      | Backend       | TeamOwner | TeamName  | Member1   | Message1       | Message2        | ContactDevice |
      | mobtown-ernie | user1Name | Messaging | user2Name | First message! | Second message! | Device1       |