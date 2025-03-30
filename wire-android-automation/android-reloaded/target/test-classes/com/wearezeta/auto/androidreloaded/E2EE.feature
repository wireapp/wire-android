Feature: E2EE

  @TC-4320 @TC-4321 @regression @RC @E2EE
  Scenario Outline: I want to see a decryption error when I receive a message which can't be decrypted
    Given There are 2 users where <Name> is me
    And User Myself is connected to <Contact>
    And User <Contact> adds a new device <ContactDevice> with label <ContactDevice>
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<Contact>" in conversation list
    And I tap on menu button on conversation list
    And I tap on Settings menu entry
    And I open manage your devices menu
    And I remember the client id of the current device
    And I close the devices screen through the back arrow
    And I tap back button
    And User <Contact> sends 1 default message to conversation Myself
    And I wait until the notification popup disappears
    And I see conversation "<Contact>" is having 1 unread messages in conversation list
    And I tap on unread conversation name "<Contact>" in conversation list
    When I type the message "<Message>" into text input field
    And I tap send button
    And I hide the keyboard
    Then I see the message "<Message>" in current conversation
    When User <Contact> breaks the session via device <ContactDevice> with my current client
    And User <Contact> sends 1 default message to conversation Myself
    And I see the message "<DecryptionError>" in current conversation
    And I hide the keyboard
    # TC-4321 - I want to be able to reset a session with a user after it was broken
    When I tap on reset session button
    Then I see "Session successfully reset" toast message in conversation view

  Examples:
  | Name      | Contact   | ContactDevice  | Message | DecryptionError                             |
  | user1Name | user2Name | Device1        | Hello!  | Message could not be decrypted (Error 406). |



