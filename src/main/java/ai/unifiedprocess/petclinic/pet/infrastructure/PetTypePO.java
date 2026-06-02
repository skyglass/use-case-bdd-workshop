package ai.unifiedprocess.petclinic.pet.infrastructure;

import ai.unifiedprocess.petclinic.pet.domain.PetType;

record PetTypePO(Integer id, String name) {

    PetType toDomain() {
        return new PetType(id, name);
    }
}
