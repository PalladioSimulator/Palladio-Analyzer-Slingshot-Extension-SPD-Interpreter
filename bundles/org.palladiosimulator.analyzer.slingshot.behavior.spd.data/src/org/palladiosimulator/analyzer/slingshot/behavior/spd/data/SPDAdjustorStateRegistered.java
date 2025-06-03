package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;

/**
 * TODO
 * 
 * @author Sophie Stieß
 */
public final class SPDAdjustorStateRegistered extends AbstractEntityChangedEvent<SPDAdjustorState> implements SpdBasedEvent {
	public SPDAdjustorStateRegistered(final SPDAdjustorState adjustorState) {
		super(adjustorState, 0);
	}
}
