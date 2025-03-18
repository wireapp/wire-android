Feature: Video Message

  @C164772 @regression
  Scenario Outline: Verify I can send video message
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I push <FileSize> video file having name "<FileFullName>" to the device
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I tap Video message button from cursor toolbar
    Then I see Video Message container in the conversation view

    Examples:
      | Name      | Contact   | FileSize | FileFullName      |
      | user1Name | user2Name | 3.00MB   | random_video.mp4  |