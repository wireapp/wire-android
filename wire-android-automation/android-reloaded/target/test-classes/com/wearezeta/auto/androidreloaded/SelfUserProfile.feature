Feature: Self User Profile

  # Delete Tests below, because they are the same as status tests

  @TC-4533 @regression @RC @selfUserProfile
  Scenario Outline: I want to successfully change my status to available
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to available on User Profile Page
    And I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    Then I see my status is set to "Available" on User Profile Page

  Examples:
  | TeamOwner | TeamName     | StatusText                                                                                                                                                                    |
  | user1Name | StatusChange | You will appear as Available to other people. You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation. |

  @TC-4534 @regression @RC @selfUserProfile
  Scenario Outline: I want to successfully change my status to busy
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to busy on User Profile Page
    And I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    Then I see my status is set to "Busy" on User Profile Page

    Examples:
      | TeamOwner | TeamName     | StatusText                                                                                                                                         |
      | user1Name | StatusChange | You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations that are not muted. |

  @TC-4535 @regression @RC @selfUserProfile
  Scenario Outline: I want to successfully change my status to away
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    When I change my status to away on User Profile Page
    And I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    Then I see my status is set to "Away" on User Profile Page

    Examples:
      | TeamOwner | TeamName     | StatusText                                                                                                        |
      | user1Name | StatusChange | You will appear as Away to other people. You will not receive notifications about any incoming calls or messages. |

  @TC-4536 @regression @RC @selfUserProfile
  Scenario Outline: I want to successfully change my status to none
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> is me
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I tap User Profile Button
    And I see User Profile Page
    And I see change status options
    And I change my status to busy on User Profile Page
    And I tap OK button on the alert
    When I change my status from busy to none on User Profile Page
    And I see text "<StatusText>" informing me about my status change
    And I tap OK button on the alert
    Then I see my status is set to "None" on User Profile Page

    Examples:
      | TeamOwner | TeamName     | StatusText                                                                                                                      |
      | user1Name | StatusChange | You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation. |
