Feature: LargeTeam

  @TC-4398 @TC-8630 @regression @RC @largeTeams
    #ToDo: Increase kalium testservice timeout so that <Member1> can login and we can receive a message from them. Add commented steps after timeout is increased.
  Scenario Outline: I want to be able to create a group conversation with members in a large team
    Given There is a known user <TeamOwner> with email <Email> and password <Password>
    Given There is a known user <Member1> with email <EmailMember> and password <Password>
    And User <TeamOwner> is me
    And User <TeamOwner> removes all their registered OTR clients
    #And User <Member1> adds a new device Device1 with label Device1
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my known email and password
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on start a new conversation button
    And I tap create new group button
    And I type user name "<Member1>" in search field
    And I see unique user name "<Member1UniqueUserName>" in Search result list
    And I select unique user name "<Member1UniqueUserName>" in Search result list
    And I clear the input field on Search page
    And I type user name "<Member2>" in search field
    And I see unique user name "<Member2UniqueUserName>" in Search result list
    And I select unique user name "<Member2UniqueUserName>" in Search result list
    And I clear the input field on Search page
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<NewConversationName>"
    And I hide the keyboard
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    Then I see group conversation "<NewConversationName>" is in foreground
    And I tap on group conversation title "<NewConversationName>" to open group details
    And I tap show more options button
    When I tap delete group button
    And I tap remove group button
    Then I see conversation list
    And I do not see conversation "<NewConversationName>" in conversation list
    # TC-8630 - I want to be able to exchange messages in a large group conversation
    When I tap on conversation name "<ExistingConversation>" in conversation list
    And I see group conversation "<ExistingConversation>" is in foreground
    When I type the message "<Message1>" into text input field
    And I tap send button
    Then I see the message "<Message1>" in current conversation
#    And User <Member1> sends message "<Message2>" to group conversation <ExistingConversation>
#    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner     | Email                         | EmailMember                     | Password   | Member1        | Member1UniqueUserName | Member2          | Member2UniqueUserName | NewConversationName | ExistingConversation | Message1 | Message2 |
      | Emery Kuvalis | smoketester+034e1dd8@wire.com | smoketester+881d169258@wire.com | Aqa123456! | Solomon Conroy | @881d169258           | Florencio Larkin | @9d966fc182           | MyTeam              | Full House           | Hello    | Hi       |
