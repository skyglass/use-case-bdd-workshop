package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.petclinic.core.application.PresentApplicationErrorUseCase;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UC-010 A2: generic application error view. Catches any uncaught
 * {@link Exception} raised during navigation — including the deliberate
 * throw from {@link CrashView} — and renders an "Something happened..."
 * page inside {@link MainLayout}.
 *
 * <p>More specific errors (currently only {@code NotFoundException}) are
 * handled by {@link NotFoundErrorView}, which wins over this view by
 * virtue of its narrower generic type.
 */
@ParentLayout(MainLayout.class)
public class ApplicationErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationErrorView.class);

    private final ErrorPanel panel;
    private final PresentApplicationErrorUseCase presentApplicationError;

    public ApplicationErrorView(PresentApplicationErrorUseCase presentApplicationError) {
        this.presentApplicationError = presentApplicationError;
        setSizeFull();
        setPadding(false);
        panel = new ErrorPanel();
        add(panel);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        LOGGER.warn("Rendering application error view for navigation failure", parameter.getException());
        String message = parameter.hasCustomMessage()
                ? parameter.getCustomMessage()
                : parameter.getException().getMessage();
        var presentation = presentApplicationError.present(
                parameter.getException(), message, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        panel.setMessage(presentation.message());
        return presentation.httpStatus();
    }
}
