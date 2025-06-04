/**
 *
 */
package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.constraint;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.TargetGroupState;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.adjustments.AbsoluteAdjustment;
import org.palladiosimulator.spd.adjustments.AdjustmentType;
import org.palladiosimulator.spd.adjustments.RelativeAdjustment;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
import org.palladiosimulator.spd.constraints.target.ThrashingConstraint;

/**
 * The thrashing constraint filter makes sure that there is a certain amount of
 * time passed since the last enacted policy that adjusted the amount of
 * resources in the opposite direction in comparison to the one being enacted.
 *
 * @author Floriment Klinaku
 *
 */
public final class ThrashingConstraintFilter extends AbstractConstraintFilter<ThrashingConstraint> {

	private enum ADJUSTMENT_SIGN {
		POSITIVE,
		NEGATIVE
	}


	public ThrashingConstraintFilter(final ThrashingConstraint constraint) {
		super(constraint);
		// TODO Auto-generated constructor stub
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {

		final TargetGroupState targetGroupState = event.getState().getTargetGroupState();

		if(targetGroupState.enactedPoliciesEmpty()) {
			//no previously executed policies, thrashing passes
			return FilterResult.success(event.getEventToFilter());
		}

		final ScalingPolicy lastEnactedPolicy = targetGroupState.getLastEnactedScalingPolicy();
		final double lastSimulationTime = targetGroupState.getLastScalingPolicyEnactmentTime();
		final double currentSimulationTime = event.getEventToFilter().time();

		final ScalingPolicy currentScalingPolicy = event.getState().getScalingPolicy();

		if(currentScalingPolicy.getAdjustmentType() instanceof AbsoluteAdjustment
				|| lastEnactedPolicy.getAdjustmentType() instanceof AbsoluteAdjustment) {
			return FilterResult.success(event.getEventToFilter());
		}

		if(!retrieveSign(currentScalingPolicy.getAdjustmentType()).equals(retrieveSign(lastEnactedPolicy.getAdjustmentType()))
				&& lastSimulationTime + constraint.getMinimumTimeNoThrashing() >= currentSimulationTime) {
			// opposite signs and min time did not pass -> disregard
			return FilterResult.disregard("Thrashing Constraint prevents this scaling Operation.");
		}
		return FilterResult.success(event.getEventToFilter());
	}



	public ADJUSTMENT_SIGN retrieveSign(final AdjustmentType adjustmentType) {
		if(adjustmentType instanceof final RelativeAdjustment relativeAdjustment) {
			return relativeAdjustment.getPercentageGrowthValue()>0?ADJUSTMENT_SIGN.POSITIVE:ADJUSTMENT_SIGN.NEGATIVE;
		} else if(adjustmentType instanceof final StepAdjustment stepAdjustment) {
			return stepAdjustment.getStepValue()>0?ADJUSTMENT_SIGN.POSITIVE:ADJUSTMENT_SIGN.NEGATIVE;
		}
		return ADJUSTMENT_SIGN.POSITIVE;
	}

}
