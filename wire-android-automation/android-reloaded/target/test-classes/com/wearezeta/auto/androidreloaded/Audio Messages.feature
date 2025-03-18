Feature: Audio Messages

  @TC-4081 @regression @RC @audioMessages
  Scenario Outline: I want to verify that I can receive and play audio files
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When User <TeamOwner> sends local audio file named "<FileName>" via device Device1 to group conversation "<GroupConversation>"
    Then I see an audio file in the conversation view
    And I see the time played in the audio file is 00:00
    When I tap play button on the audio file
    And I wait for 10 seconds
    And I tap pause button on the audio file
    Then I see the time played in the audio file is not 00:00

    Examples:
      | TeamOwner | Member1   | TeamName | GroupConversation  | FileName |
      | user1Name | user2Name | Audio    | SendYourAudioHere  | test.m4a |

  @TC-4082 @regression @RC @audioMessages
  Scenario Outline: I want to record and share an audio message
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When I tap file sharing button
    And I tap on Attach Audio option
    And I tap on start recording audio button
    And I wait for 5 seconds
    And I tap on stop recording audio button
    Then I see that my audio message was recorded
    When I send my recorded audio message
    Then I see an audio file in the conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | GroupConversation |
      | user1Name | user2Name | Audio    | SendYourAudioHere  |

  @TC-4083 @TC-4084 @regression @RC @audioMessages
  Scenario Outline: I want to listen again to my long audio message before I send it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
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
    When I tap file sharing button
    And I tap on Attach Audio option
    And I tap on start recording audio button
    And I wait for 20 seconds
    And I tap on stop recording audio button
    Then I see that my audio message was recorded
    When I tap on play button on recorded audio message
    And I wait for 2 seconds
    Then I see the time played in the audio file is not 00:00
    And I tap on pause button on recorded audio message
    # TC-4084 - I want to apply an audio filter to my audio message before I send it
    When I tap on apply audio filter checkbox
    Then I see audio filter is applied
    And I send my recorded audio message
    And I see an audio file in the conversation view

    Examples:
      | TeamOwner | Member1   | TeamName | GroupConversation |
      | user1Name | user2Name |  Audio   | SendYourAudioHere  |


