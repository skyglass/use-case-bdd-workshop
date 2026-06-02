package ai.unifiedprocess.petclinic.core.application;

import org.springframework.stereotype.Service;

/**
 * UC-010 application service. Converts router failures into the error
 * presentation model consumed by the Vaadin error views.
 */
@Service
public class PresentApplicationErrorUseCase {

    public ErrorPresentation present(Throwable exception, String customMessage, int httpStatus) {
        String message = customMessage != null && !customMessage.isBlank()
                ? customMessage
                : exception == null ? "" : exception.getMessage();
        String safeMessage = message == null ? "" : message;
        return new ErrorPresentation("Something happened...", safeMessage, httpStatus);
    }

    public record ErrorPresentation(String heading, String message, int httpStatus) {
    }
}
