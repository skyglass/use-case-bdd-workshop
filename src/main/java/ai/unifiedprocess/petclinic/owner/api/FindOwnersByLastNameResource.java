package ai.unifiedprocess.petclinic.owner.api;

import ai.unifiedprocess.petclinic.owner.application.FindOwnersByLastNameUseCase;
import ai.unifiedprocess.petclinic.owner.application.FindOwnersByLastNameUseCase.OwnerSearchResult;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerListing;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner-management/find-owners-by-last-name")
public class FindOwnersByLastNameResource {

    private final FindOwnersByLastNameUseCase findOwnersByLastName;

    public FindOwnersByLastNameResource(FindOwnersByLastNameUseCase findOwnersByLastName) {
        this.findOwnersByLastName = findOwnersByLastName;
    }

    @GetMapping("/search")
    public FindOwnersByLastNameResponse find(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        OwnerSearchResult result = findOwnersByLastName.search(prefix);
        List<OwnerListingResponse> owners = findOwnersByLastName.findListingPage(prefix, offset, limit)
                .map(OwnerListingResponse::fromDomain)
                .toList();
        return new FindOwnersByLastNameResponse(
                result.prefix(),
                result.total(),
                result.hasNoMatches(),
                result.singleMatch() == null ? null : OwnerResponse.fromDomain(result.singleMatch()),
                owners);
    }

    public record FindOwnersByLastNameResponse(
            String prefix,
            int total,
            boolean noMatches,
            OwnerResponse singleMatch,
            List<OwnerListingResponse> owners) {
    }

    public record OwnerListingResponse(OwnerResponse owner, List<String> petNames) {

        static OwnerListingResponse fromDomain(OwnerListing listing) {
            return new OwnerListingResponse(OwnerResponse.fromDomain(listing.owner()), listing.petNames());
        }
    }

    public record OwnerResponse(
            Integer id,
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone) {

        static OwnerResponse fromDomain(Owner owner) {
            return new OwnerResponse(
                    owner.id(),
                    owner.firstName(),
                    owner.lastName(),
                    owner.address(),
                    owner.city(),
                    owner.telephone());
        }
    }
}
