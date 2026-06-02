package ai.unifiedprocess.petclinic.bdd;

import org.jooq.DSLContext;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test-only fixture API for Cucumber scenarios.
 *
 * <p>It is deliberately kept under {@code src/test}. Step definitions use it
 * for reference-data checks and cleanup that are not part of the production
 * application API.
 */
public class PetClinicCucumberFixture {

    private final DSLContext dsl;

    PetClinicCucumberFixture(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void resetPersistentScenarioState() {
        dsl.update(PETS)
                .set(PETS.NAME, "Max")
                .where(PETS.ID.eq(8))
                .execute();
        dsl.update(PETS)
                .set(PETS.NAME, "Samantha")
                .where(PETS.ID.eq(7))
                .execute();
        dsl.deleteFrom(VISITS)
                .where(VISITS.DESCRIPTION.eq("Annual check-up"))
                .execute();
        dsl.deleteFrom(PETS)
                .where(PETS.NAME.eq("Buddy"))
                .execute();
        dsl.deleteFrom(OWNERS)
                .where(OWNERS.LAST_NAME.eq("Whitfield"))
                .execute();
        dsl.update(OWNERS)
                .set(OWNERS.FIRST_NAME, "Harold")
                .set(OWNERS.CITY, "Windsor")
                .where(OWNERS.ID.eq(4))
                .execute();
    }

    public void assertReferenceDataLoaded() {
        assertTrue(dsl.fetchCount(VETS) > 0, "Expected seeded veterinarians");
        assertEquals(10, dsl.fetchCount(OWNERS), "Expected the reference owner dataset");
    }
}
