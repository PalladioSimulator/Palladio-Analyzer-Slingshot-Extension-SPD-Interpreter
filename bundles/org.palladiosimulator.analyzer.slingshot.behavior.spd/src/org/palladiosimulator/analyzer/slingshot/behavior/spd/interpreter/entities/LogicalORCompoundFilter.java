package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public class LogicalORCompoundFilter extends ComboundFilter {

	private FilterResult result = null;
	private DESEvent eventToProcess;
	private int numberDisregarded;

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		result = null;
		this.eventToProcess = event.getEventToFilter();
		this.numberDisregarded = 0;
		this.next(event.getEventToFilter());
		return result;
	}

	@Override
	public void next(final DESEvent event) {
		/* The whole filter was successful, delegate to parent */
		//this.currentParentChain.next(event);

		/* Reinitialize */
		this.initialize();
	}

	@Override
	public void disregard(final Object message) {
		this.numberDisregarded++;
		if (this.numberDisregarded < this.size()) {
			super.next(this.eventToProcess);
		} else {
		//	this.currentParentChain.disregard(message);
			this.initialize();
		}

	}

	private void initialize() {
	//	this.currentParentChain = null;
		this.eventToProcess = null;
		this.numberDisregarded = 0;
	}

}
