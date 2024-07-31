package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.SimpleFireOnTrend;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.ExpectedValue;

/**
 * Creates a comparator based on the triggers {@link SimpleFireOnValue} or {@link SimpleFireOnTrend}.
 * 
 * @author Julijan Katic
 */
public interface ValueComparator {
	
	/**
	 * Compares the first and second value to some comparison semantic. For example, in
	 * the case of {@link SimpleFireOnValue} with the relational operator GREATER_THAN_VALUE,
	 * the result will be {@link ComparatorResult#IN_ACCORDANCE} if firstValue > secondValue,
	 * otherwise it will be {@link ComparatorResult#DISREGARD}.
	 * <br>
	 * If the trigger is a {@link SimpleFireOnTrend}, then the result could also be {@link ComparatorResult#WAIT}
	 * if the threshold for the aggregation has not been reached yet.
	 */
	ComparatorResult compare(final double actualValue, final ExpectedValue expectedValue);
	
	/**
	 * Creates the appropriate instance of the comparator from the trigger.
	 * 
	 * @param trigger
	 * @return
	 */
	static ValueComparator fromTrigger(BaseTrigger trigger) {
		if (trigger instanceof SimpleFireOnValue) {
			return new SimpleFireOnValueComparator((SimpleFireOnValue) trigger);
		} else if (trigger instanceof SimpleFireOnTrend) {
			return new SimpleFireOnTrendComparator((SimpleFireOnTrend) trigger);
		}
		throw new IllegalArgumentException("Only SimpleFireOnValue and SimpleFireOnTrend are supported,"
				+ " but trigger is " + trigger.getClass().getSimpleName());
	}
	
}
