@UC-009
Feature: book-visit-for-pet

  Scenario: Clinic user books a valid visit
    Given the PetClinic reference data is loaded
    When the clinic user books a visit for owner 6 pet 8 on "2026-01-15" with description "Annual check-up"
    Then pet 8 visit history includes "Annual check-up" on "2026-01-15"

  Scenario: Blank visit description is rejected
    Given the PetClinic reference data is loaded
    When the clinic user tries to book a visit for owner 6 pet 8 with blank description
    Then the visit command is rejected because description is required

  Scenario: Visit cannot be prepared through the wrong owner
    Given the PetClinic reference data is loaded
    When the clinic user prepares a visit for owner 1 and pet 8
    Then the visit form is not available
