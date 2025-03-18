Feature: Connect

  @C746 @regression
  Scenario Outline: I can send/cancel sending connection request from Search
    Given There are 2 users where <Name> is me
    Given User <Contact> sets the unique username
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I wait until <Contact> exists in backend search results
    When I open Search UI
    And I type user name "<Contact>" in search field
    And I tap on user name found on Search page <Contact>
    Then I see user name "<Contact>" on Single unconnected user details page
    And I tap back button
    And I open Search UI
    And I type user name "<Contact>" in search field
    And I tap on user name found on Search page <Contact>
    Then I see user name "<Contact>" on Single unconnected user details page
    When I tap +connect button on Single pending incoming connection page
    Then I see cancel connection request button on Single pending outgoing connection page

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |

  @C740 @regression @smoke
  Scenario Outline: Accept connection request
    Given There are 2 users where <Name> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversations
    Given User <Contact> sent connection request to me
    Given I wait for 5 seconds
    Given I see conversation "<WaitingMess>" in Recent View
    When I tap on conversation name "<WaitingMess>"
    And I see user name "<Contact>" on Single pending incoming connection page
    And I tap connect button on Single pending incoming connection page
    Then I see conversation "<Contact>" in Recent View
    And I do not see conversation "<WaitingMess>" in Recent View

    Examples:
      | Name      | Contact   | WaitingMess      |
      | user1Name | user2Name | 1 person waiting |

  @C491 @regression
  Scenario Outline: I want to send connection request by selecting unconnected user from a group conversation
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User <Contact1> is connected to Myself,<Contact2>
    Given User <Contact1> has group conversation <GroupChatName> with Myself,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I do not see conversation "<Contact2>" in Recent View
    Given I tap on conversation name "<GroupChatName>"
    When I tap conversation name from top toolbar
    And I tap participant avatar of <Contact2> on Group info page
    And I tap +Connect button on Group unconnected user details page
    And I navigate back from conversation
    Then I see conversation "<Contact2>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | GroupChatName        |
      | user1Name | user2Name | user3Name | NonConnectedUserChat |

  @C489 @regression
  Scenario Outline: I can accept/ignore connection requests from search and inbox updated correctly
    Given There are 3 users where <Name> is me
    Given User <Contact1> sets the unique username
    Given User <Contact2> sets the unique username
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given User <Contact1> sent connection request to me
    Given User <Contact2> sent connection request to me
    When I wait for 5 seconds
    Then I see conversation "<WaitingMess2>" in Recent View
    And I see pending glyph of conversation "<WaitingMess2>"
    And I wait until <Contact1> exists in backend search results
    When I open Search UI
    And I type user name "<Contact1>" in search field
    And I tap on user name found on Search page <Contact1>
    And I tap ignore button on Single pending incoming connection page
    And I wait for 1 second
    Then I see conversation "<WaitingMess1>" in Recent View
    And I tap on conversation name "<WaitingMess1>"
    When I tap connect button on Single pending incoming connection page
    And I wait for 1 second
    Then I do not see conversation "<Contact1>" in Recent View
    And I see conversation "<Contact2>" in Recent View
    And I do not see conversation "<WaitingMess1>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | WaitingMess1     | WaitingMess2     |
      | user1Name | user2Name | user3Name | 1 person waiting | 2 people waiting |

  @C488 @regression
  Scenario Outline: I can accept/ignore connection requests from conversation list and inbox updated correctly
    Given There are 3 users where <Name> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Waiting for sync to be done in the background
    Given I wait for 5 seconds
    Given User <Contact1> sent connection request to me
    Then I see conversation "<WaitingMess1>" in Recent View
    And User <Contact2> sent connection request to me
    When I wait for 2 seconds
    Then I see conversation "<WaitingMess2>" in Recent View
    And I see pending glyph of conversation "<WaitingMess2>"
    When I tap on conversation name "<WaitingMess2>"
    And I wait for 1 second
    And I tap ignore button on Single pending incoming connection page
    Then I see conversation "<WaitingMess1>" in Recent View
    When I tap on conversation name "<WaitingMess1>"
    And I wait for 1 second
    And I tap connect button on Single pending incoming connection page
    And I wait for 1 second
    Then I see conversation "<Contact2>" in Recent View
    Then I do not see conversation "<WaitingMess1>" in Recent View

    Examples:
      | Name      | Contact1  | Contact2  | WaitingMess1     | WaitingMess2     |
      | user1Name | user2Name | user3Name | 1 person waiting | 2 people waiting |

  @C499 @regression
  Scenario Outline: I can see a new inbox for connection when receive new connection request
    Given There are 2 users where <Name> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Waiting for sync to be done in the background
    Given I wait for 5 seconds
    Given I see Recent View with no conversations
    When User <Contact> sent connection request to me
    # Workaround for a bug
    When I tap on conversation name "<WaitingMsg>"
    Then I see user name "<Contact>" on Single pending incoming connection page

    Examples:
      | Name      | Contact   | WaitingMsg       |
      | user1Name | user2Name | 1 person waiting |

  @C492 @regression
    # based on AN-5070 - Pending connection view is not updated if the other side accepts my connection request
  Scenario Outline:  I want to see that the other person has accepted the connect request in the conversation view
    Given There are 2 users where <Name> is me
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I see Recent View with no conversations
    And I wait for 3 seconds
    When I open Search UI
    And I type user name "<Contact>" in search field
    And I see user <Contact> in Search result list
    And I tap on user name found on Search page <Contact>
    Then I see user name "<Contact>" on Single unconnected user details page
    When I tap +connect button on Single pending incoming connection page
    And User <Contact> accepts all requests
    Then I see conversation view

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |

  @C493 @regression
  Scenario Outline: I would not know other person has ignored my connection request
    Given There are 2 users where <Name> is me
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I see Recent View with no conversations
    And I wait for 3 seconds
    When I open Search UI
    And I type user name "<Contact>" in search field
    And I see user <Contact> in Search result list
    And I tap on user name found on Search page <Contact>
    Then I see user name "<Contact>" on Single unconnected user details page
    When I tap +connect button on Single pending incoming connection page
    And I tap back button
    And I wait up to 20 seconds until conversation "<Contact>" appears in Recent View
    And User <Contact> ignores all requests
    And I tap on conversation name "<Contact>"
    Then I see user name "<Contact>" on Single unconnected user details page

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |

  @C761 @regression
  Scenario Outline: I can receive new connection request when app in background
    Given There are 2 users where <Name> is me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversations
    When I minimize Wire
    And User <Contact> sent connection request to me
    And I restore Wire
    When I tap on conversation name "<WaitingMsg>"
    Then I see user name "<Contact>" on Single pending incoming connection page

    Examples:
      | Name      | Contact   | WaitingMsg       |
      | user1Name | user2Name | 1 person waiting |