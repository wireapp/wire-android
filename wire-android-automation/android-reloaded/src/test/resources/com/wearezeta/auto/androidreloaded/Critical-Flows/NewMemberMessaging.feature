Feature: New Member Messaging

  @TC-8605 @CriticalFlows
  Scenario Outline: I want to join new team, exchange messages, and get added to a group chat
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to group conversation "<GroupConversation>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<TeamOwnerUniqueUserName>" in search field
    Then I see user name "<TeamOwner>" in Search result list
    And I tap on user name "<TeamOwner>" in Search result list
    And I see start conversation button on connected user profile page
    When I tap start conversation button on connected user profile page
    And I type the message "<Message>" into text input field
    And I tap send button
    Then I see a message is displayed in the conversation view
    And I close the conversation view through the back arrow
    And I close the user profile through the close button
    When I tap the back arrow inside the search people field
    And I click close button on New Conversation screen to go back conversation details
    Then I see conversation list
    And User <TeamOwner> sends message "<Message2>" to User Myself
    When I tap on unread conversation name "<TeamOwner>" in conversation list
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    And I tap on conversation name "<GroupConversation>" in conversation list
    And User <TeamOwner> sends mention "@<Member1>" to group conversation <GroupConversation>
    Then I see the last mention is "@<Member1>" in current conversation
    
    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Message          | Message2         | GroupConversation | TeamOwnerUniqueUserName |
      | user1Name | Messaging | user2Name | user3Name | Hello Team Owner | Hello new member | MyTeam            | user1UniqueUsername     |
