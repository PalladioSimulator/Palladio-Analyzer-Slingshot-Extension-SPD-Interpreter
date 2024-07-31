package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/* 
 * TODO: Currently, it could happen that multiple events are spawned at the same
 * 		 simulation time, which will affect performance. Look at how only one event
 * 		 for a certain simulation time is spawned at the same time ==> Then, no target
 * 		 group checking is needed in this case.
 */
/**
 * An event that is scheduled at an exact simulation time. In context of SPD,
 * this is needed especially for triggers with simulation time stimulus.
 * 
 * This event also carries a necessary target group (identifier) in order to
 * correctly identify whether this event belongs to the right scaling policy.
 * 
 * @author Julijan Katic
 */
public final class SimulationTimeReached extends AbstractSimulationEvent implements SpdBasedEvent {

	private final String targetGroupId;

	/**
	 * Constructs a new simulation time reached event that is scheduled at exactly {@code simulationTime} and a
	 * possibly a {@code delay}.
	 * 
	 * The constructed event should then be scheduled AFTER the current simulation time. If the specified simulation time
	 * is in the past, an exception at the event bus might occur.
	 * 
	 * @param targetGroupId The target group identifier this event belongs to. 
	 * @param simulationTime A positive number that specifies the exact simulation time that should be reached.
	 * @param delay A non-negative number specifying a delay after the simulation time.
	 */
	public SimulationTimeReached(final String targetGroupId, final double simulationTime, final double delay) {
		super(delay);
		this.targetGroupId = targetGroupId;
		this.setTime(simulationTime);
	}
	
	public SimulationTimeReached(final String targetGroupId, final double simulationTime) {
		this(targetGroupId, simulationTime, 0);
	}

	public String getTargetGroupId() {
		return targetGroupId;
	}
	
	
}
