package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.constraint;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.spd.constraints.policy.IntervalConstraint;

public class IntervalConstraintFilter extends AbstractConstraintFilter<IntervalConstraint> {

	/** The sum of the offset and the interval size. */
	private final double delta;
	private final boolean repeat;

	public IntervalConstraintFilter(final IntervalConstraint constraint) {
		super(constraint);
		this.delta = constraint.getOffset() + constraint.getIntervalDuration();
		this.repeat = constraint.isRepeat();
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		final double currentSimulationTime = event.getEventToFilter().time();
		if (!this.isWithinInterval(currentSimulationTime)) {
			return FilterResult.success(event.getEventToFilter());
		} else {
			return FilterResult.disregard(this.getErrorMessage(currentSimulationTime));
		}

	}
	
	/**
	 * Helper method:
	 * 
	 * - If repeat == true, then return true if the simulationTime % (offset + interval)
	 * 	 is greater than the offset, since then the simulationTime will lie somewhere on
	 *   one of the intervals.
	 *   In order to make the upper bound inclusive, we return true as well if the result
	 *   of the modulo (%) operator is 0, (i.e. simulationTime is 5, and (offset + interval)
	 *   is 5, then the result would be 0, but we consider it to be inclusive)
	 *   HOWEVER, the first 0 (i.e. simulationTime == 0) is not allowed!
	 *   
	 * - If repeat == false, then return true iff the simulationTime lies exactly on the
	 *   first interval, i.e. if the simulationTime is equals or greater than the offset AND 
	 *   equals or smaller than (offset + interval). 
	 */
	private boolean isWithinInterval(final double simulationTime) {
		return (this.repeat && 
					simulationTime != 0 && /* Don't allow the first zero */
					(simulationTime % delta == 0 || /* However, allow any subsequent 0s */
					simulationTime % delta >= constraint.getOffset())) ||
			   (!this.repeat && simulationTime >= constraint.getOffset() && simulationTime <= delta);
	}
	
	private String getErrorMessage(final double simulationTime) {
		if (this.repeat) {
			return String.format("The simulation time is inside of the interval or zero: %f in [%f, %f]",
					(simulationTime % delta), constraint.getOffset(), delta);
		} else {
			return String.format("The simulation time is inside of the interval: %f in [%f, %f]", 
					simulationTime, constraint.getOffset(), delta);
		}
	}
}
