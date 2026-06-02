package ai.unifiedprocess.petclinic.owner.api;

import ai.unifiedprocess.petclinic.owner.application.RegisterNewOwnerUseCase;
import ai.unifiedprocess.petclinic.owner.application.RegisterNewOwnerUseCase.RegisterOwnerCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/owner-management/register-new-owner")
public class RegisterNewOwnerResource {

    private final RegisterNewOwnerUseCase registerNewOwner;

    public RegisterNewOwnerResource(RegisterNewOwnerUseCase registerNewOwner) {
        this.registerNewOwner = registerNewOwner;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterNewOwnerResponse> register(@RequestBody RegisterNewOwnerRequest request) {
        Integer ownerId = registerNewOwner.register(new RegisterOwnerCommand(
                request.firstName(),
                request.lastName(),
                request.address(),
                request.city(),
                request.telephone())).ownerId();
        return ResponseEntity.created(URI.create("/api/owner-management/view-owner-details/view/" + ownerId))
                .body(new RegisterNewOwnerResponse(ownerId));
    }

    public record RegisterNewOwnerRequest(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
    }

    public record RegisterNewOwnerResponse(Integer ownerId) {
    }
}
