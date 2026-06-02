@UC-006
Feature: update-owner

  Scenario: Clinic user updates owner contact information
    Given the PetClinic reference data is loaded
    When the clinic user updates owner 4 first name to "Harry" and city to "Springfield"
    Then owner 4 details show "Harry Davis"
    And owner 4 details show city "Springfield"
