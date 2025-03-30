Feature: Federation Classified
# The classifiedDomain feature can be ENABLED or DISABLED on the backend
# If the feature is DISABLED the UI show no banner at all.
# If the feature is ENABLED a list of classified domains are set in the backend.
# The classified domain list on the backend can contain the own backend or not (see: https://docs.wire.com/developer/reference/config-options.html#classified-domains)
# If the classified domain list contains the own backend all conversations on the same backend have the classified banner
# If the classified domain list does NOT contain the own backend all conversations on the same backend have the unclassified banner

  ######################
  # Connections
  ######################

  @TC-4682 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the classified banner in incoming connection request from same classified domain
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
    And User <TeamOwnerA> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <TeamOwnerB> sends connection request to Me
    And I see unread conversation "<TeamOwnerB>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerB>" in conversation list
    And I tap on unread conversation name "<TeamOwnerB>" in conversation list
    And I see unconnected user <TeamOwnerB> profile
    Then I see classified domain label with text "<textClassified>" on unconnected user profile page

    Examples:
      | TeamOwnerA | Email      | TeamOwnerB | TeamNameA | TeamNameB | Subtitle         | textClassified         |
      | user1Name  | user1Email | user2Name  | Avocado   | Banana    | Wants to connect | SECURITY LEVEL: VS-NfD |

  @TC-4683 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the classified banner in outgoing connection request from same classified domain
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-1 backend
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-1 backend
    And User <TeamOwnerB> sets their unique username
    And User <TeamOwnerA> is me
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
    When I tap on search people field
    And I type unique user name "@<TeamOwnerBUniqueusername>" in search field
    And I see user name "<TeamOwnerB>" in Search result list
    And I tap on user name <TeamOwnerB> found on search page
    And I see unconnected user <TeamOwnerB> profile
    Then I see classified domain label with text "<textClassified>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    Then I see classified domain label with text "<textClassified>" on unconnected user profile page

    Examples:
      | TeamOwnerA | Email      | TeamOwnerB | TeamOwnerBUniqueusername | TeamNameA | TeamNameB | textClassified         |
      | user1Name  | user1Email | user2Name  | user2UniqueUsername      | Avocado   | Banana    | SECURITY LEVEL: VS-NfD |

  @TC-4686 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the unclassified banner in incoming connection request from same unclassified domain
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-3 backend
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-3 backend
    And User <TeamOwnerA> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    When User <TeamOwnerB> sends connection request to Me
    And I see unread conversation "<TeamOwnerB>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerB>" in conversation list
    And I tap on unread conversation name "<TeamOwnerB>" in conversation list
    And I see unconnected user <TeamOwnerB> profile
    Then I see classified domain label with text "<textNonClassified>" on unconnected user profile page

    Examples:
      | TeamOwnerA | Email      | TeamOwnerB | TeamNameA | TeamNameB | Subtitle         | textNonClassified            |
      | user1Name  | user1Email | user2Name  | Avocado   | Banana    | Wants to connect | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4687 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the unclassified banner in outgoing connection request from same unclassified domain
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>" on column-3 backend
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>" on column-3 backend
    And User <TeamOwnerB> sets their unique username
    And User <TeamOwnerA> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<TeamOwnerBUniqueusername>" in search field
    And I see user name "<TeamOwnerB>" in Search result list
    And I tap on user name <TeamOwnerB> found on search page
    Then I see classified domain label with text "<textNonClassified>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    Then I see classified domain label with text "<textNonClassified>" on unconnected user profile page

    Examples:
      | TeamOwnerA | Email      | TeamOwnerB | TeamOwnerBUniqueusername | TeamNameA | TeamNameB | textNonClassified            |
      | user1Name  | user1Email | user2Name  | user2UniqueUsername      | Avocado   | Banana    | SECURITY LEVEL: UNCLASSIFIED |

  ######################
  # Conversations
  ######################

  @TC-4678 @TC-4680 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the classified banner in classified 1:1 conversation on same domain
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
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
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<TeamOwner>" in conversation list
    Then I see classified domain label with text "<textClassified>" in the conversation view
    # TC-4680 - I want to see the classified banner in incoming/outgoing/ongoing calls in a classified 1:1 conversation on same domain
    When User <TeamOwner> calls me
    And I see incoming call from <TeamOwner>
    Then I see classified domain label with text "<textClassified>" on incoming call overlay
    And I decline the call
    And <TeamOwner> stops calling me
    And <TeamOwner> accepts next incoming call automatically
    When I tap start call button
    Then I see classified domain label with text "<textClassified>" on outgoing call overlay
    And <TeamOwner> verifies that waiting instance status is changed to active in 30 seconds
    When I see ongoing 1:1 call
    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see <TeamOwner> in ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | textClassified         |
      | user1Name | user2Name | user2Email | Column1  | SECURITY LEVEL: VS-NfD |

  @TC-4679 @TC-4681 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the classified banner in classified group conversation on same domain
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChat> with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
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
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    Then I see classified domain label with text "<textClassified>" in the conversation view
    # TC-4681 - I want to see the classified banner in incoming/outgoing/ongoing calls in a classified group conversation on same domain
    When User <TeamOwner> calls <GroupChat>
    And I see incoming group call from group <GroupChat>
    Then I see classified domain label with text "<textClassified>" on incoming call overlay
    And I decline the call
    And <TeamOwner> stops calling <GroupChat>
    And <TeamOwner> accepts next incoming call automatically
    When I tap start call button
    Then I see classified domain label with text "<textClassified>" on outgoing call overlay
    And <TeamOwner> verifies that waiting instance status is changed to active in 30 seconds
    When I see ongoing group call
    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see users <TeamOwner> in ongoing group call

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | GroupChat   | textClassified         |
      | user1Name | user2Name | user2Email |  Column1  | Fruit Salad | SECURITY LEVEL: VS-NfD |

  @TC-4685 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see classified banner in group conversation when user from unclassified domain leaves
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-1 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-3 backend
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has group conversation <GroupChat> with <TeamOwner3>,<Member1> in team "<TeamName1>"
    And I see Welcome Page
    And User <TeamOwner1> is me
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    When <TeamOwner3> leaves group conversation <GroupChat>
    Then I see classified domain label with text "<textClassified>" in the conversation view

    Examples:
      | TeamOwner1 | Email      | Member1   | TeamOwner3 | TeamName1 | TeamName3 | GroupChat   | textClassified         | textNonClassified            |
      | user1Name  | user1Email | user2Name | user3Name  | Column1   | Column3   | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4697 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in classified group conversation when user from unclassified domain joins
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-1 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-3 backend
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has group conversation <GroupChat> with <Member1> in team "<TeamName1>"
    And I see Welcome Page
    And User <Member1> is me
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    Then I see classified domain label with text "<textClassified>" in the conversation view
    When User <TeamOwner1> adds user <TeamOwner3> to group conversation "<GroupChat>"
    Then I see classified domain label with text "<textNonClassified>" in the conversation view

    Examples:
      | TeamOwner1 | Member1   | Email      | TeamOwner3 | TeamName1 | TeamName3 | GroupChat   | textClassified         | textNonClassified            |
      | user1Name  | user2Name | user2Email | user3Name  | Column1   | Column3   | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4688 @TC-4690 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the unclassified banner in unclassified 1:1 conversation on same domain
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-3 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
    And User <Member1> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<TeamOwner>" in conversation list
    When I tap on conversation name "<TeamOwner>" in conversation list
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    # TC-4690 - I want to see the unclassified banner in unclassified incoming/ongoing/outgoing 1:1 call on same domain
    When User <TeamOwner> calls me
    And I see incoming call from <TeamOwner>
    Then I see classified domain label with text "<textNonClassified>" on incoming call overlay
    And I decline the call
    And <TeamOwner> stops calling me
    And <TeamOwner> accepts next incoming call automatically
    When I tap start call button
    Then I see classified domain label with text "<textNonClassified>" on outgoing call overlay
    And <TeamOwner> verifies that waiting instance status is changed to active in 30 seconds
    When I see ongoing 1:1 call
    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I see <TeamOwner> in ongoing 1:1 call

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | textNonClassified            |
      | user1Name | user2Name | user2Email | Column1  | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4694 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in 1:1 conversation with user from unclassified domain when on classified domain
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-3 backend
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-1 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has 1:1 conversation with <TeamOwner3> in team "<TeamName1>"
    And User <TeamOwner3> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<TeamOwner1>" in conversation list
    When I tap on conversation name "<TeamOwner1>" in conversation list
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    And I do not see classified domain label with text "<textClassified>" in the conversation view
    When I open conversation details for 1:1 conversation with "<TeamOwner1>"
    And I see connected user <TeamOwner1> profile
    And I see classified domain label with text "<textNonClassified>" on connected user profile page

    Examples:
      | TeamOwner1 | Member1   | TeamOwner3 | Email      | TeamName1 | TeamName3 | textNonClassified            | textClassified         |
      | user1Name  | user2Name | user3Name  | user3Email |  Column1   | Column3   | SECURITY LEVEL: UNCLASSIFIED | SECURITY LEVEL: VS-NfD |

  @TC-4695 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in 1:1 conversation with user from classified domain when on unclassified domain
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-1 backend
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-3 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has 1:1 conversation with <TeamOwner3> in team "<TeamName1>"
    And User <TeamOwner3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<TeamOwner1>" in conversation list
    When I tap on conversation name "<TeamOwner1>" in conversation list
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    And I do not see classified domain label with text "<textClassified>" in the conversation view
    When I open conversation details for 1:1 conversation with "<TeamOwner1>"
    And I see connected user <TeamOwner1> profile
    And I see classified domain label with text "<textClassified>" on connected user profile page

    Examples:
      | TeamOwner1 | Member1   | TeamOwner3 | Email      | TeamName1 | TeamName3 | textNonClassified            | textClassified         |
      | user1Name  | user2Name | user3Name  | user3Email |  Column1   | Column3   | SECURITY LEVEL: UNCLASSIFIED | SECURITY LEVEL: VS-NfD |

  @TC-4689 @TC-4691 @TC-4699 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I want to see the unclassified banner in classified group conversation on same domain
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-3 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChat> with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
    And User <Member1> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    # TC-4693 - I should not see classified but unclassified banner in group conversation when on unclassified domain and participants are only from same unclassified domain
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    And I do not see classified domain label with text "<textClassified>" in the conversation view
    # TC-4691 - I want to see the unclassified banner in unclassified incoming/ongoing/outgoing group call on same domain
    When User <TeamOwner> calls <GroupChat>
    And I see incoming group call from group <GroupChat>
    Then I see classified domain label with text "<textNonClassified>" on incoming call overlay
    And I decline the call
    And <TeamOwner> stops calling <GroupChat>
    And <TeamOwner> accepts next incoming call automatically
    When I tap start call button
    Then I see classified domain label with text "<textNonClassified>" on outgoing call overlay
    And <TeamOwner> verifies that waiting instance status is changed to active in 30 seconds
    # TC-4699 - I should not see classified but unclassified banner in group call when on unclassified domain and participants are only from same unclassified domain
    When I see ongoing group call
    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see users <TeamOwner> in ongoing group call

    Examples:
      | TeamOwner | Member1   | Email      | TeamName | GroupChat   | textNonClassified            | textClassified         | textClassified         |
      | user1Name | user2Name | user2Email | Column1  | Fruit Salad | SECURITY LEVEL: UNCLASSIFIED | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: VS-NfD |

  @TC-4692 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in group conversation when self user is on unclassified domain and all participants are on classified domain
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-1 backend
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-3 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has group conversation <GroupChat> with <Member1>,<TeamOwner3> in team "<TeamName1>"
    And User <TeamOwner3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    Then I see classified domain label with text "<textNonClassified>" in the conversation view
    And I do not see classified domain label with text "<textClassified>" in the conversation view

    Examples:
      | TeamOwner1 | Member1   | TeamOwner3 | Email      | TeamName1 | TeamName3 | GroupChat   | textNonClassified            | textClassified         |
      | user1Name  | user2Name | user3Name  | user3Email | Column1   | Column3   | Fruit Salad | SECURITY LEVEL: UNCLASSIFIED | SECURITY LEVEL: VS-NfD |

  @TC-4698 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in group call when self user is on unclassified domain and all participants are on classified domain
    Given There is a team owner "<TeamOwner1>" with team "<TeamName1>" on column-1 backend
    And User <TeamOwner1> adds users <Member1> to team "<TeamName1>" with role Member
    And There is a team owner "<TeamOwner3>" with team "<TeamName3>" on column-3 backend
    And User <TeamOwner1> is connected to <TeamOwner3>
    And User <TeamOwner1> has group conversation <GroupChat> with <Member1>,<TeamOwner3> in team "<TeamName1>"
    And <TeamOwner1>, <Member1> start 2FA instances using chrome
    And <TeamOwner1>, <Member1> accept next incoming call automatically
    And User <TeamOwner3> is me
    And I see Welcome Page
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    When I tap start call button
    Then I see classified domain label with text "<textNonClassified>" on outgoing call overlay
    And I do not see classified domain label with text "<textClassified>" on outgoing call overlay
    And <TeamOwner1>, <Member1> verify that waiting instance status is changed to active in 90 seconds
    When I see ongoing group call
    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see users <TeamOwner1>, <Member1> in ongoing group call

    Examples:
      | TeamOwner1 | Member1   | TeamOwner3 | Email      | TeamName1 | TeamName3 | GroupChat   | textNonClassified            | textClassified         |
      | user1Name  | user2Name | user3Name  | user3Email | Column1   | Column3   | Fruit Salad | SECURITY LEVEL: UNCLASSIFIED | SECURITY LEVEL: VS-NfD |

  @TC-4700 @SF.VSNFDLABEL @TSFI.UserInterface @TSFI.Federate @S0.1 @S7 @col1 @col3
  Scenario Outline: I should not see classified but unclassified banner in ongoing group call when user joins classified conversation from unclassified domain
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-3 backend
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupChat> with <Member1> in team "<TeamName>"
    And <TeamOwner> starts 2FA instance using chrome
    And <TeamOwner> accept next incoming call automatically
    And I see Welcome Page
    And User <Member1> is me
    And I open column-3 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupChat>" in conversation list
    When I tap on conversation name "<GroupChat>" in conversation list
    When I tap start call button
    Then I see classified domain label with text "<textNonClassified>" on outgoing call overlay
    And I do not see classified domain label with text "<textClassified>" on outgoing call overlay
    And <TeamOwner> verifies that waiting instance status is changed to active in 60 seconds
    When I see ongoing group call
    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textClassified>" on ongoing call overlay
    And I see users <TeamOwner> in ongoing group call
    When User <TeamOwner> adds user <Member2> to group conversation "<GroupChat>"
    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
    And I do not see classified domain label with text "<textClassified>" on ongoing call overlay

    Examples:
      | TeamOwner | Member1   | Email      | Member2   | TeamName | GroupChat   | textClassified         | textNonClassified            |
      | user1Name | user2Name | user2Email | user3Name | Column1  | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |