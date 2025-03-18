Feature: Constant Bit Rate

  ######################
  # 1:1 Calling
  ######################

  @TC-4672 @callingCBR @col1 @SF.Calls @TSFI.RESTfulAPI @S0.4 @S3 @S4 @S5
  Scenario Outline: I want to have 1:1 CBR call without enabling it in settings
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts 2FA instance using chrome
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <Member1> calls me
    And I see incoming call from <Member1>
    And I accept the call
    And I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And I unmute myself
    And User <Member1> verifies to send and receive audio
    Then User <Member1> verifies to have CBR connection
    And I tap hang up button
    And I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Email      | Member1   | TeamName    |
      | user1Name | user1Email | user2Name | WeLikeCalls |

  @TC-4673 @callingCBR @col1 @SF.Calls @TSFI.RESTfulAPI @S0.4 @S3 @S4 @S5
  Scenario Outline: I want to have 1:1 CBR Calls without enabling it in settings
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts 2FA instance using chrome
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And <Member1> accepts next incoming call automatically
    When I tap start call button
    And <Member1> verifies that waiting instance status is changed to active in 20 seconds
    And I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    Then User <Member1> verifies to have CBR connection
    And I tap hang up button
    And I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Email      | Member1   | TeamName    |
      | user1Name | user1Email | user2Name | WeLikeCalls |

  ######################
  # Group Calling
  ######################

  @TC-4674 @callingCBR @col1 @SF.Calls @TSFI.RESTfulAPI @S0.4 @S3 @S4 @S5
  Scenario Outline: I want to verify Conference Calling uses CBR without enabling it in settings when I accept
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1>,<Member2> start 2FA instances using chrome
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And <Member2> accepts next incoming call automatically
    When User <Member1> calls <GroupConversation>
    And I see incoming group call from group <GroupConversation>
    And I accept the call
    And I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    And I unmute myself
    Then Users <Member1>,<Member2> verify to have CBR connection
    And User <Member1>,<Member2> verifies to send and receive audio
    And I tap hang up button
    And I do not see ongoing group call

    Examples:
      | TeamOwner | Email      | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user1Email | user2Name | user3Name | WeLikeCalls | GroupCall         |

  @TC-4675 @callingCBR @col1 @SF.Calls @TSFI.RESTfulAPI @S0.4 @S3 @S4 @S5
  Scenario Outline: I want to verify Conference Calling uses CBR without enabling it in settings when I start it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1>,<Member2> start 2FA instances using chrome
    And I see Welcome Page
    And I open backend via deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And <Member1>,<Member2> accept next incoming call automatically
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    When I tap start call button
    And <Member1>,<Member2> verify that waiting instance status is changed to active in 90 seconds
    And I see ongoing group call
    And I see users <Member1>,<Member2> in ongoing group call
    And User <Member1>,<Member2> verify to send and receive audio
    Then Users <Member1>,<Member2> verify to have CBR connection
    And I tap hang up button
    And I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Email      | Member1   | Member2   | TeamName    | GroupConversation |
      | user1Name | user1Email | user2Name | user3Name | WeLikeCalls | GroupCall         |
