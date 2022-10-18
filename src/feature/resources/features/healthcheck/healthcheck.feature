Feature: Service healthcheck
  As a sysadmin
  I want to verify the service is healthy
  So that I can take remedial action if required

  Scenario: Service is healthy
    When the user performs a healthcheck
    Then the response code should be 200
