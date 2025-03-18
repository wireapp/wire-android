Feature: Calling

  ######################
  # 1:1 Calling
  ######################

  @TC-4258 @regression @calling @RC @smoke
  Scenario Outline: I want to be able to accept a 1on1 call from a Member of my team with the app in foreground
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <Member1> calls me
    Then I see incoming call from <Member1>
    When I accept the call
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | TeamName    |
      | user1Name | user2Name | WeLikeCalls |

  @TC-4262 @regression @calling @RC
  Scenario Outline: I want to be able to enable video before accepting a 1on1 call from a Member of my team with the app in foreground
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <Member1> calls me
    And I see incoming call from <Member1>
    And I turn camera on
    And I accept the call
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    When User <Member1> switches video on
    Then User <Member1> verifies to send and receive audio and video
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | TeamName    |
      | user1Name | user2Name | WeLikeCalls |

  @TC-4261 @regression @calling @smoke
  Scenario Outline: I want to be able to accept a 1on1 call from a Member of my team with the app in the background
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I minimise Wire
    And User <Member1> calls me
    Then I see incoming call from <Member1>
    When I accept the call
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | TeamName    |
      | user1Name | user2Name | WeLikeCalls |

  @TC-4259 @regression @RC @calling
  Scenario Outline: I want to be able to start a 1on1 call with a Member of my team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And <Member1> accepts next incoming call automatically
    When I tap start call button
    And <Member1> verifies that waiting instance status is changed to active in 20 seconds
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | TeamName    |
      | user1Name | user2Name | WeLikeCalls |

  @TC-4260 @regression @RC @calling
  Scenario Outline: I want to be able to enable and see video in 1:1 call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts instance using chrome
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And <Member1> accepts next incoming call automatically
    When I tap start call button
    And <Member1> verifies that waiting instance status is changed to active in 20 seconds
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    When I turn camera on
    And Users <Member1> switches video on
    And I wait for 3 seconds
    Then Users <Member1> verifies to send and receive audio and video
    And I see user <Member1> in ongoing 1:1 video call
    # ToDo: Add step one we can properly take a screenshot of the QR Code
    # And I see QR codes containing <Member1Email> in video grid
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | Member1Email | TeamName    |
      | user1Name | user2Name | user2Email   | WeLikeCalls |

  ######################
  # Group Calling
  ######################

  @TC-4263 @regression @RC @calling @groupCalling @smoke
  Scenario Outline: I want to be able to accept a group call with the app in foreground
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And <Member2> accept next incoming call automatically
    When User <Member1> calls <GroupConversation>
    Then I see incoming group call from group <GroupConversation>
    When I accept the call
    Then I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    When I unmute myself
    And Users <Member2> unmutes their microphone
    Then User <Member1>,<Member2> verify to send and receive audio
    When I tap hang up button
    Then I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | WeLikeCalls | GroupCall         |

  @TC-4269 @regression @calling @groupCalling @RC
  Scenario Outline: I want to be able to enable video before accepting a group call with the app in foreground
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And <Member2> accept next incoming call automatically
    When User <Member1> calls <GroupConversation>
    And I see incoming group call from group <GroupConversation>
    And I turn camera on
    And I accept the call
    Then I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    When I unmute myself
    And Users <Member2> unmutes their microphone
    And Users <Member1>,<Member2> switch video on
    Then User <Member1>,<Member2> verify to send and receive audio
    When I tap hang up button
    Then I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | WeLikeCalls | GroupCall         |

  @TC-4266 @regression @calling @groupCalling @smoke
  Scenario Outline: I want to be able to accept a group call with the app in the background
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And <Member2> accept next incoming call automatically
    When I minimise Wire
    And User <Member1> calls <GroupConversation>
    Then I see incoming group call from group <GroupConversation>
    When I accept the call
    Then I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    When I unmute myself
    Then User <Member1>,<Member2> verify to send and receive audio
    When I tap hang up button
    Then I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | WeLikeCalls | GroupCall         |

  @TC-4264 @regression @RC @calling @groupCalling
  Scenario Outline: I want to be able to start a group call with Members of my team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1>,<Member2> verify to send and receive audio
    When I tap hang up button
    Then I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | WeLikeCalls | GroupCall         |

  @TC-4265 @regression @RC @calling @groupCalling @smoke
  Scenario Outline: I want to be able to enable and see video in group call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1>,<Member2> verify to send and receive audio
    When I turn camera on
    And Users <Member1>,<Member2> switches video on
    And I wait for 2 seconds
    And Users <Member1>,<Member2> verify to send and receive audio and video
    Then I see users <Member1>,<Member2> in ongoing group video call
    # ToDo: Add step one we can properly take a screenshot of the QR Code
    # And I see QR codes containing <Member1Email>,<Member2Email> in video grid
    When I tap hang up button
    Then I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Member1   | Member1Email | Member2   | Member2Email| TeamName    | GroupConversation |
      | user1Name | user2Name | user2Email   | user3Name | user3Email  | WeLikeCalls | GroupCall         |

  @TC-4270 @regression @RC @calling @groupCalling
  Scenario Outline: I should not have to enable my video again when it was enabled and I minimise an ongoing call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
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
    And User <Member1>,<Member2> verify to send and receive audio
    When I turn camera on
    And Users <Member1>,<Member2> switches video on
    And I wait for 2 seconds
    And Users <Member1>,<Member2> verify to send and receive audio and video
    Then I see users <Member1>,<Member2> in ongoing group video call
    When I minimise the ongoing call
    And I wait for 1 second
    And I restore the ongoing call
    Then Users <Member1>,<Member2> verify to send and receive audio and video
    And I see users <Member1>,<Member2> in ongoing group video call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user2Name | user3Name | WeLikeCalls | GroupCall         |