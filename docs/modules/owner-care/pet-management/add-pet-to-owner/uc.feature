@UC-007
Feature: add-pet-to-owner

  Scenario: Clinic user adds a valid pet
    Given the PetClinic reference data is loaded
    When the clinic user adds pet "Buddy" born "2022-06-01" of type "dog" to owner 4
    Then owner 4 details include pet "Buddy"

  Scenario: Duplicate pet name is rejected
    Given the PetClinic reference data is loaded
    When the clinic user tries to add duplicate pet "Basil" to owner 2
    Then the pet command is rejected because the name already exists
