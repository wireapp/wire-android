Feature: Federation Classified
  # Most tests are currently disabled because a/b/c now have MLS.
  # Either MLS needs to be enabled for all the tests, if backend will be used for MLS cloud testing,
  # Or tests need to be deleted and replaced by tests for MLS cloud

  ######################
  # Connections
  ######################
#
#  @TC-4115 @TC-4116 @TC-4117 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner if I receive a connection request from a user who is on a classified domain when I am on a classified domain
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And There is a team owner "<TeamOwnerBella2>" with team "<TeamNameBella2>" on bella backend
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    When User <TeamOwnerAnta> sends connection request to Me
#    And I wait until the notification popup disappears
#    Then I see unread conversation "<TeamOwnerAnta>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerAnta>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerAnta>" in conversation list
#    And I see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerAnta>" on unconnected user profile page
#    And I tap accept button on unconnected user profile page
#    And I tap back button
#    # TC-4116 - I want to see the unclassified banner if I receive a connection request from a user who is not classified when my backend is a classified domain
#    When User <TeamOwnerChala> sends connection request to Me
#    Then I see unread conversation "<TeamOwnerChala>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerChala>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerChala>" in conversation list
#    And I see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerChala>" on unconnected user profile page
#    And I tap accept button on unconnected user profile page
#    And I tap back button
#    # TC-4117 - I want to see the classified banner if I receive a connection request from a user who is on the same backend which is classified
#    When User <TeamOwnerBella2> sends connection request to Me
#    Then I see unread conversation "<TeamOwnerBella2>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerBella2>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerBella2>" in conversation list
#    And I see classified domain label with text "<textClassified>" on unconnected user profile page
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | TeamOwnerChala | TeamOwnerBella2 | TeamNameAnta | TeamNameBella | TeamNameChala | TeamNameBella2 | Subtitle         | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name      | user4Name       | Avocado      | Banana        | Cherry        | Blueberry      | Wants to connect | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4118 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified indicator if I receive a connection request from a user when my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And There is a team owner "<TeamOwnerChala2>" with team "<TeamNameChala2>" on chala backend
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    When User <TeamOwnerAnta> sends connection request to Me
#    And I wait until the notification popup disappears
#    And I see unread conversation "<TeamOwnerAnta>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerAnta>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerAnta>" in conversation list
#    Then I do not see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I do not see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerChala>" on unconnected user profile page
#    And I tap ignore button on unconnected user profile page
#    When User <TeamOwnerBella> sends connection request to Me
#    And I see unread conversation "<TeamOwnerBella>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerBella>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerBella>" in conversation list
#    Then I do not see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I do not see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerChala>" on unconnected user profile page
#    And I tap accept button on unconnected user profile page
#    And I close the user profile through the close button
#    When User <TeamOwnerChala2> sends connection request to Me
#    And I see unread conversation "<TeamOwnerChala2>" in conversation list
#    And I see subtitle "<Subtitle>" of conversation "<TeamOwnerChala2>" in conversation list
#    And I tap on unread conversation name "<TeamOwnerChala2>" in conversation list
#    Then I do not see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I do not see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I do not see federated guest icon for user "<TeamOwnerChala2>" on unconnected user profile page
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | TeamOwnerChala | TeamOwnerChala2 | TeamNameAnta | TeamNameBella | TeamNameChala | TeamNameChala2 | Subtitle         | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name      | user4Name       | Avocado      | Banana        | Cherry        | Cactus         | Wants to connect | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4119 @TC-4120 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner if I sent a connection request to a user who is on the same backend which is classified
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And There is a team owner "<TeamOwnerBella2>" with team "<TeamNameBella2>" on bella backend
#    And User <TeamOwnerChala> sets their unique username
#    And User <TeamOwnerBella2> sets their unique username
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<TeamOwnerBella2Uniqueusername>" in search field
#    And I see user name "<TeamOwnerBella2>" in Search result list
#    And I tap on user name <TeamOwnerBella2> found on search page
#    And I tap connect button on unconnected user profile page
#    Then I see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I see guest icon for user "<TeamOwnerBella2>" on unconnected user profile page
#    And I close the user profile through the close button
#    And I tap back button
#    # TC-4120 - I want to see the unclassified banner if I sent a connection request to a user who is not classified when my backend is a classified domain
#    When I tap on start a new conversation button
#    And I tap on search people field
#    And I type unique user name "<TeamOwnerChalaUniqueusername><ChalaFederatedBackendName>" in search field
#    And I see user name "<TeamOwnerChala>" in Search result list
#    And I tap on user name <TeamOwnerChala> found on search page
#    And I tap connect button on unconnected user profile page
#    Then I see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerChala>" on unconnected user profile page
#
#    Examples:
#      | TeamOwnerBella | TeamOwnerChala | TeamOwnerBella2 | TeamNameBella | TeamNameChala | TeamNameBella2 | TeamOwnerBella2Uniqueusername | TeamOwnerChalaUniqueusername | ChalaFederatedBackendName | textClassified         | textNonClassified            |
#      | user1Name      | user2Name      | user3Name       | Banana        | Cherry        | Blueberry      | user3UniqueUsername           | user2UniqueUsername          | @chala.wire.link          | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4121 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner if I sent a connection request to a user who is on a classified domain when I am on a classified domain
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> sets their unique username
#    And User <TeamOwnerAnta> is me
#    And I see Welcome Page
#    And I open anta backend deep link
#    And I see alert informing me that I am about to switch to anta backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    And I type unique user name "<TeamOwnerBellaUniqueusername><BellaFederatedBackendName>" in search field
#    And I see user name "<TeamOwnerBella>" in Search result list
#    And I tap on user name <TeamOwnerBella> found on search page
#    And I tap connect button on unconnected user profile page
#    Then I see classified domain label with text "<textClassified>" on unconnected user profile page
#    And I see federated guest icon for user "<TeamOwnerBella>" on unconnected user profile page
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | TeamNameAnta | TeamNameBella | TeamOwnerBellaUniqueusername | BellaFederatedBackendName | textClassified         |
#      | user1Name     | user2Name      | Avocado      | Banana        | user2UniqueUsername          | @bella.wire.link          | SECURITY LEVEL: VS-NfD |
#
#  @TC-4122 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified indicator if I sent a connection request to a user, when my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerAnta> sets their unique username
#    And User <TeamOwnerChala> is me
#      And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I tap on start a new conversation button
#    When I tap on search people field
#    When I type unique user name "<TeamOwnerAntaUniqueusername><AntaFederatedBackendName>" in search field
#    And I see user name "<TeamOwnerAnta>" in Search result list
#    And I tap on user name <TeamOwnerAnta> found on search page
#    And I tap connect button on unconnected user profile page
#    Then I do not see classified domain label with text "<textNonClassified>" on unconnected user profile page
#    And I do not see classified domain label with text "<textClassified>" on unconnected user profile page
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerChala | TeamNameAnta | TeamNameChala | TeamOwnerAntaUniqueusername | AntaFederatedBackendName | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | Avocado      | Cherry        | user1UniqueUsername         | @anta.wire.link          | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  ######################
#  # Conversations
#  ######################
#
#  @TC-4123 @TC-4124 @TC-4125 @TC-4126 @TC-4127 @TC-4128 @TC-4129 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner if I am in a conversation with a user who is on a classified domain when I am on a classified domain
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And There is a team owner "<TeamOwnerBella2>" with team "<TeamNameBella2>" on bella backend
#    And User <TeamOwnerBella> is connected to <Member1>,<Member2>,<TeamOwnerAnta>,<TeamOwnerChala>,<TeamOwnerBella2>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> in team "<TeamNameBella>"
#    And User <TeamOwnerBella> has group conversation <GroupChat1> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> in team "<TeamNameBella>"
#    And User <TeamOwnerBella> has group conversation <GroupChat2> with <Member3>,<Member4>,<TeamOwnerBella2> in team "<TeamNameBella>"
#    And User <TeamOwnerBella> has group conversation <GroupChat3> with <TeamOwnerChala> in team "<TeamNameBella>"
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<Member1>" in conversation list
#    When I tap on conversation name "<Member1>" in conversation list
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4124 - I want to see the unclassified banner if I am in a conversation with a user who is not classified when my backend is a classified domain
#    And I see conversation "<TeamOwnerChala>" in conversation list
#    When I tap on conversation name "<TeamOwnerChala>" in conversation list
#    Then I see classified domain label with text "<textNonClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4125 - I want to see the classified banner if I am in a conversation with a user who is on the same backend which is classified
#    And I see conversation "<TeamOwnerBella2>" in conversation list
#    When I tap on conversation name "<TeamOwnerBella2>" in conversation list
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4126 - I want to see the classified banner if I am in a group conversation with classified users only when I am on a classified domain
#    And I see conversation "<GroupChat>" in conversation list
#    When I tap on conversation name "<GroupChat>" in conversation list
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4127 - I want to see the unclassified banner if I am in a group conversation with non classified users only when my backend is a classified domain
#    And I see conversation "<GroupChat3>" in conversation list
#    When I tap on conversation name "<GroupChat3>" in conversation list
#    Then I see classified domain label with text "<textNonClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4128 - I want to see the classified banner if I am in a group conversation with users from the same backend which is classified
#    And I see conversation "<GroupChat2>" in conversation list
#    When I tap on conversation name "<GroupChat2>" in conversation list
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    # TC-4129 - I want to see the unclassified banner if I am in a group conversation with non classified and classified users when my backend is a classified domain
#    And I see conversation "<GroupChat1>" in conversation list
#    When I tap on conversation name "<GroupChat1>" in conversation list
#    Then I see classified domain label with text "<textNonClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | TeamOwnerBella2 | TeamNameAnta | TeamNameBella | TeamNameChala | TeamNameBella2 | GroupChat   | GroupChat1 | GroupChat2 | GroupChat3 | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | user8Name       | Avocado      | Banana        | Cherry        | Blueberry      | Fruit Salad | Smoothie   | Sundae     | Fruit Cake | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4130 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified indicator if I am in a conversation with a user when my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> is me
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>,<TeamOwnerBella>
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<TeamOwnerAnta>" in conversation list
#    When I tap on conversation name "<TeamOwnerAnta>" in conversation list
#    Then I do not see classified domain label with text "<textClassified>" in the conversation view
#    And I do not see classified domain label with text "<textNonClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    And I see conversation "<TeamOwnerBella>" in conversation list
#    When I tap on conversation name "<TeamOwnerBella>" in conversation list
#    Then I do not see classified domain label with text "<textClassified>" in the conversation view
#    And I do not see classified domain label with text "<textNonClassified>" in the conversation view
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerChala | TeamOwnerBella | TeamNameAnta | TeamNameChala | TeamNameBella | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name      | Avocado      | Cherry        | Banana        | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4131 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified indicator if I am in a group conversation with classified and non classified users when my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And I see Welcome Page
#    And User <TeamOwnerChala> is me
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>
#    And User <TeamOwnerChala> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella> in team "<TeamNameChala>"
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    When I tap on conversation name "<GroupChat>" in conversation list
#    Then I do not see classified domain label with text "<textClassified>" in the conversation view
#    And I do not see classified domain label with text "<textNonClassified>" in the conversation view
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat  | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name      | Avocado      | Banana        | Cherry        | Fruit Cake | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4132 @TC-4133 @TC-4134 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner degrade to a non classified banner when a non classified user joins a classified group conversation
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member5> to team "<TeamNameChala>" with role Member
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member5>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> in team "<TeamNameBella>"
#    And I see Welcome Page
#    And User <TeamOwnerBella> is me
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    When I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textClassified>" in the conversation view
#    When User <TeamOwnerBella> adds user <TeamOwnerChala> to group conversation "<GroupChat>"
#    Then I see classified domain label with text "<textNonClassified>" in the conversation view
#    # TC-4133 - I want to see the non classified banner upgrade to a classified banner when a non classified user leaves from a classified group conversation and my backend is classified
#    When <TeamOwnerChala> leaves group conversation <GroupChat>
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#    # TC-4134 - I want to see the non classified banner upgrade to a classified banner when a non classified user was removed from a classified group conversation and my backend is classified
#    When User <TeamOwnerBella> adds user <Member5> to group conversation "<GroupChat>"
#    And I see classified domain label with text "<textNonClassified>" in the conversation view
#    When User <TeamOwnerBella> removes user <Member5> from group conversation "<GroupChat>"
#    Then I see classified domain label with text "<textClassified>" in the conversation view
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | Member5   | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat   | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | user8Name | Avocado      | Banana        | Cherry        | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4135 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified banner if a non classified user leaves or is removed from a group conversation and only classified users are left but my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member5> to team "<TeamNameChala>" with role Member
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4>
#    And User <TeamOwnerChala> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4>,<Member5> in team "<TeamNameChala>"
#    And User <TeamOwnerChala> has group conversation <GroupChat1> with <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4>,<Member5> in team "<TeamNameChala>"
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    When I tap on conversation name "<GroupChat>" in conversation list
#    Then I do not see classified domain label with text "<textNonClassified>" in the conversation view
#    And I do not see classified domain label with text "<textClassified>" in the conversation view
#    When <Member5> leaves group conversation <GroupChat>
#    Then I do not see classified domain label with text "<textNonClassified>" in the conversation view
#    And I do not see classified domain label with text "<textClassified>" in the conversation view
#    And I close the conversation view through the back arrow
#    And I see conversation "<GroupChat1>" in conversation list
#    And I tap on conversation name "<GroupChat1>" in conversation list
#    And I tap on group conversation title "<GroupChat1>" to open group details
#    And I see group details page
#    And I tap on Participants tab
#    And I see user <Member3> in participants list
#    When I tap on user <Member3> in participants list
#    Then I see connected user <Member3> profile
#    And I see remove from group button
#    When I tap remove from group button
#    Then I see alert asking me if I want to remove user <Member3> from group
#    When I tap remove button on alert
#    Then I do not see remove from group button
#    When I close the user profile through the close button
#    And I close the group conversation details through X icon
#    Then I do not see classified domain label with text "<textNonClassified>" in the conversation view
#    And I do not see classified domain label with text "<textClassified>" in the conversation view
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | Member5   | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat   | GroupChat1 | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | user8Name | Avocado      | Banana        | Cherry        | Fruit Salad | Sundae     | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4136 @TC-4137 @TC-4138 @regression @RC @federation @federationClassified
#  Scenario Outline: I want to see the classified banner on the user profile of a user who is on a classified domain when I am on a different classified domain
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And There is a team owner "<TeamOwnerBella2>" with team "<TeamNameBella2>" on bella backend
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<TeamOwnerChala>,<TeamOwnerBella2>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<TeamOwnerChala>,<TeamOwnerBella2> in team "<TeamNameBella>"
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I tap on group conversation title "<GroupChat>" to open group details
#    And I see group details page
#    And I tap on Participants tab
#    And I see user <TeamOwnerAnta> in participants list
#    When I tap on user <TeamOwnerAnta> in participants list
#    And I see connected user <TeamOwnerAnta> profile
#    Then I see classified domain label with text "<textClassified>" on connected user profile page
#    And I close the user profile through the close button
#    # TC-4137 - I want to see the unclassified banner on the user profile of a user who is not classified when my backend is a classified domain
#    And I see user <TeamOwnerChala> in participants list
#    When I tap on user <TeamOwnerChala> in participants list
#    And I see connected user <TeamOwnerChala> profile
#    Then I see classified domain label with text "<textNonClassified>" on connected user profile page
#    And I close the user profile through the close button
#    # TC-4138 - I want to see the classified banner on the user profile of a user who is on the same backend which is classified
#    And I see user <TeamOwnerBella2> in participants list
#    When I tap on user <TeamOwnerBella2> in participants list
#    And I see connected user <TeamOwnerBella2> profile
#    Then I see classified domain label with text "<textClassified>" on connected user profile page
#    And I close the user profile through the close button
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | TeamOwnerChala | TeamOwnerBella2 | TeamNameAnta | TeamNameBella | TeamNameChala | TeamNameBella2 | GroupChat   | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name      | user4Name       | Avocado      | Banana        | Cherry        | Blueberry      | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4139 @regression @RC @federation @federationClassified
#  Scenario Outline: I should not see any classified indicator on the user profile of a user who is classified if my backend is not classified
#    # Anta backend has feature enabled and lists Anta, Bella and Chala as classified domains
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    # Bella backend has feature enabled and lists Anta and Bella as classified domains
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member1> to team "<TeamNameBella>" with role Member
#    # Chala backend has feature disabled
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> is connected to <Member1>,<TeamOwnerAnta>,<TeamOwnerBella>
#    And User <TeamOwnerChala> has group conversation <GroupChat> with <Member1>,<TeamOwnerAnta>,<TeamOwnerBella> in team "<TeamNameChala>"
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I tap on group conversation title "<GroupChat>" to open group details
#    And I see group details page
#    And I tap on Participants tab
#    And I see user <TeamOwnerAnta> in participants list
#    When I tap on user <TeamOwnerAnta> in participants list
#    Then I do not see classified domain label with text "<textClassified>" on connected user profile page
#    And I do not see classified domain label with text "<textNonClassified>" on connected user profile page
#    And I see federated guest icon for user "<TeamOwnerAnta>" on connected user profile page
#    And I close the user profile through the close button
#    And I see user <Member1> in participants list
#    When I tap on user <Member1> in participants list
#    Then I do not see classified domain label with text "<textClassified>" on connected user profile page
#    And I do not see classified domain label with text "<textNonClassified>" on connected user profile page
#    And I see federated guest icon for user "<Member1>" on connected user profile page
#    And I close the user profile through the close button
#    And I see user <TeamOwnerBella> in participants list
#    When I tap on user <TeamOwnerBella> in participants list
#    Then I do not see classified domain label with text "<textClassified>" on connected user profile page
#    And I do not see classified domain label with text "<textNonClassified>" on connected user profile page
#    And I see federated guest icon for user "<TeamOwnerBella>" on connected user profile page
#    And I close the user profile through the close button
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | Member1   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat   | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name | user4Name      | Avocado      | Banana        | Cherry        | Fruit Salad | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  ######################
#  # 1:1 Calling
#  ######################
#
#  @TC-4140 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the classified banner if I am in a call with a user who is on a classified domain when I am on a classified domain
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerAnta> is me
#    And User <TeamOwnerAnta> is connected to <TeamOwnerBella>
#    And <TeamOwnerBella> starts instance using chrome
#    And I see Welcome Page
#    And I open anta backend deep link
#    And I see alert informing me that I am about to switch to anta backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<TeamOwnerBella>" in conversation list
#    And I tap on conversation name "<TeamOwnerBella>" in conversation list
#    And <TeamOwnerBella> accepts next incoming call automatically
#    When I tap start call button
#    Then I see federated guest icon for user "<TeamOwnerBella>" on outgoing call overlay
#    And I see classified domain label with text "<textClassified>" on outgoing call overlay
#    When <TeamOwnerBella> verifies that waiting instance status is changed to active in 30 seconds
#    And User <TeamOwnerBella> verifies to send and receive audio
#    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta  | TeamOwnerBella | TeamNameAnta | TeamNameBella | textClassified         |
#      | user1Name      | user2Name      | Avocado      | Banana        | SECURITY LEVEL: VS-NfD |
#
#  @TC-4141 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the Not Classified banner if I am in a call with a user who is not classified when my backend is a classified domain
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> is me
#    And User <TeamOwnerBella> is connected to <TeamOwnerChala>
#    And <TeamOwnerChala> starts instance using <CallBackend>
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<TeamOwnerChala>" in conversation list
#    And I tap on conversation name "<TeamOwnerChala>" in conversation list
#    And <TeamOwnerChala> accepts next incoming call automatically
#    When I tap start call button
#    Then I see federated guest icon for user "<TeamOwnerChala>" on outgoing call overlay
#    And I see classified domain label with text "<textNonClassified>" on outgoing call overlay
#    When <TeamOwnerChala> verifies that waiting instance status is changed to active in 30 seconds
#    And User <TeamOwnerChala> verifies to send and receive audio
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerBella | TeamOwnerChala | TeamNameBella | TeamNameChala | CallBackend | textNonClassified            |
#      | user1Name      | user2Name      | Banana        | Cactus        | chrome      | SECURITY LEVEL: UNCLASSIFIED |

  @TC-4142 @regression @RC @federation @federationClassified @federationCalling
  Scenario Outline: I want to see the classified banner if I am in a call with a user who is on the same backend which is classified
    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
    And User <TeamOwnerBella> adds users <Member1> to team "<TeamNameBella>" with role Member
    And User <TeamOwnerBella> is me
    And User <TeamOwnerBella> has 1:1 conversation with <Member1> in team "<TeamNameBella>"
    And <Member1> starts instance using <CallBackend>
    And I see Welcome Page
    And I open bella backend deep link
    And I see alert informing me that I am about to switch to bella backend
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When User <Member1> calls me
    Then I see incoming call from <Member1>
    And I do not see federated guest icon for user "<Member1>" on incoming call overlay
    And I see classified domain label with text "<textClassified>" on incoming call overlay
    When I accept the call
    And User <Member1> verifies to send and receive audio
    Then I see classified domain label with text "<textClassified>" on ongoing call overlay

    Examples:
      | TeamOwnerBella | Member1   | TeamNameBella | CallBackend | textClassified         |
      | user1Name      | user2Name | Banana        | chrome      | SECURITY LEVEL: VS-NfD |

