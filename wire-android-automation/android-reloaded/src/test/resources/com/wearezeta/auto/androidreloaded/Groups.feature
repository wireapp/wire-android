Feature: Groups

  ######################
  # Creating Conversations
  ######################

  @TC-4345 @regression @RC @groups
  Scenario Outline: I want to be able to create a group conversation with members from my team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on start a new conversation button
    And I tap create new group button
    And I tap on search people field
    And I type user name "<TeamOwner>" in search field
    And I see user <TeamOwner> in search suggestions list
    And I select users <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user <Member2> in search suggestions list
    And I select users <Member2> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<ConversationName>" is in foreground
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <ConversationName>
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | ConversationName | Message | Message2           |
      | user1Name | user2Name | user3Name | GroupCreation | MyTeam           | Hello!  | Hello to you, too! |

  @TC-4360 @regression @RC @groups
  Scenario Outline: I should not be able to create a group conversation as an external user in a team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role External
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on start a new conversation button
    Then I do not see create new group button

    Examples:
      | TeamOwner | Member1   | TeamName      |
      | user1Name | user2Name | GroupCreation |

  ######################
  # Deleting Conversation
  ######################

  @TC-4346 @regression @RC @groups @deleteGroup
  Scenario Outline: I want to be able to delete a group of which I am the creator
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap delete group button
    And I tap remove group button
    Then I see conversation list
    And I do not see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation |
      | user1Name | user2Name | user3Name | GroupDeletion | MyTeam            |

  @TC-4354 @regression @RC @groups @deleteGroup
  Scenario Outline: I want to be able to delete a group of which I am the creator from conversation list
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
    When I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap delete group button
    And I tap remove group button
    Then I see conversation list
    And I do not see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation |
      | user1Name | user2Name | user3Name | GroupDeletion | MyTeam            |

  @TC-4347 @regression @RC @groups @deleteGroup
  Scenario Outline: I should not be able to delete a group of which I am not the creator
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    When I tap show more options button
    Then I do not see delete group button

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation |
      | user1Name | user2Name | user3Name | GroupDeletion | MyTeam            |

  @TC-4356 @regression @RC @groups @deleteGroup
  Scenario Outline: I want to be able to receive and send messages after I deleted a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <Member1> adds a new device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversationDelete> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversationStays> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversationDelete>" in conversation list
    And I tap on conversation name "<GroupConversationDelete>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <Member1> reads the recent message from group conversation <GroupConversationDelete> via device Device1
    And User <Member1> sends message "<Message2>" to group conversation <GroupConversationDelete>
    And I see the message "<Message2>" in current conversation
    And I tap on group conversation title "<GroupConversationDelete>" to open group details
    And I tap show more options button
    When I tap delete group button
    And I tap remove group button
    Then I see conversation list
    And I do not see conversation "<GroupConversationDelete>" in conversation list
    And I see conversation "<GroupConversationStays>" in conversation list
    And I tap on conversation name "<GroupConversationStays>" in conversation list
    When I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation
    When User <Member1> reads the recent message from group conversation <GroupConversationStays> via device Device1
    And User <Member1> sends message "<Message3>" to group conversation <GroupConversationStays>
    And I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupConversationDelete | GroupConversationStays | Message | Message2 | Message3 |
      | user1Name | user2Name | user3Name | GroupDeletion | DeleteTeam              | StaysTeam              | Hello!  | Hello 2  | Hello 3  |

  @TC-4357 @regression @RC @groups @deleteGroup
  Scenario Outline: I want to be able to receive and send messages after someone else deleted a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation1> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation2> with <Member1>,<Member2> in team "<TeamName>"
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
    And I see conversation "<GroupConversation1>" in conversation list
    And I see conversation "<GroupConversation2>" in conversation list
    And I tap on conversation name "<GroupConversation1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And User <Member2> sends message "<Message2>" to group conversation <GroupConversation1>
    And I see the message "<Message2>" in current conversation
    And User <TeamOwner> reads the recent message from group conversation <GroupConversation1> via device Device1
    When Group admin user <TeamOwner> deletes conversation <GroupConversation1>
    Then I see conversation list
    And I see conversation "<GroupConversation2>" in conversation list
    And I do not see conversation "<GroupConversation1>" in conversation list
    When User <Member2> sends message "<Message3>" to group conversation <GroupConversation2>
    And I wait until the notification popup disappears
    Then I see conversation "<GroupConversation2>" is having 1 unread messages in conversation list
    And I tap on unread conversation name "<GroupConversation2>" in conversation list
    And I see the message "<Message3>" in current conversation
    And User <TeamOwner> reads the recent message from group conversation <GroupConversation2> via device Device1
    And I type the message "<Message4>" into text input field
    And I tap send button
    And I see the message "<Message4>" in current conversation
    And User <TeamOwner> reads the recent message from group conversation <GroupConversation2> via device Device1

    Examples:
      | TeamOwner | TeamName      | Member1   | Member2   | GroupConversation1 | GroupConversation2 | Message | Message2           | Message3                         | Message4     |
      | user1Name | GroupDeletion | user2Name | user3Name | DeleteMe           | Stay               | Hello!  | Hello to you, too! | This group should still be there | This worked. |

  ######################
  # Leaving Conversation
  ######################

  @TC-4348 @TC-4349 @TC-4350 @regression @RC @groups @leaveGroup
  Scenario Outline: I want to be able to leave a group conversation
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    # TC-4349 I want to be able to see conversation history after I left the conversation
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see system message "<LeftConversationText>" in conversation view
    And I see the message "<Message>" in current conversation
    # TC-4350 I should not be able to see new messages after I left the conversation
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    Then I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName   | Message | Message2     | GroupConversation | LeftConversationText      |
      | user1Name | user2Name | user3Name | LeaveGroup | Hello!  | Hello again! | MyTeam            | You left the conversation |

  @TC-4355 @regression @RC @groups @leaveGroup
  Scenario Outline: I want to be able to leave a group conversation from conversation list
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
    When I long tap on conversation name "<GroupConversation>" in conversation list
    When I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName   | GroupConversation |
      | user1Name | user2Name | user3Name | LeaveGroup | MyTeam            |

  @TC-4361 @regression @RC @groups @leaveGroup
  Scenario Outline: I want to see a system message when another member leaves a group conversation as an admin
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
    When <Member2> leaves group conversation <GroupConversation>
    Then I see system message "<Member2> left the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName   | GroupConversation  | Message |
      | user1Name | user2Name | user3Name | LeaveGroup | MyTeam             | Hello!  |

  @TC-4367 @regression @RC @groups @leaveGroup
  Scenario Outline: I want to see a system message when another member leaves a group conversation as a member
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
    When <Member2> leaves group conversation <GroupConversation>
    Then I see system message "<Member2> left the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName   | GroupConversation  | Message |
      | user1Name | user2Name | user3Name | LeaveGroup | MyTeam             | Hello!  |

  @TC-4372 @regression @RC @groups @leaveGroup
  Scenario Outline: I want to see a system message when another member leaves a group conversation as a guest
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<User> in team "<TeamName>"
    And User <User> is me
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
    When <Member2> leaves group conversation <GroupConversation>
    Then I see system message "<Member2> left the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | User      | TeamName   | GroupConversation  | Message |
      | user1Name | user2Name | user3Name | user4Name | LeaveGroup | MyTeam             | Hello!  |

  ######################
  # Removing Members
  ######################

  @TC-4351 @TC-4362 @regression @RC @groups @removeGroup
  Scenario Outline: I want to remove a participant from a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
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
    And I see group conversation "<GroupConversation>" is in foreground
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member3> in participants list
    When I tap on user <Member3> in participants list
    Then I see connected user <Member3> profile
    And I see remove from group button
    When I tap remove from group button
    Then I see alert asking me if I want to remove user <Member3> from group
    When I tap remove button on alert
    Then I do not see remove from group button
    # TC-4362 - I want to see a system message when another member was removed from the conversation as an admin
    When I close the user profile through the close button
    And I close the group conversation details through X icon
    Then I see system message "You removed <Member3> from the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName    | GroupConversation | Message | Message2           |
      | user1Name | user2Name | user3Name | user4Name | RemoveGroup | MyTeam            | Hello!  | Hello to you, too! |

  @TC-4366 @regression @RC @groups @removeGroup
  Scenario Outline: I want to see a system message when another member was removed from the conversation as a member
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> removes user <Member3> from group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> removed <Member3> from the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | RemoveGroup | MyTeam            |

  @TC-4370 @regression @RC @groups @removeGroup
  Scenario Outline: I want to see a system message when another member was removed from the conversation as a guest
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3>,<User> in team "<TeamName>"
    And User <User> is me
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
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> removes user <Member3> from group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> removed <Member3> from the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | User      | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | user5Name | RemoveGroup | MyTeam            |

  ######################
  # Adding Members
  ######################

  @TC-4352 @TC-4364 @regression @RC @groups @addGroup
  Scenario Outline: I want to add a participant to an existing group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    When I tap on Add Participants button
    Then I see user <Member3> in search suggestions list
    When I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    Then I see user <Member3> in participants list
    When I close the group conversation details through X icon
    # TC-4364 - I want to see a system message when another member gets added to a group conversation as an admin
    Then I see system message "You added <Member3> to the conversation" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation
    When User <Member3> sends message "<Message2>" to group conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName | GroupConversation | Message | Message2           |
      | user1Name | user2Name | user3Name | user4Name | AddGroup | MyTeam            | Hello!  | Hello to you, too! |

  @TC-4368 @regression @RC @groups @addGroup
  Scenario Outline: I want to see a system message when another member gets added to a group conversation as a member
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> adds user <Member3> to group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> added <Member3> to the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | AddGroup | MyTeam            |

  @TC-4371 @regression @RC @groups @addGroup
  Scenario Outline: I want to see a system message when another member gets added to a group conversation as a guest
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<User> in team "<TeamName>"
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> adds user <Member3> to group conversation "<GroupConversation>"
    Then I see system message "<TeamOwner> added <Member3> to the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | User      | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | user5Name | AddGroup | MyTeam            |

  @TC-4358 @regression @RC @groups @addGroup @WPB-3047
  Scenario Outline: I want to see the participant list updated correctly after members are being removed and added to a conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <TeamOwner> in participants list
    When I close the group conversation details through X icon
    And User <TeamOwner> adds user <Member2> to group conversation "<GroupConversation>"
    And I see system message "<TeamOwner> added <Member2> to the conversation" in conversation view
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on Participants tab
    Then I see user <Member2> in participants list
    When I close the group conversation details through X icon
    And User <TeamOwner> removes user <Member2> from group conversation "<GroupConversation>"
    And I see system message "<TeamOwner> removed <Member2> from the conversation" in conversation view
    And User <TeamOwner> adds user <Member3> to group conversation "<GroupConversation>"
    And I see system message "<TeamOwner> added <Member3> to the conversation" in conversation view
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on Participants tab
    Then I see user <Member3> in participants list
    And I do not see user <Member2> in participants list
    When I close the group conversation details through X icon
    And User <TeamOwner> adds user <Member2> to group conversation "<GroupConversation>"
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on Participants tab
    Then I see user <Member2> in participants list
    And I see user <Member3> in participants list

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | AddGroup | MyTeam            |

  ######################
  # Membership Banners
  ######################

  @TC-4375 @TC-4378 @TC-4373 @regression @RC @groups @membershipIdentifiers
  Scenario Outline: I want to see a guests banner if guests are present in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<User> in team "<TeamName>"
    And User <TeamOwner> is me
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
    Then I see a banner informing me that "Guests are present" in the conversation view
    And I close the conversation view through the back arrow
    # TC-4373 - I want to see a guest identifier on conversation list for 1:1 conversations with a guest
    When I see conversation "<User>" in conversation list
    Then I see <User> has "Guest" identifier next to his name in conversation list
    # TC-4378 - I should not see a guests banner if guests are present in a 1:1 conversation
    When I tap on conversation name "<User>" in conversation list
    Then I do not see a banner informing me that "Guests are present" in the conversation view

    Examples:
      | TeamOwner | Member1   | User      | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestConversation |

  @TC-4376 @TC-4379 @TC-4374 @regression @RC @groups @membershipIdentifiers
  Scenario Outline: I want to see an external banner if externals are present in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds users <External> to team "<TeamName>" with role External
    And User <TeamOwner> has 1:1 conversation with <External> in team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<External> in team "<TeamName>"
    And User <TeamOwner> is me
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
    Then I see a banner informing me that "Externals are present" in the conversation view
    And I close the conversation view through the back arrow
    # TC-4374 - I want to see an external identifier on conversation list for 1:1 conversations with an external
    When I see conversation "<External>" in conversation list
    Then I see <External> has "External" identifier next to his name in conversation list
    # TC-4379 - I should not see a external banner if externals are present in a 1:1 conversation
    When I tap on conversation name "<External>" in conversation list
    Then I do not see a banner informing me that "Externals are present" in the conversation view

    Examples:
      | TeamOwner | Member1   | External  | TeamName | GroupConversation    |
      | user1Name | user2Name | user3Name | External | ExternalConversation |

  #disabled because adding of bots is disabled
  @TC-4377 @groups @membershipIdentifiers @services
  Scenario Outline: I want to see a services banner if services are present in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> enables <BotName> service for team <TeamName>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds bot <BotName> to conversation <GroupConversation>
    And User <TeamOwner> is me
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
    Then I see a banner informing me that "Services are active" in the conversation view

    Examples:
      | TeamOwner | Member1   | BotName  | TeamName | GroupConversation |
      | user1Name | user2Name | Echo     | Bots     | BotsConversation  |

  #disabled because adding of bots is disabled
  @TC-4380 @groups @membershipIdentifiers @services
  Scenario Outline: I want to see a externals, guests and services banner if all of them are present in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds users <External> to team "<TeamName>" with role External
    And User <TeamOwner> enables <BotName> service for team <TeamName>
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <User>,<External> in team "<TeamName>"
    And User <TeamOwner> adds bot <BotName> to conversation <GroupConversation>
    And User <TeamOwner> is me
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
    Then I see a banner informing me that "Externals, guests and services are present" in the conversation view

    Examples:
      | TeamOwner | Member1   | User      | External  | BotName | TeamName  | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | Echo    | AllAtOnce | EverythingAtOnce |

  ######################
  # Services
  ######################

  #disabled because adding of bots is disabled
  @TC-4359 @groups @addGroup @removeGroup @services
  Scenario Outline: I want to add and remove a service to and from an existing group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> enables <BotName> service for team <TeamName>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I tap on Add Participants button
    And I tap on Services tab
    And I see user <BotName> in search suggestions list
    And I tap on user name <BotName> found on search page
    When I tap Add To Group button on add participants page
    Then I see Remove From Group button for service
    And I see toast message "Service added to group" in user profile screen
    When I wait until the notification popup disappears
    And I tap back button 2 times
    Then I see user <BotName> in participants list
    When I close the group conversation details through X icon
    # TC-4364 - I want to see a system message when a service gets added to a group conversation
    Then I see system message "You added <BotName> to the conversation" in conversation view
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <BotName> in participants list
    And I tap on user <BotName> in participants list
    Then I see Remove From Group button for service
    When I tap Remove From Group button for service
    Then I do not see Remove From Group button for service
    And I see toast message "Service removed from group" in user profile screen
    When I tap back button
    Then I do not see user <BotName> in participants list
    When I close the group conversation details through X icon
    Then I see system message "You removed <BotName> from the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   |BotName  | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name |Echo     | AddGroup | MyTeam            |
