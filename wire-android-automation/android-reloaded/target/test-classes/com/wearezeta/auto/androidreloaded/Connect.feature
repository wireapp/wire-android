Feature: Connect

  @TC-4295 @TC-4298 @regression @RC @connect
  Scenario Outline: I want to be able to send a connection request to another personal user
    Given There are personal users <User1>, <User2>
    And User <User2> sets their unique username
    And Personal user <User2> sets profile image
    And User <User1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<Uniqueusername>" in search field
    And I see user name "<User2>" in Search result list
    And I tap on user name <User2> found on search page
    Then I see username "<User2>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    And I tap close button on unconnected user profile page
    And I tap the back arrow inside the search people field
    And I close the search page through X icon
    Then I see conversation "<User2>" is having pending status in conversation list
    # TC-4298 - I want to verify that I am not able to send a message before my contact accepted my connection request
    When I tap on conversation name "<User2>" in conversation list
    Then I see unconnected user <User2> profile
    And I see text "<ConnectionText>" on unconnected user profile page
    And I tap back button
    And User <User2> accepts all requests
    Then I see conversation "<User2>" in conversation list
    And I tap on conversation name "<User2>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see the message "<Message>" in current conversation

    Examples:
      | User1     | User2     | Uniqueusername      | Message | ConnectionText                                                                            |
      | user1Name | user2Name | user2UniqueUsername | Hello!  | When your connection request is accepted, you can communicate directly with this contact. |

  @TC-4296 @regression @RC @connect
  Scenario Outline: I want to be able to cancel a connection request
    Given There are personal users <User1>, <User2>
    And User <User2> sets their unique username
    And Personal user <User2> sets profile image
    And User <User1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on start a new conversation button
    When I tap on search people field
    And I type unique user name "<Uniqueusername>" in search field
    And I see user name "<User2>" in Search result list
    And I tap on user name <User2> found on search page
    Then I see username "<User2>" on unconnected user profile page
    When I tap connect button on unconnected user profile page
    And I wait until cancel connection request button on unconnected user profile page is visible
    And I tap cancel connection request button on unconnected user profile page
    And I tap close button on unconnected user profile page
    And I tap the back arrow inside the search people field
    And I close the search page through X icon
    And I see conversation list
    Then I do not see conversation "<User2>" in conversation list

    Examples:
      | User1     | User2     | Uniqueusername      |
      | user1Name | user2Name | user2UniqueUsername |

  @TC-4297 @regression @RC @connect @smoke
  Scenario Outline: I want to be able to receive and accept a connection request
    Given There are personal users <User1>, <User2>
    And User <User2> sets their unique username
    And Personal user <User2> sets profile image
    And User <User1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <User2> sends connection request to me
    Then I see unread conversation "<User2>" in conversation list
    And I see subtitle "<Subtitle>" of conversation "<User2>" in conversation list
    And I tap on unread conversation name "<User2>" in conversation list
    And I see username "<User2>" on unconnected user profile page
    And I see accept button on unconnected user profile page
    And I see ignore button on unconnected user profile page
    And I see text "<ConnectionText>" on unconnected user profile page
    When I tap accept button on unconnected user profile page
    And I tap start conversation button on connected user profile page
    Then I see conversation view with "<User2>" is in foreground

    Examples:
      | User1     | User2     | Subtitle         | ConnectionText                       |
      | user1Name | user2Name | Wants to connect | This user wants to connect with you. |