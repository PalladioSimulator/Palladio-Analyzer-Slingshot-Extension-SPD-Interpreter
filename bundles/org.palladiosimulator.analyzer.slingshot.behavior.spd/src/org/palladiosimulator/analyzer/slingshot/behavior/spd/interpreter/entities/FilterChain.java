package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 * A filter chain describes an ordered chain of {@link Filter}s through which an event
 * is passed and possibly manipulated or transformed. Each {@link Filter} will get
 * this chain passed as an argument, and a filter can decide to pass an event to the next
 * filter {@link #next(Object)} or to cancel {@link #disregard(String)}.
 * <p>
 * If {@link #disregard(String)} is called, then the provided message will be delegated
 * to a given callback provided in the constructor.
 * <p>
 * New filters can be added with {@link #add(Filter)} and {@link #addAt(int, Filter)}.
 * Note that it is not possible to add new filter if it is currently in used (see {@link #filterIsBeingUsed()}).
 *
 * @author Julijan Katic
 *
 */
public class FilterChain {

	protected final List<Filter> filters = new ArrayList<>();
	private final Consumer<Object> doOnDisregard;
	private final SPDAdjustorState state;
	
	protected Iterator<Filter> iterator;
	private FilterResult latestResult;
	
	/**
	 * Constructs a new and empty filter chain. A non-null delegator
	 * must be provided that will be called if any filter disregards.
	 *
	 * @param doOnDisregard A non-null delegator called when disregarded.
	 * @see #FilterChain()
	 */
	public FilterChain(final Consumer<Object> doOnDisregard, final SPDAdjustorState state) {
		this.doOnDisregard = Objects.requireNonNull(doOnDisregard);
		this.state = state;
	}

	/**
	 * Constructs a new and empty filter chain with a default delegator
	 * that does nothing.
	 */
	public FilterChain(final SPDAdjustorState state) {
		this(message -> {}, state);
	}

	/**
	 * Adds a new filter at the end of the chain. The filter must not
	 * be currently in use.
	 *
	 * @param filter A non-null filter.
	 * @throws IllegalStateException if the filter is currently in use.
	 */
	public void add(final Filter filter) {
		this.checkThatChainIsNotCurrentlyUsed();
		this.filters.add(filter);
	}

	public void addAll(final Collection<? extends Filter> filters) {
		this.checkThatChainIsNotCurrentlyUsed();
		this.filters.addAll(filters);
	}

	/**
	 * Adds a new filter at a certain position, and the filter at that
	 * position and each filter afterwards are shifted to the right.
	 * The index must be within the range of the ordered list, and the
	 * filter must not be currently in use.
	 *
	 * @param i The position at which the filter should be added.
	 * @param filter The filter itself.
	 *
	 * @throws IllegalStateException if the filter is currently in use.
	 * @throws IndexOutOfBoundsException if the provided position is {@code <0} or &ge;{@code size()}.
	 */
	public void add(final int i, final Filter filter) {
		this.checkThatChainIsNotCurrentlyUsed();
		this.filters.add(i, filter);
	}

	/**
	 * Calls the next filter in the chain with the given event.
	 * If there are no filters anymore, then the chain is finalized,
	 * and {@link #filterIsBeingUsed()} will return false afterwards.
	 * <p>
	 * If the chain is not currently in use, then the chain will start at
	 * the beginning.
	 *
	 * @param event An event that should be passed to the next filter if there is one.
	 */
	public void next(final DESEvent event) {
		if (!this.filterIsBeingUsed()) {
			this.iterator = this.filters.iterator();
		}
		if (this.iterator.hasNext()) {
			try {
				final Filter filter = this.iterator.next();
				this.latestResult = filter.doProcess(new FilterObjectWrapper(event, state));
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
			this.next(success.nextEvent());
		} else if (this.latestResult instanceof final FilterResult.Disregard disregard) {
			this.disregard(disregard.reason().toString());
		}
	}
	
	public FilterResult getLatestResult() {
		return this.latestResult;
	}

	/**
	 * Disregards the rest of the chain's filter, and instead calls
	 * the provided delegator with the message. This method also re-initializes the
	 * chain, so {@link #filterIsBeingUsed()} will return false afterwards.
	 *
	 * @param message A message describing why the chain is cancelled.
	 */
	public void disregard(final Object message) {
		this.iterator = null;
		this.doOnDisregard.accept(message);
	}

	/**
	 * Returns whether the filter is currently being used; that is, whether {@link #next(Object)} was
	 * called before and filter hasn't reached its end yet, or {@link #disregard(String)} hasn't been called
	 * yet.
	 *
	 * @return true iff chain is in use.
	 */
	public boolean filterIsBeingUsed() {
		return this.iterator != null;
	}

	public int size() {
		return this.filters.size();
	}

	private void checkThatChainIsNotCurrentlyUsed() {
		if (this.filterIsBeingUsed()) {
			throw new IllegalStateException("The filter chain is currently in use (next() was called before) or hasn't reached "
					+ "the end yet). Either disregard first or wait until chain has finished.");
		}
	}
}
