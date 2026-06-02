package ai.unifiedprocess.petclinic.owner.api;

import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase;
import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase.OwnerDetails;
import ai.unifiedprocess.petclinic.owner.application.ViewOwnerDetailsUseCase.PetDetails;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner-management/view-owner-details")
public class ViewOwnerDetailsResource {

    private final ViewOwnerDetailsUseCase viewOwnerDetails;

    public ViewOwnerDetailsResource(ViewOwnerDetailsUseCase viewOwnerDetails) {
        this.viewOwnerDetails = viewOwnerDetails;
    }

    @GetMapping("/view/{ownerId}")
    public ViewOwnerDetailsResponse view(@PathVariable Integer ownerId) {
        return viewOwnerDetails.findDetails(ownerId)
                .map(ViewOwnerDetailsResponse::fromDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner " + ownerId + " not found"));
    }

    public record ViewOwnerDetailsResponse(OwnerResponse owner, List<PetDetailsResponse> pets) {

        static ViewOwnerDetailsResponse fromDomain(OwnerDetails details) {
            return new ViewOwnerDetailsResponse(
                    OwnerResponse.fromDomain(details.owner()),
                    details.pets().stream().map(PetDetailsResponse::fromDomain).toList());
        }
    }

    public record OwnerResponse(
            Integer id,
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {

        static OwnerResponse fromDomain(Owner owner) {
            return new OwnerResponse(
                    owner.id(),
                    owner.firstName(),
                    owner.lastName(),
                    owner.address(),
                    owner.city(),
                    owner.telephone());
        }
    }

    public record PetDetailsResponse(PetResponse pet, List<VisitResponse> visits) {

        static PetDetailsResponse fromDomain(PetDetails details) {
            return new PetDetailsResponse(
                    PetResponse.fromDomain(details.pet()),
                    details.visits().stream().map(VisitResponse::fromDomain).toList());
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
}
