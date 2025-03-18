Feature: Ping

  @C737 @regression
  Scenario Outline: Send ping and ping again to contact
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I tap Ping button from cursor toolbar
    Then I see Ping message "<Message1>" in the conversation view
    When I tap Ping button from cursor toolbar
    Then I see <Count> Ping messages in the conversation view

    Examples:
      | Name      | Contact   | Message1   | Count |
      | user1Name | user2Name | YOU PINGED | 2     |

  @C766 @regression
  Scenario Outline: Receive Ping and Ping Again in group conversation
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User Myself has group conversation <GroupChatName> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see that synchronizing the conversation list is finished within 60 seconds
    Given User <Contact1> pings conversation <GroupChatName>
    Given I see Recent View with conversations
    Given I see the subtitle "1 ping" of conversation <GroupChatName>
    When I tap on conversation name "<GroupChatName>"
    Then I see Ping message "<PingMessage>" in the conversation view
    When User <Contact1> pings conversation <GroupChatName>
    Then I see <Count> Ping messages in the conversation view

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName | PingMessage | Count |
      | user1Name | user2Name | user3Name | PingChat      | Pinged      | 2     |