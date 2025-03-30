Feature: Group Messaging

  @TC-8606 @CriticalFlows
  Scenario Outline: I want to validate group chat flow for team owner and members
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>, <Member2>, <Member3>, <Member4>, <Member5> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>, <Member2>, <Member3>, <Member4>, <Member5> in team "<TeamName>"
    Then I see conversation "<GroupConversation>" in conversation list
    When I tap on search conversation field
    And I type first 3 chars of group name "<GroupConversation>" in search field
    Then I see conversation name "<GroupConversation>" in Search result list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When I close the conversation view through the back arrow
    And User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    And I see conversation "<GroupConversation>" is having 1 unread messages in conversation list
    And I wait until the notification popup disappears
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message2>" in current conversation
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    And I see OFF timer button is currently selected
    And I see 10 seconds timer button
    And I see 5 minutes timer button
    And I see 1 hour timer button
    And I see 1 day timer button
    And I see 7 days timer button
    And I see 4 weeks timer button
    And I tap on 10 seconds timer button
    Then I see self deleting message label in text input field
    When I type the self deleting message "<Message3>" into text input field
    And I tap send button
    And I see the message "<Message3>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message3>" in current conversation
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    And I tap delete group button
    And I tap remove group button
    And I do not see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Member3   | Member4   | Member5   | Message             | Message2             | GroupConversation | Message3              |
      | user1Name | Messaging | user2Name |user3Name  | user4Name | user5Name | user6Name | Hello Team Members  | Hello fellow members | MyTeam            | Self deleting message |