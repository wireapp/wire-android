Feature: Status

  @TC-8413 @regression @RC @status
  Scenario Outline: I want to set my status to busy
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to busy on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I see my status is set to "Busy" on User Profile Page
    And I tap close button on User Profile Page
    And I see conversation list
    And I see status icon displayed next to my avatar on conversation list

    Examples:
      | TeamOwner | Member1   | TeamName | StatusText                                                                                                                                         |
      | user1Name | user2Name | Status   | You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations that are not muted. |

  @TC-8412 @regression @RC @status
  Scenario Outline: I want to set my status to away
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to away on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I see my status is set to "Away" on User Profile Page
    And I tap close button on User Profile Page
    And I see conversation list
    And I see status icon displayed next to my avatar on conversation list

    Examples:
      | TeamOwner | Member1   | TeamName | StatusText                                                                                                                                         |
      | user1Name | user2Name | Status   | You will appear as Away to other people. You will not receive notifications about any incoming calls or messages. |

  @TC-8414 @regression @RC @status
  Scenario Outline: I want to set my status to available
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to available on User Profile Page
    Then I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    And I see my status is set to "Available" on User Profile Page
    And I tap close button on User Profile Page
    And I see conversation list
    And I see status icon displayed next to my avatar on conversation list

    Examples:
      | TeamOwner | Member1   | TeamName | StatusText                                                                                                                                                                    |
      | user1Name | user2Name | Status   | You will appear as Available to other people. You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation. |

  @TC-8415 @TC-8416 @TC-8417 @TC-8426 @regression @RC @status
  Scenario Outline: I want to see when another user changed their status
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<Member2>" in conversation list
    # Initial message is needed for availability to be send
    And User <Member2> sends message "<Message>" to User <Member1>
    And I see the message "<Message>" in current conversation
    # TC-8416 - I want to see in a 1:1 conversation when another user changed their status
    When User <Member2> sets availability status to BUSY
    Then I see status icon displayed next to the profile picture of user "<Member2>" in conversation title
    When I close the conversation view through the back arrow
    #TC-8415 - I want to see on conversation list when another user changed their status
    Then I see status icon displayed next to user "<Member2>" avatar on conversation list
    # TC-8417 - I want to see in a group conversation when another user changed their status and sends a message
    When I tap on conversation name "<GroupChatName>" in conversation list
    And User <Member2> sends message "<Message>" to group conversation <GroupChatName>
    Then I see status icon displayed next to the profile picture of user "<Member2>" in the group conversation
    # TC-8426 - I want to see on participants list when another user changed their status
    When I tap on group conversation title "<GroupChatName>" to open group details
    And I see group details page
    And I tap on Participants tab
    And I see user <Member2> in participants list
    Then I see status icon displayed next to user "<Member2>" avatar on participants list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupChatName  | Message |
      | user1Name | user2Name | user3Name | Status   | We like status | Hello!  |