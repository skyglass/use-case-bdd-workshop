package ai.unifiedprocess.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.core.ApplicationModules;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AiupPetclinicApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verifiesSpringModulithBoundaries() {
        ApplicationModules.of(AiupPetclinicApplication.class).verify();
    }

}
