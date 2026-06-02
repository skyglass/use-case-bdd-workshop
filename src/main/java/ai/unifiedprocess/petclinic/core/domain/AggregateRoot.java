package ai.unifiedprocess.petclinic.core.domain;

/**
 * Marker for the domain entity that defines a transactional consistency boundary.
 */
public interface AggregateRoot<ID extends ValueObject> extends DomainEntity<ID> {
}
