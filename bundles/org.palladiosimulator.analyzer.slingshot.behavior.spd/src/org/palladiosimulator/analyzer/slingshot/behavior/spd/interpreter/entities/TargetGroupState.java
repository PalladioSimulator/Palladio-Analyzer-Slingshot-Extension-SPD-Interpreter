package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.spd.ModelBasedScalingPolicy;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.TargetGroup;

import com.google.common.collect.Iterables;

public final class TargetGroupState {

    // state of
    private final TargetGroup targetGroup;

    private final List<Double> enactmentTimeOfScalingPolicies = new ArrayList<>();
    private final List<ScalingPolicy> enactedScalingPolicies = new ArrayList<>();
    private final List<Integer> enactedDynamicScalingDecisions = new ArrayList<>();

    public TargetGroupState(final TargetGroup target) {
        this.targetGroup = target;
    }

    public TargetGroup getTargetGroup() {
        return this.targetGroup;
    }

    public void addEnactedPolicy(final double simulationTime, final ScalingPolicy enactedPolicy) {
        this.enactmentTimeOfScalingPolicies.add(simulationTime);
        this.enactedScalingPolicies.add(enactedPolicy);
        if (enactedPolicy instanceof final ModelBasedScalingPolicy enactedModelBasedPolicy) {
            this.enactedDynamicScalingDecisions.add(enactedModelBasedPolicy.getAdjustment());
        }
    }

    public double getLastScalingPolicyEnactmentTime() {
        return Iterables.getLast(this.enactmentTimeOfScalingPolicies);
    }

    public int getLastDynamicScalingDecision() {
        return Iterables.getLast(this.enactedDynamicScalingDecisions);
    }

    public ScalingPolicy getLastEnactedScalingPolicy() {
        return Iterables.getLast(this.enactedScalingPolicies);
    }

    public boolean enactedPoliciesEmpty() {
        return Iterables.isEmpty(this.enactedScalingPolicies);
    }

}
