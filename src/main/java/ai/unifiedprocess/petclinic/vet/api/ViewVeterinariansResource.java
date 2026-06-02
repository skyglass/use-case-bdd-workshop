package ai.unifiedprocess.petclinic.vet.api;

import ai.unifiedprocess.petclinic.vet.application.ViewVeterinariansUseCase;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/veterinary-directory/view-veterinarians")
public class ViewVeterinariansResource {

    private final ViewVeterinariansUseCase viewVeterinarians;

    public ViewVeterinariansResource(ViewVeterinariansUseCase viewVeterinarians) {
        this.viewVeterinarians = viewVeterinarians;
    }

    @GetMapping("/view")
    public ViewVeterinariansResponse view(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return new ViewVeterinariansResponse(
                viewVeterinarians.count(),
                viewVeterinarians.findPage(offset, limit).map(VeterinarianResponse::fromDomain).toList());
    }

    public record ViewVeterinariansResponse(int total, List<VeterinarianResponse> veterinarians) {
    }

    public record VeterinarianResponse(Integer id, String firstName, String lastName, List<String> specialties) {

        static VeterinarianResponse fromDomain(Vet vet) {
            return new VeterinarianResponse(vet.id(), vet.firstName(), vet.lastName(), vet.specialties());
        }
    }
}
