Feature: Deletes company exemption resource from the database

  Scenario Outline: Successfully deletes a company exemptions resource from Mongo

    Given exemptions exists for company number "<company_number>"
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka Api service is invoked for "<company_number>" for a delete
    And the resource does not exist in the database for "<company_number>"

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Successfully calls CHS Kafka API even when resource to delete could not be found

    Given the resource does not exist in the database for "<company_number>"
    When a delete request is sent after to the delete endpoint for "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka Api service is invoked for "<company_number>" for a delete

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: A DELETE request sent when Mongo is unavailable

    Given the company exemptions database isn't available
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
    Then a response status code of 200 should be returned
    And the resource does not exist in the database for "<company_number>"

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: A DELETE request is sent without ERIC headers causing an unauthorised error

    When a DELETE request is sent without ERIC headers for "<company_number>"
    Then a response status code of 401 should be returned

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: A DELETE request is sent with a stale delta causing a conflict error

    Given exemptions exists for company number "<company_number>" with delta_at "<delta_at>"
    When a request is sent to the delete endpoint for "<company_number>"
    Then a response status code of 409 should be returned
    And the CHS Kafka API service is not invoked
    And the resource has been persisted for "<company_number>"

    Examples:
      | company_number | delta_at             |
      | 00006400       | 20240819123045999999 |

  Scenario Outline: A DELETE request is sent without a delta_at causing a bad request error

    Given exemptions exists for company number "<company_number>"
    When a delete request is sent without delta_at for "<company_number>"
    Then a response status code of 400 should be returned
    And the CHS Kafka API service is not invoked
    And the resource has been persisted for "<company_number>"

    Examples:
      | company_number |
      | 00006400       |