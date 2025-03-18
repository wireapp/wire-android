Feature: Teams

  @C574576 @regression
  Scenario Outline: Verify logging out from account
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    Given User <TeamOwner> adds users <Member1>,<Member2> to team <TeamName> with role Member
    Given User <TeamOwner> has conversation <ConversationName> with <Member1>,<Member2> in team <TeamName>
    Given There are personal users <PersonalAccount>,<PersonalContact>
    Given User <PersonalAccount> is connected to <PersonalContact>
    Given Users add the following devices: {"<PersonalContact>": [{}], "<Member2>": [{}]}
    Given User <Member1> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I open Self profile
    Given I tap New Team or Account button on Self profile page
    Given User <PersonalAccount> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Member2> sends 1 "<GroupMsg>" message to conversation <ConversationName>
    Given User <PersonalContact> sends 1 "<PersonalMsg>" message to conversation Myself
    Given I wait for 5 seconds
    Given I open Self profile
    Given I tap Account <TeamName> button on Self profile page
    Given I open Self profile
    Given I tap Settings button on Self profile page
    Given I select "Account" settings menu item
    When I select "Log out" settings menu item
    And I confirm sign out
    Then I see Recent View with conversations
    When I open Self profile
    And I tap Settings button on Self profile page
    And I select "Account" settings menu item
    And I select "Log out" settings menu item
    Then I confirm sign out
    When User <Member1> is me
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I wait for 5 seconds
    And I see Recent View with conversations
    And I tap on conversation name "<ConversationName>"
    Then I see the message "<GroupMsg>" in the conversation view
    And I navigate back from conversation
    And I open Self profile
    And I tap New Team or Account button on Self profile page
    And User <PersonalAccount> is me
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I see Recent View with conversations
    And I tap on conversation name "<PersonalContact>"
    Then I see the message "<PersonalMsg>" in the conversation view

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | ConversationName | PersonalAccount | PersonalContact | PersonalMsg | GroupMsg |
      | user1Name | SuperTeam | user2Name | user3Name | ConvoWithGuest   | user4Name       | user5Name       | Yo          | Hello    |
