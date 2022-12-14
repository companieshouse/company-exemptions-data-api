Feature: Retrieves company exemption resource from the database

  Scenario Outline: Successfully retrieves a company exemptions resource from Mongo

    Given the company exemptions data api service is running
    And exemptions exists for a company with company number "<company_number>"
    When a GET request is sent for a company with company number "<company_number>"
    Then a response status code of 200 should be returned
    And the response body should match the data found within "<file>"

    Examples:
      | company_number  | file                              |
      | 00006400        | retrieved_exemptions_resource     |