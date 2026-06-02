package ai.unifiedprocess.petclinic.owner.application;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerListing;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

/**
 * UC-004 application service. Owns the search command and its branching
 * decision: no match, exactly one match, or a lazy result list.
 */
@Service
public class FindOwnersByLastNameUseCase {

    private final OwnerRepository ownerRepository;

    public FindOwnersByLastNameUseCase(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public OwnerSearchResult search(String lastNamePrefix) {
        String prefix = normalize(lastNamePrefix);
        int total = ownerRepository.countByLastNamePrefix(prefix);
        Owner singleMatch = total == 1
                ? ownerRepository.findByLastNamePrefix(prefix, 0, 1).findFirst().orElseThrow()
                : null;
        return new OwnerSearchResult(prefix, total, singleMatch);
    }

    public Stream<OwnerListing> findListingPage(String lastNamePrefix, int offset, int limit) {
        return ownerRepository.findListingByLastNamePrefix(normalize(lastNamePrefix), offset, limit);
    }

    private static String normalize(String lastNamePrefix) {
        return lastNamePrefix == null ? "" : lastNamePrefix.trim();
    }

    public record OwnerSearchResult(String prefix, int total, Owner singleMatch) {

        public boolean hasNoMatches() {
            return total == 0;
        }

        public boolean hasSingleMatch() {
            return singleMatch != null;
        }
    }
}
