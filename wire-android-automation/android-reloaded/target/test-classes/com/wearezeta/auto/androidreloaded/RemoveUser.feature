Feature: Remove User

  ######################
  # 1:1
  ######################

  @TC-4494 @regression @RC @removeUser
  Scenario Outline: I want 1:1 conversation to be removed from my conversation list after user is removed from team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member2>" in conversation list
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<Member2>" in conversation list
    And User <Member2> sends message "<Message>" to User Myself
    And I see the message "<Message>" in current conversation
    And I type the message "<Message2>" into text input field
    And I tap send button
    Then I see the message "<Message2>" in current conversation
    And I hide the keyboard
    When User <TeamOwner> removes user <Member2> from team <TeamName>
    Then I do not see conversation "<Member2>" in conversation list
    But I see conversation "<TeamOwner>" in conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | Message | Message2           |
      | user1Name | user2Name | user3Name | Migration | Hello!  | Hello to you, too! |

  ######################
  # Groups
  ######################

  @TC-4369 @regression @RC @groups @removeUser
  Scenario Outline: I want to see a system message when another member gets removed from the team as a guest
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And There is a personal user <User>
    And User <TeamOwner> is connected to <User>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3>,<User> in team "<TeamName>"
    And User <User> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> removes user <Member3> from team <TeamName>
    Then I see system message "<Member3> left the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | User      | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | user5Name | RemoveGroup | MyTeam            |

  @TC-4363 @regression @RC @groups @removeUser
  Scenario Outline: I want to see a system message when another member gets removed from the team as an admin
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> removes user <Member3> from team <TeamName>
    Then I see system message "<Member3> was removed from the team" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | RemoveGroup | MyTeam            |

  @TC-4365 @regression @RC @groups @removeUser
  Scenario Outline: I want to see a system message when another member gets removed from the team as a member
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When User <TeamOwner> removes user <Member3> from team <TeamName>
    Then I see system message "<Member3> was removed from the team" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | Member3   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | user4Name | RemoveGroup | MyTeam            |