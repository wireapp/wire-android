@col1
Feature: Calling

  ######################
  # 1:1
  ######################

  @TC-4718
  Scenario Outline: I want to make 1:1 conversation call in proteus while MLS is enabled
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts 2FA instance using chrome
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <TeamOwner> configures MLS for team "<TeamName>"
    And Conversation <Member1> from user <TeamOwner> uses proteus protocol
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    And <Member1> accepts next incoming call automatically
    When I tap start call button
    And <Member1> verifies that waiting instance status is changed to active in 20 seconds
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And Users <Member1> unmutes their microphone
    And User <Member1> verifies to send and receive audio
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Email      | Member1   | TeamName |
      | user1Name | user1Email | user2Name | MLS      |

  @TC-4720 @TC-4709
  Scenario Outline: I want to initiate an MLS 1:1 conversation audio/video call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts 2FA instance using chrome
    And I see Welcome Page
    And I open column-1 backend deep link
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
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And User <Member1> verifies to send and receive audio
    # TC-4709 - I want to initiate a MLS 1:1 conversation video call
    When I turn camera on
    And Users <Member1> switches video on
    Then User <Member1> verifies to send and receive audio and video
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Email      | Member1   | TeamName |
      | user1Name | user1Email | user2Name | MLS      |

  @TC-4721 @TC-4711
  Scenario Outline: I want to receive an MLS 1:1 conversation audio/video call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <TeamOwner> is me
    And <Member1> starts 2FA instance using chrome
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I tap on user name "<Member1>" in Search result list
    And I see connected user <Member1> profile
    And I tap start conversation button on connected user profile page
    And I see conversation view with "<Member1>" is in foreground
    When User <Member1> calls <TeamOwner>
    Then I see incoming call from <Member1>
    When I accept the call
    Then I see ongoing 1:1 call
    And I see <Member1> in ongoing 1:1 call
    And I unmute myself
    And User <Member1> verifies to send and receive audio
    # TC-4711 - I want to receive a MLS 1:1 conversation video call
    When I turn camera on
    And Users <Member1> switches video on
    And I see a QR code with <Member1Email> in video stream
    And User <Member1> verifies to send and receive audio and video
    When I tap hang up button
    Then I do not see ongoing 1:1 call

    Examples:
      | TeamOwner | Email      | Member1   | Member1Email | TeamName |
      | user1Name | user1Email | user2Name | user2Email   | MLS      |

  ######################
  # Groups
  ######################

  @TC-4717 @TC-4705
  Scenario Outline: I want to initiate an MLS group conversation audio/video call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And <TeamOwner>,<Member2>,<Member3> start 2FA instances using chrome
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And <TeamOwner>,<Member2>,<Member3> accept next incoming call automatically
    When I tap start call button
    And <TeamOwner>,<Member2>,<Member3> verify that waiting instance status is changed to active in 60 seconds
    Then I see ongoing group call
    And I see users <TeamOwner>,<Member2>,<Member3> in ongoing group call
    And User <TeamOwner>,<Member2>,<Member3> verify to send and receive audio
    # TC-4705 - I want to have a MLS conversation call with a video streams
    When I turn camera on
    And Users <TeamOwner>,<Member2>,<Member3> switches video on
    And Users <TeamOwner>,<Member2>,<Member3> unmutes their microphone
    And I wait for 5 seconds
    And Users <TeamOwner>,<Member2>,<Member3> verify to send and receive audio and video
    Then I see users <TeamOwner>,<Member2>,<Member3> in ongoing group video call
    When I tap hang up button
    Then I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLSCalling       |

  @TC-4719 @TC-4712
  Scenario Outline: I want to receive an MLS group conversation audio/video call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And <TeamOwner>,<Member2>,<Member3> start 2FA instances using chrome
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And <Member2>,<Member3> accept next incoming call automatically
    When User <TeamOwner> calls <ConversationName>
    Then I see incoming group call from group <ConversationName>
    When I accept the call
    Then I see ongoing group call
    And <Member2>,<Member3> verify that waiting instance status is changed to active in 60 seconds
    And I see users <TeamOwner>,<Member2>,<Member3> in ongoing group call
    When I turn camera on
    And Users <TeamOwner>,<Member2>,<Member3> switches video on
    And I wait for 5 seconds
    # TC-4712 - I want to verify CBR traffic when in a MLS group call
    Then Users <TeamOwner>,<Member2>,<Member3> verify to have CBR connection
    And Users <TeamOwner>,<Member2>,<Member3> verify to send and receive audio and video
    Then I see users <TeamOwner>,<Member2>,<Member3> in ongoing group video call
    When I tap hang up button
    Then I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLSCalling       |

  @TC-4710
  Scenario Outline: I want to receive a MLS conversation video group call
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1>,<Member2>,<Member3> to team "<TeamName>" with role Member
    And User <TeamOwner> configures MLS for team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And <TeamOwner>,<Member2>,<Member3> start 2FA instances using chrome
    And I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see user name "<Member2>" in Search result list
    And I select user <Member2> in search suggestions list
    And I clear the input field on Search page
    And I type user name "<Member3>" in search field
    And I see user name "<Member3>" in Search result list
    And I select user <Member3> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I hide the keyboard
    And I see the protocol used for creating conversation is MLS
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    And <Member2>,<Member3> accept next incoming call automatically
    When User <TeamOwner> calls <ConversationName>
    Then I see incoming group call from group <ConversationName>
    When  I turn camera on
    And I accept the call
    Then I see ongoing group call
    And <Member2>,<Member3> verify that waiting instance status is changed to active in 60 seconds
    And I see users <TeamOwner>,<Member2>,<Member3> in ongoing group call
    When I unmute myself
    And Users <TeamOwner>,<Member2>,<Member3> switches video on
    And I wait for 5 seconds
    And Users <TeamOwner>,<Member2>,<Member3> verify to send and receive audio and video
    Then I see users <TeamOwner>,<Member2>,<Member3> in ongoing group video call
    When I tap hang up button
    Then I do not see ongoing group call
    And I see join button in group conversation view

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | Member3   | TeamName | ConversationName |
      | user1Name | user2Name | user2Email | user3Name | user4Name | MLS      | MLSCalling       |

#  TC-4704	I want to stay on a call in MLS conversation while receiving a call from other clients	No	Medium

#  TC-4706	I want to see participant drop when they get removed from the team during a group call	No	Medium
#  TC-4707	I want to see bad connection indicator for call participants who have a bad network condition	No	Medium
#  TC-4708	I want to see bad connection indicator for myself when I have a bad network condition during call	No	Medium
#
#  TC-4715	I want to see shared screen during MLS group call	No	Medium
#  TC-4716	I want to see shared screen during MLS 1:1 call	No	Medium
