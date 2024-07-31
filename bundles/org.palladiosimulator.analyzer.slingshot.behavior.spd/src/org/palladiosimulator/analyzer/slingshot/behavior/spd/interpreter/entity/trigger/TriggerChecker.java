package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.Filter;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.SimpleFireOnTrend;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.NoExpectation;
import org.palladiosimulator.spd.triggers.expectations.ExpectedPrimitive;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

import com.google.common.base.Preconditions;

/**
 * A checker that should compare the values or simulation state to
 * the expected value. This abstract class provides a utility method
 * {@link #compareToTrigger(double)} to compare the current measured
 * value to the expected value.
 * 
 * @author Julijan Katic
 *
 * @param <T> The stimulus of the trigger
 */
public abstract class TriggerChecker<T extends Stimulus> implements Filter {

	protected final ValueComparator valueComparator;
	protected final BaseTrigger trigger;
	private final Set<Class<? extends ExpectedPrimitive>> allowedExpectedPrimitives;
	
	/**
	 * Constructor of the Trigger checker.
	 * 
	 * This constructor also requires a set of allowed {@link ExpectedPrimitive}.
	 * If the provided SPD uses a different {@link ExpectedPrimitive}, the trigger
	 * should abort.
	 * 
	 * @param trigger 					The actual trigger to use for checking.
	 * @param stimulusType 				The stimulus type.
	 * @param allowedExpectedPrimitives Set of all allowed expected primitives for this trigger.
	 */
	TriggerChecker(final BaseTrigger trigger, 
				   final Class<T> stimulusType,
				   final Set<Class<? extends ExpectedPrimitive>> allowedExpectedPrimitives) {
		this.valueComparator = ValueComparator.fromTrigger(trigger);
		this.trigger = trigger;
		this.allowedExpectedPrimitives = allowedExpectedPrimitives;
		Preconditions.checkArgument(stimulusType.isAssignableFrom(trigger.getStimulus().getClass()), String.format("Trigger must be of type %s, but is %s", stimulusType.getSimpleName(), trigger.getStimulus().getClass().getSimpleName()));
	}
	
	@SuppressWarnings("unchecked")
	protected T getStimulus() {
		return (T) trigger.getStimulus();
	}
	
	/**
	 * Utility method to compare the defined expected value to the
	 * actual {@code value}. Returns a comparator result to be used
	 * for further processing (see {@link ComparatorResult} for further
	 * description).
	 * 
	 * @param value The actual, measured value to compare.
	 * @return The comparator result.
	 */
	protected ComparatorResult compareToTrigger(final double value) {
		if (trigger instanceof SimpleFireOnValue) {
			if (trigger.getExpectedValue() instanceof NoExpectation) {
				return ComparatorResult.IN_ACCORDANCE;
			} else if (allowedExpectedPrimitives.stream().anyMatch(cls -> cls.isAssignableFrom(trigger.getExpectedValue().getClass()))) {
				return this.valueComparator.compare(value, trigger.getExpectedValue());
			}
			
			return ComparatorResult.WRONG_EXPECTED_VALUE;
		} else if (trigger instanceof SimpleFireOnTrend) {
			return this.valueComparator.compare(value, trigger.getExpectedValue());
		}
		
		return ComparatorResult.WRONG_TRIGGER;
	}
	
	
}
