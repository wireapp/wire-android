Feature: Conversation View

  @C466 @regression
  Scenario Outline: Create group conversation from 1:1
    Given There are 6 users where <Name> is me
    Given User Myself is connected to all other users
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact1>"
    Given I tap conversation name from top toolbar
    When I see username <Contact1> on Single connected user details page
    And I tap Create Group button on Single connected user details page
    And I type group name "<GroupChatName>" on Group creation page
    And I tap Confirm on Group creation page
    And I search for <Contact2> on Add people page
    And I select contact <Contact2> on Add people page
    And I search for <Contact3> on Add people page
    And I select contact <Contact3> on Add people page
    And I search for <Contact4> on Add people page
    And I select contact <Contact4> on Add people page
    And I search for <Contact5> on Add people page
    And I select contact <Contact5> on Add people page
    And I tap Done button on Add people page
    Then I see new introduction message with conversation title <GroupChatName> in conversation view
    And I navigate back from conversation
    And I see conversation "<GroupChatName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | Contact3  | Contact4  | Contact5  | GroupChatName |
      | user1Name | user2Name | user3Name | user4Name | user5Name | user6Name | GroupChat     |

  @C772 @regression
  Scenario Outline: Verify editing the conversation name
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <OldGroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<OldGroupChatName>"
    When I tap conversation name from top toolbar
    And I rename group conversation to <NewConversationName> on Group info page
    And I tap back button
    Then I see a message informing me that I renamed the conversation to <NewConversationName>
    And I navigate back from conversation
    And I see conversation "<NewConversationName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | OldGroupChatName | NewConversationName |
      | user1Name | user2Name | user3Name | oldGroupChat     | newGroupName        |

  @C815 @regression
  Scenario Outline: Send sketch
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact1>"
    Given I tap Sketch button from cursor toolbar
    Given I draw a sketch with <NumColors> colors
    When I send my sketch
    Then I see a picture in the conversation view
    And I tap Image container in the conversation view

    Examples:
      | Name      | Contact1  | NumColors |
      | user1Name | user2Name | 6         |

  @C816 @regression
  Scenario Outline: Send sketch on picture from gallery
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact1>"
    When I tap File button from cursor toolbar
    Then I see file chooser popup in the testing gallery
    And I tap Image button on file chooser popup in the testing gallery
    And I see a picture in the conversation view
    And I tap Image container in the conversation view
    And I tap Sketch button on Image Fullscreen page
    And I draw a sketch with <NumColors> colors
    And I send my sketch
    Then I see a picture in the conversation view
    And I tap Image container in the conversation view

    Examples:
      | Name      | Contact1  | NumColors |
      | user1Name | user2Name | 6         |

  @C741 @regression
  Scenario Outline: Mute and unmute conversation from conversation details
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    Given I tap conversation name from top toolbar
    Given I tap More Actions button on Single connected user details page
    When I tap <ItemSilence> button on Single conversation options menu
    And I tap More Actions button on Single connected user details page
    Then I see <ItemNotify> button on Single conversation options menu
    And I tap Back button 2 times
    And I navigate back from conversation
    When I tap on conversation name "<Contact>"
    And I tap conversation name from top toolbar
    And I tap More Actions button on Single connected user details page
    And I tap <ItemNotify> button on Single conversation options menu
    And I tap More Actions button on Single connected user details page
    Then I see <ItemSilence> button on Single conversation options menu
    When I tap Back button 2 times
    When I navigate back from conversation
    Then I do not see muted glyph of conversation "<Contact>"

    Examples:
      | Name      | Contact   | ItemSilence | ItemNotify |
      | user1Name | user2Name | Mute        | Unmute     |
