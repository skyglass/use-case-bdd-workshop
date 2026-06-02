package ai.unifiedprocess.petclinic.vet.domain;

import ai.unifiedprocess.petclinic.core.domain.AggregateRoot;

import java.util.List;

/**
 * Veterinarian directory aggregate root used by the read-only directory use case.
 */
public final class Vet implements AggregateRoot<VetId> {

    private final VetId id;
    private final VetName name;
    private final List<Specialty> specialties;

    private Vet(VetId id, VetName name, List<Specialty> specialties) {
        this.id = id;
        this.name = name;
        this.specialties = List.copyOf(specialties == null ? List.of() : specialties);
    }

    public static Vet rehydrate(Integer id, String firstName, String lastName, List<String> specialties) {
        return new Vet(
                new VetId(id),
                new VetName(firstName, lastName),
                specialties == null ? List.of() : specialties.stream().map(Specialty::new).toList());
    }

    public Integer id() {
        return id.value();
    }

    public String firstName() {
        return name.firstName();
    }

    public String lastName() {
        return name.lastName();
    }

    public List<String> specialties() {
        return specialties.stream()
                .map(Specialty::name)
                .toList();
    }

    /**
     * Human-readable label for the specialties column. See UC-002 main success
     * scenario step 3: a comma-separated list, or {@code "none"} if the vet
     * holds no specialties.
     */
    public String specialtiesLabel() {
        return specialties.isEmpty() ? "none" : String.join(", ", specialties());
    }
}
