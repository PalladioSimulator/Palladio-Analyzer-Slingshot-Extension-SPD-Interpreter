package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.utils.MeasuringPointInsideTargetGroup;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 * This class holds the state for each scaling policy definition.
 * The state include information about when the latest adjustment
 * has happened, as well as how many scaling has happened.
 * 
 * In addition it holds a reference to the target group state.
 * 
 * @author Julijan Katic, Floriment Klinaku
 */
public final class SPDAdjustorState {

	private double latestAdjustmentAtSimulationTime = 0;
	private int numberScales = 0;
	
	// Reference to the targetGroupState
	private final TargetGroupState targetGroupState;
	private final ScalingPolicy scalingPolicy;
	
	// state for evaluating cooldown constraints
	private double coolDownEnd = 0;
	private int numberOfScalesInCooldown = 0;
	
	
	public SPDAdjustorState(final ScalingPolicy scalingPolicy, final TargetGroupState targetGroupState) {
		this.scalingPolicy = Objects.requireNonNull(scalingPolicy);
		this.targetGroupState = Objects.requireNonNull(targetGroupState);
	}

	public double getLatestAdjustmentAtSimulationTime() {
		return latestAdjustmentAtSimulationTime;
	}

	public void setLatestAdjustmentAtSimulationTime(double latestAdjustmentAtSimulationTime) {
		this.latestAdjustmentAtSimulationTime = latestAdjustmentAtSimulationTime;
	}
	
	public TargetGroupState getTargetGroupState() {
		return targetGroupState;
	}
	
	public void incrementNumberScales() {
		this.numberScales++;
	}
	
	public int numberOfScales() {
		return this.numberScales;
	}

	public double getCoolDownEnd() {
		return coolDownEnd;
	}

	public void setCoolDownEnd(double coolDownEnd) {
		this.coolDownEnd = coolDownEnd;
	}

	public int getNumberOfScalesInCooldown() {
		return numberOfScalesInCooldown;
	}

	public void setNumberOfScalesInCooldown(int numberOfScalesInCooldown) {
		this.numberOfScalesInCooldown = numberOfScalesInCooldown;
	}
	
	public void incrementNumberOfAdjustmentsInCooldown() {
		this.numberOfScalesInCooldown++;
	}

	public ScalingPolicy getScalingPolicy() {
		return scalingPolicy;
	}
	
}
