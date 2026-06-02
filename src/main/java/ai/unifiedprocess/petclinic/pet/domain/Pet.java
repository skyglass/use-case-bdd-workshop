package ai.unifiedprocess.petclinic.pet.domain;

import ai.unifiedprocess.petclinic.core.domain.AggregateRoot;
import ai.unifiedprocess.petclinic.owner.domain.OwnerId;

import java.time.LocalDate;

/**
 * Pet is the aggregate root for adding and updating an animal in an owner context.
 */
public final class Pet implements AggregateRoot<PetId> {

    private final PetId id;
    private final PetName name;
    private final LocalDate birthDate;
    private final PetType type;
    private final OwnerId ownerId;

    private Pet(PetId id, PetName name, LocalDate birthDate, PetType type, OwnerId ownerId) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date is required");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date must not be in the future");
        }
        if (type == null) {
            throw new IllegalArgumentException("Pet type is required");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.type = type;
        this.ownerId = ownerId;
    }

    public static Pet addToOwner(Integer ownerId, String name, LocalDate birthDate, PetType type) {
        return new Pet(null, new PetName(name), birthDate, type, new OwnerId(ownerId));
    }

    public static Pet rehydrate(Integer id, String name, LocalDate birthDate, PetType type, Integer ownerId) {
        return new Pet(new PetId(id), new PetName(name), birthDate, type, new OwnerId(ownerId));
    }

    public Pet assignId(Integer newId) {
        if (id != null) {
            throw new IllegalStateException("Pet already has an id");
        }
        return new Pet(new PetId(newId), name, birthDate, type, ownerId);
    }

    public Pet changeDetails(String newName, LocalDate newBirthDate, PetType newType) {
        return new Pet(id, new PetName(newName), newBirthDate, newType == null ? type : newType, ownerId);
    }

    public boolean belongsTo(Integer candidateOwnerId) {
        return ownerId.value().equals(candidateOwnerId);
    }

    public Integer id() {
        return id == null ? null : id.value();
    }

    public String name() {
        return name.value();
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public PetType type() {
        return type;
    }

    public Integer ownerId() {
        return ownerId.value();
    }
}
