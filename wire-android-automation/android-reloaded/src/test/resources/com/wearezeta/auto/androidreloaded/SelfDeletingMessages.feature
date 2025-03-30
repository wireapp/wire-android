Feature: Self Deleting Messages

  ######################
  # Messaging
  ######################

  @TC-4517 @TC-4523 @regression @RC @selfDeletingMessages @smoke
  Scenario Outline: I want to send a 10 seconds self deleting message in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    # TC-4523 - I want to see all available timer options for self deleting messages
    Then I see OFF timer button is currently selected
    And I see 10 seconds timer button
    And I see 5 minutes timer button
    And I see 1 hour timer button
    And I see 1 day timer button
    And I see 7 days timer button
    And I see 4 weeks timer button
    When I tap on 10 seconds timer button
    And I see self deleting message label in text input field
    And I type the self deleting message "<Message2>" into text input field
    And I tap send button
    And I see the message "<Message2>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> reads the recent message from group conversation <GroupConversation> via device Device1
    Then I do not see the message "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> sends message "<Message3>" via device Device1 to group conversation <GroupConversation>
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                             | Message2                           | Message3                          |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting messages! | This will delete after 10 seconds. | I do not see the message anymore. |

  @TC-4524 @regression @selfDeletingMessages
  Scenario Outline: I want to send a 1 minute self deleting message in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    Then I see OFF timer button is currently selected
    And I see 1 minute timer button
    When I tap on 1 minute timer button
    And I type the self deleting message "<Message2>" into text input field
    And I tap send button
    And I see the message "<Message2>" in current conversation
    And I wait for 60 seconds
    Then I do not see the message "<Message2>" in current conversation
    When User <TeamOwner> reads the recent message from group conversation <GroupConversation> via device Device1
    Then I do not see the message "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <TeamOwner> sends message "<Message3>" via device Device1 to group conversation <GroupConversation>
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                             | Message2                             | Message3                         |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting messages! | This will delete after 60 seconds.   | I cannot see the message anymore. |

  @TC-4518 @regression @RC @selfDeletingMessages
  Scenario Outline: I want to receive a self deleting message in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <TeamOwner> sends ephemeral message "<Message2>" with timer 10 seconds via device Device1 to conversation <GroupConversation>
    Then I see the message "<Message2>" in current conversation
    When I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation
    When I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                             | Message2                           | Message3                          |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting messages! | This will delete after 10 seconds. | I do not see the message anymore. |

  @TC-4519 @regression @RC @selfDeletingMessages
  Scenario Outline: I want to send a 10 seconds self deleting message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
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
    And I tap on conversation name "<Member1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    And I tap on 10 seconds timer button
    And I type the self deleting message "<Message2>" into text input field
    And I tap send button
    And I see the message "<Message2>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation
    And I see the self deleting message hint "After <Member1> has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <Member1> reads the recent message from user <TeamOwner> via device Device1
    Then I do not see the self deleting message hint "After <Member1> has seen your message and the timer has expired on their side, this note disappears." in current conversation
    When User <Member1> sends message "<Message3>" via device Device1 to User <TeamOwner>
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | Message                             | Message2                           | Message3                          |
      | user1Name | Deleting | user2Name | Let us test self deleting messages! | This will delete after 10 seconds. | I do not see the message anymore. |

  @TC-4520 @regression @RC @selfDeletingMessages
  Scenario Outline: I want to receive a self deleting message in a 1on1 conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> adds a new device Device1 with label Device1
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
    And I tap on conversation name "<Member1>" in conversation list
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When User <Member1> sends ephemeral message "<Message2>" with timer 10 seconds via device Device1 to conversation <TeamOwner>
    Then I see the message "<Message2>" in current conversation
    When I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation
    When I type the message "<Message3>" into text input field
    And I tap send button
    Then I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | Message                             | Message2                           | Message3                          |
      | user1Name | Deleting | user2Name | Let us test self deleting messages! | This will delete after 10 seconds. | I do not see the message anymore. |

  ######################
  # Assets
  ######################

  @TC-4525 @regression @RC @selfDeletingMessages @fileSharing
  Scenario Outline: I want to send a 10 seconds self deleting image in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    Then I see OFF timer button is currently selected
    When I tap on 10 seconds timer button
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
    When I wait for 10 seconds
    Then I do not see an image in the conversation view
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                           |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting assets! |

  @TC-4527 @regression @RC @selfDeletingMessages @fileSharing
  Scenario Outline: I want to send a 10 seconds self deleting file in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    Then I see OFF timer button is currently selected
    When I tap on 10 seconds timer button
    And I tap file sharing button
    When I push 1KB sized file with name "textfile.txt" to file storage
    And I tap on Attach File option
    And I select file with name containing "textfile.txt" in DocumentsUI
    And I see file "textfile.txt" on preview page
    And I tap send button on preview page
    Then I see a file with name "textfile.txt" in the conversation view
    When I wait for 10 seconds
    Then I do not see a file with name "textfile.txt" in the conversation view
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                           |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting assets! |

  @TC-4526 @regression @RC @selfDeletingMessages @fileSharing
  Scenario Outline: I want to receive a 10 seconds self deleting image in a group conversation
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I hide the keyboard
    When User <TeamOwner> switches group conversation <GroupConversation> to ephemeral mode with 10 seconds timeout
    And User <TeamOwner> sends image "testing.jpg" to conversation <GroupConversation>
    Then I see an image in the conversation view
    When I wait for 10 seconds
    Then I do not see an image in the conversation view
    And I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                           |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting assets! |

  @TC-4531 @regression @selfDeletingMessages @fileSharing @WPB-439
  Scenario Outline: I want to see the correct options in the context menu for self deleting images
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
    And I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    When I tap on self deleting messages button
    Then I see OFF timer button is currently selected
    When I tap on 1 minute timer button
    And I tap file sharing button
    And I see sharing option for File is displayed
    And I see sharing option for Gallery is displayed
    And I see sharing option for Camera is displayed
    And I see sharing option for Video is displayed
    And I see sharing option for Audio is displayed
    And I push image with QR code containing "Image" to file storage
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    Then I see an image with QR code "Image" in the conversation view
    And I tap on the image
    Then I see context menu for images
    When I open context menu for images
    Then I see download option
    And I see delete option
    And I see message details option
    And I do not see reply option
    And I do not see copy option
    And I do not see edit option
    And I do not see reactions options
    And I tap back button 2 times
    When I longtap on the image
    Then I see download option
    And I see delete option
    And I see message details option
    And I do not see reply option
    And I do not see copy option
    And I do not see edit option
    And I do not see reactions options
    And I tap back button
    When I wait for 30 seconds
    Then I do not see an image in the conversation view
    And I see the self deleting message hint "After one participant has seen your message and the timer has expired on their side, this note disappears." in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                           |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting assets! |

  @TC-4529 @regression @RC @selfDeletingMessages
  Scenario Outline: I want to set a timer for self deleting messages for a group conversation and exchange messages
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see Self Deleting messages option is in OFF state
    When I tap on Self Deleting messages option for group conversation
    And I tap on Self Deleting messages toggle
    And I tap on 10 seconds timer button for changing the timer for group conversation
    And I tap on Apply button
    And I close the group conversation details through X icon
    Then I see system message "You set self-deleting messages to 10 seconds for everyone" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message>" in current conversation
    When User <Member1> sends message "<Message2>" to group conversation <GroupConversation>
    And I wait for 10 seconds
    Then I do not see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                            | Message2                                   |
      | user1Name | Deleting | user2Name | SelfDeleting      | This will delete after 10 seconds. | This will delete after 10 seconds as well. |

  @TC-4530 @TC-4528 @regression @RC @selfDeletingMessages @WPB-286 @WPB-10854
  Scenario Outline: I want to see a system message when I set a timer for self deleting messages for a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see Self Deleting messages option is in OFF state
    When I tap on Self Deleting messages option for group conversation
    And I tap on Self Deleting messages toggle
    And I tap on 10 seconds timer button for changing the timer for group conversation
    And I tap on Apply button
    And I close the group conversation details through X icon
    Then I see system message "You set self-deleting messages to 10 seconds for everyone" in conversation view
    # TC-4528 - I should not see a duplicated system messages for the change of self deleting timer in a group conversation after sending an asset
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I see Self Deleting messages option is in ON state
    And I tap on Self Deleting messages option for group conversation
    And I tap on Self Deleting messages toggle
    And I tap on Apply button
    And I close the group conversation details through X icon
    Then I see system message "You turned off the timer for self-deleting messages for everyone" in conversation view
    When I tap file sharing button
    And I push image with QR code containing "Image" to file storage
    And I tap on Attach Picture option
    And I select image with QR code "Image" in DocumentsUI
    And I select add button in DocumentsUI
    And I see image preview page
    And I tap send button on preview page
    Then I see an image with QR code "Image" in the conversation view
    And I see the system message "You set self-deleting messages to 10 seconds for everyone" only once in the conversation
    And I see the system message "You turned off the timer for self-deleting messages for everyone" only once in the conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation |
      | user1Name | Deleting | user2Name | SelfDeleting      |

  @TC-4532 @regression @RC @selfDeletingMessages
  Scenario Outline: I should not be able to change an already set self deleting timer
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds user <Member1> to team "<TeamName>" with role Member
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
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I see Self Deleting messages option is in OFF state
    When I tap on Self Deleting messages option for group conversation
    And I tap on Self Deleting messages toggle
    And I tap on 10 seconds timer button for changing the timer for group conversation
    And I tap on Apply button
    And I close the group conversation details through X icon
    Then I see system message "You set self-deleting messages to 10 seconds for everyone" in conversation view
    When I tap on the text input field
    Then I see the self deleting message button in current conversation
    When I tap on self deleting messages button
    Then I do not see self deleting timer options
    When I type the message "<Message>" into text input field
    And I tap send button
    And I see the message "<Message>" in current conversation
    And I wait for 10 seconds
    Then I do not see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName | Member1   | GroupConversation | Message                             |
      | user1Name | Deleting | user2Name | SelfDeleting      | Let us test self deleting messages! |
