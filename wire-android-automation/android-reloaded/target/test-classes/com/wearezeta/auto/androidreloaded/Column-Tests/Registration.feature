Feature: Registration

  @TC-4852 @TC-4851 @registration @col1 @col3
  Scenario: I should not be able to register a team account
    When I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    Then I do not see Create a Team button on Welcome Page
    # TC-4851 - I should not be able to create a personal account
    And I do not see Create a Personal Account link on Welcome Page