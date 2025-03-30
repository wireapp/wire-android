Feature: File Sharing Restrictions

  @TC-8120 @regression @RC @fileSharingRestrictions
  Scenario Outline: I want to see alert first time after File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User Myself has 1:1 conversation with <TeamOwner> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When User <TeamOwner> disables File Sharing for team <TeamName>
    Then I see alert informing me that my Team settings have changed
    And I see subtext "Sharing and receiving files of any type is now disabled" in the Team settings change alert

    Examples:
      | Member1   | TeamOwner | TeamName     |
      | user1Name | user2Name | File sharing |

  @TC-8121 @TC-8147 @TC-8148 @regression @RC @fileSharingRestrictions
  Scenario Outline: I should not to be able to receive images when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And There is a personal user <Contact>
    And User <Contact> is connected to <TeamOwner>,<Member1>
    And User <Member1> is me
    And User <Contact> adds a new device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Contact> in team "<TeamName>"
    And User <TeamOwner> disables File Sharing for team <TeamName>
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
    When User <Contact> sends image "testing.jpg" to conversation <GroupConversation>
    Then I see receiving of images is prohibited in conversation view
    #  TC-8147 - I should not to be able to receive Video message when File sharing is disabled for team
    When User <Contact> sends local video named "<FileNameVideo>" via device Device1 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    Then I see receiving of video is prohibited in conversation view
    #  TC-8148 - I should not be able to receive  Audio message when File sharing is disabled for team
    When User <Contact> sends local audio file named "<FileNameAudio>" via device Device1 to group conversation "<GroupConversation>"
    And I scroll to the bottom of conversation view
    Then I see receiving of audio messages is prohibited in conversation view

    Examples:
      | TeamOwner | Member1   | Contact   | TeamName    | GroupConversation | FileNameVideo | FileNameAudio |
      | user1Name | user2Name | user3Name | FileSharing | SendFilesHere     | testing.mp4   | test.m4a      |

  @TC-8149 @TC-8047 @regression @RC @fileSharingRestrictions
  Scenario Outline: I want to see placeholder for received File when File sharing is disabled for team
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And There is a personal user <Contact>
    And User <Contact> is connected to <TeamOwner>,<Member1>
    And User <Member1> is me
    And User <Contact> adds a new device Device1 with label Device1
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Contact> in team "<TeamName>"
    And User <TeamOwner> disables File Sharing for team <TeamName>
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
    When User <Contact> sends 1.00MB file having name "<FileName>" and MIME type "text/plain" via device Device1 to group conversation "<GroupConversation>"
    Then I see a file with name "qa_random" in the conversation view
    And I see receiving of files is prohibited for file "qa_random" in conversation view
    When I tap on the file with name "qa_random" in the conversation view
    Then I do not see download alert for files
    # TC-8047 - I should not see image share button when File sharing is disabled for team
    When I tap file sharing button
    Then I do not see sharing option for File is displayed
    And I do not see sharing option for Gallery is displayed
    And I do not see sharing option for Camera is displayed
    And I do not see sharing option for Video is displayed
    And I do not see sharing option for Audio is displayed


    Examples:
      | TeamOwner | Member1   | Contact   | TeamName    | GroupConversation | FileName      |
      | user1Name | user2Name | user3Name | FileSharing | SendFilesHere     | qa_random.txt |