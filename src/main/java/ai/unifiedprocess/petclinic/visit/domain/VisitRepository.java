package ai.unifiedprocess.petclinic.visit.domain;

import java.util.List;

/**
 * Repository port for the Visit aggregate root.
 */
public interface VisitRepository {

    /** Visits for a pet, ascending by date (UC-005 BR-001). */
    List<Visit> findByPetId(Integer petId);

    Visit save(Visit visit);
}
