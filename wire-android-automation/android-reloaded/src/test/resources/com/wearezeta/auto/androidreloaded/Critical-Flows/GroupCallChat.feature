Feature: Group Call Chat

  @TC-8602 @CriticalFlows
  Scenario Outline: I want start a group call and participant exchange message, file and location during the call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member2> adds a new device Device1 with label Device1
    And User <TeamOwner> is me
    And <Member1>,<Member2> start instances using chrome
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
    And <Member1>,<Member2> accept next incoming call automatically
    When I tap start call button
    And <Member1>,<Member2> verify that waiting instance status is changed to active in 90 seconds
    Then I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    And Users <Member2> unmutes their microphone
    And Users <Member1> unmutes their microphone
    And User <Member1>,<Member2> verify to send and receive audio
    When I minimise the ongoing call
    And User <Member1> sends message "<Message>" to group conversation <GroupConversation>
    Then I see the message "<Message>" in current conversation
    And I tap file sharing button
    And I see sharing option for File is displayed
    And I see sharing option for Gallery is displayed
    And I see sharing option for Camera is displayed
    And I see sharing option for Video is displayed
    And I see sharing option for Audio is displayed
    And I see sharing option for Location is displayed
    When I push image with QR code containing "Image" to file storage
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    Then I see an image with QR code "Image" in the conversation view
    When User <Member2> shares the default location to user <GroupConversation> via device <Device>
    Then I see location map container in the conversation view
    Then I restore the ongoing call
    Then I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName      | GroupConversation | Message  | Device   |
      | user1Name | user2Name | user3Name | WeLikeCalling | GroupCall         | Hello!   | Device1  |
