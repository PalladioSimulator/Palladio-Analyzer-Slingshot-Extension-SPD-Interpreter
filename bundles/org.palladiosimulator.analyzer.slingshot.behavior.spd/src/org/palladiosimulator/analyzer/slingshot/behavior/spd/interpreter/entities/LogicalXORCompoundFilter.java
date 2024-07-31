package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public class LogicalXORCompoundFilter extends ComboundFilter {

	private FilterResult currentResult;
	private DESEvent eventToProcess;
	private DESEvent outputEvent;

	private int numberContinued;
	private int numberDisregarded;

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		this.currentResult = null;
		this.eventToProcess = event.getEventToFilter();
		this.numberContinued = 0;
		this.numberDisregarded = 0;
		this.next(event.getEventToFilter());
		return this.currentResult;
	}

	@Override
	public void next(final DESEvent event) {
		if (this.numberContinued == 0) {
			/* We allow one call, but we need to check that any other filter was called. */
			this.outputEvent = event;
			this.numberContinued++;
			super.next(this.eventToProcess);
		} else {
			/* We called it too often. */
			this.currentResult = FilterResult.disregard("More than one filter through XOR");
			this.initialize();
		}
	}

	@Override
	public void disregard(final Object message) {
		this.numberDisregarded++;
		if (this.numberDisregarded + this.numberContinued < this.size()) {
			super.next(this.eventToProcess);
		} else if (this.numberContinued == 1) {
			this.currentResult = FilterResult.success(this.outputEvent);
		} else {
			this.currentResult = FilterResult.disregard("No filter went through. Last message: " + message);
			this.initialize();
		}

	}

	private void initialize() {
		this.eventToProcess = null;
		this.outputEvent = null;
		this.numberContinued = 0;
		this.numberDisregarded = 0;
	}

}
