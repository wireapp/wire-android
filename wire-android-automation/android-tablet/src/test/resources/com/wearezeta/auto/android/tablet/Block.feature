Feature: Block

  @C496 @regression
  Scenario Outline: I can find blocked user in Search and unblock
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>,<Contact2>
    Given User <Name> blocks user <Contact1>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    When I tap on conversation name "<Contact2>"
    And I navigate back from conversation
    And I wait until <Contact1> exists in backend search results
    And I open Search UI
    And I type user name "<Contact1>" in search field
    And I see user <Contact1> in Search result list
    And I tap on user name found on Search page <Contact1>
    Then I see unblock button on Single blocked user details page
    When I tap on unblock button on Single blocked user details page
    And I see conversation "<Contact1>" in Recent View
    And I tap on conversation name "<Contact1>"
    Then I see conversation view


    Examples:
      | Name      | Contact1  | Contact2  |
      | user1Name | user2Name | user3Name |

  @C764 @regression
  Scenario Outline: (AN-5797) I want to block a person from 1:1 conversation
    Given There are 3 users where <Name> is me
    # Having the extra user is a workaround for an app bug
    Given User Myself is connected to <Contact1>,<Contact2>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    When I tap on conversation name "<Contact1>"
    And I tap conversation name from top toolbar
    And I tap More Actions button on Single connected user details page
    And I tap Block… button on Single conversation options menu
    And I tap BLOCK button on Confirm overlay page
    Then I do not see conversation "<Contact1>" in Recent View
    And I wait until <Contact1> exists in backend search results
    When I open Search UI
    And I type user name "<Contact1>" in search field
    And I see user <Contact1> in Search result list
    And I tap on user name found on Search page <Contact1>
    Then I see unblock button on Single blocked user details page

    Examples:
      | Name      | Contact1  | Contact2  |
      | user1Name | user2Name | user3Name |