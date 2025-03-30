Feature: Group Details

  @TC-4344 @regression @RC @groups @groupDetails
  Scenario Outline: I want to be able to change guests and services states
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    When I tap on group conversation title "<GroupConversation>" to open group details
    Then I see the guests options is "<State1>" on conversation details page
    When I tap on guest options on conversation details page
    Then I see the Guests switch is at "<State1>" state
    When I tap on the guest switch
    And I tap on disable button on pop up
    Then I see the Guests switch is at "<State2>" state
    And I tap back button on guests options page
    When I tap on the services switch
    And I tap on disable button on pop up
    Then I see the Services switch is at "<State2>" state

    Examples:
        | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation | State1 | State2 |
        | user1Name | user2Name | user3Name | GroupDeletion | MyTeam            | ON     | OFF    |

  @TC-4353 @regression @RC @groups @groupDetails
  Scenario Outline: I want to be able to change group name as a Team owner
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see "<GroupConversation>" as group name
    And I tap on "<GroupConversation>" group name
    When I change group name to "<NewGroupConversation>" as new group name
    Then I see "<NewGroupConversation>" as group name
    And I see toast message "<ToastMessage>" on group details page
    And I close the group conversation details through X icon
    And I see system message "You renamed the conversation" in conversation view

    Examples:
        | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation | NewGroupConversation | ToastMessage         |
        | user1Name | user2Name | user3Name | GroupDeletion | GroupName         | NewGroupName         | Conversation renamed |
