Feature: LegalHold

#  TC-4403	I want to be informed when admin turns off legal hold for me
#  TC-4404	I want to see system messages when legal hold is turned off for me

#  TC-4408	I want to be informed when admin turns on legal hold for me and accept it while I am in a call
#  TC-4409	I want to be informed when admin turns on legal hold for me and dismiss it while I am in a call

#  TC-4410	I want to see all legal hold dialog dismissed on other devices when I accept on one of my devices

#  TC-4420	I want to detect legal hold when I open devices section of legal hold user

#  TC-4428	I want to see that all guests are removed from group conversation if some of other members are under Legal hold
#  TC-4425	I should not be able to use existing conversation with guest when I am under Legal hold
#  TC-4429	I should not be able to add guests to group conversation while conversation is under legal hold

#  TC-4426	I should not be able to send a connection request to a non-team member when I am under legal hold
#  TC-4427	I should not be able to send a connection request to a non-team member who is under legal hold

  # Can't Do (because we can't identify the red dot in automation):
  #  TC-4424	I want still see legal hold indicator in group conversation if several users had legal hold enabled and it was deactivated for one user
  #  TC-4417	I want to see legal hold indicator in a group conversation view
  #  TC-4418	I want to see legal hold indicator in a 1on1 conversation view
  #  TC-4419	I want to see all users who are under legal hold when I open legal hold overview from conversation list

  @TC-4401 @TC-4400 @TC-4402 @legalHold
  Scenario Outline: I want to be informed when admin turns on legal hold for me and accept it
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on conversation name "<TeamOwner>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    # TC-4402 - I want to see system messages when legal hold is turned on for me
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "You are now subject to legal hold." in conversation view
    # TC-4400 - I should not be able to remove legal hold device
    When I close the conversation view through the back arrow
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open manage your devices menu
    Then I see my current device is listed under devices
    And I see my other device "LegalHold" is listed under other devices section
    When I tap on my device "LegalHold" listed under devices
    Then I do not see remove device button

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | LegalHold | user2Name |

  @TC-4403 @TC-4404 @legalHold
  Scenario Outline: I want to see system messages when legal hold is turned off for me
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    And I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    And I see conversation list
    And I tap on conversation name "<TeamOwner>" in conversation list
    When Admin user <TeamOwner> turns off Legal Hold for user <Member1>
    Then I see legal hold deactivated modal
    And I see explanation "Future messages will not be recorded." on legal hold deactivated modal
    And I tap OK button on legal hold deactivated modal
    # TC-4404 - I want to see system messages when legal hold is turned off for me
    And I see system message "Legal hold is no longer active for this conversation." in conversation view
    And I see system message "You are no longer subject to legal hold." in conversation view

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | LegalHold | user2Name |

  @TC-4411 @legalHold
  Scenario Outline: I want to see legal hold dialog when it is already requested before I log in for the first time
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    #ToDo: Find better name for step below (instead of "alert")
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I see conversation list
    And I do not see legal hold request modal

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | LegalHold | user2Name |

  @TC-4405 @legalHold
  Scenario Outline: I want to see legal hold dialog again if I dismiss it and reopen the app
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap on conversation name "<TeamOwner>" in conversation list
    When Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    When I delay Legal Hold request
    Then I do not see legal hold request modal
    When I swipe the app away from background
    And I restart Wire
    Then I see legal hold request modal

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | LegalHold | user2Name |

  @TC-4407 @legalHold
  Scenario Outline: I want to see legal hold dialog again when I dismiss it and click on legal hold pending icon
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    When I delay Legal Hold request
    Then I do not see legal hold request modal
    When I tap User Profile Button
    And I see Legal hold is pending message on my user profile
    And I tap accept button for legal hold pending request
    Then I see legal hold request modal
    And I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I do not see legal hold request modal

    Examples:
      | TeamOwner | TeamName  | Member1   |
      | user1Name | LegalHold | user2Name |

  @TC-4430 @legalHold
  Scenario Outline: I want to accept legal hold request without entering password as a SSO user
    Given There is a team owner "<TeamOwner>" with SSO team "<TeamName>" configured for okta
    And User <TeamOwner> adds user <OktaMember1> to okta
    And SSO user <OktaMember1> is me
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I tap on SSO login tab
    When I type the default SSO code on SSO Login Tab
    And I tap login button on Login Page
    And I tap use without an account button if visible
    And I sign in with my credentials on Okta Page
    And I tap login button on Okta Page
    And I wait until I am logged in from okta page
    And I submit my Username <UserName> on registration page
    And I tap confirm button on UserName Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    When Admin user <TeamOwner> sends Legal Hold request for user <OktaMember1>
    And I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    Then I do not see password field on legal hold request modal
    When I accept Legal Hold request
    Then I see conversation list
    And I do not see legal hold request modal

    Examples:
      | TeamOwner | TeamName  | OktaMember1 | UserName            |
      | user1Name | LegalHold | user2Name   | user2UniqueUsername |

  @TC-4406 @legalHold
  Scenario Outline: I want to see error message when I submit the wrong password for legal hold request
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter invalid password "<Password>" password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I see invalid password error message on legal hold request modal
    And I see legal hold request modal
    And I clear the password input field on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I accept Legal Hold request
    Then I see conversation list
    And I do not see legal hold request modal

    Examples:
      | TeamOwner | TeamName  | Member1   | Password     |
      | user1Name | LegalHold | user2Name | Qwertz12345! |

  @TC-4432 @legalHold
  Scenario Outline: I want to be able to exchange messages with another user under which is not under legal hold while I am under legal hold
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    When I tap on conversation name "<TeamOwner>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "You are now subject to legal hold." in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<Member1>" via device Device1
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Message     | Message2 |
      | user1Name | LegalHold | user2Name | Hold this!  | Hi!      |

  @TC-4431 @legalHold
  Scenario Outline: I want to be able to exchange messages in a group with users who are not under legal hold while I am under legal hold
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on conversation name "<GroupConversation>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "You are now subject to legal hold." in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<GroupConversation>" via device Device1
    And User <Member2> sends message "<Message3>" under legal hold to conversation "<GroupConversation>" via device Device2
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | GroupConversation | Message     | Message2 | Message3          |
      | user1Name | LegalHold | user2Name | user3Name | LegalHold         | Hold this!  | Hi!      | Are we legal now? |

  @TC-4412 @TC-4414 @legalHold
  Scenario Outline: I want to be able to exchange messages with another user under legal hold while I am not under legal hold
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    When I tap on conversation name "<TeamOwner>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <TeamOwner>
    And User <TeamOwner> accepts pending Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "Legal hold activated for <TeamOwner>" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    # TC-4414 - I want to see an alert informing me that the conversation is under legal hold when I send the first message to a user under legal hold
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<Member1>" via device Device1
    And I see the message "<Message2>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Message     | Message2 |
      | user1Name | LegalHold | user2Name | Hold this!  | Hi!      |

  @TC-4413 @TC-4415 @TC-4416 @TC-4422 @legalHold
  Scenario Outline: I want to be able to exchange messages in a group which is under legal hold while I am not under legal hold
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on conversation name "<GroupConversation>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member2>
    And User <Member2> accepts pending Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    # TC-4416 - I want to see a system message in a conversation when another user is under legal hold while I am not under legal hold
    And I see system message "Legal hold activated for <Member2>" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    # TC-4415 - I want to see an alert informing me that the conversation is under legal hold when I send the first message to a group conversation with a user under legal hold
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<GroupConversation>" via device Device1
    And User <Member2> sends message "<Message3>" under legal hold to conversation "<GroupConversation>" via device Device2
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    #TC-4422 - I want to send and a receive messages to a conversation which was under legal hold, but is not anymore
    When Admin user <TeamOwner> turns off Legal Hold for user <Member2>
    Then I see system message "Legal hold is no longer active for this conversation." in conversation view
    And I see system message "Legal hold deactivated for <Member2>." in conversation view
    And User <Member2> sends message "<Message4>" to group conversation <GroupConversation>
    And I see the message "<Message4>" in current conversation
    And User <TeamOwner> sends message "<Message5>" to group conversation <GroupConversation>
    And I see the message "<Message5>" in current conversation
    And I type the message "<Message6>" into text input field
    And I tap send button
    Then I see the message "<Message6>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | GroupConversation | Message     | Message2 | Message3          | Message4   | Message5 | Message6           |
      | user1Name | LegalHold | user2Name | user3Name | LegalHold         | Hold this!  | Hi!      | Are we legal now? | It is gone | Yes.     | I am not legal now |

  @TC-4423 @legalHold
  Scenario Outline: I want to see legal hold deactivated for group conversation after legal hold subject left the conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And User <TeamOwner> adds a new device Device1 with label Device1
    And User <Member2> adds a new device Device2 with label Device2
    And User <Member1> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    When I tap on conversation name "<GroupConversation>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member2>
    And User <Member2> accepts pending Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "Legal hold activated for <Member2>" in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<GroupConversation>" via device Device1
    And User <Member2> sends message "<Message3>" under legal hold to conversation "<GroupConversation>" via device Device2
    And I see the message "<Message2>" in current conversation
    And I see the message "<Message3>" in current conversation
    When <Member2> leaves group conversation <GroupConversation>
    Then I see system message "Legal hold is no longer active for this conversation." in conversation view
    And I see system message "<Member2> left the conversation" in conversation view
    And User <TeamOwner> sends message "<Message4>" to group conversation <GroupConversation>
    And I see the message "<Message4>" in current conversation
    And I type the message "<Message5>" into text input field
    And I tap send button
    Then I see the message "<Message5>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Member2   | GroupConversation | Message     | Message2 | Message3          | Message4     | Message5 |
      | user1Name | LegalHold | user2Name | user3Name | LegalHold         | Hold this!  | Hi!      | Are we legal now? | Not anymore. | Yes.     |

  @TC-4421 @legalHold
  Scenario Outline: I should not be able to send message when I cancel the legal hold warning dialog
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1> to team "<TeamName>" with role Member
    And User <TeamOwner> registers legal hold service with team "<TeamName>"
    And User <TeamOwner> has 1:1 conversation with <Member1> in team "<TeamName>"
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
    When I tap on conversation name "<TeamOwner>" in conversation list
    And Admin user <TeamOwner> sends Legal Hold request for user <Member1>
    Then I see legal hold request modal
    And I see explanation "All future messages will be recorded by the device with fingerprint:" on legal hold request modal
    When I enter password my account password on legal hold request modal
    And I hide the keyboard
    And I accept Legal Hold request
    Then I see system message "Legal hold is now active for this conversation." in conversation view
    And I see system message "You are now subject to legal hold." in conversation view
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap on Cancel button in current conversation
    Then I do not see the message "<Message>" in current conversation
    And User <TeamOwner> sends message "<Message2>" under legal hold to conversation "<Member1>" via device Device1
    And I see the message "<Message2>" in current conversation
    When I type the message "<Message>" into text input field
    And I tap send button
    Then I see alert "The conversation is now subject to legal hold" in conversation view
    And I see text in alert "Do you still want to send your message?" in conversation view
    When I tap send anyway button
    Then I see the message "<Message>" in current conversation

    Examples:
      | TeamOwner | TeamName  | Member1   | Message     | Message2 |
      | user1Name | LegalHold | user2Name | Hold this!  | Hi!      |
