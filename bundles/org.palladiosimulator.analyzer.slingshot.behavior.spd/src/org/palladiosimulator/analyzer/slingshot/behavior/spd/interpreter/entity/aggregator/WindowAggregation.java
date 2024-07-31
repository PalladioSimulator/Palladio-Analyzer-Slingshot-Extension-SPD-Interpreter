package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import org.palladiosimulator.spd.triggers.stimuli.ManagedElementsStateStimulus;

/**
 * The WindowAggregation interface defines the contract of how aggregations are
 * supposed to be processed in context of SPD.
 * 
 * A ScalingPolicy applies to a TargetGroup which in PCM can be a set of
 * ResourceContainers or AssemblyCtxts. If the policy uses triggers that as
 * stimulus use {@link ManagedElementsStateStimulus}, for example a
 * CPUUtilizationTrigger, then several measurements source from elements that
 * are within the scope of the TargetGroup.
 * 
 * This interface provides three methods that are necessary to provide a
 * meaningful workflow of handling the described case.
 * 
 * First, {@link #aggregate(double, double)} allows to share to the
 * WindowAggregator a new value which can be sourced from one of the elements.
 * When that value is passed to the WindowAggregation, then the policy looses
 * track of the individual identity of the measurement (or its source identity).
 * 
 * The {@link #isEmittable()} allows for the client (i.e., a TriggerChecker) to
 * check whether it is worth emitting an aggregate value for analysis. An
 * implementation could use this method to implement rate limitting behavior
 * (e.g., emitting a value every 10 seconds) or to constraint the emitting based
 * on the number of datapoints in the window.
 * 
 * The {@link #getCurrentValue()} allows to retrieve the current aggregated
 * value. The method must throw an AssertionError in case {@link #isEmittable()}
 * returns false.
 * 
 * @author Floriment Klinaku
 *
 */
public sealed interface WindowAggregation permits AbstractWindowAggregation {

	/**
	 * This method should return the current aggregated value. The implementation
	 * should only yield a value when the emittable condition evaluates true.
	 * Otherwise an AssertionError is thrown.
	 * 
	 * @return The aggregated value.
	 */
	public double getCurrentValue() throws AssertionError;

	/**
	 * Returns whether the window is full or whether some measurements are still
	 * missing.
	 * 
	 * This way a trigger can check whether enough data is in the aggregation.
	 * 
	 * @return true iff the window is full.
	 */
	public boolean isEmittable();

	/**
	 * This method should allow an implementation to include a new datapoint.
	 * 
	 * @param time
	 * @param newValue
	 * @return The aggregated value after the newValue has been included.
	 */
	public double aggregate(double time, double newValue);

}