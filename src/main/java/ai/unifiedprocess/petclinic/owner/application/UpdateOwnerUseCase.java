package ai.unifiedprocess.petclinic.owner.application;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UC-006 application service for loading and updating owner contact data.
 */
@Service
public class UpdateOwnerUseCase {

    private final OwnerRepository ownerRepository;

    public UpdateOwnerUseCase(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public Optional<Owner> findOwnerToUpdate(Integer ownerId) {
        return ownerRepository.findById(ownerId);
    }

    @Transactional
    public void update(UpdateOwnerCommand command) {
        Owner owner = ownerRepository.findById(command.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner " + command.ownerId() + " not found"));
        Owner changed = owner.changeProfile(
                command.firstName(),
                command.lastName(),
                command.address(),
                command.city(),
                command.telephone());
        ownerRepository.save(changed);
    }

    public record UpdateOwnerCommand(
            Integer ownerId,
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
    }
}
