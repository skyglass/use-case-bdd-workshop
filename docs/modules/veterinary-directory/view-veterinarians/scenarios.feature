@UC-002
Feature: view-veterinarians

  Scenario: Visitor browses the veterinarian directory
    Given the PetClinic reference data is loaded
    When the visitor requests the first veterinarian page
    Then the veterinarian directory contains "James Carter"
    And veterinarian "Helen Leary" has specialty "radiology"
