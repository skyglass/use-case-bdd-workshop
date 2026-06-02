@UC-004
Feature: find-owners-by-last-name

  Scenario: Prefix search returns matching owners
    Given the PetClinic reference data is loaded
    When the clinic user searches owners by last name prefix "Dav"
    Then the owner search returns 2 matches

  Scenario: Empty search returns all owners
    Given the PetClinic reference data is loaded
    When the clinic user searches owners by last name prefix ""
    Then the owner search returns 10 matches

  Scenario: No matching owner is reported
    Given the PetClinic reference data is loaded
    When the clinic user searches owners by last name prefix "Nonexistent"
    Then the owner search has no matches
