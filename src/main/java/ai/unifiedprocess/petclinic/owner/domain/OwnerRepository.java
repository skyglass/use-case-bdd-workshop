package ai.unifiedprocess.petclinic.owner.domain;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository port for the Owner aggregate root.
 */
public interface OwnerRepository {

    /**
     * Prefix match on last name (BR-001 of UC-004). An empty prefix returns
     * every owner (BR-003 of UC-004). Results are ordered for stable lazy
     * loading.
     */
    Stream<Owner> findByLastNamePrefix(String lastNamePrefix, int offset, int limit);

    /**
     * Prefix match on last name with each owner's pet names pre-joined in a
     * single query (UC-004 main flow). Avoids the N+1 that a per-row
     * {@link ai.unifiedprocess.petclinic.pet.domain.PetRepository} lookup
     * would cause in the lazy-loaded results grid. Pet names are alphabetical
     * to match {@code PetRepository.findByOwnerId}.
     */
    Stream<OwnerListing> findListingByLastNamePrefix(String lastNamePrefix, int offset, int limit);

    int countByLastNamePrefix(String lastNamePrefix);

    Optional<Owner> findById(Integer id);

    Owner save(Owner owner);
}
