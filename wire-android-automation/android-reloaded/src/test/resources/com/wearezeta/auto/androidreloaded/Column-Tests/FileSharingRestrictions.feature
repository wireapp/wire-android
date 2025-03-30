Feature: File Sharing Restrictions

  @TC-8048 @TC-8051 @TC-8050 @col1
  Scenario Outline: I want to be able to receive images when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When User <TeamOwner> sends image "testing.jpg" to conversation <GroupConversation>
    Then I see an image in the conversation view
    And I do not see receiving of files is prohibited for file "testing.jpg" in conversation view
    #  TC-8051 - I want to be able to receive Video message when File sharing is disabled for team
    When User <TeamOwner> sends local video named "<FileNameVideo>" via device Device1 to group conversation "<GroupConversation>"
    Then I see a file with name "<FileNameVideo>" in the conversation view
    And I do not see receiving of files is prohibited for file "testing.mp4" in conversation view
    #  TC-8050 - I want to be able to receive  Audio message when File sharing is disabled for team
    When User <TeamOwner> sends local audio file named "<FileNameAudio>" via device Device1 to group conversation "<GroupConversation>"
    Then I see an audio file in the conversation view
    And I do not see receiving of files is prohibited for file "test.m4a" in conversation view

    Examples:
      | TeamOwner | Member1   | Email      |  TeamName    | GroupConversation | FileNameVideo | FileNameAudio |
      | user1Name | user2Name | user2Email | FileSharing  | SendFilesHere     | testing.mp4   | test.m4a      |

  @TC-8049 @col1
  Scenario Outline: I want to see placeholder for received File when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When User <TeamOwner> sends 1.00MB file having name "<FileName><FileExtension>" and MIME type "application/x-zip" via device Device1 to group conversation "<GroupConversation>"
    Then I see a file with name "<FileName>" in the conversation view
    And I see receiving of files is prohibited for file "<FileName>" in conversation view
    When I tap on the file with name "<FileName>" in the conversation view
    Then I do not see download alert for files

    Examples:
      | TeamOwner | Member1   | Email      |  TeamName    | GroupConversation | FileName  | FileExtension |
      | user1Name | user2Name | user2Email | FileSharing  | SendFilesHere     | qa_random | .zip          |

  @TC-8053 @TC-8054 @col1
  Scenario Outline: I want to be able to share an image when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
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
   # TC-8054 - I want to be able to share an audio message when File sharing is disabled for team
    When I tap file sharing button
    And I tap on Attach Audio option
    And I tap on start recording audio button
    And I wait for 5 seconds
    And I tap on stop recording audio button
    Then I see that my audio message was recorded
    When I send my recorded audio message
    And I tap file sharing button
    Then I see an audio file in the conversation view

    Examples:
      | TeamOwner | TeamName    | Member1   | Email      | GroupConversation |
      | user1Name | FileSharing | user2Name | user2Email | SendFilesHere     |

  @TC-8052 @col1
  Scenario Outline: I should not be able to share a file when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>" on column-1 backend
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> adds a new 2FA device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And I see Welcome Page
    And I open column-1 backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I now start 2FA verification email monitoring for <Email>
    And I sign in using my email
    And I tap login button on email Login Page
    And I type 2FA verification code from email
    And I wait until I am fully logged in
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I see group conversation "<GroupConversation>" is in foreground
    And I tap file sharing button
    When I push 1KB sized file with name "textfile.zip" to file storage
    And I tap on Attach File option
    And I select file with name containing "textfile.zip" in DocumentsUI
    And I see file "textfile.zip" on preview page
    And I tap send button on preview page
    Then I see "Sending of files is forbidden due to company restrictions" toast message in conversation view
    And I do not see a file with name "textfile.zip" in the conversation view

    Examples:
      | TeamOwner | TeamName    | Member1   | Email      | GroupConversation |
      | user1Name | FileSharing | user2Name | user2Email | SendFilesHere     |