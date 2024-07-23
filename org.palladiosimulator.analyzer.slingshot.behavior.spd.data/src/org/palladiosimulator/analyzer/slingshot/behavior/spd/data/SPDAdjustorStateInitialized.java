package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * Indicates the initialisation of a {@code SPDAdjustorState} to the given
 * values.
 *
 * @author Sarah Stieß
 *
 */
public class SPDAdjustorStateInitialized extends AbstractSimulationEvent implements SpdBasedEvent {

	private final SPDAdjustorStateValues stateValues;

	public SPDAdjustorStateInitialized(final SPDAdjustorStateValues stateValues) {
		super();
		this.stateValues = stateValues;
	}

	public SPDAdjustorStateValues getStateValues() {
		return stateValues;
	}

}