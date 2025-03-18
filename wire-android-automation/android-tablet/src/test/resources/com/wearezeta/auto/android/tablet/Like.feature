Feature: Like

  @C246204 @regression
  Scenario Outline: I can like/unlike text message by heart/long tap
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<Contact>"
    And I type the message "<Message>" and send it by cursor Send button
    And I tap the Text message "<Message>" in the conversation view
    And I remember the state of like button
  # Tap heart like
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed
    And I see a Like from "<Name>" in conversation view
  # Tap heart unlike
    When I tap Like button in conversation view
    Then I see Message status with expected text "<MessageStatus>" in conversation view
    And I verify the state of like button item is not changed
  # Long tap to like
    When I long tap the Text message "<Message>" in the conversation view
    And I tap Like button on the message bottom menu
    Then I see a Like from "<Name>" in conversation view
    And I verify the state of like button item is changed
  # Long tap to unlike
    When I long tap the Text message "<Message>" in the conversation view
    And I tap Unlike button on the message bottom menu
    Then I see Message status with expected text "<MessageStatus>" in conversation view
    And I verify the state of like button item is not changed

    Examples:
      | Name      | Contact   | MessageStatus | Message |
      | user1Name | user2Name | Sent          | OMG     |

  @C246205 @regression
  Scenario Outline: (AN-4483) I can like link by heart
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<Contact>"
    And I type the message "<Url>" and send it by cursor Send button
    And I tap Link Preview container in the conversation view
    And I wait for 5 seconds
    And I tap Back button
    And I remember the state of like button
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed
    And I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | Url                     |
      | user1Name | user2Name | https://www.github.com/ |

  @C246206 @regression
  Scenario Outline: I can like a picture by heart
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given User <Contact> sends image "<Picture>" to single user conversation <Name>
    Given I wait for 3 seconds
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I tap Image container in the conversation view
    And I tap Like button on Image Fullscreen page
    And I tap Back button on Image Fullscreen page
    Then I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | Picture     |
      | user1Name | user2Name | testing.jpg |

  @C246207 @regression
  Scenario Outline: I can like sketch by heart/long tap
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    Given I tap Sketch button from cursor toolbar
    Given I draw a sketch with 2 colors
    Given I send my sketch
    Given I scroll to the bottom of conversation view
    # Tap heart like
    When I remember the state of like button
    And I tap Image container in the conversation view
    And I tap Like button on Image Fullscreen page
    And I tap Back button on Image Fullscreen page
    Then I see a Like from "<Name>" in conversation view
    # Tap heart unlike
    When I tap Image container in the conversation view
    And I tap Like button on Image Fullscreen page
    And I tap Back button on Image Fullscreen page
    Then I see Message status with expected text "<MessageStatus>" in conversation view
    And I verify the state of like button item is not changed
    # Long tap to like
    When I long tap Image container in the conversation view
    And I tap Like button on the message bottom menu
    Then I see a Like from "<Name>" in conversation view
    And I verify the state of like button item is changed
    # Long tap to unlike
    When I long tap Image container in the conversation view
    And I tap Unlike button on the message bottom menu
    Then I see Message status with expected text "<MessageStatus>" in conversation view
    And I verify the state of like button item is not changed

    Examples:
      | Name      | Contact   | MessageStatus |
      | user1Name | user2Name | Sent          |

  @C246209 @regression
  Scenario Outline: (AN-4483) I can like youtube
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<Contact>"
    And I type the message "<YoutubeLink>" and send it by cursor Send button
    And I long tap Youtube container in the conversation view
    And I tap Like button on the message bottom menu
    And I scroll to the bottom of conversation view
    And I remember the state of like button
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed

    Examples:
      | Name      | Contact   | YoutubeLink                                 |
      | user1Name | user2Name | https://www.youtube.com/watch?v=wTcNtgA6gHs |

  @C246210 @regression
  Scenario Outline: I can like location
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<DeviceName>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given User <Contact> shares their location to user Myself via device <DeviceName>
    Given I wait for 5 seconds
    When I tap on conversation name "<Contact>"
    And I tap Share Location container in the conversation view
    And I wait for 5 seconds
    And I restore Wire
    And I reopen the conversation name "<Contact>"
    And I long tap Share Location container in the conversation view
    And I tap Like button on the message bottom menu
    And I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | DeviceName |
      | user1Name | user2Name | device1    |

  @C246211 @regression
  Scenario Outline: I can like audio message
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<DeviceName>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given <Contact> sends local file named "<FileName>" and MIME type "<MIMEType>" via device <DeviceName> to user Myself
    Given I wait for 5 seconds
    Given I tap on conversation name "<Contact>"
    Given I scroll to the bottom of conversation view
    When I long tap File Upload container in the conversation view
    And I tap Like button on the message bottom menu
    And I tap Like button in conversation view
    Then I do not see Like button in conversation view

    Examples:
      | Name      | Contact   | FileName | MIMEType  | DeviceName |
      | user1Name | user2Name | test.m4a | audio/mp4 | Device1    |

  @C246212 @regression
  Scenario Outline: I can like video message
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    When I tap on conversation name "<Contact>"
    And I tap Video message button from cursor toolbar
    And I scroll to the bottom of conversation view
    And I tap Video Message container in the conversation view
    And I scroll to the bottom of conversation view
    And I remember the state of like button
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed
    And I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | FileSize | FileFullName     |
      | user1Name | user2Name | 1.00MB   | random_video.mp4 |

  @C246213 @regression
  Scenario Outline: I want to like file transfer
    Given There are 2 users where <Name> is me
    And User Myself is connected to <Contact>
    And I tap Log in button on Welcome page
    And I sign in using my email
    And I accept First Time overlay
    And I see Recent View with conversations
    And I tap on conversation name "<Contact>"
    When I tap File button from cursor toolbar
    And I see file chooser popup in the testing gallery
    And I tap Textfile button on file chooser popup in the testing gallery
    And I wait up to <UploadingTimeout> seconds until file is uploaded
    Then I see the result of "<FileSize><FileSizeType>" file upload having name "<FileName>.<FileExtension>" and extension "<FileExtension>"
    When I tap File Upload container in the conversation view
    And I remember the state of like button
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed
    And I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | FileName  | FileSize | FileExtension | UploadingTimeout | FileSizeType |
      | user1Name | user2Name | textfile  | 1.00MB   | txt           | 20               | MB           |

  @C246214 @regression
  Scenario Outline: Verify like icon is visible and sorted liker name next to the like icon, and I could like it.
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given Users add the following devices: {"<Contact>": [{"name": "<ContactDevice>", "label": "<ContactDevice>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I type the message "<Message>" and send it by cursor Send button
    And User <Contact> likes the recent message from user Myself via device <ContactDevice>
    Then I see a Like from "<Contact>" in conversation view
    And I see Like button in conversation view
    When I remember the state of like button
    And I tap Like button in conversation view
    Then I verify the state of like button item is changed
    And I see a Like from "<Contact>" in conversation view
    And I see a Like from "<Name>" in conversation view

    Examples:
      | Name      | Contact   | Message | ContactDevice |
      | user1Name | user2Name | Hi      | Device1       |

  @C246215 @regression
  Scenario Outline: I see likers count instead of names and first/second likes avatars
    Given There are 5 users where <Name> is me
    Given User <Contact1> is connected to Myself,<Contact2>,<Contact3>,<Contact4>
    Given User <Contact1> has group conversation <Group> with Myself,<Contact2>,<Contact3>,<Contact4>
    Given Users add the following device: {"<Contact2>": [{"name": "<D2>", "label": "<D2>"}],"<Contact3>": [{"name": "<D3>", "label": "<D3>"}],"<Contact4>": [{"name": "<D4>", "label": "<D4>"}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Group>"
    Given I type the message "<Message>" and send it by cursor Send button
    # Wait until all users receive the message
    Given I wait for 5 seconds
    Given I hide keyboard
    When User <Contact2> likes the recent message from group conversation <Group> via device <D2>
    And User <Contact3> likes the recent message from group conversation <Group> via device <D3>
    And User <Contact4> likes the recent message from group conversation <Group> via device <D4>
    Then I see Likes from 3 people in conversation view
    When I tap Like description in conversation view
    Then I see user <Contact2> in Liker list
    And I see user <Contact3> in Liker list
    And I see user <Contact4> in Liker list

    Examples:
      | Name      | Contact1  | Contact2  | Contact3  | Contact4  | Group     | Message | D2 | D3 | D4 |
      | user1Name | user2Name | user3Name | user4Name | user5Name | LikeGroup | Hi      | D2 | D3 | D4 |

