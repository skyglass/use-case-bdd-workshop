package ai.unifiedprocess.petclinic.welcome.application;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UC-001 application service for the static welcome-page slice.
 */
@Service
public class ViewWelcomePageUseCase {

    public WelcomePage view() {
        return new WelcomePage(
                "images/pets.png",
                List.of("Home", "Find Owners", "Veterinarians", "Error"));
    }

    public record WelcomePage(String decorativeImagePath, List<String> navigationLabels) {
    }
}
