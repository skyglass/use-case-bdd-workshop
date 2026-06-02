package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase;
import ai.unifiedprocess.petclinic.pet.application.AddPetToOwnerUseCase.AddPetCommand;
import ai.unifiedprocess.petclinic.pet.domain.DuplicatePetNameException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-007: Add Pet to Owner.
 *
 * <p>Route {@code /owners/:ownerId/pets/new}. Shows the owner's name for
 * context and the shared {@link PetForm}. On successful submit, delegates to
 * the {@link AddPetToOwnerUseCase} and returns to the Owner Details view with
 * the "New Pet has been Added" notification.
 */
@Route(value = "owners/:ownerId/pets/new", layout = MainLayout.class)
@PageTitle("Add Pet")
public class AddPetView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUCCESS_MESSAGE = "New Pet has been Added";

    private final H2 heading;
    private final Paragraph ownerPara;
    private final PetForm petForm;
    private final Button saveButton;
    private final Button cancelButton;

    private final AddPetToOwnerUseCase addPetToOwner;
    private Owner owner;

    public AddPetView(AddPetToOwnerUseCase addPetToOwner) {
        this.addPetToOwner = addPetToOwner;

        setSizeFull();

        heading = new H2("Add New Pet");
        heading.addClassNames(LumoUtility.Margin.NONE);

        ownerPara = new Paragraph();
        ownerPara.addClassNames(LumoUtility.TextColor.SECONDARY);

        petForm = new PetForm(addPetToOwner.availablePetTypes());

        saveButton = new Button("Add Pet", click -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("Cancel", click -> getUI().ifPresent(ui ->
                ui.navigate(OwnerDetailsView.class, OwnerRouteParameters.forOwner(owner.id()))));

        add(heading, ownerPara, petForm, new HorizontalLayout(saveButton, cancelButton));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Integer ownerId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(() -> new NotFoundException("Missing owner id"));
        owner = addPetToOwner.findOwner(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner " + ownerId + " not found"));
        ownerPara.setText("Owner: " + owner.firstName() + " " + owner.lastName());
    }

    private void save() {
        petForm.validateAndRead(null, owner.id(), true).ifPresent(pet -> {
            try {
                addPetToOwner.add(new AddPetCommand(owner.id(), pet.name(), pet.birthDate(), pet.type()));
            } catch (DuplicatePetNameException e) {
                petForm.rejectName("already exists");
                return;
            }
            Notification.show(SUCCESS_MESSAGE);
            getUI().ifPresent(ui -> ui.navigate(
                    OwnerDetailsView.class, OwnerRouteParameters.forOwner(owner.id())));
        });
    }
}
