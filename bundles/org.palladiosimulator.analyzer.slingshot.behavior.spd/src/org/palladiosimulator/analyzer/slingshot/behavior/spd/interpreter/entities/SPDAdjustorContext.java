package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.adjustor.Adjustor;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.constraint.AbstractConstraintFilter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.targetgroup.TargetGroupChecker;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.TriggerChecker;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.EventHandler;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.Subscriber;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.constraints.target.ThrashingConstraint;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.ComposedTrigger;
import org.palladiosimulator.spd.triggers.ScalingTrigger;

/**
 *
 * For each {@code SPDAdjustorContext} there must be at most one Subscriber per
 * EventType.
 *
 * This is mostly relevant for {@link ComposedTrigger}s. For
 * {@link BaseTrigger}, it is not relevant, there is always only one Subscriber
 * for those. However a {@link ComposedTrigger} may consist of multiple triggers
 * with the same stimulus type, which results in multiple subscribers for one
 * type of event, which results in the FilterChain being triggered more often
 * than it should.
 *
 * @author Julijan Katic, Sarah Stieß
 */
public final class SPDAdjustorContext {

	private static final Logger LOGGER = Logger.getLogger(SPDAdjustorContext.class);

	private final FilterChain filterChain;
	private final ScalingPolicy scalingPolicy;
	private final Set<Subscriber<? extends DESEvent>> associatedHandlers;

	private SPDAdjustorState state;
	private final SPDAdjustorState previousState;

	public SPDAdjustorContext(final ScalingPolicy policy,
			final Filter triggerChecker,
			final List<Subscriber.Builder<? extends DESEvent>> associatedHandlers, final TargetGroupState targetGroupState) {
		this.scalingPolicy = policy;

		state = new SPDAdjustorState(policy, targetGroupState);
		previousState = new SPDAdjustorState(policy, targetGroupState);

		this.filterChain = new FilterChain(this::doOnDisregard, state);

		initializeFilterChain(triggerChecker);

		final PublishResultingEventFilter publisher = new PublishResultingEventFilter();


		this.associatedHandlers = associatedHandlers.stream()
				.map(builder -> builder.handler(publisher))
				.map(builder -> builder.build())
				.collect(Collectors.toSet());
	}


	/**
	 * Initializes the filter chain.
	 *
	 * The first filter is always a {@link TargetGroupChecker}, the second filter is
	 * always a {@link TriggerChecker}, the last filter is always the
	 * {@link Adjustor}. In between are the Checker for the Constraints.
	 *
	 * @param triggerChecker filter to check the policie's {@link ScalingTrigger}.
	 */
	private void initializeFilterChain(final Filter triggerChecker) {
		this.filterChain.add(new TargetGroupChecker(this.scalingPolicy.getTargetGroup()));

		this.filterChain.add(triggerChecker);


		scalingPolicy.getTargetGroup().getTargetConstraints().stream().filter(constraint -> constraint instanceof final ThrashingConstraint thrashingConstraint).map(constraint -> (ThrashingConstraint) constraint).forEach(constraint ->
				this.filterChain.add(AbstractConstraintFilter.createAbstractConstraintFilter(constraint)));

		scalingPolicy.getPolicyConstraints().forEach(constraint ->
		this.filterChain.add(AbstractConstraintFilter.createAbstractConstraintFilter(constraint))
				);

		this.filterChain.add(new Adjustor(this.scalingPolicy));
	}


	public FilterChain getFilterChain() {
		return filterChain;
	}

	public ScalingPolicy getScalingPolicy() {
		return scalingPolicy;
	}

	public Collection<Subscriber<? extends DESEvent>> getAssociatedHandlers() {
		return associatedHandlers;
	}

	private void doOnDisregard(final Object reason) {
		this.state = previousState;
		LOGGER.info("Filter was not successful: " + reason.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.scalingPolicy.getId());
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (other instanceof final SPDAdjustorContext otherContext) {
			return Objects.equals(this.scalingPolicy.getId(), otherContext.scalingPolicy.getId());
		}
		return false;
	}

	/**
	 * After all filters are successful, the resulting event should be published.
	 */
	private class PublishResultingEventFilter implements EventHandler<DESEvent> {

		@Override
		public Result<?> acceptEvent(final DESEvent event) throws Exception {
			filterChain.next(event);
			final FilterResult filterResult = filterChain.getLatestResult();

			if (filterResult instanceof final FilterResult.Success success) {
				final Object result = success.nextEvent();
				LOGGER.debug("Got a result after filtering! " + result.getClass().getSimpleName());

				return Result.of(result);
			} else {
				return Result.empty();
			}
		}

	}
}
