package ai.unifiedprocess.petclinic.core.api;

import ai.unifiedprocess.petclinic.core.application.PresentApplicationErrorUseCase;
import ai.unifiedprocess.petclinic.core.application.PresentApplicationErrorUseCase.ErrorPresentation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic-experience/view-application-error")
public class ViewApplicationErrorResource {

    private final PresentApplicationErrorUseCase presentApplicationError;

    public ViewApplicationErrorResource(PresentApplicationErrorUseCase presentApplicationError) {
        this.presentApplicationError = presentApplicationError;
    }

    @PostMapping("/present")
    public ErrorPresentation present(@RequestBody PresentApplicationErrorRequest request) {
        int status = request.httpStatus() == null ? 500 : request.httpStatus();
        String message = request.message() == null ? "" : request.message();
        return presentApplicationError.present(new RuntimeException(message), message, status);
    }

    public record PresentApplicationErrorRequest(String message, Integer httpStatus) {
    }
}
