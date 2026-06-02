package ai.unifiedprocess.petclinic.bdd;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest
@Import({TestcontainersConfiguration.class, CucumberFixtureConfiguration.class})
public class CucumberSpringConfiguration {
}
