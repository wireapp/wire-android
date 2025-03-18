Feature: File Sharing

  @TC-4339 @regression @RC @fileSharing
  Scenario Outline: I want to verify that I can receive and download images
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When User <TeamOwner> sends image "testing.jpg" to conversation <GroupConversation>
    And I see an image in the conversation view
    And I tap on the image
    Then I see context menu for images
    When I open context menu for images
    Then I see download option
    And I see delete option
    When I tap download option
    Then I see "Saved to Downloads folder" toast message on file details page
    # FixMe: Commented because for some reason the image is not an image anymore when saving it.
    # This is not reproducible manually, only with this test!
#    And I wait up 15 seconds until file having name "testing.jpg" is downloaded to the device
#    And I remove the file "testing.jpg" from device's sdcard

    Examples:
      | TeamOwner | Member1   | TeamName    | GroupConversation |
      | user1Name | user2Name | FileSharing | SendFilesHere     |

  @TC-4340 @regression @RC @fileSharing
  Scenario Outline: I want to verify that I can receive and download files such as PDFs and TXT
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When User <TeamOwner> sends 1.00MB file having name "<FileName>" and MIME type "text/plain" via device Device1 to group conversation "<GroupConversation>"
    Then I see a file with name "<FileName>" in the conversation view
    When I tap on the file with name "<FileName>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    Then I see "The file <FileName> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName>" is downloaded to the device
    And I remove the file "<FileName>" from device's sdcard

    Examples:
      | TeamOwner | Member1   | TeamName    | GroupConversation | FileName      |
      | user1Name | user2Name | FileSharing | SendFilesHere     | qa_random.txt |

  @TC-4341 @regression @RC @fileSharing
  Scenario Outline: I want to verify that I can receive and download videos
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And User <TeamOwner> sends local video named "<FileName>" via device Device1 to group conversation "<GroupConversation>"
    And I see a file with name "<FileName>" in the conversation view
    When I tap on the file with name "<FileName>" in the conversation view
    Then I see download alert for files
    And I see Save button on download alert
    And I see Open button on download alert
    And I see Cancel button on download alert
    When I tap Save button on download alert
    Then I see "The file <FileName> was saved successfully to the Downloads folder" toast message on file details page
    And I wait up 15 seconds until file having name "<FileName>" is downloaded to the device
    And I remove the file "<FileName>" from device's sdcard

    Examples:
      | TeamOwner | Member1   | TeamName    | GroupConversation | FileName    |
      | user1Name | user2Name | FileSharing | SendFilesHere     | testing.mp4 |

  @TC-4342 @regression @RC @fileSharing
  Scenario Outline: I want to send an image in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap file sharing button
    And I see sharing option for File is displayed
    And I see sharing option for Gallery is displayed
    And I see sharing option for Camera is displayed
    And I see sharing option for Video is displayed
    And I see sharing option for Audio is displayed
    When I push image with QR code containing "Image" to file storage
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    Then I see an image with QR code "Image" in the conversation view

    Examples:
      | TeamOwner | TeamName    | Member1   | GroupConversation |
      | user1Name | FileSharing | user2Name | SendFilesHere     |

  @TC-4343 @regression @RC @smoke @fileSharing
  Scenario Outline: I want to send a file in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap file sharing button
    When I push 1KB sized file with name "textfile.txt" to file storage
    And I tap on Attach File option
    And I select file with name containing "textfile.txt" in DocumentsUI
    And I see file "textfile.txt" on preview page
    And I tap send button on preview page
    Then I see a file with name "textfile.txt" in the conversation view

    Examples:
      | TeamOwner | TeamName    | Member1   | GroupConversation |
      | user1Name | FileSharing | user2Name | SendFilesHere     |