#  @TC-4143 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I should not see any classified banner if I am in a call with a user when my backend is not classified
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>
#    And User <TeamOwnerChala> has 1:1 conversation with <TeamOwnerAnta> in team "<TeamNameChala>"
#    And User <TeamOwnerChala> is me
#    And <TeamOwnerAnta> starts instance using <CallBackend>
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<TeamOwnerAnta>" in conversation list
#    And I tap on conversation name "<TeamOwnerAnta>" in conversation list
#    When User <TeamOwnerAnta> calls me
#    Then I see incoming call from <TeamOwnerAnta>
#    Then I see federated guest icon for user "<TeamOwnerChala>" on outgoing call overlay
#    And I do not see classified domain label with text "<textClassified>" on outgoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on outgoing call overlay
#    When I accept the call
#    And User <TeamOwnerAnta> verifies to send and receive audio
#    Then I do not see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    Examples:
#      | TeamOwnerAnta  | TeamOwnerChala | TeamNameAnta | TeamNameChala | CallBackend | textClassified         | textNonClassified            |
#      | user1Name      | user2Name      | Avocado      | Banana        | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  ######################
#  # Group Calling
#  ######################
#
#  @TC-4144 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the classified banner if I am in a group call with classified users only when I am on a classified domain
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<Member1>,<Member2>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> start instances using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textClassified>" in the conversation view
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> in ongoing group call
#    And Users <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> verify to send and receive audio
#    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamNameAnta | TeamNameBella | GroupChat | CallBackend | textClassified         |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | Avocado      | Banana        | Sundae    | chrome      | SECURITY LEVEL: VS-NfD |
#
#  @TC-4145 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the Not Classified banner if I am in a group call with non classified users only when my backend is a classified domain
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member1>,<Member2> to team "<TeamNameChala>" with role Member
#    And User <TeamOwnerBella> is connected to <TeamOwnerChala>,<Member1>,<Member2>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerChala>,<Member1>,<Member2> in team "<TeamNameBella>"
#    And <TeamOwnerChala>,<Member1>,<Member2> start instances using <CallBackend>
#    And <TeamOwnerChala>,<Member1>,<Member2> accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textNonClassified>" in the conversation view
#    When I tap start call button
#    And <TeamOwnerChala>,<Member1>,<Member2> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerChala>,<Member1>,<Member2> in ongoing group call
#    And Users <TeamOwnerChala>,<Member1>,<Member2> verify to send and receive audio
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerBella | TeamOwnerChala  | Member1   | Member2   | TeamNameBella | TeamNameChala  | GroupChat | CallBackend | textNonClassified            |
#      | user1Name      | user2Name       | user3Name | user4Name | Banana        | Cactus         | Sundae    | chrome      | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4146 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the classified banner if I am in a group call with users from the same backend which is classified
#    Given There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member1>,<Member2> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerBella2>" with team "<TeamNameBella2>" on bella backend
#    And User <TeamOwnerBella2> adds users <Member3>,<Member4> to team "<TeamNameBella2>" with role Member
#    And User <TeamOwnerBella> is connected to <TeamOwnerBella2>,<Member3>,<Member4>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <Member1>,<Member2>,<TeamOwnerBella2>,<Member3>,<Member4> in team "<TeamNameBella>"
#    And <Member1>,<Member2>,<TeamOwnerBella2>,<Member3>,<Member4> starts instance using <CallBackend>
#    And <TeamOwnerBella2>,<Member1>,<Member2>,<Member3>,<Member4> accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textClassified>" in the conversation view
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerBella2>,<Member1>,<Member2>,<Member3>,<Member4> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerBella2>,<Member1>,<Member2>,<Member3>,<Member4> in ongoing group call
#    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerBella | Member1   | Member2   | TeamOwnerBella2 | Member3   | Member4   | TeamNameBella | TeamNameBella2 | GroupChat | CallBackend | textClassified         |
#      | user1Name      | user2Name | user3Name | user4Name       | user5Name | user6Name | Banana        | Blueberry      | Sundae    | chrome      | SECURITY LEVEL: VS-NfD |
#
#  @TC-4147 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the Not Classified banner if I am in a group call with non classified and classified users when my backend is a classified domain
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> adds users <Member3>,<Member4> to team "<TeamNameChala>" with role Member
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4> accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textNonClassified>" in the conversation view
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>,<Member3>,<Member4> in ongoing group call
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella  | TeamOwnerChala | Member3   | Member4   | TeamNameAnta | TeamNameBella | TeamNameChala  | GroupChat | CallBackend | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name       | user5Name      | user6Name | user7Name | Avocado      | Banana        | Cactus         | Sundae    | chrome      | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4148 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I should not see any classified indicator if I am in a group call with classified and non classified users when my backend is not classified
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4>
#    And User <TeamOwnerChala> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> in team "<TeamNameChala>"
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> accept next incoming call automatically
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerChala>,<TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> in ongoing group call
#    Then I do not see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    When User <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member3>,<Member4> switch video on
#    Then I do not see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat   | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | Avocado      | Banana        | Cherry        | Fruit Salad | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4149 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the classified banner degrade to a non classified banner when a non classified user joins a classified group and my backend is classified - when minimising the call
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member1>,<Member2> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<TeamOwnerChala>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<Member2> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>, accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textClassified>" in the conversation view
#    When I tap start call button
#    And <TeamOwnerAnta>,<Member1>,<Member2> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2> in ongoing group call
#    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I minimise the ongoing call
#    And <TeamOwnerChala> starts instance using <CallBackend>
#    And I tap on group conversation title "<GroupChat>" to open group details
#    And I see group details page
#    And I tap on Participants tab
#    When I tap on Add Participants button
#    Then I see user <TeamOwnerChala> in search suggestions list
#    When I select user <TeamOwnerChala> in search suggestions list
#    And I tap Continue button on add participants page
#    Then I see user <TeamOwnerChala> in participants list
#    When I close the group conversation details through X icon
#    Then I see classified domain label with text "<textNonClassified>" in the conversation view
#    When I restore the ongoing call
#    And User <TeamOwnerChala> calls <GroupChat>
#    And Users <TeamOwnerChala> verifies that call status to <GroupChat> is changed to active in 60 seconds
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala> in ongoing group call
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | Member1   | Member2   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name | user4Name |  user5Name     | Avocado      | Banana        | Cherry        | Sundae    | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4150 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the classified banner degrade to a non classified banner when a non classified user joins a classified group and my backend is classified - when not minimising the call
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member1>,<Member2> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<TeamOwnerChala>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> accept next incoming call automatically
#    And User <Member2> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textClassified>" in the conversation view
#    When I tap start call button
#    And <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> in ongoing group call
#    When User <TeamOwnerBella> adds user <TeamOwnerChala> to group conversation "<GroupChat>"
#    And <TeamOwnerChala> starts instance using <CallBackend>
#    And User <TeamOwnerChala> calls <GroupChat>
#    And Users <TeamOwnerChala> verifies that call status to <GroupChat> is changed to active in 60 seconds
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    And I see users <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<TeamOwnerChala> in ongoing group call
#
#    Examples:
#      | TeamOwnerAnta | TeamOwnerBella | Member1   | Member2   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name      | user3Name | user4Name | user5Name      | Avocado      | Banana        | Cherry        | Sundae    | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4151 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the non classified banner upgrade to a classified banner when a non classified user leaves a classified group and my backend is classified - when minimising the call
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> accept next incoming call automatically
#    And User <TeamOwnerBella> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textNonClassified>" in the conversation view
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> in ongoing group call
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    And I minimise the ongoing call
#    And I tap on group conversation title "<GroupChat>" to open group details
#    And I see group details page
#    And I tap on Participants tab
#    And I see user <TeamOwnerChala> in participants list
#    When I tap on user <TeamOwnerChala> in participants list
#    Then I see connected user <TeamOwnerChala> profile
#    And I see remove from group button
#    When I tap remove from group button
#    Then I see alert asking me if I want to remove user <TeamOwnerChala> from group
#    When I tap remove button on alert
#    Then I do not see remove from group button
#    When I close the user profile through the close button
#    And I close the group conversation details through X icon
#    When I restore the ongoing call
#    And I see users <TeamOwnerBella>,<TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4> in ongoing group call
#    Then I see classified domain label with text "<textClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | Avocado      | Banana        | Cherry        | Sundae    | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4152 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I want to see the non classified banner upgrade to a classified banner when a non classified user leaves a classified group and my backend is classified - when not minimising the call
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And User <TeamOwnerAnta> adds users <Member1>,<Member2> to team "<TeamNameAnta>" with role Member
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And User <TeamOwnerBella> adds users <Member3>,<Member4> to team "<TeamNameBella>" with role Member
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> is connected to <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerChala>
#    And User <TeamOwnerBella> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<Member2>,<Member3>,<Member4>,<TeamOwnerChala> in team "<TeamNameBella>"
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member4>,<TeamOwnerChala> starts instance using <CallBackend>
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member4>,<TeamOwnerChala> accept next incoming call automatically
#    And User <Member3> is me
#    And I see Welcome Page
#    And I open bella backend deep link
#    And I see alert informing me that I am about to switch to bella backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I see classified domain label with text "<textNonClassified>" in the conversation view
#    When I tap start call button
#    And I see start call alert
#    And I tap call button on start call alert
#    And <TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member4>,<TeamOwnerChala> verify that waiting instance status is changed to active in 90 seconds
#    And I see ongoing group call
#    And I see users <Member3>,<TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member4>,<TeamOwnerChala> in ongoing group call
#    Then I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    And I see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    When User <TeamOwnerBella> removes user <TeamOwnerChala> from group conversation "<GroupChat>"
#    And I wait for 5 seconds
#    And I see users <Member3>,<TeamOwnerAnta>,<Member1>,<Member2>,<TeamOwnerBella>,<Member4> in ongoing group call
#    And I see classified domain label with text "<textClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | Member2   | TeamOwnerBella | Member3   | Member4   | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user3Name | user4Name      | user5Name | user6Name | user7Name      | Avocado      | Banana        | Cherry        | Sundae    | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
#
#  @TC-4153 @regression @RC @federation @federationClassified @federationCalling
#  Scenario Outline: I should not see any classified banner if a non classified user leaves from a group and only classified users are left but my backend is not classified
#    Given There is a team owner "<TeamOwnerAnta>" with team "<TeamNameAnta>" on anta backend
#    And There is a team owner "<TeamOwnerBella>" with team "<TeamNameBella>" on bella backend
#    And There is a team owner "<TeamOwnerChala>" with team "<TeamNameChala>" on chala backend
#    And User <TeamOwnerBella> adds users <Member1> to team "<TeamNameBella>" with role Member
#    And User <TeamOwnerChala> is connected to <TeamOwnerAnta>,<Member1>,<TeamOwnerBella>
#    And User <TeamOwnerChala> has group conversation <GroupChat> with <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> in team "<TeamNameChala>"
#    And <TeamOwnerAnta>,<Member1>,<TeamOwnerBella> start instances using <CallBackend>
#    And <TeamOwnerAnta>,<TeamOwnerBella>,<Member1> accept next incoming call automatically
#    And User <TeamOwnerChala> is me
#    And I see Welcome Page
#    And I open chala backend deep link
#    And I see alert informing me that I am about to switch to chala backend
#    And I tap proceed button on custom backend alert
#    And I tap login button on Welcome Page
#    And I sign in using my email
#    And I tap login button on email Login Page
#    And I wait until I am fully logged in
#    And I see conversation "<GroupChat>" in conversation list
#    And I tap on conversation name "<GroupChat>" in conversation list
#    And I do not see classified domain label with text "<textClassified>" in the conversation view
#    And I do not see classified domain label with text "<textNonClassified>" in the conversation view
#    When I tap start call button
#    And <TeamOwnerAnta>,<TeamOwnerBella>,<Member1> verifies that waiting instance status is changed to active in 90 seconds
#    And I wait for 5 seconds
#    And I see ongoing group call
#    Then I do not see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
#    When <Member1> leaves group conversation <GroupChat>
#    And I wait for 5 seconds
#    Then I do not see classified domain label with text "<textClassified>" on ongoing call overlay
#    And I do not see classified domain label with text "<textNonClassified>" on ongoing call overlay
#
#    Examples:
#      | TeamOwnerAnta | Member1   | TeamOwnerBella | TeamOwnerChala | TeamNameAnta | TeamNameBella | TeamNameChala | GroupChat   | CallBackend | textClassified         | textNonClassified            |
#      | user1Name     | user2Name | user4Name      | user7Name      | Avocado      | Banana        | Cherry        | Fruit Salad | chrome      | SECURITY LEVEL: VS-NfD | SECURITY LEVEL: UNCLASSIFIED |
