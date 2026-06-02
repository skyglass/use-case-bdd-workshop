package ai.unifiedprocess.petclinic.owner.api;

import ai.unifiedprocess.petclinic.owner.application.UpdateOwnerUseCase;
import ai.unifiedprocess.petclinic.owner.application.UpdateOwnerUseCase.UpdateOwnerCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner-management/update-owner")
public class UpdateOwnerResource {

    private final UpdateOwnerUseCase updateOwner;

    public UpdateOwnerResource(UpdateOwnerUseCase updateOwner) {
        this.updateOwner = updateOwner;
    }

    @PutMapping("/update/{ownerId}")
    public ResponseEntity<Void> update(
            @PathVariable Integer ownerId,
            @RequestBody UpdateOwnerRequest request) {
        updateOwner.update(new UpdateOwnerCommand(
                ownerId,
                request.firstName(),
                request.lastName(),
                request.address(),
                request.city(),
                request.telephone()));
        return ResponseEntity.noContent().build();
    }

    public record UpdateOwnerRequest(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
    }
}
