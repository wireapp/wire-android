Feature: Recall Message

  @C246267 @C246269 @regression
  Scenario Outline: Verify I can delete my message everywhere and I see others delete the message everywhere(1:1)
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact1>
    Given User adds the following device: {"<Contact1>": [{"name": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Delete from otherview
    Given I tap on conversation name "<Contact1>"
    When User <Contact1> send message "<Message>" via device <ContactDevice> to user Myself
    And I see the message "<Message>" in the conversation view
    And User <Contact1> delete the recent message everywhere from user Myself via device <ContactDevice>
    Then I do not see the message "<Message>" in the conversation view
    # C202328
    # FIXME: AA-484
    # And I see the trashcan next to the name of <Contact1> in the conversation view
    # Delete from my view
    When User adds the following device: {"Myself": [{"name": "<MySecondDevice>"}]}
    And User Myself send message "<Message2>" via device <MySecondDevice> to user <Contact1>
    And I see the message "<Message2>" in the conversation view
    And User <Contact1> remember the recent message from user Myself via device <ContactDevice>
    And I long tap the Text message "<Message2>" in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see the message "<Message2>" in the conversation view
    And User <Contact1> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds
    And I do not see the trashcan next to the name of Myself in the conversation view

    Examples:
      | Name      | Contact1  | Message           | ContactDevice | MySecondDevice | Message2 |
      | user1Name | user2Name | DeleteTextMessage | Device2       | Device1        | Del2     |

  @C246268 @C246270 @regression
  Scenario Outline: Verify I can delete my message everywhere and I see others delete the message everywhere(group)
    Given There are 3 users where <Name> is me
    Given User Myself is connected to <Contact1>, <Contact2>
    Given User Myself has group conversation <Group> with <Contact1>,<Contact2>
    Given User adds the following device: {"<Contact1>": [{"name": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Delete from otherview
    Given I tap on conversation name "<Group>"
    When User <Contact1> send message "<Message>" via device <ContactDevice> to group conversation <Group>
    And I see the message "<Message>" in the conversation view
    And User <Contact1> delete the recent message everywhere from group conversation <Group> via device <ContactDevice>
    Then I do not see the message "<Message>" in the conversation view
    # C202329
    # FIXME: AA-484
    # And I see the trashcan next to the name of <Contact1> in the conversation view
    # Delete from my view
    When User adds the following device: {"Myself": [{"name": "<MySecondDevice>"}]}
    And User Myself send message "<Message2>" via device <MySecondDevice> to group conversation <Group>
    And I see the message "<Message2>" in the conversation view
    And User <Contact1> remember the recent message from group conversation <Group> via device <ContactDevice>
    And I long tap the Text message "<Message2>" in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see the message "<Message2>" in the conversation view
    And User <Contact1> see the recent message from group conversation <Group> via device <ContactDevice> is changed in 15 seconds
    And I do not see the trashcan next to the name of Myself in the conversation view

    Examples:
      | Name      | Contact1  | Contact2  | Group  | Message           | ContactDevice | MySecondDevice | Message2 |
      | user1Name | user2Name | user3Name | TGroup | DeleteTextMessage | Device2       | Device1        | Del2     |

  @C246271 @regression
  Scenario Outline: Verify I can delete everywhere works for images
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I tap File button from cursor toolbar
    And I see file chooser popup in the testing gallery
    And I tap Image button on file chooser popup in the testing gallery
    And I scroll to the bottom of conversation view
    Then I see a picture in the conversation view
    And I long tap Image container in the conversation view
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see any pictures in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | ContactDevice |
      | user1Name | user2Name | Device1       |

  @C246272 @unstable
  Scenario Outline: Verify delete everywhere works for giphy
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I tap on text input
    And I type the message "<Message>"
    And I tap Gif button from cursor toolbar
    And I select a random gif from the grid preview
    Then I see giphy preview page
    When I tap on the giphy Send button
    # wait for gif to be sent
    And I wait for 3 seconds
    Then I see a picture in the conversation view
    And I see the picture in the conversation is animated
    When I long tap Image container in the conversation view
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see any pictures in the conversation view
    And I see the message "via giphy.com" in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | Message | ContactDevice |
      | user1Name | user2Name | Yo      | Device1       |

  @C246273 @regression
  Scenario Outline: Verify delete everywhere works for link preview
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    Given I type the message "<Link>" and send it by cursor Send button
    # Give it some time to generate the preview
    Given I wait for 5 seconds
    Given I hide keyboard
    When I long tap Link Preview container in the conversation view
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see Link Preview container in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | Link                    | ContactDevice |
      | user1Name | user2Name | https://www.github.com/ | Device1       |

  @C246274 @regression
  Scenario Outline: (AN-5045) Verify delete everywhere works for Share location
    Given I am on Android with Google Location Service
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I tap Share location button from cursor toolbar
    And I tap OK button on the alert
    And I tap Send button on Share Location page
    And I long tap Share Location container in the conversation view
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see Share Location container in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | ContactDevice |
      | user1Name | user2Name | device1       |

  @C246275 @regression
  Scenario Outline: I want to verify delete everywhere works for file sharing
    Given There are 2 users where <Name> is me
    And User Myself is connected to <Contact>
    And User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I tap on conversation name "<Contact>"
    When I tap File button from cursor toolbar
    And I see file chooser popup in the testing gallery
    And I tap Textfile button on file chooser popup in the testing gallery
    And I scroll to the bottom of conversation view
    And I wait up to <UploadingTimeout> seconds until file is uploaded
    Then I see File Upload container in the conversation view
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    When I long tap File Upload container in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see File Upload container in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | ContactDevice | UploadingTimeout |
      | user1Name | user2Name | device1       | 20               |

  @C246276 @regression
  Scenario Outline: Verify delete everywhere works for audio messages
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I long tap Audio message button <TapDuration> seconds from cursor toolbar
    And I tap audio recording Send button
    # Wait for sync
    And I wait for 5 seconds
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I long tap Audio Message container in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see Audio Message container in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | TapDuration | ContactDevice |
      | user1Name | user2Name | 5           | Device1       |

  @C246277 @regression
  Scenario Outline: Verify delete everywhere works for video messages
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I push <FileSize> video file having name "<FileFullName>" to the device
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I tap Video message button from cursor toolbar
    Then I see Video Message container in the conversation view
    # Should be enough to upload the nessage
    And I wait for 7 seconds
    And User <Contact> remember the recent message from user Myself via device <ContactDevice>
    And I long tap Video Message container in the conversation view
    And I tap Delete button on the message bottom menu
    And I tap Delete for everyone button on the message bottom menu
    And I tap Delete button on the alert
    Then I do not see Video Message container in the conversation view
    And User <Contact> see the recent message from user Myself via device <ContactDevice> is changed in 15 seconds

    Examples:
      | Name      | Contact   | FileSize | FileFullName     | ContactDevice |
      | user1Name | user2Name | 1.00MB   | random_video.mp4 | Device1       |