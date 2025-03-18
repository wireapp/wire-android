Feature: Calling

  @C426 @calling_basic @regression
  Scenario Outline: [Teams] Verify incoming 1:1 call not showed during ongoing group call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Contact1>,<Contact2>,<Contact3> to team <TeamName> with role Member
    Given User <Member1> is me
    Given User <Member1> has conversation <GroupChatName> with <Contact1>,<Contact2> in team <TeamName>
    Given User <Member1> has 1:1 conversation with <Contact3> in team <TeamName>
    Given <Contact1>,<Contact2>,<Contact3> start instances using <CallBackend>
    Given <Contact2> accepts next incoming call automatically
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    When I tap on conversation name "<GroupChatName>"
    And <Contact1> calls <GroupChatName>
    Then I see incoming call from <GroupChatName>
    When I accept the call
    Then I see ongoing call
    When <Contact3> calls me
    Then I do not see incoming call

    Examples:
      | Member1   | Contact1  | Contact2  | Contact3  | GroupChatName | CallBackend | TeamOwner | TeamName  |
      | user1Name | user2Name | user3Name | user4Name | GroupCallChat | chrome      | user5Name | SuperTeam |

  @C783 @regression @calling_basic
  Scenario Outline: Calling bar buttons are clickable and change its state (portrait)
    Given There are personal users <Name>,<Contact>
    Given User <Name> is me
    Given User <Contact> is connected to me
    Given User <Contact> sets the unique username
    Given <Contact> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When <Contact> calls me
    Then I see incoming call
    When I accept the call
    Then I see ongoing call
    When I remember state of mute button for call
    And I tap mute button for ongoing call
    And I wait for 3 seconds
    Then I see state of mute button has changed for call
    When I remember state of mute button for call
    And I tap mute button for ongoing call
    And I wait for 3 seconds
    Then I see state of mute button has changed for call
    When I hang up ongoing call
    Then I do not see ongoing call

    Examples:
      | Name      | Contact   | CallBackend |
      | user1Name | user2Name | chrome      |

  @C487 @calling_basic @regression
  Scenario Outline: I see miss call notification on the list and inside conversation view (portrait)
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User <Contact2> sets the unique username
    Given <Contact2> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact1>"
    When <Contact2> calls me
    And I see incoming call
    And <Contact2> stops calling me
    Then I do not see incoming call
    When I navigate back from conversation
    Then I see missed call glyph of conversation "<Contact2>"
    When I tap on conversation name "<Contact2>"
    Then I see missed call from <Contact2> in the conversation
    When I navigate back from conversation
    Then I do not see missed call glyph of conversation "<Contact2>"

    Examples:
      | CallBackend | Name      | Contact1  | Contact2  |
      | chrome      | user1Name | user2Name | user3Name |

  @C811 @regression @calling_basic @smoke
  Scenario Outline: Receive call while Wire is running in the background (portrait)
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given User <Contact> sets the unique username
    Given <Contact> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I minimize Wire
    When <Contact> calls me
    Then I see incoming call from <Contact>
    When I accept the call
    Then I see ongoing call
    And <Contact> stops calling me
    When I restore Wire
    Then I do not see ongoing call

    Examples:
      | Name      | Contact   | CallBackend |
      | user1Name | user2Name | chrome      |

  @C486 @calling_basic @regression
  Scenario Outline: Other wire user trying to call me while I'm already in wire call
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given Personal Users <Contact1> enables conference calling feature via backdoor
    Given Users <Contact1>,<Contact2> set the unique username
    Given <Contact1>,<Contact2> start instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When <Contact1> calls me
    Then I see incoming call from <Contact1>
    When I accept the call
    Then I see ongoing call
    When <Contact2> calls me
    Then I do not see incoming call

    Examples:
      | Name      | Contact1  | Contact2  | CallBackend |
      | user1Name | user2Name | user3Name | chrome      |

  @C813 @regression @calling_basic
  Scenario Outline: Silence an incoming call (portrait)
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given User <Contact> sets the unique username
    Given <Contact> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    And I tap on conversation name "<Contact>"
    And <Contact> calls me
    And I see incoming call
    When I ignore the call
    Then I do not see incoming call

    Examples:
      | Name      | Contact   | CallBackend |
      | user1Name | user2Name | chrome      |
