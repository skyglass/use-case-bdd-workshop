@UC-010
Feature: view-application-error

  Scenario: Unexpected error is presented without a stack trace
    Given the PetClinic application is available
    When an unexpected error is presented with message "Expected: controller used to showcase what happens when an exception is thrown"
    Then the application error presentation has status 500 and message "Expected: controller used to showcase what happens when an exception is thrown"

  Scenario: Missing resource is presented as not found
    Given the PetClinic application is available
    When a not found error is presented with message "Owner 99999 not found"
    Then the application error presentation has status 404 and message "Owner 99999 not found"
