package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;

/**
 *
 * Exports a reference to the given {@code SPDAdjustorState}. The entity is of
 * type {@code Object}, because the class {@code SPDAdjustorState} is not
 * located in the {@code data} bundle, and thus cannot be referenced directly
 * from here.
 *
 * @author Sarah Stieß
 *
 */
public class SPDAdjustorStateExported extends AbstractEntityChangedEvent<Object> implements SpdBasedEvent {

	public SPDAdjustorStateExported(final Object entity) {
		super(entity, 0);
	}
}