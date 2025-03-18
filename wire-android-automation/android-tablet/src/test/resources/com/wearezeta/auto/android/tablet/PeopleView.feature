Feature: People View

  @C742 @regression
  Scenario Outline: Check contact personal info in portrait mode
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<Contact>"
    And I tap conversation name from top toolbar
    Then I see username <Contact> on Single connected user details page

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |

  @C739 @regression
  Scenario Outline: Leave group conversation in portrait mode
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    And I tap open menu button on Group info page
    When I tap <ItemLeave> button on Group conversation options menu
    And I tap LEAVE GROUP button on Confirm overlay page
    Then I do not see Group info page
    And I do not see conversation "<GroupChatName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName  | ItemLeave    |
      | user1Name | user2Name | user3Name | LeaveGroupChat | Leave group… |

  @C468 @regression
  Scenario Outline: Remove from group conversation in portrait mode
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    When I tap participant avatar of <Contact2> on Group info page
    And I tap More Actions button on Group connected user details page
    And I see Remove From Group... conversation action button on Group connected user details page
    And I tap remove button on Group connected user options menu
    And I tap REMOVE button on Confirm overlay page
    Then I do not see participant <Contact2> on Group info page
    When I tap Back button 1 times
    Then I do not see Group info page
    And I see system message contains "<Action> <Contact2>" on group page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName       | Action      |
      | user1Name | user2Name | user3Name | RemoveFromGroupChat | You removed |
    
  @C508 @regression
  Scenario Outline: Start 1:1 conversation from group pop-over
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Contact1> sends message "<Message>" to user Myself
    And I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    When I tap participant avatar of <Contact1> on Group info page
    And I tap the Open Conversation button on Group connected user details page
    Then I do not see Group info page
    And I see the message "<Message>" in the conversation view

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | Message |
      | user1Name | user2Name | user3Name | GroupChat     | Msg     |

  @C530 @regression
  Scenario Outline: I can access user details page from group details pop-over
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User <Contact1> sets the unique username
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    And I tap participant avatar of <Contact1> on Group info page
    Then I see user name "<Contact1>" on User profile popup page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName |
      | user1Name | user2Name | user3Name | GroupChat     |

  @C763 @regression
  Scenario Outline: I see conversation name, number of participants and their avatars in Group info page
    Given There are personal users <Name>,<Contact1>,<Contact2>
    Given User <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<GroupChatName>"
    When I tap conversation name from top toolbar
    Then I see participant <Contact1> on Group info page
    And I see participant <Contact2> on Group info page
    And I see the conversation name is <GroupChatName> on Group info page
    And I see there are 2 members on Group info page
    # And I see there are 2 participants on Group info page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName |
      | user1Name | user2Name | user3Name | GroupChat     |

  @C507 @regression
  Scenario Outline: Check interaction with options menu
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    And I see Group info page
    When I tap open menu button on Group info page
    And I see <ItemLeave> button on Group conversation options menu
    And I tap Back button
    Then I do not see <ItemLeave> button on Group conversation options menu
    And I see Group info page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | ItemLeave    |
      | user1Name | user2Name | user3Name | GroupChat     | Leave group… |

  @C773 @regression
  Scenario Outline: Verify you cannot start a 1:1 conversation from a group conversation if the other user is not in your contacts list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User <Contact1> is connected to Myself,<Contact2>
    Given User <Contact1> has group conversation <GroupChatName> with Myself,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I do not see conversation "<Contact2>" in Recent View
    Given I tap on conversation name "<GroupChatName>"
    When I tap conversation name from top toolbar
    And I tap participant avatar of <Contact2> on Group info page
    Then I see user name "<Contact2>" on Group unconnected user details page

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName        |
      | user1Name | user2Name | user3Name | NonConnectedUserChat |
      