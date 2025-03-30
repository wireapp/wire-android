Feature: Reactions

  @TC-4478 @regression @RC @reactions
  Scenario Outline: I want to see if my message received a reaction in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device <Device> with label <Device>
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
    When User <Member1> likes the recent message from user Myself via device <Device>
    Then I see a "<ReactionHeart>" from 1 user as reaction to my message
    When User <Member1> toggles reaction "👍" on the recent message from conversation Myself via device <Device>
    Then I see a "<ReactionThumbsUp>" from 1 user as reaction to my message
    When User <Member1> toggles reaction "😁" on the recent message from conversation Myself via device <Device>
    Then I see a "<ReactionLaught>" from 1 user as reaction to my message
    When User <Member1> toggles reaction "🙂" on the recent message from conversation Myself via device <Device>
    Then I see a "<ReactionSmile>" from 1 user as reaction to my message

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Device  | ReactionHeart |ReactionThumbsUp |ReactionLaught |ReactionSmile |
      | user1Name | Reactions | user2Name | Hello!  | Device1 | ❤️            |👍               |😁             |🙂            |

  @TC-4479 @TC-4480 @TC-4481 @regression @RC @reactions
  Scenario Outline: I want to see if my message received multiple reactions in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> adds a new device <Device1> with label <Device1>
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <Member1> toggles reaction "❤️" on the recent message from conversation <GroupConversation> via device Device1
    When User <Member1> toggles reaction "👍" on the recent message from conversation <GroupConversation> via device Device1
    # TC-4480 - I want to see if my message received multiple reactions
    When User <Member2> toggles reaction "❤️" on the recent message from conversation <GroupConversation> via device Device2
    When User <Member2> toggles reaction "👍" on the recent message from conversation <GroupConversation> via device Device2
    When User <Member2> toggles reaction "😁" on the recent message from conversation <GroupConversation> via device Device2
    When User <Member2> toggles reaction "👎" on the recent message from conversation <GroupConversation> via device Device2
    When User <Member3> toggles reaction "❤️" on the recent message from conversation <GroupConversation> via device Device3
    When User <Member3> toggles reaction "🙂" on the recent message from conversation <GroupConversation> via device Device3
    Then I see a "<ReactionHeart>" from 3 users as reaction to user <TeamOwner> message
    Then I see a "<ReactionThumbsUp>" from 2 users as reaction to user <TeamOwner> message
    Then I see a "<ReactionLaught>" from 1 users as reaction to user <TeamOwner> message
    Then I see a "<ReactionSmile>" from 1 users as reaction to user <TeamOwner> message
    Then I see a "<ReactionThumbsDown>" from 1 users as reaction to user <TeamOwner> message
    # TC-4481 - I want to see who reacted to my message in message details
    When I long tap on the message "<Message>" in current conversation
    And I see message details option
    And I tap on message details option
    Then I see 8 reactions in reactions tab
    And I see user <Member1> in the list of users that reacted
    And I see user <Member2> in the list of users that reacted
    And I see user <Member3> in the list of users that reacted

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Member3   | Device1 | GroupConversation | Message | ReactionHeart |ReactionThumbsUp |ReactionLaught |ReactionSmile  |ReactionThumbsDown |
      | user1Name | Reactions | user2Name | user3Name | user4Name | Device1 | ReactHere!        | Hello!  | ❤️            |👍               |😁             |🙂             |👎                 |

  @TC-4475 @TC-4483 @regression @RC @reactions
  Scenario Outline: I want to be able to react to a text message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device with label Device
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
    When User <Member1> sends message "<Message>" via device Device to User Myself
    Then I see the message "<Message>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I see reactions options
    And I tap on <Reaction> icon
    Then I see a "<Reaction>" from 1 user as reaction to user <Member1> message
    When I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    Then I see 1 reactions in reactions tab
    And I see user <TeamOwner> in the list of users that reacted
    # TC-4483 - I want to be able to remove my reaction to a text message in a 1on1 conversation
    And I tap back button
    When I tap on "<Reaction>" from user <Member1> message
    Then I do not see a "<Reaction>" to user <Member1> message

    Examples:
      | TeamOwner | TeamName  | Member1   | Message | Reaction |
      | user1Name | Reactions | user2Name | Hello!  | ❤️       |
      | user1Name | Reactions | user2Name | Hello!  | 👍       |


  @TC-4476 @TC-4484 @regression @RC @reactions
  Scenario Outline: I want to be able to react to a text message in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
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
    When User <Member1> sends message "<Message>" via device Device1 to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I see reactions options
    And I tap on <ReactionHeart> icon
    Then I see a "<ReactionHeart>" from 1 user as reaction to user <Member1> message
    When I long tap on the message "<Message>" in current conversation
    And I see reactions options
    And I tap on <ReactionSad> icon
    Then I see a "<ReactionSad>" from 1 user as reaction to user <Member1> message
    When I long tap on the message "<Message>" in current conversation
    And I see reactions options
    And I tap on <ReactionThumbsDown> icon
    Then I see a "<ReactionThumbsDown>" from 1 user as reaction to user <Member1> message
    When User <Member1> toggles reaction "❤️" on the recent message from conversation <GroupConversation> via device Device1
    When User <Member2> toggles reaction "❤️" on the recent message from conversation <GroupConversation> via device Device2
    When User <Member3> toggles reaction "👎" on the recent message from conversation <GroupConversation> via device Device3
    When I long tap on the message "<Message>" in current conversation
    And I tap on message details option
    Then I see 6 reactions in reactions tab
    And I see user <TeamOwner> in the list of users that reacted
    And I see user <Member2> in the list of users that reacted
    And I see user <Member3> in the list of users that reacted
    # TC-4484 - I want to be able to remove my reaction to a text message in a group conversation
    And I tap back button
    When I tap on "<ReactionHeart>" from user <Member1> message
    Then I see a "<ReactionHeart>" from 2 users as reaction to user <Member1> message

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Member3   | GroupConversation | Message | ReactionHeart |ReactionSad |ReactionThumbsDown |
      | user1Name | Reactions | user2Name | user3Name | user4Name | ReactHere!        | Hello!  | ❤️            |☹️          |👎                 |

  @TC-4477 @TC-4485 @regression @RC @reactions
  Scenario Outline: I want to be able to react to an image in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
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
    When User <Member1> sends image "<Picture>" to conversation <GroupConversation>
    Then I see an image in the conversation view
    When I longtap on the image
    And I see reactions options
    And I tap on <ReactionHeart> icon
    Then I see a "<ReactionHeart>" from 1 user as reaction to user <Member1> message
    When I longtap on the image
    And I see reactions options
    And I tap on <ReactionSmile> icon
    Then I see a "<ReactionSmile>" from 1 user as reaction to user <Member1> message
    When I longtap on the image
    And I see reactions options
    And I tap on <ReactionSad> icon
    Then I see a "<ReactionSad>" from 1 user as reaction to user <Member1> message
    When User <Member2> likes the recent message from group conversation <GroupConversation> via device Device2
    And User <Member3> likes the recent message from group conversation <GroupConversation> via device Device3
    Then I see a "<ReactionHeart>" from 3 users as reaction to user <Member1> message
    When I longtap on the image
    And I tap on message details option
    Then I see 5 reactions in reactions tab
    And I see user <TeamOwner> in the list of users that reacted
    And I see user <Member2> in the list of users that reacted
    And I see user <Member3> in the list of users that reacted
    # TC-4485 - I want to be able to remove my reaction to an image in a group conversation
    And I tap back button
    When I tap on "<ReactionHeart>" from user <Member1> message
    Then I see a "<ReactionHeart>" from 2 users as reaction to user <Member1> message

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Member3   | GroupConversation | Picture     | ReactionHeart |ReactionSmile |ReactionSad |
      | user1Name | Reactions | user2Name | user3Name | user4Name | ReactHere!        | testing.jpg | ❤️            |🙂            |☹️          |

  @TC-4482 @TC-4486 @regression @RC @reactions
  Scenario Outline: I want to verify that my reaction is deleted after a message was edited by another user
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member3> adds a new device Device3 with label Device3
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
    When User <Member1> sends message "<Message>" via device Device1 to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation
    When I long tap on the message "<Message>" in current conversation
    And I see reactions options
    And I tap on <Reaction> icon
    Then I see a "<Reaction>" from 1 user as reaction to user <Member1> message
    When User <Member1> edits the recent message to "Good Day" from group conversation <GroupConversation> via device Device1
    Then I do not see a "<Reaction>" to user <Member1> message
    # TC-4486 - I want to verify that reactions from other users are deleted after a message was edited by another user
    When User <Member2> likes the recent message from group conversation <GroupConversation> via device Device2
    Then I see a "<Reaction>" from 1 user as reaction to user <Member1> message
    And User <Member3> likes the recent message from group conversation <GroupConversation> via device Device3
    Then I see a "<Reaction>" from 2 users as reaction to user <Member1> message
    When User <Member1> edits the recent message to "Good Morning" from group conversation <GroupConversation> via device Device1
    Then I do not see a "<Reaction>" to user <Member1> message

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | Member3   | GroupConversation | Message | Reaction |
      | user1Name | Reactions | user2Name | user3Name | user4Name | ReactHere!        | Hello!  | ❤️       |

  @TC-4487 @regression @RC @reactions @WPB-3525
  Scenario Outline: I want to be able to react to a message after I scrolled through a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
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
    When User <Member1> sends 40 default message to conversation <GroupConversation>
    And I wait until the notification popup disappears
    And I tap on unread conversation name "<GroupConversation>" in conversation list
    And I scroll to the bottom of conversation view
    And I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    And I see the message "<Message>" in current conversation
    And User <Member1> sends message "<Message2>" via device Device1 to group conversation <GroupConversation>
    And I see the message "<Message2>" in current conversation
    And I scroll to the top of conversation view
    And I scroll to the bottom of conversation view
    And I long tap on the message "<Message2>" in current conversation
    And I tap on <Reaction> icon
    Then I see a "<Reaction>" from 1 user as reaction to user <Member1> message

    Examples:
      | TeamOwner | TeamName  | Member1   | GroupConversation | Message                   | Message2 | Reaction |
      | user1Name | Reactions | user2Name | ReactHere!        | That is a lot of messages | Yes!     | ❤️       |
