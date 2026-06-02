package ai.unifiedprocess.petclinic.bdd;

import ai.unifiedprocess.petclinic.core.application.PresentApplicationErrorUseCase;
import ai.unifiedprocess.petclinic.core.application.PresentApplicationErrorUseCase.ErrorPresentation;
import ai.unifiedprocess.petclinic.owner.application.FindOwnersByLastNameUseCase;
import ai.unifiedprocess.petclinic.owner.application.FindOwnersByLastNameUseCase.OwnerSearchResult;
import ai.unifiedprocess.petclinic.owner.application.RegisterNewOwnerUseCase;
import ai.unifiedprocess.petclinic.owner.application.RegisterNewOwnerUseCase.RegisterOwnerCommand;
import ai.unifiedprocess.petclinic.owner.application.UpdateOwnerUseCase;
import ai.unifiedprocess.petclinic.owner.application.UpdateOwnerUseCase.UpdateOwnerCommand;
import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase;
import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase.OwnerDetails;
import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase.PetDetails;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase;
import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase.AddPetCommand;
import ai.unifiedprocess.petclinic.pet.application.UpdatePetUseCase;
import ai.unifiedprocess.petclinic.pet.application.UpdatePetUseCase.UpdatePetCommand;
import ai.unifiedprocess.petclinic.pet.domain.DuplicatePetNameException;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import ai.unifiedprocess.petclinic.vet.application.ViewVeterinariansUseCase;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.visit.application.BookVisitForPetUseCase;
import ai.unifiedprocess.petclinic.visit.application.BookVisitForPetUseCase.BookVisitCommand;
import ai.unifiedprocess.petclinic.visit.domain.BlankVisitDescriptionException;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.welcome.application.ViewWelcomePageUseCase;
import ai.unifiedprocess.petclinic.welcome.application.ViewWelcomePageUseCase.WelcomePage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PetClinicStepDefinitions {

    private final ViewWelcomePageUseCase viewWelcomePage;
    private final ViewVeterinariansUseCase viewVeterinarians;
    private final RegisterNewOwnerUseCase registerNewOwner;
    private final FindOwnersByLastNameUseCase findOwnersByLastName;
    private final ViewOwnerDetailsUseCase viewOwnerDetails;
    private final UpdateOwnerUseCase updateOwner;
    private final AddPetToOwnerUseCase addPetToOwner;
    private final UpdatePetUseCase updatePet;
    private final BookVisitForPetUseCase bookVisitForPet;
    private final PresentApplicationErrorUseCase presentApplicationError;
    private final PetClinicCucumberFixture fixture;

    private WelcomePage welcomePage;
    private List<Vet> veterinarians;
    private RegisterNewOwnerUseCase.RegisteredOwner registeredOwner;
    private OwnerSearchResult ownerSearchResult;
    private OwnerDetails ownerDetails;
    private RuntimeException petCommandError;
    private RuntimeException visitCommandError;
    private BookVisitForPetUseCase.VisitBookingForm visitBookingForm;
    private ErrorPresentation errorPresentation;
    private Integer lastOwnerId;

    public PetClinicStepDefinitions(
            ViewWelcomePageUseCase viewWelcomePage,
            ViewVeterinariansUseCase viewVeterinarians,
            RegisterNewOwnerUseCase registerNewOwner,
            FindOwnersByLastNameUseCase findOwnersByLastName,
            ViewOwnerDetailsUseCase viewOwnerDetails,
            UpdateOwnerUseCase updateOwner,
            AddPetToOwnerUseCase addPetToOwner,
            UpdatePetUseCase updatePet,
            BookVisitForPetUseCase bookVisitForPet,
            PresentApplicationErrorUseCase presentApplicationError,
            PetClinicCucumberFixture fixture) {
        this.viewWelcomePage = viewWelcomePage;
        this.viewVeterinarians = viewVeterinarians;
        this.registerNewOwner = registerNewOwner;
        this.findOwnersByLastName = findOwnersByLastName;
        this.viewOwnerDetails = viewOwnerDetails;
        this.updateOwner = updateOwner;
        this.addPetToOwner = addPetToOwner;
        this.updatePet = updatePet;
        this.bookVisitForPet = bookVisitForPet;
        this.presentApplicationError = presentApplicationError;
        this.fixture = fixture;
    }

    @Given("the PetClinic application is available")
    public void theApplicationIsAvailable() {
        assertNotNull(viewWelcomePage);
    }

    @Given("the PetClinic reference data is loaded")
    public void referenceDataIsLoaded() {
        fixture.assertReferenceDataLoaded();
    }

    @When("the visitor opens the welcome page use case")
    public void visitorOpensWelcomePage() {
        welcomePage = viewWelcomePage.view();
    }

    @Then("the welcome page navigation contains {string}, {string}, {string}, and {string}")
    public void welcomeNavigationContains(String first, String second, String third, String fourth) {
        assertEquals(List.of(first, second, third, fourth), welcomePage.navigationLabels());
    }

    @When("the visitor requests the first veterinarian page")
    public void visitorRequestsVeterinarians() {
        veterinarians = viewVeterinarians.findPage(0, 20).toList();
    }

    @Then("the veterinarian directory contains {string}")
    public void veterinarianDirectoryContains(String fullName) {
        assertTrue(veterinarians.stream().anyMatch(vet -> fullName(vet).equals(fullName)),
                "Expected veterinarian directory to contain " + fullName);
    }

    @Then("veterinarian {string} has specialty {string}")
    public void veterinarianHasSpecialty(String fullName, String specialty) {
        Vet vet = veterinarians.stream()
                .filter(candidate -> fullName(candidate).equals(fullName))
                .findFirst()
                .orElseThrow();
        assertTrue(vet.specialties().contains(specialty),
                "Expected " + fullName + " to have specialty " + specialty);
    }

    @When("the clinic user registers owner {string} {string} at {string} in {string} with telephone {string}")
    public void clinicUserRegistersOwner(String firstName, String lastName, String address, String city, String telephone) {
        registeredOwner = registerNewOwner.register(
                new RegisterOwnerCommand(firstName, lastName, address, city, telephone));
    }

    @Then("owner {string} can be viewed by the returned owner id")
    public void ownerCanBeViewedByReturnedId(String fullName) {
        Owner owner = viewOwnerDetails.findDetails(registeredOwner.ownerId()).orElseThrow().owner();
        assertEquals(fullName, fullName(owner));
    }

    @When("the clinic user searches owners by last name prefix {string}")
    public void clinicUserSearchesOwners(String prefix) {
        ownerSearchResult = findOwnersByLastName.search(prefix);
    }

    @Then("the owner search returns {int} matches")
    public void ownerSearchReturnsMatches(int expectedMatches) {
        assertEquals(expectedMatches, ownerSearchResult.total());
    }

    @Then("the owner search has no matches")
    public void ownerSearchHasNoMatches() {
        assertTrue(ownerSearchResult.hasNoMatches());
    }

    @When("the clinic user views owner {int} details")
    public void clinicUserViewsOwnerDetails(int ownerId) {
        lastOwnerId = ownerId;
        ownerDetails = viewOwnerDetails.findDetails(ownerId).orElseThrow();
    }

    @Then("owner details show {string}")
    public void ownerDetailsShow(String fullName) {
        assertEquals(fullName, fullName(ownerDetails.owner()));
    }

    @Then("owner details list pets in order {string}, {string}")
    public void ownerDetailsListPetsInOrder(String firstPet, String secondPet) {
        assertEquals(List.of(firstPet, secondPet), petNames(ownerDetails));
    }

    @Then("pet {string} visits are chronological")
    public void petVisitsAreChronological(String petName) {
        List<LocalDate> dates = petDetailsByName(petName).visits().stream()
                .map(Visit::visitDate)
                .toList();
        List<LocalDate> sorted = dates.stream().sorted().toList();
        assertEquals(sorted, dates);
    }

    @When("the clinic user updates owner {int} first name to {string} and city to {string}")
    public void clinicUserUpdatesOwner(int ownerId, String firstName, String city) {
        Owner current = updateOwner.findOwnerToUpdate(ownerId).orElseThrow();
        updateOwner.update(new UpdateOwnerCommand(
                ownerId,
                firstName,
                current.lastName(),
                current.address(),
                city,
                current.telephone()));
    }

    @Then("owner {int} details show {string}")
    public void ownerDetailsForIdShow(int ownerId, String fullName) {
        Owner owner = viewOwnerDetails.findDetails(ownerId).orElseThrow().owner();
        assertEquals(fullName, fullName(owner));
    }

    @Then("owner {int} details show city {string}")
    public void ownerDetailsForIdShowCity(int ownerId, String city) {
        Owner owner = viewOwnerDetails.findDetails(ownerId).orElseThrow().owner();
        assertEquals(city, owner.city());
    }

    @When("the clinic user adds pet {string} born {string} of type {string} to owner {int}")
    public void clinicUserAddsPet(String petName, String birthDate, String typeName, int ownerId) {
        lastOwnerId = ownerId;
        addPetToOwner.add(new AddPetCommand(ownerId, petName, LocalDate.parse(birthDate), petType(typeName)));
    }

    @When("the clinic user tries to add duplicate pet {string} to owner {int}")
    public void clinicUserTriesToAddDuplicatePet(String petName, int ownerId) {
        try {
            addPetToOwner.add(new AddPetCommand(ownerId, petName, LocalDate.of(2022, 1, 1), petType("cat")));
        } catch (RuntimeException e) {
            petCommandError = e;
        }
    }

    @Then("owner {int} details include pet {string}")
    public void ownerDetailsIncludePet(int ownerId, String petName) {
        ownerDetails = viewOwnerDetails.findDetails(ownerId).orElseThrow();
        assertTrue(petNames(ownerDetails).contains(petName));
    }

    @Then("the pet command is rejected because the name already exists")
    public void petCommandRejectedBecauseNameExists() {
        assertInstanceOf(DuplicatePetNameException.class, petCommandError);
    }

    @When("the clinic user renames pet {int} of owner {int} to {string}")
    public void clinicUserRenamesPet(int petId, int ownerId, String newName) {
        lastOwnerId = ownerId;
        Pet pet = updatePet.findPetForOwner(ownerId, petId).orElseThrow();
        updatePet.update(new UpdatePetCommand(ownerId, petId, newName, pet.birthDate(), pet.type()));
    }

    @When("the clinic user tries to rename pet {int} of owner {int} to {string}")
    public void clinicUserTriesToRenamePet(int petId, int ownerId, String newName) {
        Pet pet = updatePet.findPetForOwner(ownerId, petId).orElseThrow();
        try {
            updatePet.update(new UpdatePetCommand(ownerId, petId, newName, pet.birthDate(), pet.type()));
        } catch (RuntimeException e) {
            petCommandError = e;
        }
    }

    @When("the clinic user books a visit for owner {int} pet {int} on {string} with description {string}")
    public void clinicUserBooksVisit(int ownerId, int petId, String visitDate, String description) {
        lastOwnerId = ownerId;
        bookVisitForPet.book(new BookVisitCommand(ownerId, petId, LocalDate.parse(visitDate), description));
    }

    @Then("pet {int} visit history includes {string} on {string}")
    public void petVisitHistoryIncludes(int petId, String description, String visitDate) {
        OwnerDetails details = viewOwnerDetails.findDetails(lastOwnerId).orElseThrow();
        boolean found = details.pets().stream()
                .filter(petDetails -> petDetails.pet().id().equals(petId))
                .flatMap(petDetails -> petDetails.visits().stream())
                .anyMatch(visit -> visit.visitDate().equals(LocalDate.parse(visitDate))
                        && visit.description().equals(description));
        assertTrue(found, "Expected visit history to contain " + description);
    }

    @When("the clinic user tries to book a visit for owner {int} pet {int} with blank description")
    public void clinicUserTriesToBookVisitWithBlankDescription(int ownerId, int petId) {
        try {
            bookVisitForPet.book(new BookVisitCommand(ownerId, petId, LocalDate.now(), " "));
        } catch (RuntimeException e) {
            visitCommandError = e;
        }
    }

    @Then("the visit command is rejected because description is required")
    public void visitCommandRejectedBecauseDescriptionRequired() {
        assertInstanceOf(BlankVisitDescriptionException.class, visitCommandError);
    }

    @When("the clinic user prepares a visit for owner {int} and pet {int}")
    public void clinicUserPreparesVisit(int ownerId, int petId) {
        visitBookingForm = bookVisitForPet.prepare(ownerId, petId).orElse(null);
    }

    @Then("the visit form is not available")
    public void visitFormIsNotAvailable() {
        assertNull(visitBookingForm);
    }

    @When("an unexpected error is presented with message {string}")
    public void unexpectedErrorIsPresented(String message) {
        errorPresentation = presentApplicationError.present(new RuntimeException(message), null, 500);
    }

    @When("a not found error is presented with message {string}")
    public void notFoundErrorIsPresented(String message) {
        errorPresentation = presentApplicationError.present(new RuntimeException(message), message, 404);
    }

    @Then("the application error presentation has status {int} and message {string}")
    public void errorPresentationHasStatusAndMessage(int status, String message) {
        assertEquals(status, errorPresentation.httpStatus());
        assertEquals(message, errorPresentation.message());
        assertFalse(errorPresentation.message().contains("\tat "), "Stack traces must not be exposed");
    }

    private static String fullName(Vet vet) {
        return vet.firstName() + " " + vet.lastName();
    }

    private static String fullName(Owner owner) {
        return owner.firstName() + " " + owner.lastName();
    }

    private static List<String> petNames(OwnerDetails details) {
        return details.pets().stream()
                .map(petDetails -> petDetails.pet().name())
                .toList();
    }

    private PetDetails petDetailsByName(String petName) {
        return ownerDetails.pets().stream()
                .filter(details -> details.pet().name().equals(petName))
                .findFirst()
                .orElseThrow();
    }

    private PetType petType(String typeName) {
        return addPetToOwner.availablePetTypes().stream()
                .filter(type -> type.name().equals(typeName))
                .min(Comparator.comparing(PetType::id))
                .orElseThrow();
    }
}
