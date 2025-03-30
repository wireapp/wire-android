Feature: Guest Links

  ######################
  # Without Password
  ######################

  @TC-4381 @TC-4382 @regression @RC @guestLinks
  Scenario Outline: I want to be able to create an invite deep link as a group admin if guests are enabled in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    And I tap on create link option on guests page
    And I select create without password to create the guest link
    Then I see guest link was created
    When I tap on copy link button on guests Link page
    And I close guest page through the back arrow
    And I close the group conversation details through X icon
    And I tap on the text input field
    And I paste the copied text into the text input field
    And I tap send button
    Then I see guest link is displayed in the current conversation
    #  TC-4382	I want to be able to revoke an invite deep link as a group admin by disabling guests in a group conversation
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    And I tap on the guest switch
    And I tap on disable button on pop up
    Then I do not see the guest link displayed on guests page
    And I see the Guests switch is at "OFF" state

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-8122 @regression @RC @guestLinks
  Scenario Outline: I want to join a conversation via invite deep link with a team account
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link for conversation <GroupConversation>
    When I open deep link for joining conversation <GroupConversation> that user <TeamOwner> has sent me
    And I see join conversation alert
    And I see subtext "You have been invited to a conversation." in the join conversation alert
    And I see conversation name "<GroupConversation>" in the join conversation alert
    And I tap join button on join conversation alert
    Then I see group conversation "<GroupConversation>" is in foreground
    And I see system message "You joined the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-4388 @regression @RC @guestLinks
  Scenario Outline: I want to open the conversation through deeplink if I am already a participant
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link for conversation <GroupConversation>
    When I open deep link for joining conversation <GroupConversation> that user <TeamOwner> has sent me
    Then I see group conversation "<GroupConversation>" is in foreground
    And I do not see system message "You joined the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-4389 @regression @RC @guestLinks
  Scenario Outline: I want to cancel to join a conversation via invite deep link with a team account
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link for conversation <GroupConversation>
    When I open deep link for joining conversation <GroupConversation> that user <TeamOwner> has sent me
    And I see join conversation alert
    And I see subtext "You have been invited to a conversation." in the join conversation alert
    And I see conversation name "<GroupConversation>" in the join conversation alert
    And I tap cancel button on join conversation alert
    Then I do not see group conversation "<GroupConversation>" is in foreground
    And I see conversation list

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-8123 @regression @RC @guestLinks
  Scenario Outline: I want to see alert when trying to join a conversation that was deleted through deep link
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link for conversation <GroupConversation>
    And I remember deep link that User <TeamOwner> created for conversation <GroupConversation> as invite
    And Group admin user <TeamOwner> deletes conversation <GroupConversation>
    When I open remembered deep link for joining conversation
    Then I see can not join conversation alert
    And I see subtext "Due to an error you could not be added to the group conversation." in the can not join conversation alert
    And I tap OK button on the alert
    And I do not see group conversation "<GroupConversation>" is in foreground

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-8124 @regression @RC @guestLinks
  Scenario Outline: I want to see alert when trying to join a conversation through deep link after link was revoked
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link for conversation <GroupConversation>
    And I remember deep link that User <TeamOwner> created for conversation <GroupConversation> as invite
    And User <TeamOwner> revokes invite link for conversation <GroupConversation>
    When I open remembered deep link for joining conversation
    Then I see can not join conversation alert
    And I see subtext "Due to an error you could not be added to the group conversation." in the can not join conversation alert
    And I tap OK button on the alert
    And I do not see group conversation "<GroupConversation>" is in foreground

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-8125 @regression @RC @guestLinks
  Scenario Outline: I want to be able to see previously created guestlink without password as a new group admin
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    And I tap on create link option on guests page
    And I select create without password to create the guest link
    Then I see guest link was created
    And I close guest page through the back arrow
    And I tap on Participants tab
    And I see user <Member2> in participants list
    When I tap on user <Member2> in participants list
    And I see connected user <Member2> profile
    And I tap on edit button to change user role
    And I change the user role to admin
    And I see new role for user is admin
    And I close the user profile to go back to conversation details
    And I close the group conversation details through X icon
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I tap New Team or Account button
    And I see Welcome Page
    And User <Member2> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    Then I see guest link was created

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        |

  @TC-8150 @regression @RC @guestLinks
  Scenario Outline: I want to join the conversation with last active account if I have permission when I am logged in to multiple accounts
    Given There is a team owner "<TeamOwnerA>" with team "<TeamNameA>"
    Given There is a team owner "<TeamOwnerB>" with team "<TeamNameB>"
    And User <TeamOwnerA> adds users <Member1>,<Member2> to team "<TeamNameA>" with role Member
    And User <Member1> is me
    And User <TeamOwnerA> has group conversation <GroupConversation> with <Member2> in team "<TeamNameA>"
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
    And I tap New Team or Account button
    And User <TeamOwnerB> is me
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
    When I switch to <Member1> account
    And I see conversation list
    And User <TeamOwnerA> creates invite link for conversation <GroupConversation>
    And I open deep link for joining conversation <GroupConversation> that user <TeamOwnerA> has sent me
    And I see join conversation alert
    And I see subtext "You have been invited to a conversation." in the join conversation alert
    And I see conversation name "<GroupConversation>" in the join conversation alert
    And I tap join button on join conversation alert
    Then I see group conversation "<GroupConversation>" is in foreground
    And I see system message "You joined the conversation" in conversation view
    When I close the conversation view through the back arrow
    And I tap User Profile Button
    Then I see User Profile Page for account <Member1> as my currently active account
    And I see my other account <TeamOwnerB> is listed under other logged in accounts

    Examples:
      | TeamOwnerA | Member1   | Member2   | TeamOwnerB | TeamNameA | TeamNameB | GroupConversation |
      | user1Name  | user2Name | user3Name | user4Name  | Guests   | NoGuests   | GuestsHere        |

  ######################
  # With Password
  ######################

  @TC-8126 @regression @RC @guestLinks
  Scenario Outline: I want to be able to create an invite deep link with password as a group admin if guests are enabled in a group conversation
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    When I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    And I tap on create link option on guests page
    When I select create with password to create the guest link
    And I see create password for guestlinks page
    And I type my password "<Password>" on guestlink page
    And I type my confirm password "<Password>" on guestlink page
    And I tap create Link button on guest link password page
    Then I see guest link was created
    And I see that guest link was created with password
    When I tap on copy link button on guests Link page
    And I close guest page through the back arrow
    And I close the group conversation details through X icon
    And I tap on the text input field
    And I paste the copied text into the text input field
    And I tap send button
    Then I see guest link is displayed in the current conversation

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation | Password    |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        | Password123! |

  @TC-8127 @regression @RC @guestLinks
  Scenario Outline: I want to join a conversation via invite deep link with password with a team account
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link with password "<Password>" for conversation <GroupConversation>
    When I open deep link for joining conversation <GroupConversation> that user <TeamOwner> has sent me
    And I see join conversation alert
    And I see subtext "You have been invited to a conversation." in the join conversation alert
    And I see conversation name "<GroupConversation>" in the join conversation alert
    And I enter password "<Password>" on join conversation alert
    And I tap join button on join conversation alert
    Then I see group conversation "<GroupConversation>" is in foreground
    And I see system message "You joined the conversation" in conversation view

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation | Password     |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        | Password123! |

  @TC-8128 @regression @RC @guestLinks
  Scenario Outline: I should not be able to join a conversation via invite deep link with password when I enter the wrong password
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <Member1> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And User <TeamOwner> creates invite link with password "<Password>" for conversation <GroupConversation>
    When I open deep link for joining conversation <GroupConversation> that user <TeamOwner> has sent me
    And I see join conversation alert
    And I see subtext "You have been invited to a conversation." in the join conversation alert
    And I see conversation name "<GroupConversation>" in the join conversation alert
    And I enter password "<WrongPassword>" on join conversation alert
    And I tap join button on join conversation alert
    Then I see invalid password error on join conversation alert
    And I do not see group conversation "<GroupConversation>" is in foreground

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation | Password     | WrongPassword |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        | Password123! | ThisIsWrong   |

  @TC-8129 @regression @RC @guestLinks
  Scenario Outline: I want to be able to see previously created guestlink with password as a new group admin
    Given There is a team owner "<TeamOwner>" with team "<TeamName>"
    And User <TeamOwner> adds users <Member1>,<Member2> to team "<TeamName>" with role Member
    And User <TeamOwner> is me
    And User <TeamOwner> has group conversation <GroupConversation> with <Member1>,<Member2> in team "<TeamName>"
    And I see Welcome Page
    And I open staging backend deep link
    And I tap proceed button on custom backend alert
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    And I tap on create link option on guests page
    When I select create with password to create the guest link
    And I see create password for guestlinks page
    And I type my password "<Password>" on guestlink page
    And I type my confirm password "<Password>" on guestlink page
    And I tap create Link button on guest link password page
    Then I see guest link was created
    And I see that guest link was created with password
    And I close guest page through the back arrow
    And I tap on Participants tab
    And I see user <Member2> in participants list
    When I tap on user <Member2> in participants list
    And I see connected user <Member2> profile
    And I tap on edit button to change user role
    And I change the user role to admin
    And I see new role for user is admin
    And I close the user profile to go back to conversation details
    And I close the group conversation details through X icon
    And I close the conversation view through the back arrow
    And I tap User Profile Button
    And I tap New Team or Account button
    And I see Welcome Page
    And User <Member2> is me
    And I tap login button on Welcome Page
    And I sign in using my email
    And I tap login button on email Login Page
    And I wait until I am fully logged in
    And I decline share data alert
    Then I see conversation list
    And I see conversation "<GroupConversation>" in conversation list
    And I tap on conversation name "<GroupConversation>" in conversation list
    And I tap on group conversation title "<GroupConversation>" to open group details
    And I tap on guest options on conversation details page
    And I see the Guests switch is at "ON" state
    Then I see guest link was created
    And I see that guest link was created with password

    Examples:
      | TeamOwner | Member1   | Member2   | TeamName | GroupConversation | Password     |
      | user1Name | user2Name | user3Name | Guests   | GuestsHere        | Password123! |