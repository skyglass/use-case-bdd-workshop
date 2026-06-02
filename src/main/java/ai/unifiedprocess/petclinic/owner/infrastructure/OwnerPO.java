package ai.unifiedprocess.petclinic.owner.infrastructure;

import ai.unifiedprocess.petclinic.owner.domain.Owner;

record OwnerPO(
        Integer id,
        String firstName,
        String lastName,
        String address,
        String city,
        String telephone) {

    static OwnerPO fromDomain(Owner owner) {
        return new OwnerPO(
                owner.id(),
                owner.firstName(),
                owner.lastName(),
                owner.address(),
                owner.city(),
                owner.telephone());
    }

    Owner toDomain() {
        return Owner.rehydrate(id, firstName, lastName, address, city, telephone);
    }
}
