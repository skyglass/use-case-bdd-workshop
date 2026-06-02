package ai.unifiedprocess.petclinic.owner.infrastructure;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerListing;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;

@Repository
class JooqOwnerRepository implements OwnerRepository {

    private final DSLContext dsl;

    JooqOwnerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Stream<Owner> findByLastNamePrefix(String lastNamePrefix, int offset, int limit) {
        return dsl.select(
                        OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                        OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
                .from(OWNERS)
                .where(OWNERS.LAST_NAME.startsWith(normalize(lastNamePrefix)))
                .orderBy(OWNERS.LAST_NAME.asc(), OWNERS.FIRST_NAME.asc(), OWNERS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(OwnerPO::new))
                .stream()
                .map(OwnerPO::toDomain);
    }

    @Override
    public Stream<OwnerListing> findListingByLastNamePrefix(String lastNamePrefix, int offset, int limit) {
        Field<List<String>> petNames = multiset(
                select(PETS.NAME)
                        .from(PETS)
                        .where(PETS.OWNER_ID.eq(OWNERS.ID))
                        .orderBy(PETS.NAME.asc()))
                .convertFrom(r -> r.getValues(PETS.NAME));

        return dsl.select(
                        row(OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                                OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE).mapping(OwnerPO::new),
                        petNames)
                .from(OWNERS)
                .where(OWNERS.LAST_NAME.startsWith(normalize(lastNamePrefix)))
                .orderBy(OWNERS.LAST_NAME.asc(), OWNERS.FIRST_NAME.asc(), OWNERS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping((owner, names) -> new OwnerListing(owner.toDomain(), names)))
                .stream();
    }

    @Override
    public int countByLastNamePrefix(String lastNamePrefix) {
        return dsl.fetchCount(
                dsl.selectFrom(OWNERS)
                        .where(OWNERS.LAST_NAME.startsWith(normalize(lastNamePrefix))));
    }

    @Override
    public Optional<Owner> findById(Integer id) {
        return dsl.select(
                        OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                        OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
                .from(OWNERS)
                .where(OWNERS.ID.eq(id))
                .fetchOptional(mapping(OwnerPO::new))
                .map(OwnerPO::toDomain);
    }

    @Override
    public Owner save(Owner owner) {
        OwnerPO po = OwnerPO.fromDomain(owner);
        if (po.id() == null) {
            Integer id = dsl.insertInto(OWNERS)
                    .set(OWNERS.FIRST_NAME, po.firstName())
                    .set(OWNERS.LAST_NAME, po.lastName())
                    .set(OWNERS.ADDRESS, po.address())
                    .set(OWNERS.CITY, po.city())
                    .set(OWNERS.TELEPHONE, po.telephone())
                    .returning(OWNERS.ID)
                    .fetchOne()
                    .getId();
            return owner.assignId(id);
        }

        dsl.update(OWNERS)
                .set(OWNERS.FIRST_NAME, po.firstName())
                .set(OWNERS.LAST_NAME, po.lastName())
                .set(OWNERS.ADDRESS, po.address())
                .set(OWNERS.CITY, po.city())
                .set(OWNERS.TELEPHONE, po.telephone())
                .where(OWNERS.ID.eq(po.id()))
                .execute();
        return owner;
    }

    private static String normalize(String lastNamePrefix) {
        return lastNamePrefix == null ? "" : lastNamePrefix;
    }
}
