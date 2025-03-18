Feature: Lock/Unlock

  @C472 @regression
  Scenario Outline: (AN-5068) UI saves its state after device lock/unlock
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Contact1> sends message "<MessageGroup>" to group conversation <GroupChatName>
    Given User <Contact1> sends message "<Message1to1>" to user Myself
    When I lock the device
    And I unlock the device
    Then I see conversation "<Contact1>" in Recent View
    When I lock the device
    And I unlock the device
    And I open Self profile
    And I tap Settings button on Self profile page
    And I see settings page
    And I lock the device
    And I unlock the device
    Then I see settings page
    When I tap Back button
    And I tap Back button on Self profile page
    And I tap on conversation name "<GroupChatName>"
    And I lock the device
    And I unlock the device
    Then I see the message "<MessageGroup>" in the conversation view
    When I tap conversation name from top toolbar
    And I see Group info page
    And I lock the device
    And I unlock the device
    Then I see Group info page
    And I tap Back button 1 times
    When I navigate back from conversation
    And I tap on conversation name "<Contact1>"
    And I lock the device
    And I unlock the device
    Then I see the message "<Message1to1>" in the conversation view
    When I tap conversation name from top toolbar
    And I see user name "<Contact1>" on User profile popup page
    And I lock the device
    And I unlock the device
    Then I see user name "<Contact1>" on User profile popup page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName     | Message1to1 | MessageGroup |
      | user1Name | user2Name | user3Name | SendMessGroupChat | Msg1to1     | MsgGroup     |
