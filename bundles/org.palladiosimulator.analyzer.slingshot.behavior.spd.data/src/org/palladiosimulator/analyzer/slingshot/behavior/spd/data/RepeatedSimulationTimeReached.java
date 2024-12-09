package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 * An event that is scheduled at an exact simulation time and repeats on a schedule. In context of
 * SPD, this is needed especially for predictive triggers that recompute some measures on a
 * schedule.
 * 
 * This event also carries a necessary target group (identifier) in order to correctly identify
 * whether this event belongs to the right scaling policy.
 * 
 * @author Jens Berberich, Julijan Katic
 */
public class RepeatedSimulationTimeReached extends AbstractSimulationEvent implements SpdBasedEvent {

    private final String targetGroupId;
    private double repetitionTime;

    public RepeatedSimulationTimeReached(String targetGroupId, double simulationTime, double delay,
            double repetitionTime) {
        super(delay);
        this.targetGroupId = targetGroupId;
        this.setTime(simulationTime);
        this.repetitionTime = repetitionTime;
    }

    public double getRepetitionTime() {
        return repetitionTime;
    }

    public String getTargetGroupId() {
        return targetGroupId;
    }
}