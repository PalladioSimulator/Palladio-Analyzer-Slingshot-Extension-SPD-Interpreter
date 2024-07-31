package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public class LogicalANDComboundFilter extends ComboundFilter {

	private FilterResult result;

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		result = null;
		this.next(event.getEventToFilter());
		return result;
	}

	@Override
	public void next(final DESEvent event) {
		super.next(event);
		if (!this.filterIsBeingUsed()) {
			/* This means that no filter was used. */
			result = FilterResult.success(event);
		}
	}

	@Override
	public void disregard(final Object message) {
		result = FilterResult.disregard(message);
	}

}
