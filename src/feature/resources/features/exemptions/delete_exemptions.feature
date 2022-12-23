Feature: Deletes company exemption resource from the database

  Scenario Outline: Successfully deletes a company exemptions resource from Mongo

    Given CHS Kafka API Service is available
    And exemptions exists for company number "<company_number>"
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka Api service is invoked for "<company_number>" for a delete
    And the resource does not exist in the database for "<company_number>"

    Examples:
    | company_number |
    | 00006400       |

  Scenario Outline: 404 status code is returned when resource not found in Mongo

    Given CHS Kafka API Service is available
    And the resource does not exist in the database for "<company_number>"
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 404 should be returned
    And the CHS Kafka API service is not invoked

    Examples:
    | company_number |
    | 00006400       |

  Scenario Outline: A DELETE request sent when Mongo is unavailable

    Given CHS Kafka API Service is available
    And the company exemptions database isn't available
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 503 should be returned
    And the CHS Kafka API service is not invoked

    Examples:
    | company_number |
    | 00006400       |

  Scenario Outline: A DELETE request sent when CHS Kafka Api is unavailable

    Given the CHS Kafka API service is unavailable
    And exemptions exists for company number "<company_number>"
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 503 should be returned

    Examples:
    | company_number |
    | 00006400       |

  Scenario Outline: A DELETE request is sent without ERIC headers causing an unauthorised error

    When a DELETE request is sent without ERIC headers for "<company_number>"
    Then a response status code of 401 should be returned

    Examples:
    | company_number |
    | 00006400       |