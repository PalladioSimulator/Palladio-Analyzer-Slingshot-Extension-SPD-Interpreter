package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

/**
 * A filter is a process within a {@link FilterChain}, which can filter out
 * or transform events.
 *
 * @author Julijan Katic
 *
 */
@FunctionalInterface
public interface Filter {

	/**
	 * Processes an event, by either disregarding it or transforming it into a new
	 * event.
	 * 
	 * @param event The event to transform.
	 * @return An object describing the result of this process.
	 */
	public FilterResult doProcess(final FilterObjectWrapper event);

}
