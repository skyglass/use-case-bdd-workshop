package ai.unifiedprocess.petclinic.visit.api;

import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.visit.application.BookVisitForPetUseCase;
import ai.unifiedprocess.petclinic.visit.application.BookVisitForPetUseCase.BookVisitCommand;
import ai.unifiedprocess.petclinic.visit.application.BookVisitForPetUseCase.VisitBookingForm;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visit-management/book-visit-for-pet")
public class BookVisitForPetResource {

    private final BookVisitForPetUseCase bookVisitForPet;

    public BookVisitForPetResource(BookVisitForPetUseCase bookVisitForPet) {
        this.bookVisitForPet = bookVisitForPet;
    }

    @GetMapping("/prepare/{ownerId}/{petId}")
    public VisitBookingFormResponse prepare(@PathVariable Integer ownerId, @PathVariable Integer petId) {
        return bookVisitForPet.prepare(ownerId, petId)
                .map(VisitBookingFormResponse::fromDomain)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pet " + petId + " does not belong to owner " + ownerId));
    }

    @PostMapping("/book")
    public ResponseEntity<BookVisitForPetResponse> book(@RequestBody BookVisitForPetRequest request) {
        Integer visitId = bookVisitForPet.book(new BookVisitCommand(
                request.ownerId(),
                request.petId(),
                request.visitDate(),
                request.description())).visitId();
        return ResponseEntity.created(URI.create("/api/owner-management/view-owner-details/view/" + request.ownerId()))
                .body(new BookVisitForPetResponse(visitId));
    }

    public record VisitBookingFormResponse(PetResponse pet, List<VisitResponse> previousVisits) {

        static VisitBookingFormResponse fromDomain(VisitBookingForm form) {
            return new VisitBookingFormResponse(
                    PetResponse.fromDomain(form.pet()),
                    form.previousVisits().stream().map(VisitResponse::fromDomain).toList());
        }
    }

    public record PetResponse(Integer id, String name, LocalDate birthDate, String type, Integer ownerId) {

        static PetResponse fromDomain(Pet pet) {
            return new PetResponse(pet.id(), pet.name(), pet.birthDate(), pet.type().name(), pet.ownerId());
        }
    }

    public record VisitResponse(Integer id, LocalDate visitDate, String description, Integer petId) {

        static VisitResponse fromDomain(Visit visit) {
            return new VisitResponse(visit.id(), visit.visitDate(), visit.description(), visit.petId());
        }
    }

    public record BookVisitForPetRequest(Integer ownerId, Integer petId, LocalDate visitDate, String description) {
    }

    public record BookVisitForPetResponse(Integer visitId) {
    }
}
