package ai.unifiedprocess.petclinic.visit.infrastructure;

import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.visit.domain.VisitRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.jooq.Records.mapping;

@Repository
class JooqVisitRepository implements VisitRepository {

    private final DSLContext dsl;

    JooqVisitRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<Visit> findByPetId(Integer petId) {
        return dsl.select(VISITS.ID, VISITS.VISIT_DATE, VISITS.DESCRIPTION, VISITS.PET_ID)
                .from(VISITS)
                .where(VISITS.PET_ID.eq(petId))
                .orderBy(VISITS.VISIT_DATE.asc(), VISITS.ID.asc())
                .fetch(mapping(VisitPO::new))
                .stream()
                .map(VisitPO::toDomain)
                .toList();
    }

    @Override
    public Visit save(Visit visit) {
        VisitPO po = VisitPO.fromDomain(visit);
        if (po.id() != null) {
            throw new IllegalArgumentException("Visits are append-only in this use case");
        }
        Integer id = dsl.insertInto(VISITS)
                .set(VISITS.VISIT_DATE, po.visitDate())
                .set(VISITS.DESCRIPTION, po.description())
                .set(VISITS.PET_ID, po.petId())
                .returning(VISITS.ID)
                .fetchOne()
                .getId();
        return visit.assignId(id);
    }
}
