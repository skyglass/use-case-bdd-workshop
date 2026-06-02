package ai.unifiedprocess.petclinic.visit.domain;

import org.springframework.stereotype.Service;

/**
 * Domain service for UC-009 BR-001.
 */
@Service
public class VisitDescriptionPolicy {

    public void requireDescription(String description) {
        new VisitDescription(description);
    }
}
