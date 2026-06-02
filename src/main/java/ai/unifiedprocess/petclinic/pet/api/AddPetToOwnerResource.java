package ai.unifiedprocess.petclinic.pet.api;

import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase;
import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase.AddPetCommand;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/pet-management/add-pet-to-owner")
public class AddPetToOwnerResource {

    private final AddPetToOwnerUseCase addPetToOwner;

    public AddPetToOwnerResource(AddPetToOwnerUseCase addPetToOwner) {
        this.addPetToOwner = addPetToOwner;
    }

    @PostMapping("/add")
    public ResponseEntity<AddPetToOwnerResponse> add(@RequestBody AddPetToOwnerRequest request) {
        Integer petId = addPetToOwner.add(new AddPetCommand(
                request.ownerId(),
                request.name(),
                request.birthDate(),
                petType(request.typeId()))).petId();
        return ResponseEntity.created(URI.create("/api/owner-management/view-owner-details/view/" + request.ownerId()))
                .body(new AddPetToOwnerResponse(petId));
    }

    private PetType petType(Integer typeId) {
        return addPetToOwner.availablePetTypes().stream()
                .filter(type -> type.id().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown pet type " + typeId));
    }

    public record AddPetToOwnerRequest(Integer ownerId, String name, LocalDate birthDate, Integer typeId) {
    }

    public record AddPetToOwnerResponse(Integer petId) {
    }
}
