Feature: Upsert company exemption resource to database

  Scenario Outline: Processes exemptions upsert request successfully

    Given CHS Kafka API Service is available
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka API service is invoked for upsert with "<company_number>"

    Examples:
      | company_number  | file                              |
      | 00006400        | exemptions_api_request            |

    Scenario Outline: Processing exemptions upsert unsuccessfully after bad request.

      When a PUT request matching payload within "<file>" is sent for "<company_number>"
      Then a response status code of 400 should be returned
      And the CHS Kafka API service is not invoked
      And nothing is persisted in the database

      Examples:
        | company_number  | file                              |
        | 00006400        | exemptions_bad_request            |

    Scenario Outline: Processing exemptions upsert unsuccessfully but information saved to database

      Given the CHS Kafka API service is unavailable
      When a PUT request matching payload within "<file>" is sent for "<company_number>"
      Then a response status code of 503 should be returned
      And the exemptions "<exemptions>" for "<company_number>" have been saved in the database
      And the CHS Kafka API service is invoked for upsert with "<company_number>"

      Examples:
        | company_number  | file                   | exemptions                        |
        | 00006400        | exemptions_api_request | retrieved_exemptions_resource |

  Scenario Outline: Processing exemptions upsert while database is down

    Given the exemptions database is unavailable
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 503 should be returned
    And the CHS Kafka API service is not invoked

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_api_request |

    Scenario: Processing exemptions upsert without ERIC headers causing an unauthorised error

      When a PUT request is sent without ERIC headers
      Then a response status code of 401 should be returned