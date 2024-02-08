package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.TargetGroup;

import com.google.common.collect.Iterables;

public class TargetGroupState {

	// state of 
	private final TargetGroup targetGroup;
	
	
	private final List<Double> enactmentTimeOfScalingPolicies = new ArrayList<>();
	private final List<ScalingPolicy> enactedScalingPolicies = new ArrayList<>() ;
	
	public TargetGroupState(final TargetGroup target) {
		this.targetGroup = target;
	}
	
	public TargetGroup getTargetGroup() {
		return targetGroup;
	}
	
	public void addEnactedPolicy(double simulationTime, ScalingPolicy enactedPolicy) {
		enactmentTimeOfScalingPolicies.add(simulationTime);
		enactedScalingPolicies.add(enactedPolicy);
	}
	
	public double getLastScalingPolicyEnactmentTime() {
		return Iterables.getLast(enactmentTimeOfScalingPolicies);
	}
	
	public ScalingPolicy getLastEnactedScalingPolicy() {
		return Iterables.getLast(enactedScalingPolicies);
	}
	
	public boolean enactedPoliciesEmpty() {
		return Iterables.isEmpty(enactedScalingPolicies);
	}
	
	
	
	
	
}
