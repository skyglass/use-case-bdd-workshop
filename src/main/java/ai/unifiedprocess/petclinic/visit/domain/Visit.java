package ai.unifiedprocess.petclinic.visit.domain;

import ai.unifiedprocess.petclinic.core.domain.AggregateRoot;
import ai.unifiedprocess.petclinic.pet.domain.PetId;

import java.time.LocalDate;

/**
 * Visit is the aggregate root for recording an appointment against a pet.
 */
public final class Visit implements AggregateRoot<VisitId> {

    private final VisitId id;
    private final LocalDate visitDate;
    private final VisitDescription description;
    private final PetId petId;

    private Visit(VisitId id, LocalDate visitDate, VisitDescription description, PetId petId) {
        if (visitDate == null) {
            throw new IllegalArgumentException("Visit date is required");
        }
        if (description == null) {
            throw new IllegalArgumentException("Visit description is required");
        }
        if (petId == null) {
            throw new IllegalArgumentException("Pet id is required");
        }
        this.id = id;
        this.visitDate = visitDate;
        this.description = description;
        this.petId = petId;
    }

    public static Visit bookForPet(Integer petId, LocalDate visitDate, String description) {
        LocalDate effectiveDate = visitDate == null ? LocalDate.now() : visitDate;
        return new Visit(null, effectiveDate, new VisitDescription(description), new PetId(petId));
    }

    public static Visit rehydrate(Integer id, LocalDate visitDate, String description, Integer petId) {
        return new Visit(new VisitId(id), visitDate, new VisitDescription(description), new PetId(petId));
    }

    public Visit assignId(Integer newId) {
        if (id != null) {
            throw new IllegalStateException("Visit already has an id");
        }
        return new Visit(new VisitId(newId), visitDate, description, petId);
    }

    public Integer id() {
        return id == null ? null : id.value();
    }

    public LocalDate visitDate() {
        return visitDate;
    }

    public String description() {
        return description.value();
    }

    public Integer petId() {
        return petId.value();
    }
}
