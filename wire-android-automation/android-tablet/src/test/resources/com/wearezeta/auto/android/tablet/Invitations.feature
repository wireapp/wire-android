Feature: Invitations

  @C1061983 @regression
  Scenario Outline: Verify impossibility of sending invite to the person with a wrong email or phone
    Given I delete all contacts from Address Book
    Given There is 1 user where <Name> is me
    Given I add <Contact> into Address Book
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with no conversations
    When I open Search UI
    Then I do not see user "<Contact>" in Search result list

    Examples:
      | Name      | Contact   |
      | user1Name | user2Name |