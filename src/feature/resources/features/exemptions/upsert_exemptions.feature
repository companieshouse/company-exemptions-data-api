Feature: Retrieves company exemption resource from the database

  Scenario Outline: Processes exemptions upsert request successfully

    Given the company exemptions data api service is running
    When CHS Kafka API Service is available
    When a PUT request matching payload within "<file>" is sent for company number "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka API service is invoked for company number "<company_number>"

    Examples:
      | company_number  | file                              |
      | 00006400        | exemptions_api_request            |

    Scenario Outline: Processing exemptions information unsuccessfully after bad request.

      Given the company exemptions data api service is running
      When a PUT request matching payload within "<file>" is sent for company number "<company_number>"
      Then a response status code of 400 should be returned
      And the CHS Kafka API service is not invoked for company number "<company_number>"
      And nothing is persisted in the database

      Examples:
        | company_number  | file                              |
        | 00006400        | exemptions_bad_request            |

    Scenario Outline: Processing exemptions information unsuccessfully after internal server error.

      Given the company exemptions data api service is running
      When a PUT request matching payload within "<file>" is sent for company number "<company_number>"
      Then a response status code of 500 should be returned
      And the CHS Kafka API service is not invoked for company number "<company_number>"
      And nothing is persisted in the database

      Examples:
        | company_number  | file                                      |
        | 00006400        | exemptions_internal_server_error_request  |

    Scenario Outline: Processing exemptions information unsuccessfully but information saved to database

      Given the company exemptions data api service is running
      When the CHS Kafka API service is unavailable
      And a PUT request matching payload within "<file>" is sent for company number "<company_number>"
      Then a response status code of 503 should be returned
      And a GET request is sent for company number "<company_number>"
      Then the response body should match the data found within "<result>"
      And the CHS Kafka API service is invoked for company number "<company_number>"

      Examples:
        | company_number  | file                   | result                        |
        | 00006400        | exemptions_api_request | retrieved_exemptions_resource |

  Scenario Outline: Processing exemptions information while database is down after service unavailable

    Given the company exemptions data api service is running
    And the exemptions database is down
    When a PUT request matching payload within "<file>" is sent for company number "<company_number>"
    Then a response status code of 503 should be returned
    And the CHS Kafka API service is not invoked for company number "<company_number>"

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_api_request |

    Scenario: Processing exemptions information unsuccessfully without ERIC headers after forbidden error

      Given the company exemptions data api service is running
      When a Put request is sent without ERIC headers
      Then a response status code of 401 should be returned