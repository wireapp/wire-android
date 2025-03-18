Feature: Rich Media

  @C759 @regression
  Scenario Outline: Send GIF format pic
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    # Waiting for sync to be done in the background
    Given I wait for 5 second
    Given I see Recent View with conversations
    Given User <Contact> sends image "<GifName>" to single user conversation Myself
    Given I see the subtitle "<Message>" of conversation <Contact>
    Given I tap on conversation name "<Contact>"
    When I scroll to the bottom of conversation view
      And I see conversation view
    Then I see a picture in the conversation view
    And I see the picture in the conversation is animated
    When I tap Image container in the conversation view
    Then I see the picture in the preview is animated

    Examples:
      | Name      | Contact   | GifName      | Message          |
      | user1Name | user2Name | animated.gif | Shared a picture |

  @C788 @unstable
  Scenario Outline: I can send giphy image by typing message and tapping GIF cursor button and confirm the selection
    Given There are 2 users where <Name> is me
    Given User <Contact> is connected to me
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given I tap on conversation name "<Contact>"
    When I tap on text input
    # And I type the message "<Message>"
    And I tap Gif button from cursor toolbar
    And I type "<Message>" in Giphy toolbar input field and hide keyboard
    And I see the giphy grid preview
    Then I see Giphy search field with text "<Message>"
    When I select a random gif from the grid preview
    # C250824
    Then I see Send button on Giphy Preview page
    And I see Cancel button on Giphy Preview page
    When I tap on the giphy Send button
    # C787
    Then I see a picture in the conversation view
    # And I see the most recent conversation message is "<Message> · via giphy.com"

    Examples:
      | Name      | Contact   | Message |
      | user1Name | user2Name | Yo      |

  @C462166 @regression
  Scenario Outline: Show preview for links to SoundCloud, Spotify on Tablet
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{}]}
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given User <Contact> sends 1 "<SCLink>" message to conversation Myself
    Given I wait for 5 seconds
    Given I tap on conversation name "<Contact>"
    When I tap Soundcloud container in the conversation view
    Then I see the Wire app is not in foreground
    When I restore Wire
    And I tap on text input
    And I type the message "<SpotifyLink>" and send it by cursor Send button
    And I tap Spotify container in the conversation view
    Then I see the Wire app is not in foreground

    Examples:
      | Name      | Contact   | SCLink                                          | SpotifyLink                                          |
      | user1Name | user2Name | https://soundcloud.com/joeybadass/rockabye-baby | http://open.spotify.com/track/0txZeFLQXDo0RSEubMr12b |
