@col1
Feature: E2EI Revocation

  @TC-8045
  Scenario Outline: I want to see another users certificate as revoked
    Given There is a team owner "<Owner>" with team "MLS"
    And User <Owner> adds users <Member1>,<Member2> to team "MLS" with role Member
    And User <Owner> adds users <Member1>,<Member2> to keycloak for E2EI
    And User <Owner> configures MLS for team "MLS"
    And Admin user <Owner> enables E2EI with ACME server for team "MLS"
    And User <Member1> is me
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member1Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member1Email> on keycloak login page
    And I enter password <Member1Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I tap Certificate Details button on E2EI certificate updated alert
    And I remember certificate from certificate details
    And I tap back button on certificate details
    And I tap OK button on E2EI certificate updated alert
    And I wait until I am fully logged in
    When Admin of column-1 backend revokes remembered certificate on ACME server
    And I tap User Profile Button
    And I see User Profile Page
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I tap log out button on clear data alert
    And I see Welcome Page
    And User <Member2> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I start 2FA verification email monitoring for <Member2Email>
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I see get certificate alert
    And I clear cache of system browser
    And I tap get certificate alert
    And I tap use without an account button if visible
    And I enter email <Member2Email> on keycloak login page
    And I enter password <Member2Password> on keycloak login page
    And I hide the keyboard
    And I click sign in on keycloak login page
    And I tap OK button on E2EI certificate updated alert
    And I wait until I am fully logged in
    And I tap on start a new conversation button
    And I type user name "<Member1>" in search field
    And I see user name "<Member1>" in Search result list
    And I tap on user name "<Member1>" in Search result list
    And I see connected user <Member1> profile
    And I tap start conversation button on connected user profile page
    And I see conversation view with "<Member1>" is in foreground
    And I open conversation details for 1:1 conversation with "<Member1>"
    And I tap on devices tab in connected user profile
    And I scroll to the bottom of user profile page
    And I tap device "Phone" listed under devices in connected user profile
    Then I see MLS device status is revoked on device details

    Examples:
      | Owner     | Member1   | Member1Email | Member1Password | Member2   | Member2Email | Member2Password |
      | user1Name | user2Name | user2Email   | user2Password   | user3Name | user3Email   | user3Password   |