Feature: Clear Content

  @TC-4271 @regression @RC @groups @clearContent
  Scenario Outline: I want to verify that I can clear the content of a group conversation from group details
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap clear content button on group details page
    And I tap clear content confirm button on group details page
    Then I see "Group content was deleted" toast message on group details page
    When I close the group conversation details through X icon
    And I see conversation view with "<GroupConversation>" is in foreground
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2     | GroupConversation |
      | user1Name | user2Name | user3Name | Clearing | Hello!  | Good Morning | ClearContent      |

  @TC-4272 @regression @RC @clearContent @smoke
  Scenario Outline: I want to verify that I can clear the content of a group conversation from conversation list
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I wait until the notification popup disappears
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I close the conversation view through the back arrow
    When I long tap on conversation name "<GroupConversation>" in conversation list
    And I tap clear content button on conversation list
    And I tap clear content confirm button on conversation list
    Then I see "Group content was deleted" toast message on conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2     | GroupConversation |
      | user1Name | user2Name | user3Name | Clearing | Hello!  | Good Morning | ClearContent      |

  @TC-4273 @regression @RC @groups @clearContent
  Scenario Outline: I want to verify that I can clear the content of a group conversation from conversation list after I left the group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I see system message "You left the conversation" in conversation view
    And I close the conversation view through the back arrow
    When I long tap on conversation name "<GroupConversation>" in conversation list
    And I tap clear content button on conversation list
    And I tap clear content confirm button on conversation list
    Then I see "Group content was deleted" toast message on conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation
    #And I see system message "You left the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2     | GroupConversation |
      | user1Name | user2Name | user3Name | Clearing | Hello!  | Good Morning | ClearContent      |

  @TC-4274 @regression @RC @groups @clearContent
  Scenario Outline: I want to verify that I can clear the content of a group conversation from conversation list after I was removed from the group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <TeamOwner> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    When User <TeamOwner> removes user <Member1> from group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> removed you from the conversation" in conversation view
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    When I close the conversation view through the back arrow
    And I long tap on conversation name "<GroupConversation>" in conversation list
    And I tap clear content button on conversation list
    And I tap clear content confirm button on conversation list
    Then I see "Group content was deleted" toast message on conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation
    #And I see system message "<TeamOwner> removed you from the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2     | GroupConversation |
      | user1Name | user2Name | user3Name | Clearing | Hello!  | Good Morning | ClearContent      |

  @TC-4275 @regression @RC @groups @clearContent
  Scenario Outline: I want to verify that I can clear the content of a group conversation which contains assets and messages from conversation list
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> adds a new device Device1 with label Device1
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And User <Member2> sends image "testing.jpg" to conversation <GroupConversation>
    And I see an image in the conversation view
    And User <Member1> sends local audio file named "test.m4a" via device Device1 to group conversation "<GroupConversation>"
    And I see an audio file in the conversation view
    And User <Member1> sends local video named "testing.mp4" via device Device1 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    And I see a file with name "testing.mp4" in the conversation view
    And User <Member1> sends 1.00MB file having name "qa_random.txt" and MIME type "text/plain" via device Device1 to group conversation "<GroupConversation>"
    And I see a file with name "qa_random.txt" in the conversation view
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap clear content button on group details page
    And I tap clear content confirm button on group details page
    Then I see "Group content was deleted" toast message on group details page
    When I close the group conversation details through X icon
    And I see conversation view with "<GroupConversation>" is in foreground
    Then I do not see the message "<Message>" in current conversation
    And I do not see the message "<Message2>" in current conversation
    And I do not see an image in the conversation view
    And I do not see an audio file in the conversation view
    And I do not see a file with name "testing.mp4" in the conversation view
    And I do not see a file with name "qa_random.txt" in the conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | Message | Message2     | GroupConversation |
      | user1Name | user2Name | user3Name | Clearing | Hello!  | Good Morning | ClearContent      |