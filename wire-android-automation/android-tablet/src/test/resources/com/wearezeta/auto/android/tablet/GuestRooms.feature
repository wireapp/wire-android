Feature: Guest Options

  @C662724 @regression
  Scenario Outline: I want to see Wire and Wireless guest(s) are removed when I change the Allow guests from on to off
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given There are personal users <Guest1>,<Guest2>
    Given User <Member1> is connected to <Guest1>,<Guest2>
    Given User <TeamOwner> enables <BotName> service for team <TeamName>
    Given User <Member1> is me
    Given User <Member1> has conversation <GroupChatName> with <TeamOwner>,<Member2>,<Guest1>,<Guest2> in team <TeamName>
    Given User <Member1> adds bot <BotName> to conversation <GroupChatName>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Waiting for sync to be done in the background
    Given I wait for 5 seconds
    Given Team user <Member1> invites wireless user <WirelessGuest> to conversation <GroupChatName>
    When I tap on conversation name "<GroupChatName>"
    And I tap conversation name from top toolbar
    Then I see participant <TeamOwner> on Group info page
    And I see participant <Member2> on Group info page
    And I see <BotName> under Services section on group participants view
    And I see Wireless guest <WirelessGuest> on Group info page
    When I tap Guest options button on Group info page
    Then I see description for Guest toggle on Guest options page
    And I see Guest toggle is ON on Guest options page
    When I switch Guest toggle on Guest options page
    Then I tap Cancel button on Guest options page
    When I switch Guest toggle on Guest options page
    Then I tap Remove button on Guest options page
      And I wait for 5 seconds
    When I tap Back button
    Then I do not see participant <Guest1> on Group info page
    And I do not see participant <Guest2> on Group info page
    And I do not see <BotName> under Services section on group participants view

    Examples:
      | Member1   | Member2   | Guest1    | Guest2    | TeamOwner | TeamName  | GroupChatName | WirelessGuest | BotName |
      | user1Name | user2Name | user3Name | user4Name | user5Name | SuperTeam | GroupChat     | user6Name     | Echo    |

  @C662725 @regression
  Scenario Outline: Verify the right information is displayed on a Wireless guest's details view
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given User <Member1> is me
    Given User <Member1> has conversation <GroupChatName> with <TeamOwner>,<Member2> in team <TeamName>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<GroupChatName>"
    And Team user <Member1> invites wireless user <WirelessGuest> to conversation <GroupChatName>
    And I see system message contains "<WirelessGuest> JOINED" on group page
    And I see conversation banner informing me that there are guests
    And I tap conversation name from top toolbar
    And I tap participant avatar of <WirelessGuest> on Group info page
    Then I do not see unique user name on Group unconnected user details page
    And I do not see user name on Group unconnected user details page
    And I do not see +Connect button on Group unconnected user details page
    And I see Guest icon for team <TeamName> on Group connected user details page
    And I see expiration time on Group unconnected user details page
    And I tap More Actions button on Group connected user details page
    And I tap remove button on Group connected user options menu
    And I tap REMOVE button on Confirm overlay page
    Then I do not see participant <WirelessGuest> on Group info page
    When I tap back button
    And I see system message contains "<Message> <WirelessGuest>" on group page

    Examples:
      | Member1   | Member2   | TeamOwner | TeamName  | GroupChatName | WirelessGuest     | Message     |
      | user1Name | user2Name | user3Name | SuperTeam | GroupChat     | user4Name         | You removed |

  @C662726 @regression
  Scenario Outline: I want to create, share, revoke a link for a guest room
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given User <Member1> is me
    Given User <Member1> has conversation <GroupChatName> with <TeamOwner>,<Member2> in team <TeamName>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<GroupChatName>"
    Given I tap conversation name from top toolbar
    When I tap Guest options button on Group info page
    Then I see Guest toggle is ON on Guest options page
    When I tap Create link button on Guest options page
    Then I see description for link creation on Guest options page
    And I see link is in the correct format on Guest options page
    And I do not see Create link button on Guest options page
    And I see Copy link button on Guest options page
    And I see Share link button on Guest options page
    And I see Revoke link button on Guest options page
    When I tap Share link button on Guest options page
    Then I see the Wire app is not in foreground
    And I tap back button
  # I check my clipboard is filled properly
    When I tap Copy link button on Guest options page
    Then I verify that Android clipboard contains invite link
  # I can cancel revocation of a link
    When I tap Revoke link button on Guest options page
    Then I tap Cancel button on Guest options page
    When I tap Revoke link button on Guest options page
    And I tap Revoke link button on the alert
    Then I see Create link button on Guest options page
    And I do not see Share link button on Guest options page

    Examples:
      | Member1   | Member2   | TeamOwner | TeamName  | GroupChatName |
      | user1Name | user2Name | user3Name | SuperTeam | GroupChat     |