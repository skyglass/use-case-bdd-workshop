package ai.unifiedprocess.petclinic.vet.application;

import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

/**
 * UC-002 application service for browsing veterinarians.
 */
@Service
public class ViewVeterinariansUseCase {

    private final VetRepository vetRepository;

    public ViewVeterinariansUseCase(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
    }

    public Stream<Vet> findPage(int offset, int limit) {
        return vetRepository.findPage(offset, limit);
    }

    public int count() {
        return vetRepository.count();
    }
}
