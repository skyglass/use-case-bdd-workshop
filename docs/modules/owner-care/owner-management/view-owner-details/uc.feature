@UC-005
Feature: view-owner-details

  Scenario: Clinic user views owner details with pets and visits
    Given the PetClinic reference data is loaded
    When the clinic user views owner 6 details
    Then owner details show "Jean Coleman"
    And owner details list pets in order "Max", "Samantha"
    And pet "Max" visits are chronological
