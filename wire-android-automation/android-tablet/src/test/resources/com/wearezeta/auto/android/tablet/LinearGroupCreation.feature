Feature: LinearGroupCreation

  @C662727 @regression
  Scenario Outline: I can start the group conversation flow from 1:1
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given User <Member1> is me
    Given User <Member1> has 1:1 conversation with <Member2> in team <TeamName>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Member2>"
    When I tap conversation name from top toolbar
    And I tap add participants button on Group info page
    And I type group name "<GroupChatName>" on Group creation page
    And I tap Confirm on Group creation page
    Then I see selected contact <Member2> on Add people page
    When I tap Done button on Add people page
    Then I see new introduction message with conversation title <GroupChatName> in conversation view
    And I see Invite button in conversation view

    Examples:
      | Member1   | Member2   | TeamOwner | TeamName  | GroupChatName |
      | user1Name | user2Name | user3Name | SuperTeam | GroupChat     |

  @C662728 @regression
  Scenario Outline: I cannot create a group conversation using old flow anymore
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given User <Member1> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I open Search UI
    When I tap on user name found on search page <Member2>
    Then I see conversation title in top toolbar is <Member2>

    Examples:
      | Member1   | Member2   | TeamOwner | TeamName  |
      | user1Name | user2Name | user5Name | SuperTeam |

  @C662729 @regression
  Scenario Outline: I should not see team-related options if I am a personal user
    Given There are personal users <Name>,<Contact1>,<Contact2>,<Contact3>
    Given User <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>,<Contact3>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I open Search UI
    Given I do not see Create guest room button on Search UI page
    When I tap Create group button on Search page
    Then I do not see Guest toggle on Group creation page
    And I type group name "<GroupChatName>" on Group creation page
    And I tap Confirm on Group creation page
    When I select contacts <Contact1>,<Contact2>,<Contact3> on Add people page
    And I tap Done button on Add people page
    Then I see new introduction message with conversation title <GroupChatName> in conversation view
    And I do not see Invite button in conversation view
    When I tap conversation name from top toolbar
    Then I do not see Guest options button on Group info page

    Examples:
      | Name      | Contact1  | Contact2  | Contact3  | GroupChatName |
      | user1Name | user2Name | user3Name | user4Name | GroupChat     |