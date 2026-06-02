package ai.unifiedprocess.petclinic.visit.infrastructure;

import ai.unifiedprocess.petclinic.visit.domain.Visit;

import java.time.LocalDate;

record VisitPO(Integer id, LocalDate visitDate, String description, Integer petId) {

    static VisitPO fromDomain(Visit visit) {
        return new VisitPO(visit.id(), visit.visitDate(), visit.description(), visit.petId());
    }

    Visit toDomain() {
        return Visit.rehydrate(id, visitDate, description, petId);
    }
}
