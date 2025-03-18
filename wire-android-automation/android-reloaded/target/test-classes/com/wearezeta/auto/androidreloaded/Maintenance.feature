Feature: Maintenance

    @TC-431252352353
    Scenario Outline: I want to create personal users on backend
    And There are users <User1>, <User2>, <User3>, <User4>, <User5>, <User6>, <User7>, <User8>, <User9> on staging backend
    And I wait for 20 seconds
    And There are users <User10>, <User11>, <User12>, <User13>, <User14>, <User15> on staging backend
    And I wait for 20 seconds
    And There are users <User16>, <User17>, <User18>, <User19>, <User20>, <User21> on staging backend
    #And I wait for 20 seconds
    #And There are users <User22>, <User23>, <User24>, <User25>, <User26>, <User27> on staging backend
    And I print all created users in the execution log

    Examples:
      | User1     | User2     | User3     | User4     | User5     | User6     | User7     | User8     | User9     | User10     | User11     | User12     | User13     | User14     | User15     | User16     | User17     | User18     | User19     | User20     | User21     | User22     | User23     | User24     | User25     | User26     | User27     |
      | user1Name | user2Name | user3Name | user4Name | user5Name | user6Name | user7Name | user8Name | user9Name | user10Name | user11Name | user12Name | user13Name | user14Name | user15Name | user16Name | user17Name | user18Name | user19Name | user20Name | user21Name | user22Name | user23Name | user24Name | user25Name | user26Name | user27Name |
