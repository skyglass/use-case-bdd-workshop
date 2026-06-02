package ai.unifiedprocess.petclinic.owner.domain;

import ai.unifiedprocess.petclinic.core.domain.AggregateRoot;

/**
 * A pet owner registered with the clinic.
 *
 * <p>Owner is the aggregate root for owner registration and contact updates.
 * It owns the behavior that creates and changes owner profile data; persistence
 * adapters only translate it to and from database-shaped PO objects.
 */
public final class Owner implements AggregateRoot<OwnerId> {

    private final OwnerId id;
    private final OwnerName name;
    private final OwnerContact contact;

    private Owner(OwnerId id, OwnerName name, OwnerContact contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
    }

    public static Owner register(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
        return new Owner(null, new OwnerName(firstName, lastName), OwnerContact.of(address, city, telephone));
    }

    public static Owner rehydrate(
            Integer id,
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
        return new Owner(new OwnerId(id), new OwnerName(firstName, lastName), OwnerContact.of(address, city, telephone));
    }

    public Owner assignId(Integer newId) {
        if (id != null) {
            throw new IllegalStateException("Owner already has an id");
        }
        return new Owner(new OwnerId(newId), name, contact);
    }

    public Owner changeProfile(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {
        return new Owner(id, new OwnerName(firstName, lastName), OwnerContact.of(address, city, telephone));
    }

    public Integer id() {
        return id == null ? null : id.value();
    }

    public String firstName() {
        return name.firstName();
    }

    public String lastName() {
        return name.lastName();
    }

    public String address() {
        return contact.address();
    }

    public String city() {
        return contact.city();
    }

    public String telephone() {
        return contact.telephone();
    }

    public String fullName() {
        return name.fullName();
    }
}
