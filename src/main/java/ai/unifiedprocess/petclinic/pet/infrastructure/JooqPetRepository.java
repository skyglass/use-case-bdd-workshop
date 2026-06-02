package ai.unifiedprocess.petclinic.pet.infrastructure;

import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.TYPES;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.lower;

@Repository
class JooqPetRepository implements PetRepository {

    private final DSLContext dsl;

    JooqPetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<PetType> findAllTypes() {
        return dsl.select(TYPES.ID, TYPES.NAME)
                .from(TYPES)
                .orderBy(TYPES.NAME.asc())
                .fetch(mapping(PetTypePO::new))
                .stream()
                .map(PetTypePO::toDomain)
                .toList();
    }

    @Override
    public List<Pet> findByOwnerId(Integer ownerId) {
        return dsl.select(
                        PETS.ID, PETS.NAME, PETS.BIRTH_DATE,
                        TYPES.ID, TYPES.NAME,
                        PETS.OWNER_ID)
                .from(PETS)
                .join(TYPES).on(TYPES.ID.eq(PETS.TYPE_ID))
                .where(PETS.OWNER_ID.eq(ownerId))
                .orderBy(PETS.NAME.asc())
                .fetch(mapping(PetPO::new))
                .stream()
                .map(PetPO::toDomain)
                .toList();
    }

    @Override
    public Optional<Pet> findById(Integer id) {
        return dsl.select(
                        PETS.ID, PETS.NAME, PETS.BIRTH_DATE,
                        TYPES.ID, TYPES.NAME,
                        PETS.OWNER_ID)
                .from(PETS)
                .join(TYPES).on(TYPES.ID.eq(PETS.TYPE_ID))
                .where(PETS.ID.eq(id))
                .fetchOptional(mapping(PetPO::new))
                .map(PetPO::toDomain);
    }

    @Override
    public boolean existsByOwnerAndName(Integer ownerId, String name, Integer excludePetId) {
        var condition = PETS.OWNER_ID.eq(ownerId)
                .and(lower(PETS.NAME).eq(name == null ? "" : name.toLowerCase()));
        if (excludePetId != null) {
            condition = condition.and(PETS.ID.ne(excludePetId));
        }
        return dsl.fetchExists(dsl.selectFrom(PETS).where(condition));
    }

    @Override
    public Pet save(Pet pet) {
        PetPO po = PetPO.fromDomain(pet);
        if (po.id() == null) {
            Integer id = dsl.insertInto(PETS)
                    .set(PETS.NAME, po.name())
                    .set(PETS.BIRTH_DATE, po.birthDate())
                    .set(PETS.TYPE_ID, po.typeId())
                    .set(PETS.OWNER_ID, po.ownerId())
                    .returning(PETS.ID)
                    .fetchOne()
                    .getId();
            return pet.assignId(id);
        }

        dsl.update(PETS)
                .set(PETS.NAME, po.name())
                .set(PETS.BIRTH_DATE, po.birthDate())
                .set(PETS.TYPE_ID, po.typeId())
                .where(PETS.ID.eq(po.id()))
                .execute();
        return pet;
    }
}
