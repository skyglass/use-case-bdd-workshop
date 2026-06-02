package ai.unifiedprocess.petclinic.bdd;

import org.jooq.DSLContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CucumberFixtureConfiguration {

    @Bean
    PetClinicCucumberFixture petClinicCucumberFixture(DSLContext dsl) {
        return new PetClinicCucumberFixture(dsl);
    }
}
