Feature: Collections

  @C399837 @regression
  Scenario Outline: Verify main overview shows media from all categories (picture, file, link)
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given User adds the following device: {"<Contact>": [{"name": "<ContactDevice>"}]}
    Given I create temporary file <FileSize> in size with name "<FileName>" and extension "<FileExt>"
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I accept First Time overlay
    Given User <Contact> sends image "<Picture>" to single user conversation <Name>
    Given User Myself sends 1 temporary files <FileName>.<FileExt> to conversation <Contact>
    Given User Myself sends message "<Link>" to user <Contact>
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    # Wait until for animation
    Given I wait for 2 seconds
    When I tap Search button from top toolbar
    Then I see PICTURES category on Collections page
    And I see LINKS category on Collections page
    # FIXME: Scroll to the bottom
    # And I see FILES category on Collections page
    
    Examples:
      | Name      | Contact   | Picture        | Link                  | FileName | FileSize | FileExt | ContactDevice |
      | user1Name | user2Name | avatarTest.png | https://www.wire.com/ | testing  | 10 KB    | bin     | device1       |