@UC-001
Feature: view-welcome-page

  Scenario: Visitor opens the welcome page
    Given the PetClinic application is available
    When the visitor opens the welcome page use case
    Then the welcome page navigation contains "Home", "Find Owners", "Veterinarians", and "Error"
