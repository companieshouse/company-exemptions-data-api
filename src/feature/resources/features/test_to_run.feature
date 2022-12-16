Feature: Retrieves company exemption resource from the database

  Scenario Outline: Successfully retrieves a company exemptions resource from Mongo

    Given the company exemptions data api service is running
    When a PUT request matching payload within "<file>" is sent for company number "<company_number>"
    Then a response status code of 200 should be returned
    And the CHS Kafka API service is invoked for company number "<company_number>"

    Examples:
      | company_number  | file                              |
      | 00006400        | exemptions_api_request            |