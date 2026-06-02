package ai.unifiedprocess.petclinic.welcome.api;

import ai.unifiedprocess.petclinic.welcome.application.ViewWelcomePageUseCase;
import ai.unifiedprocess.petclinic.welcome.application.ViewWelcomePageUseCase.WelcomePage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic-experience/view-welcome-page")
public class ViewWelcomePageResource {

    private final ViewWelcomePageUseCase viewWelcomePage;

    public ViewWelcomePageResource(ViewWelcomePageUseCase viewWelcomePage) {
        this.viewWelcomePage = viewWelcomePage;
    }

    @GetMapping("/view")
    public WelcomePage view() {
        return viewWelcomePage.view();
    }
}
