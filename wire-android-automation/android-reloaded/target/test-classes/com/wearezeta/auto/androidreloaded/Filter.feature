Feature: Filter

  @TC-8642 @TC-8652 @TC-8645 @regression @filter
  Scenario Outline: I want to see existing favorites that I created before I logged out with clear data when I login again
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And There is a team owner "<TeamOwnerC>" with team "<TeamNameC>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> is connected to <TeamOwnerC>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    And I see conversation "<TeamOwnerC>" in conversation list
    When I long tap on conversation name "<GroupConversation>" in conversation list
    Then I see Filter Group on filter bottom sheet
    And I see Add to Favorites button on filter bottom sheet
    When I tap Add to Favorites button on filter bottom sheet
    Then I see "“Filter Group” was added to Favorites" toast message in conversation view
    When I tap filter conversation button
    Then I see Filter Conversations on filter bottom sheet
    And I see Favorites button on filter bottom sheet
    When I tap Favorites button on filter bottom sheet
    Then I get navigated to Favorites page and I see Favorites page heading
    And I see conversation "<GroupConversation>" in favorites list
    When I tap User Profile Button
    And I see User Profile Page
    # TC-8652 - I want to see existing favorites that I created before I logged out with clear data when I login again
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "<clearData>" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    Then I see Welcome Page
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    And I see conversation "<TeamOwnerC>" in conversation list
    When I tap filter conversation button
    Then I see Filter Conversations on filter bottom sheet
    And I see Favorites button on filter bottom sheet
    When I tap Favorites button on filter bottom sheet
    Then I get navigated to Favorites page and I see Favorites page heading
    And I see conversation "<GroupConversation>" in favorites list
    When I long tap on conversation name "<GroupConversation>" in favorites list
    Then I see Filter Group on filter bottom sheet
    And I see Remove from Favorites button on filter bottom sheet
    # TC-8645 - I want to remove a conversation from favorites
    When I tap Remove from Favorites button on filter bottom sheet
    Then I see "“Filter Group” was removed from Favorites" toast message in conversation view
    And I do not see conversation "<GroupConversation>" in favorites list

    Examples:
      | TeamOwner | TeamOwnerB | TeamOwnerC | Member1   | TeamName | TeamNameB       | TeamNameC        | GroupConversation | clearData                                                             |
      | user1Name | user5Name  | user2Name  | user3Name | Filters  | ConnectedFriend | ConnectedFriend2 | Filter Group      | Delete all your personal information and conversations on this device |

  @TC-8643 @TC-8644 @TC-8646 @TC-8666 @regression @filter
  Scenario Outline: I want to see existing folder that I created before I logged out with clear data when I login again
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    When I long tap on conversation name "<GroupConversation>" in conversation list
    And I see Folder Group on filter bottom sheet
    And I see Move to Folder... button on filter bottom sheet
    And I tap Move to Folder... button on filter bottom sheet
    Then I get navigated to move to folder page and I see Move to Folder... page heading
    And I see New Folder button
    And I see Done button
    When I tap New Folder button on move to folder page
    And I get navigated to new folder page I see New Folder as page heading
    And I enter <FolderName> as folder name
    And I tap Create Folder button on new folder page
    And I wait for 1 seconds
    Then I see toast message "<GroupConversation>" was moved to "<FolderName>" in conversation view
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see Folders button on filter bottom sheet
    And I tap Folders button on filter bottom sheet
    And I see folders bottom sheet with Folders as the header title
    And I see folder <FolderName> on folder bottom sheet
    And I tap existing folder <FolderName> on folder bottom sheet
    Then I get navigated to folder page and I see <FolderName> as the page heading
    And I see conversation "<GroupConversation>" in folder list
    When I tap User Profile Button
    And I see User Profile Page
    # TC-8666 - I want to see existing folders that I created before I logged out with clear data when I login again
    And I tap log out button on User Profile Page
    And I see alert informing me that I am about to clear my data when I log out
    And I see option to "<clearData>" when I will log out
    And I select checkbox to clear my data
    And I tap log out button on clear data alert
    Then I see Welcome Page
    When I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    # TC-8644 - I want to move a conversation to an existing folder
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    And I see conversation name "<TeamOwnerB>" on filter bottom sheet header is displayed
    And I see Move to Folder... button on filter bottom sheet
    And I tap Move to Folder... button on filter bottom sheet
    Then I get navigated to move to folder page and I see Move to Folder... page heading
    And I see <FolderName> on filter bottom sheet
    And I see New Folder button
    And I see Done button
    When I tap existing folder <FolderName> on folder bottom sheet
    And I tap Done button on move to folder page
    And I wait for 1 seconds
    Then I see "“<TeamOwnerB>” was moved to “Test Folder”" toast message in conversation view
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see Folders button on filter bottom sheet
    And I tap Folders button on filter bottom sheet
    Then I see folders bottom sheet with Folders as the header title
    And I see folder <FolderName> on folder bottom sheet
    When I tap existing folder <FolderName> on folder bottom sheet
    Then I get navigated to folder page and I see <FolderName> as the page heading
   # TC-8646 - I want to remove a conversation from a folder
    And I see conversation "<GroupConversation>" in folder list
    And I see conversation "<TeamOwnerB>" in folder list
    When I long tap on conversation name "<GroupConversation>" in folder list
    And I see Folder Group on filter bottom sheet
    And I see Remove from Folder “Test Folder” button on filter bottom sheet
    And I tap Remove from Folder “Test Folder” button on filter bottom sheet
    And I wait for 1 seconds
    Then I see toast message "<GroupConversation>" was removed from "<FolderName>" in conversation view
    And I do not see conversation "<GroupConversation>" in folder list

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | TeamName | TeamNameB       | GroupConversation | FolderName  | clearData                                                             |
      | user1Name | user2Name  | user3Name | Folder   | ConnectedFriend | Folder Group      | Test Folder | Delete all your personal information and conversations on this device |

  @TC-8651 @TC-8650 @regression @filter
  Scenario Outline: I want to move a 1on1 conversation to a folder from the user profile
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Member1>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    When I long tap on conversation name "<Member1>" in conversation list
    And I see conversation name "<Member1>" on filter bottom sheet header is displayed
    And I see Add to Favorites button on filter bottom sheet
    And I tap Add to Favorites button on filter bottom sheet
    Then I see "“<Member1>” was added to Favorites" toast message in conversation view
    And I tap on conversation name "<Member1>" in conversation list
    And I open conversation details for 1:1 conversation with "<Member1>"
    When I tap show more options button on user profile screen
    And I see conversation name "<Member1>" on filter bottom sheet header is displayed
    And I see Move to Folder... button on filter bottom sheet
    And I tap Move to Folder... button on filter bottom sheet
    Then I get navigated to move to folder page and I see Move to Folder... page heading
    And I see New Folder button
    And I see Done button
    When I tap New Folder button on move to folder page
    And I get navigated to new folder page I see New Folder as page heading
    And I enter <FolderName> as folder name
    And I tap Create Folder button on new folder page
    And I wait for 1 seconds
    Then I see toast message "<Member1>" was moved to "<FolderName>" in conversation view
    When I close the user profile through the close button
    And I close the conversation view through the back arrow
    And I see conversation "<Member1>" in conversation list
  # TC-8650 I want to see a conversation in favorites and folder if I marked it as favorite and moved it to a folder
    And I tap filter conversation button
    Then I see Filter Conversations on filter bottom sheet
    And I see Favorites button on filter bottom sheet
    When I tap Favorites button on filter bottom sheet
    Then I get navigated to Favorites page and I see Favorites page heading
    And I see conversation "<Member1>" in favorites list
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see Folders button on filter bottom sheet
    And I tap Folders button on filter bottom sheet
    And I see folders bottom sheet with Folders as the header title
    And I see folder <FolderName> on folder bottom sheet
    And I tap existing folder <FolderName> on folder bottom sheet
    Then I get navigated to folder page and I see <FolderName> as the page heading
    And I see conversation "<Member1>" in folder list

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | TeamName     | TeamNameB       | FolderName        |
      | user1Name | user2Name  | user3Name | folderFilter | ConnectedFriend | Filter and Folder |

  @TC-8654 @TC-8655 @regression @filter
  Scenario Outline: I want to see only group conversations when I filter by groups
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see Groups button on filter bottom sheet
    And I tap Groups button on filter bottom sheet
    When I get navigated to page Groups and I see Groups as the page heading
    And I see conversation "<GroupConversation>" in conversation list
    And I do not see conversation "<TeamOwnerB>" in conversation list
    # TC-8655 I want to see only 1on1 conversations when I filter by 1on1 conversations
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see 1:1 Conversations button on filter bottom sheet
    And I tap 1:1 Conversations button on filter bottom sheet
    When I get navigated to page 1:1 Conversations and I see 1:1 Conversations as the page heading
    And I see conversation "<TeamOwnerB>" in conversation list
    And I do not see conversation "<GroupConversation>" in conversation list

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | TeamName | TeamNameB       | GroupConversation |
      | user1Name | user2Name  | user3Name | Folder   | ConnectedFriend | Folder Group      |

  @TC-8647 @regression @filter
  Scenario Outline: I should not be able to move a conversation to a folder from Archive
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    And I see conversation name "<TeamOwnerB>" on filter bottom sheet header is displayed
    And I see Move to Archive button on filter bottom sheet
    And I tap Move to Archive button on filter bottom sheet
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    And I do not see conversation "<TeamOwnerB>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<TeamOwnerB>" in archive list
    When I tap on conversation name "<TeamOwnerB>" in archive list
    And I open conversation details for 1:1 conversation with "<TeamOwnerB>"
    And I tap show more options button on user profile screen
    Then I see conversation name "<TeamOwnerB>" on filter bottom sheet header is displayed
    And I do not see Move to Folder... button on filter bottom sheet

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | TeamName | TeamNameB       | GroupConversation |
      | user1Name | user2Name  | user3Name | Folder   | ConnectedFriend | Folder Group      |

  @TC-8648 @regression @filter
  Scenario Outline: I want to see a conversation in the same folder as before after I archived and unarchived it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwner> adds users <Member1>  to team "<TeamName>" with role Member
    And User <TeamOwner> is connected to <TeamOwnerB>
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1> in team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I see conversation "<TeamOwnerB>" in conversation list
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    And I see conversation name "<TeamOwnerB>" on filter bottom sheet header is displayed
    And I see Move to Folder... button on filter bottom sheet
    And I tap Move to Folder... button on filter bottom sheet
    Then I get navigated to move to folder page and I see Move to Folder... page heading
    And I see New Folder button
    And I see Done button
    When I tap New Folder button on move to folder page
    And I get navigated to new folder page I see New Folder as page heading
    And I enter <FolderName> as folder name
    And I tap Create Folder button on new folder page
    And I wait for 1 seconds
    Then I see toast message "<TeamOwnerB>" was moved to "<FolderName>" in conversation view
    When I long tap on conversation name "<TeamOwnerB>" in conversation list
    Then I see conversation name "<TeamOwnerB>" on filter bottom sheet header is displayed
    And I see Move to Archive button on filter bottom sheet
    When I tap move to archive button
    And I confirm archive conversation
    Then I see "Conversation was archived" toast message on conversation list
    And I do not see conversation "<TeamOwnerB>" in conversation list
    When I open the main navigation menu
    And I tap on archive menu entry
    Then I see conversation "<TeamOwnerB>" in archive list
    When I long tap on conversation name "<TeamOwnerB>" in archive list
    And I tap move out of archive button
    Then I see "Conversation was unarchived" toast message on archive list
    And I do not see conversation "<TeamOwnerB>" in archive list
    When I open the main navigation menu
    And I tap on conversations menu entry
    And I see conversation "<GroupConversation>" in conversation list
    Then I see conversation "<TeamOwnerB>" in conversation list
    When I tap filter conversation button
    And I see Filter Conversations on filter bottom sheet
    And I see Folders button on filter bottom sheet
    And I tap Folders button on filter bottom sheet
    Then I see folders bottom sheet with Folders as the header title
    And I see folder <FolderName> on folder bottom sheet
    When I tap existing folder <FolderName> on folder bottom sheet
    Then I get navigated to folder page and I see <FolderName> as the page heading
    And I see conversation "<TeamOwnerB>" in folder list

    Examples:
      | TeamOwner | TeamOwnerB | Member1   | TeamName | TeamNameB       | GroupConversation | FolderName  |
      | user1Name | user2Name  | user3Name | Folder   | ConnectedFriend | Folder Group      | Test Folder |