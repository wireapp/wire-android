@mobtown
Feature: Assets

  @TC-4667
  Scenario Outline: I want to send and receive images on both ingress instances
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds a new device Device1 with label Device1
    And I see Welcome Page
    And I open <Backend> backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When User <Member1> sends image with QR code containing "Example1" to conversation <TeamOwner>
    Then I see an image with QR code "Example1" in the conversation view
    When I push image with QR code containing "Image" to file storage
    And I tap file sharing button
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    And I tap back button
    Then I see an image with QR code "Image" in the conversation view

    Examples:
      | Backend       | TeamOwner | TeamName  | Member1   |
      | mobtown-ernie | user1Name | Messaging | user2Name |
      | mobtown-test  | user1Name | Messaging | user2Name |

  @TC-4668
  Scenario Outline: I want to send and receive files on both ingress instances
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And User <Member1> adds a new device Device1 with label Device1
    And I see Welcome Page
    And I open <Backend> backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I see conversation "<Member1>" in conversation list
    And I tap on conversation name "<Member1>" in conversation list
    When User <Member1> sends 1KB file having name "example1.txt" and MIME type "text/plain" via device Device1 to group conversation "<TeamOwner>"
    Then I see a file with name "example1.txt" in the conversation view

    Examples:
      | Backend       | TeamOwner | TeamName  | Member1   |
      | mobtown-ernie | user1Name | Messaging | user2Name |
      | mobtown-test  | user1Name | Messaging | user2Name |