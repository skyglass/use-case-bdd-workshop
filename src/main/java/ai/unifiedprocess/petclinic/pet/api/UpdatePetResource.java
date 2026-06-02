package ai.unifiedprocess.petclinic.pet.api;

import ai.unifiedprocess.petclinic.pet.application.UpdatePetUseCase;
import ai.unifiedprocess.petclinic.pet.application.UpdatePetUseCase.UpdatePetCommand;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/pet-management/update-pet")
public class UpdatePetResource {

    private final UpdatePetUseCase updatePet;

    public UpdatePetResource(UpdatePetUseCase updatePet) {
        this.updatePet = updatePet;
    }

    @PutMapping("/update/{ownerId}/{petId}")
    public ResponseEntity<Void> update(
            @PathVariable Integer ownerId,
            @PathVariable Integer petId,
            @RequestBody UpdatePetRequest request) {
        Pet current = updatePet.findPetForOwner(ownerId, petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pet " + petId + " does not belong to owner " + ownerId));
        PetType type = request.typeId() == null ? current.type() : petType(request.typeId());
        updatePet.update(new UpdatePetCommand(ownerId, petId, request.name(), request.birthDate(), type));
        return ResponseEntity.noContent().build();
    }

    private PetType petType(Integer typeId) {
        return updatePet.availablePetTypes().stream()
                .filter(type -> type.id().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown pet type " + typeId));
    }

    public record UpdatePetRequest(String name, LocalDate birthDate, Integer typeId) {
    }
}
