Feature: Group video

  @TC-8608 @CriticalFlows
  Scenario Outline: I want to be able to enable and see video in group call
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerB> sets their unique username
    And I wait for 3 seconds
    And TeamOwner "<TeamOwnerA>" enables conference calling feature for team <TeamNameA> via backdoor
    And TeamOwner "<TeamOwnerB>" enables conference calling feature for team <TeamNameB> via backdoor
    And User <TeamOwnerA> adds users <Member1>,<Member2>,<Member3> to team "<TeamNameA>" with role Member
    And User <TeamOwnerA> has group conversation <GroupConversation> with <Member1>,<Member2>,<Member3> in team "<TeamNameA>"
    And User <Member3> adds a new device Device1 with label Device1
    And User <TeamOwnerA> is me
    And <Member1>,<Member2>,<Member3> start instances using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    When I tap on start a new conversation button
    And I tap on search people field
    And I type unique user name "<Uniqueusername>" in search field
    And I see user name "<TeamOwnerB>" in Search result list
    And I tap on user name <TeamOwnerB> found on search page
    Then I see username "<TeamOwnerB>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    And I tap close button on unconnected user profile page
    And I tap the back arrow inside the search people field
    And I close the search page through X icon
    Then I see conversation "<TeamOwnerB>" is having pending status in conversation list
    When User <TeamOwnerB> accepts all requests
    And I see conversation "<GroupConversation>" in conversation list
    Then I see conversation "<TeamOwnerB>" in conversation list
    When I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on Participants tab
    And I tap on Add Participants button
    And I see user <TeamOwnerB> in search suggestions list
    And I select user <TeamOwnerB> in search suggestions list
    And I tap Continue button on add participants page
    And I see user <TeamOwnerB> in participants list
    And I close the group conversation details through X icon
    Then I see system message "You added <TeamOwnerB> to the conversation" in conversation view
    When <TeamOwnerB> start instances using chrome
    And <Member1>,<Member2>,<Member3>,<TeamOwnerB> accept next incoming call automatically
    And I tap start call button
    And <Member1>,<Member2>,<Member3>, <TeamOwnerB> verify that waiting instance status is changed to active in 90 seconds
    Then I see ongoing group call
    And I see users <Member1>,<Member2>,<Member3>, <TeamOwnerB> in ongoing group call
    When I turn camera on
    And Users <Member1>,<Member2>,<Member3>, <TeamOwnerB> switches video on
    And Users <Member1>,<Member2>,<Member3>, <TeamOwnerB> verify to send and receive audio and video
    Then I see users <Member1>,<Member2>,<Member3>, <TeamOwnerB> in ongoing group video call
    When I minimise the ongoing call
    And I tap on the text input field
    And I tap on ping button
    And I see ping alert
    And I see confirmation alert with text "Are you sure you want to ping 5 people?" in conversation view
    And I tap on ping button alert
    Then I see system message "You pinged" in conversation view
    When I hide the keyboard
    And I tap file sharing button
    And I tap on Attach Audio option
    And I tap on start recording audio button
    Then I see "You can't record an audio message during a call." toast message in conversation view
    When I tap file sharing button
    And User <Member3> sends local audio file named "<FileNameAudio>" via device Device1 to group conversation "<GroupConversation>"
    Then I see an audio file in the conversation view
    And I see the time played in the audio file is 00:00
    When I tap play button on the audio file
    And I wait for 10 seconds
    And I tap pause button on the audio file
    Then I see the time played in the audio file is not 00:00
    When I restore the ongoing call
    And I see users <Member1>,<Member2>,<Member3>, <TeamOwnerB> in ongoing group video call
    And I tap hang up button
    Then I do not see ongoing group call

    Examples:
      | TeamOwnerA | Member1   | Member2   | Member3   | TeamOwnerB | TeamNameA   | TeamNameB  | GroupConversation | Uniqueusername      | FileNameAudio |
      | user1Name  | user2Name | user3Name | user4Name | user5Name  | WeLikeCalls | IJoinCalls | GroupCall         | user5UniqueUsername | test.m4a      |
