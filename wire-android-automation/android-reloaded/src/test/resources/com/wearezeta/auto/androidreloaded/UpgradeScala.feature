Feature: UpgradeScala

  @TC-4572 @upgradeScala
  Scenario Outline: I want to be able to update from the old scala build to the new android build without losing my history as a team user
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    Then I see conversation "<GroupConversation>" in Recent View in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<GroupConversation>" in the old scala app
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in the conversation view in the old scala app
    And I see the message "<Message2>" in the conversation view in the old scala app
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message3>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message3>" in the conversation view in the old scala app
    And I tap back button
    When User <Member2> sends message "<Message>" to User <Member1>
    And User <TeamOwner> sends message "<Message2>" to User <Member1>
    And I wait for 3 seconds
    Then I see conversation <Member2> with unread icon showing 1 new messages in the old scala app
    And I see conversation <TeamOwner> with unread icon showing 1 new messages in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
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
    Then I see the message "<Message5>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4               | Message5                 | Picture     |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration! | Migration was a success! | testing.jpg |

  @TC-4573 @upgradeScala
  Scenario Outline: I want to be able to update from the old scala build to the new android build without losing my history as a personal user
    Given I reinstall the old Wire Version
    And There are 3 users where <Name> is me
    And User Myself is connected to <Contact1>,<Contact2>
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    And I accept Help us make Wire better popup in the old scala app
    And I see conversation "<Contact1>" in Recent View in the old scala app
    And I see conversation "<Contact2>" in Recent View in the old scala app
    When I tap on conversation name "<Contact1>" in the old scala app
    And User <Contact1> sends message "<Message>" to User Myself
    And I see the message "<Message>" in the conversation view in the old scala app
    And User <Contact1> sends image "<Picture>" to conversation Myself
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message2>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message2>" in the conversation view in the old scala app
    And I tap back button
    When User <Contact2> sends message "<Message>" to User <Name>
    And I wait for 3 seconds
    And I see conversation <Contact2> with unread icon showing 1 new messages in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    Then I see unread conversation "<Contact2>" in conversation list
    And I see conversation "<Contact2>" is having 1 unread messages in conversation list
    And I see conversation "<Contact1>" in conversation list
    When I tap on conversation name "<Contact1>" in conversation list
    And I see the message "<Message2>" in current conversation
    And I see an image in the conversation view
    And I see the message "<Message>" in current conversation
    When User <Contact1> sends message "<Message3>" to User Myself
    Then I see the message "<Message3>" in current conversation
    When I type the message "<Message4>" into text input field
    And I tap send button
    Then I see the message "<Message4>" in current conversation

    Examples:
      | Name      | Contact1  | Contact2  | Message | Message2           | Message3               | Message4                 | Picture     |
      | user1Name | user2Name | user3Name | Hello!  | Hello to you, too! | Hello after Migration! | Migration was a success! | testing.jpg |

  @TC-4574 @upgradeScala
  Scenario Outline: I want to be able to update from the old scala build to the new android build and see all my assets
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
    And User <TeamOwner> adds a new device Device2 with label Device2
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    Then I see conversation "<GroupConversation>" in Recent View in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<GroupConversation>" in the old scala app
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <Member2> sends image "testing.jpg" to conversation <GroupConversation>
    And I see an image in the conversation view in the old scala app
    And User <TeamOwner> sends local audio file named "test.m4a" via device Device2 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    And I see the play button on the audio message in the conversation view in the old scala app
    And User <TeamOwner> sends local video named "testing.mp4" via device Device2 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    And I see a file with name "testing.mp4" in the conversation view in the old scala app
    And User <Member2> sends 1.00MB file having name "qa_random.txt" and MIME type "text/plain" via device Device1 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    And I see a file with name "qa_random.txt" in the conversation view in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see an image in the conversation view
    And I see an audio file in the conversation view
    And I see a file with name "testing.mp4" in the conversation view
    And I see a file with name "qa_random.txt" in the conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  |

  @TC-4577 @upgradeScala
  Scenario Outline: I want to see the content of a group conversation which I left in the old scala app after I migrated to the new app
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    Then I see conversation "<GroupConversation>" in Recent View in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<GroupConversation>" in the old scala app
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in the conversation view in the old scala app
    And I see the message "<Message2>" in the conversation view in the old scala app
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message3>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message3>" in the conversation view in the old scala app
    When I tap conversation name from top toolbar in the old scala app
    And I tap open menu button on Group info page in the old scala app
    And I tap Leave group… button on Group conversation options menu in the old scala app
    And I tap LEAVE GROUP button on Confirm overlay page in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    Then I see the message "<Message3>" in current conversation
    And I see the message "<Message2>" in current conversation
    And I see an image in the conversation view
    And I see the message "<Message>" in current conversation
    When User <Member2> sends message "<Message4>" to group conversation <GroupConversation>
    Then I do not see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4              | Picture     |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration | testing.jpg |

  @TC-4578 @upgradeScala
  Scenario Outline: I should not see the content of a group conversation which I cleared in the old scala app after I migrated to the new app
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    Then I see conversation "<GroupConversation>" in Recent View in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<GroupConversation>" in the old scala app
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in the conversation view in the old scala app
    And I see the message "<Message2>" in the conversation view in the old scala app
    And User <Member2> sends image "<Picture>" to conversation <GroupConversation>
    And I scroll to the bottom of conversation view
    And I see an image in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message3>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message3>" in the conversation view in the old scala app
    When I tap conversation name from top toolbar in the old scala app
    And I tap open menu button on Group info page in the old scala app
    And I tap Clear content… button on Group conversation options menu in the old scala app
    And I tap CLEAR CONTENT button on Confirm overlay page in the old scala app
    And I tap back button 2 times
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    And I tap on menu button on conversation list
    And I tap on archive menu entry
    And I tap on archive menu entry
    Then I see conversation "<GroupConversation>" in archive list
    When I tap on conversation name "<GroupConversation>" in archive list
    Then I do not see the message "<Message3>" in current conversation
    And I do not see the message "<Message2>" in current conversation
    And I do not see an image in the conversation view
    And I do not see the message "<Message>" in current conversation
    When User <Member2> sends message "<Message4>" to group conversation <GroupConversation>
    Then I see the message "<Message4>" in current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       | Message4              | Picture     |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! | Hello after Migration | testing.jpg |

  @TC-4575 @upgradeScala
  Scenario Outline: I want to be able to update from the old scala build to the new android build when I had a conversation with a deleted user
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<Member2>" in the old scala app
    And User <Member2> sends message "<Message>" to User Myself
    And I see the message "<Message>" in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message2>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message2>" in the conversation view in the old scala app
    And I tap back button
    When User <TeamOwner> removes user <Member2> from team <TeamName>
    And I terminate Wire
    And I restart Wire
    And I tap on conversation name "<Member2>" in the old scala app
    Then I see the system message "<Member2> was removed" in the conversation view in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    Then I do not see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | Message | Message2           |
      | user1Name | user2Name | user3Name | Migration | Hello!  | Hello to you, too! |

  @TC-4576 @upgradeScala
  Scenario Outline: I want to be able to update from the old scala build to the new android build when I had a group conversation which was deleted
    Given I reinstall the old Wire Version
    And There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And User <Member1> is me
    And I open staging backend deep link
    And I accept custom backend alert in the old scala app
    And I tap Login with Email button on Custom backend welcome page in the old scala app
    And I sign in using my email in the old scala app
    And I accept First Time overlay in the old scala app
    And I see conversation "<GroupConversation>" in Recent View in the old scala app
    And I see conversation "<Member2>" in Recent View in the old scala app
    And I see conversation "<TeamOwner>" in Recent View in the old scala app
    When I tap on conversation name "<GroupConversation>" in the old scala app
    And User <Member2> sends message "<Message>" to group conversation <GroupConversation>
    And User <TeamOwner> sends message "<Message2>" to group conversation <GroupConversation>
    And I see the message "<Message>" in the conversation view in the old scala app
    And I see the message "<Message2>" in the conversation view in the old scala app
    And I tap on text input in the old scala app
    And I type the message "<Message3>" and send it by cursor Send button in the old scala app
    Then I see the message "<Message3>" in the conversation view in the old scala app
    And I tap back button
    When Group admin user <TeamOwner> deletes conversation <GroupConversation>
    Then I do not see conversation "<GroupConversation>" in Recent View in the old scala app
    And I minimise Wire
    When I upgrade Wire to the recent version
    And I wait until I am fully logged in after upgrading from the old app
    Then I see Welcome to New Android alert on conversation list
    And I see Learn more button on welcome to new android alert
    And I see Start Using Wire button on welcome to new android alert
    When I tap Start Using Wire button on welcome to new android alert
    Then I do not see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    And I see conversation "<Member2>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | GroupConversation | Message | Message2           | Message3       |
      | user1Name | user2Name | user3Name | Migration | HappyMigration    | Hello!  | Hello to you, too! | Hello as well! |
