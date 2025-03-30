Feature: Upgrade

  @TC-4853 @upgrade @col1 @col3
  Scenario Outline: I want to be able to update from previous column build to the new column build without losing my history as a team user
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view
    When I type the message "<Message3>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message3>" in current conversation
    And I tap back button
    When User <Member2> sends message "<Message>" to User <Member1>
    And User <TeamOwner> sends message "<Message2>" to User <Member1>
    And I wait until the notification popup disappears
    And I see conversation "<Member2>" is having 1 unread messages in conversation list
    And I see conversation "<TeamOwner>" is having 1 unread messages in conversation list
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    Then I see unread conversation "<Member2>" in conversation list
    And I see conversation "<Member2>" is having 1 unread messages in conversation list
    And I see unread conversation "<TeamOwner>" in conversation list
    And I see conversation "<TeamOwner>" is having 1 unread messages in conversation list
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message3>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I see an image in the conversation view
    And I see the message "<Message>" in current conversation
    When User <TeamOwner> sends message "<Message4>" to group conversation <GroupConversation>
    Then I see the message "<Message4>" in current conversation
    When I type the message "<Message5>" into text input field
    And I tap send button
    And I scroll to the bottom of conversation view
    Then I see the message "<Message5>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4               | Message5                 | Picture     |
      | user1Name | user2Name | user2Email | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration! | Migration was a success! | testing.jpg |

  @TC-4854 @upgrade @col1 @col3
  Scenario Outline:  want to be able to update from previous column build to the new column build and see all my assets
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    Then I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <Member2> sends image "testing.jpg" to conversation <GroupConversation>
    And I see an image in the conversation view
    And User <TeamOwner> sends 1 video file testing.mp4 to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see a file with name "testing.mp4" in the conversation view
    When User <Member2> sends 1.00KB sized file with MIME type text/plain and name qa_random.txt to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see a file with name "qa_random" in the conversation view
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see an image in the conversation view
    And I see a file with name "testing.mp4" in the conversation view
    And I see a file with name "qa_random" in the conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | GroupConversation | Message |
      | user1Name | user2Name | user2Email | user3Name | Migration | HappyMigration    | Hello!  |

  @TC-4855 @upgrade @col1 @col3
  Scenario Outline: I want to see the content of a group conversation which I left in the previous column app after I migrated to the new column app
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    Then I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view
    And I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap leave group button
    And I tap leave group confirm button
    Then I see you left conversation toast message
    And I see conversation "<Member2>" in conversation list
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message3>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I see an image in the conversation view
    And I see the message "<Message>" in current conversation
    When User <Member2> sends message "<Message4>" to group conversation <GroupConversation>
    Then I do not see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4              | Picture     |
      | user1Name | user2Name | user2Email | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration | testing.jpg |

  @TC-4856 @upgrade @col1 @col3
  Scenario Outline: I should not see the content of a group conversation which I cleared in the previous column build after I migrated to the new column build
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    Then I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view
    And I type the message "<Message3>" into text input field
    And I tap send button
    And I scroll to the bottom of conversation view
    Then I see the message "<Message3>" in current conversation
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap show more options button
    When I tap clear content button on group details page
    And I tap clear content confirm button on group details page
    Then I see "Group content was deleted" toast message on group details page
    When I close the group conversation details through X icon
    And I see conversation view with "<GroupConversation>" is in foreground
    And I close the conversation view through the back arrow
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I do not see the message "<Message3>" in current conversation
    And I do not see the message "<Message2>" in current conversation
    And I do not see an image in the conversation view
    And I do not see the message "<Message>" in current conversation
    When User <Member2> sends message "<Message4>" to group conversation <GroupConversation>
    Then I see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4              | Picture     |
      | user1Name | user2Name | user2Email | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration | testing.jpg |

  @TC-4857 @upgrade @col1 @col3
  Scenario Outline: I want to be able to update from previous column build to the new column build when I had a conversation with a deleted user
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<Member2>" in conversation list
    And User <Member2> sends message "<Message>" to User Myself
    And I see the message "<Message>" in current conversation
    And I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" in current conversation
    And I tap back button
    And I close the conversation view through the back arrow
    When User <TeamOwner> removes user <Member2> from team <TeamName>
    Then I do not see conversation "<Member2>" in conversation list
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    Then I do not see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | Message | Message2           |
      | user1Name | user2Name | user2Email | user3Name | Migration | Hello!  | Hello to you, too! |

  @TC-4858 @upgrade @col1 @col3
  Scenario Outline: I want to be able to update from the previous column build to the new column build when I had a group conversation which was deleted
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member2> adds 1 2FA device
    And User <TeamOwner> adds 1 2FA device
    And User <Member1> is me
    And I see Welcome Page
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    Then I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation
    And I tap back button
    When Group admin user <TeamOwner> deletes conversation <GroupConversation>
    Then I do not see conversation "<GroupConversation>" in conversation list
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in
    Then I do not see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    And I see conversation "<Member2>" in conversation list

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       |
      | user1Name | user2Name | user2Email | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! |