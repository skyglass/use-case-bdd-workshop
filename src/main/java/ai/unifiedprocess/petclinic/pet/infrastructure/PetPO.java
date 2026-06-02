package ai.unifiedprocess.petclinic.pet.infrastructure;

import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetType;

import java.time.LocalDate;

record PetPO(Integer id, String name, LocalDate birthDate, Integer typeId, String typeName, Integer ownerId) {

    static PetPO fromDomain(Pet pet) {
        return new PetPO(
                pet.id(),
                pet.name(),
                pet.birthDate(),
                pet.type().id(),
                pet.type().name(),
                pet.ownerId());
    }

    Pet toDomain() {
        return Pet.rehydrate(id, name, birthDate, new PetType(typeId, typeName), ownerId);
    }
}
