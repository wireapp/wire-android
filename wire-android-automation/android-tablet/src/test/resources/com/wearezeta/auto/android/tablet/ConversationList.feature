Feature: Conversation List

  @C503 @regression
  Scenario Outline: Mute and unmute conversation from conversations list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I long tap on conversation name "<Contact1>"
    When I tap <ItemSilence> button on Single conversation options menu
    Then I do not see Single conversation options menu
    And I see muted glyph of conversation "<Contact1>"
    When I long tap on conversation name "<Contact1>"
    When I tap <ItemNotify> button on Single conversation options menu
    Then I do not see Single conversation options menu
    Then I do not see muted glyph of conversation "<Contact1>"

    Examples:
      | Name      | Contact1  | Contact2  | ItemSilence | ItemNotify |
      | user1Name | user2Name | user3Name | Mute        | Unmute     |

  @C548 @regression
  Scenario Outline: Verify I can delete a group conversation from conversation list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Contact1> sends message "<Msg1>" to group conversation <GroupChatName>
    When I long tap on conversation name "<GroupChatName>"
    And I tap Clear content… button on Group conversation options menu
    And I tap CLEAR CONTENT button on Confirm overlay page
    Then I do not see conversation "<GroupChatName>" in Recent View
    When I open Search UI
    And I type user name "<GroupChatName>" in search field
    Then I see group <GroupChatName> in Search result list
    When I tap X button on Search page
    And User <Contact1> sends message "<Msg2>" to group conversation <GroupChatName>
    Then  I see conversation "<GroupChatName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | Msg1       | Msg2       |
      | user1Name | user2Name | user3Name | GroupChat     | YoMessage1 | YoMessage2 |

  @C551 @regression
  Scenario Outline: Verify I can delete and leave a group conversation from conversation list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I long tap on conversation name "<GroupChatName>"
    And I tap Clear content… button on Group conversation options menu
    And I tap CLEAR CONTENT AND LEAVE button on Confirm overlay page
    And I do not see conversation "<GroupChatName>" in Recent View
    When I open Search UI
    And I type user name "<GroupChatName>" in search field
    Then I do not see group <GroupChatName> in Search result list
    And I tap X button on Search page
    And User <Contact1> sends message "<Message>" to group conversation <GroupChatName>
    Then I do not see conversation "<GroupChatName>" in Recent View
    When I open Archive
    Then I do not see conversation "<GroupChatName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | Message |
      | user1Name | user2Name | user3Name | DELETELeave   | huhuhu  |

  @C552 @regression
  Scenario Outline: Verify I see picture, ping and call after I delete a group conversation from conversation list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given User <Contact1> sets the unique username
    Given <Contact1> starts instance using <CallBackend>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    And I see Recent View with conversations
    When I long tap on conversation name "<GroupChatName>"
    And I tap Clear content… button on Group conversation options menu
    And I tap CLEAR CONTENT button on Confirm overlay page
    And I do not see conversation "<GroupChatName>" in Recent View
    When I open Search UI
    And I type user name "<GroupChatName>" in search field
    Then I see group <GroupChatName> in Search result list
    And I tap X button on Search page
    And User <Contact1> sends image "<Image>" to group conversation <GroupChatName>
    Then I see conversation "<GroupChatName>" in Recent View
    When I long tap on conversation name "<GroupChatName>"
    And I tap Clear content… button on Group conversation options menu
    And I tap CLEAR CONTENT button on Confirm overlay page
    Then I do not see conversation "<GroupChatName>" in Recent View
    When User <Contact1> pings conversation <GroupChatName>
    Then I see conversation "<GroupChatName>" in Recent View
    When I long tap on conversation name "<GroupChatName>"
    And I tap Clear content… button on Group conversation options menu
    And I tap CLEAR CONTENT button on Confirm overlay page
    Then I do not see conversation "<GroupChatName>" in Recent View
    When <Contact1> calls <GroupChatName>
    And <Contact1> stops calling <GroupChatName>
    Then I see conversation "<GroupChatName>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | Image       | CallBackend |
      | user1Name | user2Name | user3Name | DELETE        | testing.jpg | chrome      |

  @C562 @regression
  Scenario Outline: I can mute 1:1 conversation from the conversation list
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I long tap on conversation name "<Contact1>"
    Then I see Single conversation options menu
    When I tap <SilenceItem> button on Single conversation options menu
    Then I see muted glyph of conversation "<Contact1>"

    Examples:
      | Name      | Contact1  | SilenceItem |
      | user1Name | user2Name | Mute        |

  @C560 @regression
  Scenario Outline: I can unmute 1:1 conversation from the conversation list
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User Myself mutes conversation with user <Contact1>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I see muted glyph of conversation "<Contact1>"
    When I long tap on conversation name "<Contact1>"
    Then I see Single conversation options menu
    When I tap <NotifyItem> button on Single conversation options menu
    Then I do not see muted glyph of conversation "<Contact1>"

    Examples:
      | Name      | Contact1  | NotifyItem |
      | user1Name | user2Name | Unmute     |

  @C558 @regression
  Scenario Outline: I can mute group conversation from the conversation list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I long tap on conversation name "<GroupChatName>"
    Then I see Single conversation options menu
    When I tap <SilenceItem> button on Single conversation options menu
    Then I see muted glyph of conversation "<GroupChatName>"

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | SilenceItem |
      | user1Name | user2Name | user3Name | MUTE          | Mute        |

  @C556 @regression
  Scenario Outline: I can unmute group conversation from the conversation list
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User Myself mutes conversation with user <GroupChatName>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I see muted glyph of conversation "<GroupChatName>"
    When I long tap on conversation name "<GroupChatName>"
    Then I see Single conversation options menu
    When I tap <NotifyItem> button on Single conversation options menu
    Then I do not see muted glyph of conversation "<Contact1>"

    Examples:
      | Name      | Contact1  | Contact2  | NotifyItem | GroupChatName |
      | user1Name | user2Name | user3Name | Unmute     | UNMUTE        |



