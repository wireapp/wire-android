Feature: File Sharing

  @TC-8603 @CriticalFlows
  Scenario Outline: I want to verify that I can receive, play, and download various file types from someone in a different team
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerA> adds users <Member1> to team "<TeamNameA>" with role Member
    And User <TeamOwnerB> adds users <Member2> to team "<TeamNameB>" with role Member
    And User <Member2> adds a new device Device1 with label Device1
    And User <Member2> sends connection request to <Member1>
    And User <Member1> sets their unique username
    And Personal user <Member1> sets profile image
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see unread conversation "<Member2>" in conversation list
    When I tap on unread conversation name "<Member2>" in conversation list
    And I see accept button on unconnected user profile page
    And I see ignore button on unconnected user profile page
    Then I see text "<ConnectionText>" on unconnected user profile page
    When I tap accept button on unconnected user profile page
    And I tap start conversation button on connected user profile page
    Then I see conversation view with "<Member2>" is in foreground
    When User <Member2> sends local audio file named "<FileName>" via device Device1 to user "<Member1>"
    Then I see an audio file in the conversation view
    And I see the time played in the audio file is 00:00
    When I tap play button on the audio file
    And I wait for 10 seconds
    And I tap pause button on the audio file
    Then I see the time played in the audio file is not 00:00
    When I long press on audio slider button
    Then I see a audio file download bottom sheet is visible
    And I see Message Details button on download alert
    And I see Reply button on download alert
    And I see Download button on download alert
    And I see Share button on download alert
    And I see Open button on download alert
    And I see Delete button on download alert
    When I tap Download button on download alert
    Then I see download alert for files
    When I tap Save button on download alert
    Then I see "The file <FileName> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName>" is downloaded to the device
    And I remove the file "<FileName>" from device's sdcard
    When User <Member2> sends local image named "<FileName2>" via device Device1 to user "<Member1>"
    Then I see a file with name "<FileName2>" in the conversation view
    When I tap on the file with name "<FileName2>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    Then I see "The file <FileName2> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName2>" is downloaded to the device
    And I remove the file "<FileName2>" from device's sdcard
    When User <Member2> sends 1.00MB file having name "<FileName3>" and MIME type "text/plain" via device Device1 to group conversation "<Member1>"
    Then I see a file with name "<FileName3>" in the conversation view
    When I tap on the file with name "<FileName3>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    Then I see "The file <FileName3> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName3>" is downloaded to the device
    And I remove the file "<FileName3>" from device's sdcard
    When User <Member2> sends local video named "<FileName4>" via device Device1 to user "<Member1>"
    Then I see a file with name "<FileName4>" in the conversation view
    When I tap on the file with name "<FileName4>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    Then I see "The file <FileName4> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName4>" is downloaded to the device
    And I remove the file "<FileName4>" from device's sdcard
    When I tap on the file with name "<FileName4>" in the conversation view
    And I tap Open button on download alert
    Then I see the Wire app is not in foreground

    Examples:
      | TeamOwnerA | TeamOwnerB | Member1   | Member2   | TeamNameA  | TeamNameB     | ConnectionText                       | FileName | FileName2   |  FileName3     | FileName4   |
      | user1Name  | user2Name  | user3Name | user4Name | TeamToSend | TeamToReceive | This user wants to connect with you. | test.m4a | testing.jpg |  qa_random.txt | testing.mp4 |
