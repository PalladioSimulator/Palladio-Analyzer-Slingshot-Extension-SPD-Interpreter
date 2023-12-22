package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 *
 * TODO
 *
 * @author Julijan Katic, Sarah Stieß
 *
 */
public class LogicalORCompoundFilter extends ComboundFilter {

	private static final Logger LOGGER = Logger.getLogger(LogicalORCompoundFilter.class);

	private DESEvent eventToProcess;
	private int numberDisregarded;

	private FilterObjectWrapper wrapper;

	private FilterResult latestResult;

	private String nesteDisregardMessage;

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {

		this.wrapper = event;

		latestResult = null;

		this.eventToProcess = event.getEventToFilter();
		this.numberDisregarded = 0;
		this.next(event.getEventToFilter());
		this.reset();
		return latestResult;
	}

	@Override
	public void next(final DESEvent event) {
		if (!this.filterIsBeingUsed()) {
			this.iterator = this.filters.iterator();
		}
		if (this.iterator.hasNext()) {
			try {
				final Filter filter = this.iterator.next();
				LOGGER.debug("Next Filter : " + filter.getClass().getSimpleName());
				this.latestResult = filter.doProcess(wrapper);
			} catch (final Exception e) {
				this.latestResult = FilterResult.disregard(e);
			}
			checkResult();
		} else {
			this.iterator = null;
		}
	}

	private void checkResult() {
		if (this.latestResult instanceof final FilterResult.Success success) {
			return; // short circuit
		} else if (this.latestResult instanceof final FilterResult.Disregard disregard) {
			this.disregard(disregard.reason().toString());
		}
	}

	@Override
	public void disregard(final Object message) {
		this.numberDisregarded++;
		if (this.numberDisregarded < this.size()) {
			this.nesteDisregardMessage = (String) message;
			this.next(this.eventToProcess);
		} else {
			this.latestResult = FilterResult.disregard(this.nesteDisregardMessage + " AND " + (String) message);
		}
	}

	private void reset() {
		this.iterator = null;
		this.eventToProcess = null;
		this.numberDisregarded = 0;
	}

}
