Feature: TestingGallery

  @testing-gallery-give-permission
  Scenario: I want to give all the permissions to Testing Gallery
    #waiting for initialisation to be done
    Given I wait for 5 seconds
    Given I open the Testing Gallery App
    When I give Permission to Testing Gallery App
    Then I give permission to TestingGallery to access Notifications
    Then I give permission to TestingGallery to set default video recorder
    Then I give permission to TestingGallery to set default doc receiver