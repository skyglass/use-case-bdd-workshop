package ai.unifiedprocess.petclinic.vet.infrastructure;

import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

import static ai.unifiedprocess.demo.petclinic.database.Tables.*;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@Repository
class JooqVetRepository implements VetRepository {

    private final DSLContext dsl;

    JooqVetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Stream<Vet> findPage(int offset, int limit) {
        Field<List<String>> specialties =
                multiset(
                        select(SPECIALTIES.NAME)
                                .from(VET_SPECIALTIES)
                                .join(SPECIALTIES).on(SPECIALTIES.ID.eq(VET_SPECIALTIES.SPECIALTY_ID))
                                .where(VET_SPECIALTIES.VET_ID.eq(VETS.ID))
                                .orderBy(SPECIALTIES.NAME.asc()))
                        .convertFrom(r -> r.getValues(SPECIALTIES.NAME));

        return dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, specialties)
                .from(VETS)
                .orderBy(VETS.LAST_NAME.asc(), VETS.FIRST_NAME.asc(), VETS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(VetPO::new))
                .stream()
                .map(VetPO::toDomain);
    }

    @Override
    public int count() {
        return dsl.fetchCount(VETS);
    }
}
