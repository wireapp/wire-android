Feature: Proxy

  @TC-8444 @col1
  Scenario Outline: I can login via an authenticated socks proxy, open a new conversation and send messages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on socks-access-backend-wire backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And I see Welcome Page
    And I open socks-access-backend-wire backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I enter credentials for SOCKS proxy
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I tap create new group button
    And I tap on search people field
    And I type user name "<TeamOwner>" in search field
    And I see user name "<TeamOwner>" in Search result list
    And I select user <TeamOwner> in search suggestions list
    And I tap Continue button on add participants page
    And I see create new group details page
    And I type new group name "<ConversationName>"
    And I tap continue button on create new group details page
    And I see create new group settings page
    And I tap continue button on create new group settings page
    And I see group conversation "<ConversationName>" is in foreground
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation


    Examples:
      | TeamOwner | Member1   | Member1Email | TeamName | ConversationName  | Message |
      | user1Name | user2Name | user2Email   | MyTeam   | ProxyConversation | Hello   |