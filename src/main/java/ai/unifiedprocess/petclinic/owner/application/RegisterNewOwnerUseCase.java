package ai.unifiedprocess.petclinic.owner.application;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-003 application service. Handles the command that starts the owner
 * registration lifecycle and returns the generated aggregate identifier.
 */
@Service
public class RegisterNewOwnerUseCase {

    private final OwnerRepository ownerRepository;

    public RegisterNewOwnerUseCase(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Transactional
    public RegisteredOwner register(RegisterOwnerCommand command) {
        Owner owner = Owner.register(
                command.firstName(),
                command.lastName(),
                command.address(),
                command.city(),
                command.telephone());
        Owner registeredOwner = ownerRepository.save(owner);
        return new RegisteredOwner(registeredOwner.id());
    }

    public record RegisterOwnerCommand(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
    }

    public record RegisteredOwner(Integer ownerId) {
    }
}
