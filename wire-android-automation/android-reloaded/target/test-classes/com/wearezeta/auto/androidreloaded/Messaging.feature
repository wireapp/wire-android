Feature: Messaging

  @TC-4450 @regression @RC @messaging @smoke
  Scenario Outline: I want to exchange a message in a 1:1 conversation with a user from my team
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
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" to User Myself
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Message2           |
      | user1Name | Messaging | user2Name | Hello!  | Hello to you, too! |

  @TC-4452 @regression @RC @messaging
  Scenario Outline: I want to exchange a very long message in a 1:1 conversation with a user from my team
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
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When I type a generic message with <NumberOfChars> characters into text input field
    And I tap send button
    Then I see a message is displayed in the conversation view
    When User <Member1> sends message "<Message>" to User Myself
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | NumberOfChars | Message |
      | user1Name | Messaging | user2Name | 8000          | Hello! |

  @TC-4451 @regression @RC @messaging
  Scenario Outline: I want to exchange a very long message in a group conversation with users from my team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I type a generic message with <NumberOfChars> characters into text input field
    And I tap send button
    Then I see a message is displayed in the conversation view
    When User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | NumberOfChars | Message |
      | user1Name | Messaging | user2Name | LongMessage       | 8000          | Hello!  | 

  @TC-4453 @regression @RC @messaging
  Scenario Outline: I should not be able to send a message which is longer then 8000 characters
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I type a generic message with <NumberOfChars> characters into text input field and remember it before I send it
    And I tap send button
    And I do not see the remembered message displayed in the conversation view
    When User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | NumberOfChars | Message |
      | user1Name | Messaging | user2Name | LongMessage       | 8002          | Hello!  |

  ######################
  # Sending messages when offline
  ######################

  @TC-4454 @TC-4456 @regression @RC @messaging
  Scenario Outline: I want to resend my message when it was not sent due to network issues
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I disable Wi-Fi on the device
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "Message could not be sent due to connectivity issues." in current conversation
    And I see Retry button in current conversation
    And I see Cancel button in current conversation
    # TC-4456 - I want to see the retry button again when I was not able to send my message due to network issues and did not recover yet
    When I tap on Retry button in current conversation
    Then I see the message "Message could not be sent due to connectivity issues." in current conversation
    And I see Retry button in current conversation
    And I see Cancel button in current conversation
    When I enable Wi-Fi on the device
    And I wait until Wifi is enabled again
    And I tap on Retry button in current conversation
    Then I see the message "<Message>" in current conversation
    And I do not see the message "Message could not be sent due to connectivity issues." in current conversation
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message                                      | Message2             |
      | user1Name | Messaging | user2Name | GoingOffline      | Going offline is healthy from time to time.  | Welcome back online. |

  @TC-4455 @regression @RC @messaging
  Scenario Outline: I want to cancel resending my message when it was not sent due to network issues
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I disable Wi-Fi on the device
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "Message could not be sent due to connectivity issues." in current conversation
    And I see Retry button in current conversation
    And I see Cancel button in current conversation
    When I enable Wi-Fi on the device
    And I wait until Wifi is enabled again
    And I tap on Cancel button in current conversation
    And I tap delete for me button
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "Message could not be sent due to connectivity issues." in current conversation
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message                                      | Message2             |
      | user1Name | Messaging | user2Name | GoingOffline      | Going offline is healthy from time to time.  | Welcome back online! |
