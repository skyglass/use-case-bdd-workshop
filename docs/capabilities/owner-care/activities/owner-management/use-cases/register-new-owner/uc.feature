@UC-003
Feature: register-new-owner

  Scenario: Clinic user registers a valid owner
    Given the PetClinic reference data is loaded
    When the clinic user registers owner "Jane" "Whitfield" at "123 Oak St" in "Madison" with telephone "5551234567"
    Then owner "Jane Whitfield" can be viewed by the returned owner id
