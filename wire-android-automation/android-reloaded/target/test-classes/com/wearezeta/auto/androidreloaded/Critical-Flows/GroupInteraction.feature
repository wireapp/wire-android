Feature: Group Interaction

  @TC-8601 @CriticalFlows
  Scenario Outline: I want validate group chat flow, reactions, managing services and interactions for team owner
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>, <Member2>  to team "<TeamName>" with role Member
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> adds a new device Device1 with label Device1
#ToDo: Commented steps will be uncommented when service is no longer behind flag.
   # And User <TeamOwner> enables <BotName> service for team <TeamName>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>, <Member2>, <TeamOwnerB> in team "<TeamName>"
   # And User <TeamOwner> adds bot <BotName> to conversation <GroupConversation>
    And User <TeamOwnerB> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
#    Then I see a banner informing me that "Services are active" in the conversation view
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation
    When I long tap on the message "<Message2>" in current conversation
    And I see reactions options
    And I tap on <Reaction> icon
    Then I see a "<Reaction>" from 1 user as reaction to user <Member1> message
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When User <TeamOwner> toggles reaction "👍" on the recent message from conversation <GroupConversation> via device Device1
    Then I see a "<ReactionThumbsUp>" from 1 users as reaction to user <TeamOwnerB> message
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on Participants tab
    Then I see user <Member2> in participants list
    When I close the group conversation details through X icon
    And User <TeamOwner> removes user <Member2> from group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> removed <Member2> from the conversation" in conversation view
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
#    And I see user <BotName> in participants list
#    And I tap on user <BotName> in participants list
#    Then I see Remove From Group button for service
#    When I tap Remove From Group button for service
#    Then I do not see Remove From Group button for service
#    And I see toast message "Service removed from group" in user profile screen
#    When I tap back button
#    Then I do not see user <BotName> in participants list
#    When I close the group conversation details through X icon
#    Then I see system message "You removed <BotName> from the conversation" in conversation view

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | Member2   | BotName | TeamName | TeamNameB       | GroupConversation | Message            | Message2             | ReactionThumbsUp | Reaction |
      | user1Name | user5Name  | user2Name | user3Name | Echo    | Bots     | ConnectedFriend | BotsConversation  | Hello Team Members | Hello fellow members | 👍               | ❤️       |
