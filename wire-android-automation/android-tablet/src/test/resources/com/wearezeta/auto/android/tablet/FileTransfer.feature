Feature: File Transfer

  @C164771 @regression
  Scenario Outline: Verify I can send file and placeholder is shown
    Given There are 2 users where <Name> is me
    Given User Myself is connected to <Contact>
    Given I tap Log in button on Welcome page
    Given I sign in using my email
    Given I push <FileSize><FileSizeType> file having name "<FileName>.<FileExtension>" to the device
    Given I accept First Time overlay
    Given I see Recent View with conversations
    Given I tap on conversation name "<Contact>"
    When I tap File button from cursor toolbar
      And I see file chooser popup in the testing gallery
      And I tap Textfile button on file chooser popup in the testing gallery
      And I wait up to <UploadingTimeout> seconds until file is uploaded
    Then I see the result of "<FileSize><FileSizeType>" file upload having name "<FileName>.<FileExtension>" and extension "<FileExtension>"

    Examples:
      | Name      | Contact   | FileName  | FileExtension | FileSize | UploadingTimeout | FileSizeType |
      | user1Name | user2Name | textfile  | txt           | 1.00     | 20               | MB           |
