package ai.unifiedprocess.petclinic.vet.domain;

import java.util.stream.Stream;

/**
 * Repository port for veterinarian directory data.
 */
public interface VetRepository {

    /**
     * Fetch a page of vets in stable order (last name, first name, id), each
     * with their specialties pre-loaded alphabetically (BR-002).
     */
    Stream<Vet> findPage(int offset, int limit);

    int count();
}
