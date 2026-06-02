package ai.unifiedprocess.petclinic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseCase {

	/**
	 * Dash-separated use-case id. This matches the folder name under
	 * docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>
	 * and the Gherkin Feature name. It is not the JIRA ticket id recorded in uc.md.
	 */
	String id();

	String scenario() default "Main Success Scenario";

	String[] businessRules() default {};

}
