package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult.Disregard;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 *
 * A CompoundFilter that connects all filters of the chain with an OR.
 *
 * Thus, this filter only results in a {@link Disregard} if all parts of the
 * chain result in a {@link Disregard}.
 *
 * @author Julijan Katic, Sarah Stieß
 *
 */
public class LogicalORCompoundFilter extends ComboundFilter {

	private DESEvent eventToProcess;
	private int numberDisregarded;

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		this.eventToProcess = event.getEventToFilter();
		this.numberDisregarded = 0;
		this.next(event.getEventToFilter());
		this.reset();
		return this.getLatestResult();
	}

	@Override
	public void checkResult() {
		if (this.getLatestResult() instanceof final FilterResult.Success success) {
			return; // short circuit
		} else if (this.getLatestResult() instanceof final FilterResult.Disregard disregard) {
			this.disregard(disregard.reason().toString());
		}
	}

	@Override
	public void disregard(final Object message) {
		this.numberDisregarded++;
		if (this.numberDisregarded < this.size()) {
			this.next(this.eventToProcess);
		} else {
			super.disregard(message); // TODO : it would be nice to concatenate the messages.
		}
	}

	private void reset() {
		this.iterator = null;
		this.eventToProcess = null;
		this.numberDisregarded = 0;
	}

}
