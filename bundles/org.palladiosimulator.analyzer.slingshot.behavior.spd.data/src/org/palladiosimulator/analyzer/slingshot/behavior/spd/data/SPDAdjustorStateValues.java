package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

/**
 *
 * Record of the values of an {@code SPDAdjustorState}.
 *
 * This is a distinct data type, as this is itself not a state, but only carries
 * the values to be set in some other state.
 *
 * @author Sarah Stieß
 *
 */
public record SPDAdjustorStateValues(String scalingPolicyId, double latestAdjustmentAtSimulationTime, int numberScales,
		double coolDownEnd, int numberOfScalesInCooldown) {
}