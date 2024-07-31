package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

/**
 * Indicates the result of a comparison in a {@link ValueComparator}.
 * 
 * @author Julijan Katic
 */
public enum ComparatorResult {
	/** 
	 * Tells that the first value is in accordance to the second value according to the
	 * relational operator or trend.
	 */
	IN_ACCORDANCE,
	
	/**
	 * Tells that the first value is not in accordance to the second value according to
	 * the relational operator or trend.
	 */
	DISREGARD,
	
	/** Tells that no conclusion can be made yet (for example, in aggregation). */
	WAIT,
	
	/** Tells that the value provided is not for this trigger. */
	WRONG_TRIGGER,
	
	/** Tells that the trigger uses a different expected value than allowed. */
	WRONG_EXPECTED_VALUE
	;
}
