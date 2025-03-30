Feature: Conferencing Restriction

  @TC-4277 @TC-4285 @TC-4276 @TC-4280 @regression @RC @conferenceRestrictions
  Scenario Outline: I want to see an upgrade info dialog as a team member while initiating a conference call if I am not a part of paying team
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamChatName>" in conversation list
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    And I tap OK button on the alert
    And I do not see ongoing group call
    # TC-4280 - I want to be informed as soon as my team is upgraded to paying team - as a team member
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    # Instances will start after conference calling is enabled, to not see the upgrade alert on webapp
    And <TeamOwner>,<Member2> start instances using chrome
    And <TeamOwner>,<Member2> accept next incoming call automatically
    Then I see Wire Enterprise alert
    And I see subtext "Your team was upgraded to Wire Enterprise, which gives you access to features such as conference calls and more." in the Wire Enterprise alert
    And I see link "Learn more about Wire Enterprise" in the Wire Enterprise alert
    When I tap on Learn more link on the Enterprise alert
    Then I see webpage with "<URL>" is in foreground
    And I tap back button
    And I tap OK button on the alert
    # TC-4285 - I want to initiate a group conference call after my team is upgraded to paying team - as a team member
    # TC-4276 - I want to initiate an audio conference call (more than 2 participants) if I am a part of paying team
    When I tap start call button
    Then I see ongoing group call
    And <TeamOwner>,<Member2> verify that waiting instance status is changed to active in 90 seconds
    And I see users <TeamOwner>,<Member2> in ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | TeamChatName      | URL                    |
      | user1Name | user2Name | user3Name | SuperTeam | ConferenceCall    | wire.com/en/enterprise |

  @TC-4281 @TC-4283 @TC-4284 @TC-4288 @regression @RC @conferenceRestrictions
  Scenario Outline: I want to see a upgrade info dialog as a team admin while initiating an audio conference call if I am not a part of paying team
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamChatName>" in conversation list
    When I tap start call button
    Then I see upgrade to enterprise alert
    And I see subtext of upgrade to Enterprise alert containing "Your team is currently on the free Basic plan. Upgrade to Enterprise for access to features such as starting conferences and more."
    # TC-4284 - I want to see the wire pricing info page on clicking Learn more about wire pricing on dialog as a team admin
    When I tap on Learn more link on the Enterprise alert
    #ToDo: Check if link should be `wire.com/en/pricing/` instead of `teams.wire.com`
    Then I see webpage with "<PricingPageURL>" is in foreground
    And I tap back button
    # TC-4283 - I want to see group conversation after cancelling the upgrade dialog
    When I tap cancel button on the alert
    Then I see group conversation "<TeamChatName>" is in foreground
    And I do not see ongoing group call
    When I tap start call button
    And I tap Upgrade now button on the Enterprise alert
    Then I see webpage with "<TeamsPageURL>" is in foreground
    When I tap back button
    # TC-4288 - I want to initiate a group conference call after my team is upgraded to paying team - as a team admin
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    # Instances will start after conference calling is enabled, to not see the upgrade alert on webapp
    And <Member1>,<Member2> start instances using <CallBackend>
    And <Member1>,<Member2> accept next incoming call automatically
    Then I see Wire Enterprise alert
    And I see subtext "Your team was upgraded to Wire Enterprise, which gives you access to features such as conference calls and more." in the Wire Enterprise alert
    And I see link "Learn more about Wire Enterprise" in the Wire Enterprise alert
    And I tap OK button on the alert
    When I tap start call button
    And <Member1>,<Member2> verify that waiting instance status is changed to active in 90 seconds
    Then I see ongoing group call
    When User <Member1>,<Member2> switch video on
    And I wait for 2 seconds
    Then I see users <Member1>,<Member2> in ongoing group video call
    And Users <Member1>,<Member2> verifies to send and receive audio and video
    And I tap hang up button

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName  | TeamChatName      | CallBackend | PricingPageURL | TeamsPageURL   |
      | user1Name | user2Name | user3Name | SuperTeam | ConferenceCall    | chrome      | teams.wire.com | teams.wire.com |

  @TC-4278 @TC-4279 @regression @RC @conferenceRestrictions
  Scenario Outline: I should not be able to initiate a conference call as a personal user if I am not a part of paying team
    Given There are personal users <Name>,<Contact1>
    And Personal user <Contact1> sets profile image
    And User <Name> is me
    And User Myself is connected to <Contact1>
    And User Myself has group conversation <PersonalChatName> with <Contact1> as a personal user
    And <Contact1> starts instances using chrome
    And <Contact1> accepts next incoming call automatically
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<PersonalChatName>" in conversation list
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    And I tap OK button on the alert
    And I do not see ongoing group call
    # TC-4279 - I want to initiate an audio call with 1 participants while I am not a part of paying team - as a personal user
    When I tap back button
    And I tap on conversation name "<Contact1>" in conversation list
    And I tap start call button
    Then I do not see alert informing me that this feature is unavailable
    And <Contact1> verifies that waiting instance status is changed to active in 30 seconds
    And I see ongoing 1:1 call
    And I see <Contact1> in ongoing 1:1 call

    Examples:
      | Name      | Contact1  | PersonalChatName |
      | user1Name | user2Name | ConferenceCall   |

  @TC-4286 @TC-4289 @regression @RC @conferenceRestrictions
  Scenario Outline: I want to join a conference call in a group I am added while I am not a part of paying team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1>,<Member2> in team "<TeamName>"
    And There is a personal user <Guest>
    And User <TeamOwner> is connected to <Guest>
    And User <TeamOwner> adds user <Guest> to group conversation "<TeamChatName>"
    And User <Guest> is me
    And <Member1>,<Member2> start instances using chrome
    And <Member1> accepts next incoming call automatically
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamChatName>" in conversation list
    When User <Member2> calls <TeamChatName>
    And I see incoming group call from group <TeamChatName>
    And I accept the call
    And <Member1> verifies that waiting instance status is changed to active in 60 seconds
    And User <Member1>,<Member2> switch video on
    And Users <Member1>,<Member2> verify to send and receive audio and video
    Then I see users <Member1>,<Member2> in ongoing group video call
    And <Member1> stops calling
    And I tap hang up button
    And <Member2> stops calling <TeamChatName>
    And I wait for 5 seconds
    # TC-4289 - I should not be able to initiate a conference call in a group I am added while I am not a part of paying team - as a personal user
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    And I tap OK button on the alert
    And I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | Member2   | Guest     | TeamName  | TeamChatName      |
      | user1Name | user2Name | user3Name | user4Name | SuperTeam | ConferenceCall    |

  @TC-4287 @regression @conferenceRestrictions
  Scenario Outline: I want to join a conference call using guest link while I am not a part of paying team or as a personal user
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And I wait for 3 seconds
    And TeamOwner "<TeamOwner>" enables conference calling feature for team <TeamName> via backdoor
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> creates invite link for conversation <TeamChatName>
    And There is a personal user <Guest>
    And User <Guest> is me
    And <Member1>,<Member2> start instances using <CallBackend>
    And <Member2> accepts next incoming call automatically
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I minimise Wire
    And I open deep link for joining conversation <TeamChatName> that user <TeamOwner> has sent me
    And I tap join button on join conversation alert
    And User <Member1> calls <TeamChatName>
    And I see incoming group call from group <TeamChatName>
    And I accept the call
    And <Member2> verifies that waiting instance status is changed to active in <Timeout> seconds
    And User <Member2> verifies to send and receive audio
    And User <Member1> switches video on
    And I wait for 5 seconds
    Then I see a QR code with <Member1Email> in video stream
    And <Member1> stops calling <TeamChatName>
    And <Member2> stops calling
    And I tap hang up button
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    When I tap OK button on the alert
    Then I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | Member1Email | Member2   | Member2Email | Guest     | TeamName  | TeamChatName      | CallBackend    | Timeout |
      | user1Name | user2Name | user2Email   | user3Name | user3Email   | user4Name | SuperTeam | ConferenceCall    | chrome         | 10      |

  @TC-4290 @TC-4291 @regression @RC @conferenceRestrictions
  Scenario Outline: I want to initiate a call with 1 participants while I am a part of a non paying team as a team member
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> has 1:1 conversation with <Member2> in team "<TeamName>"
    And <Member2> starts instance using chrome
    And User <Member1> is me
    And <Member2> accepts next incoming call automatically
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<Member2>" in conversation list
    When I tap start call button
    Then I do not see alert informing me that this feature is unavailable
    And <Member2> verifies that waiting instance status is changed to active in 30 seconds
    And User <Member2> verifies to send and receive audio
    And I see ongoing 1:1 call
    And I see <Member2> in ongoing 1:1 call
    # TC-4291 - I want to have a video call with 1 participants while I am not a part of paying team - as a team member
    When I turn camera on
    And User <Member2> switches video on
    And I wait for 2 seconds
    Then I see user <Member2> in ongoing group video call
    And User <Member2> verifies to send and receive audio and video
    And I tap hang up button

    Examples:
      | Member1   | Member2   | TeamOwner | TeamName  |
      | user1Name | user2Name | user3Name | SuperTeam |

  @TC-4292 @regression @RC @conferenceRestrictions
  Scenario Outline: I shouldn't be able to initiate a call in a group in a non paying team
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1> in team "<TeamName>"
    And <Member1> start instances using chrome
    And <Member1> accept next incoming call automatically
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamChatName>" in conversation list
    When I tap start call button
    Then I see upgrade to enterprise alert
    And I see subtext of upgrade to Enterprise alert containing "Your team is currently on the free Basic plan. Upgrade to Enterprise for access to features such as starting conferences and more."
    And I tap cancel button on the alert
    And I do not see ongoing group call

    Examples:
      | TeamOwner | Member1   | TeamName  | TeamChatName      |
      | user1Name | user2Name | SuperTeam | ConferenceCall    |

  @TC-4293 @regression @RC @conferenceRestrictions
  Scenario Outline: I shouldn't be able to initiate an audio or video call in a group with 1 participant as a non paying team member
    Given There is a team owner "<TeamOwner>" with non paying team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <TeamChatName> with <Member1> in team "<TeamName>"
    And <Member1> start instances using <CallBackend>
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamChatName>" in conversation list
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    And I tap OK button on the alert
    And I do not see ongoing group call
    #FIXME: Add correct alert text once https://wearezeta.atlassian.net/browse/AR-2660 is done
#    Then I see alert message containing "Upgrade to Enterprise" in the title
#    And I see subtext of alert containing  "Your team is currently on the free Basic plan. Upgrade to Enterprise for access to features such as starting conferences and more." in the body


    Examples:
      | TeamOwner | Member1   | TeamName  | TeamChatName      | CallBackend |
      | user1Name | user2Name | SuperTeam | ConferenceCall    | chrome      |

  @TC-4294 @regression @RC @conferenceRestrictions
  Scenario Outline: I shouldn't be able to initiate an audio or video call in a group with 1 participant as a personal user
    Given There are personal users <Name>,<Contact>
    And User <Name> is me
    And User Myself is connected to <Contact>
    And User Myself has group conversation <ConversationName> with <Contact> as a personal user
    And <Contact> starts instances using <CallBackend>
    And <Contact> accepts next incoming call automatically
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<ConversationName>" in conversation list
    When I tap start call button
    Then I see alert informing me that this feature is unavailable
    And I see subtext of feature unavailable alert containing "To start a conference call, your team needs to upgrade to the Enterprise plan."
    And I tap OK button on the alert
    And I do not see ongoing group call

    Examples:
      | Name      | Contact   | CallBackend | ConversationName |
      | user1Name | user2Name | chrome      | Group            |