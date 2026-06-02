@UC-008
Feature: update-pet

  Scenario: Clinic user updates pet details
    Given the PetClinic reference data is loaded
    When the clinic user renames pet 8 of owner 6 to "Max Jr"
    Then owner 6 details include pet "Max Jr"

  Scenario: Duplicate sibling pet name is rejected
    Given the PetClinic reference data is loaded
    When the clinic user tries to rename pet 7 of owner 6 to "Max"
    Then the pet command is rejected because the name already exists
