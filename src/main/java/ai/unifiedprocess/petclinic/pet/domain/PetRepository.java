package ai.unifiedprocess.petclinic.pet.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for the Pet aggregate root and pet type reference data.
 */
public interface PetRepository {

    List<PetType> findAllTypes();

    /** An owner's pets, alphabetically by name (UC-005 BR-002). */
    List<Pet> findByOwnerId(Integer ownerId);

    Optional<Pet> findById(Integer id);

    /**
     * True if the owner already has a pet with the given name (case-insensitive),
     * excluding a pet with {@code excludePetId} (so it can be passed for updates
     * without matching the pet being edited). Pass {@code null} for creation.
     */
    boolean existsByOwnerAndName(Integer ownerId, String name, Integer excludePetId);

    Pet save(Pet pet);
}
