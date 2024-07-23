package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
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
import org.palladiosimulator.spd.triggers.ScalingTrigger;

public final class SPDAdjustorContext {

	private static final Logger LOGGER = Logger.getLogger(SPDAdjustorContext.class);

	private final FilterChain filterChain;
	private final ScalingPolicy scalingPolicy;
	private final List<Subscriber<? extends DESEvent>> associatedHandlers;

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
				.collect(Collectors.toList());

		this.associatedHandlers.add(Subscriber.builder(SPDAdjustorStateInitialized.class)
				.name("StateInitializationHandler")
				.handler(new StateInitializationHandler()).build());
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

	/**
	 * For State Exploration only.
	 *
	 * @return the state.
	 */
	public SPDAdjustorState getState() {
		return state;
	}

	public ScalingPolicy getScalingPolicy() {
		return scalingPolicy;
	}

	public List<Subscriber<? extends DESEvent>> getAssociatedHandlers() {
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

	/**
	 * Initialize the {@code state} of this adjustor context according to values
	 * provided by the {@link SPDAdjustorStateInitialized} event.
	 *
	 * This subscriber is not part of the filter chain.
	 */
	private class StateInitializationHandler implements EventHandler<SPDAdjustorStateInitialized> {

		@Override
		public Result<?> acceptEvent(final SPDAdjustorStateInitialized event) throws Exception {

			if (!event.getStateValues().scalingPolicyId().equals(state.getScalingPolicy().getId())) {
				return Result.empty();
			}

			state.setCoolDownEnd(event.getStateValues().coolDownEnd());
			state.setLatestAdjustmentAtSimulationTime(event.getStateValues().latestAdjustmentAtSimulationTime());
			state.setNumberOfScalesInCooldown(event.getStateValues().numberOfScalesInCooldown());

			while (state.numberOfScales() < event.getStateValues().numberScales()) {
				state.incrementNumberScales();
			}

			return Result.empty();
		}

	}
}
