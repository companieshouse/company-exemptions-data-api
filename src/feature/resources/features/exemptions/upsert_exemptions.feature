Feature: Upsert company exemption resource to database

  Scenario Outline: Processes exemptions upsert request successfully

    Given CHS Kafka API Service is available
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka API service is invoked for upsert with "<company_number>"

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_api_request |

  Scenario Outline: Processing exemptions upsert out of date delta.

    Given exemptions exists for company number "<company_number>" with delta_at "<delta_at>"
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 409 should be returned
    And the CHS Kafka API service is not invoked
    And the exemptions "<exemptions>" for "<company_number>" exist in the database

    Examples:
      | company_number | delta_at             | file                               | exemptions                    |
      | 00006400       | 20221216121025774312 | exemptions_api_request_out_of_date | retrieved_exemptions_resource |

  Scenario Outline: Processing exemptions upsert unsuccessfully after bad request.

    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 400 should be returned
    And the CHS Kafka API service is not invoked
    And nothing is persisted in the database

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_bad_request |

  Scenario Outline: Processing exemptions upsert fails call to chs-kafka-api and does not persist to the database

    Given the CHS Kafka API service is unavailable
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 503 should be returned
    And the CHS Kafka API service is invoked for upsert with "<company_number>"
    And the resource does not exist in the database for "<company_number>"

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_api_request |

  Scenario Outline: Processing exemptions upsert while database is down

    Given the exemptions database is unavailable
    When a PUT request matching payload within "<file>" is sent for "<company_number>"
    Then a response status code of 503 should be returned
    And the CHS Kafka API service is not invoked

    Examples:
      | company_number | file                   |
      | 00006400       | exemptions_api_request |

  Scenario Outline: Processing exemptions upsert without ERIC headers causing an unauthorised error

    When a PUT request is sent without ERIC headers for "<company_number>"
    Then a response status code of 401 should be returned

    Examples:
      | company_number |
      | 00006400       |