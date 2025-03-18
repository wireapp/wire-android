Feature: Search

  @C490 @regression
  Scenario Outline: I ignore someone from search and clear my inbox (portrait)
    Given There are 2 users where <Name> is me
    Given User <Contact> sets the unique username
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversation
    Given User <Contact> sent connection request to me
    Given I wait for 5 seconds
    Given I see conversation "<WaitingMess>" in Recent View
    Given I wait until <Contact> exists in backend search results
    When I open Search UI
    And I type user name "<Contact>" in search field
    And I tap on user name found on Search page <Contact>
    And I see user name "<Contact>" on Single pending incoming connection page
    And I tap ignore button on Single pending incoming connection page
    Then I see Recent View with no conversations

    Examples:
      | Name      | Contact   | WaitingMess      |
      | user1Name | user2Name | 1 person waiting |

  @C762 @regression
  Scenario Outline: I want to discard the new connect request (sending) by returning to the search results after selecting someone I’m not connected to
    Given There are 2 users where <Name> is me
    Given User <Contact> sets the unique username
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversations
    Given I wait until <Contact> exists in backend search results
    When I open Search UI
    And I type user name "<Contact>" in search field
    And I tap on user name found on Search page <Contact>
    And I see user name "<Contact>" on Single unconnected user details page
    And I tap Back button 1 times
    Then I see Recent View with no conversations

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |