package ai.unifiedprocess.petclinic.owner.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record OwnerContact(String address, String city, TelephoneNumber telephoneNumber) implements ValueObject {

    public OwnerContact {
        address = requireText(address, "Address");
        city = requireText(city, "City");
        if (telephoneNumber == null) {
            throw new IllegalArgumentException("Telephone is required");
        }
    }

    public static OwnerContact of(String address, String city, String telephone) {
        return new OwnerContact(address, city, new TelephoneNumber(telephone));
    }

    public String telephone() {
        return telephoneNumber.value();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
