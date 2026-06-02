package ai.unifiedprocess.petclinic.vet.infrastructure;

import ai.unifiedprocess.petclinic.vet.domain.Vet;

import java.util.List;

record VetPO(Integer id, String firstName, String lastName, List<String> specialties) {

    Vet toDomain() {
        return Vet.rehydrate(id, firstName, lastName, specialties);
    }
}
