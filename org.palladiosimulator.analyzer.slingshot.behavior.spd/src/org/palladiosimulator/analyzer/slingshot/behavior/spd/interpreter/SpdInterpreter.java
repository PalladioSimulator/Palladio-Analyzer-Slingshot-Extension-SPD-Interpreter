package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateExported;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SpdBasedEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterChain;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.SPDAdjustorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.TargetGroupState;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.Subscriber;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.util.SpdSwitch;

/**
 * A simple SPD interpreter that will build a {@link FilterChain} for each
 * scaling policy.
 *
 * @author Julijan Katic
 */
class SpdInterpreter extends SpdSwitch<SpdInterpreter.InterpretationResult> {

	private static final Logger LOGGER = Logger.getLogger(SpdInterpreter.class);

	private final Map<TargetGroup, TargetGroupState> targetGroupStates = new HashMap<>();

	@Override
	public InterpretationResult caseSPD(final SPD spd) {
		LOGGER.debug("Interpreting SPD Model " + spd.getEntityName() + "[" + spd.getId() + "]");


		spd.getTargetGroups().stream().forEach(target -> targetGroupStates.put(target, new TargetGroupState(target)));

		return spd.getScalingPolicies().stream()
				.map(this::doSwitch)
				.reduce(InterpretationResult::add)
				.orElseGet(() -> InterpretationResult.EMPTY_RESULT);
	}

	@Override
	public InterpretationResult caseScalingPolicy(final ScalingPolicy policy) {
		LOGGER.debug("Interpreting ScalingPolicy Model " + policy.getEntityName() + "[" + policy.getId() + "]");

		if (!policy.isActive()) {
			return new InterpretationResult();
		}

		final ScalingTriggerInterpreter.InterpretationResult intrResult = (new ScalingTriggerInterpreter(policy)).doSwitch(policy.getScalingTrigger());

		final SPDAdjustorContext context = new SPDAdjustorContext(policy, intrResult.getTriggerChecker(),
				intrResult.getEventsToListen(), targetGroupStates.get(policy.getTargetGroup()));

		return (new InterpretationResult())
				.adjustorContext(context)
				.eventsToSchedule(intrResult.getEventsToSchedule())
				.eventsToSchedule(Set.of(new SPDAdjustorStateExported(context.getState())));
	}

	/**
	 * An object that combines all the necessary information of interpretation result.
	 *
	 * @author Julijan Katic
	 */
	public static final class InterpretationResult {

		public static final InterpretationResult EMPTY_RESULT = new InterpretationResult();

		private final List<SPDAdjustorContext> adjustorContexts;
		private final List<SpdBasedEvent> eventsToSchedule;
		private final List<Subscriber<? extends DESEvent>> subscribers;

		InterpretationResult() {
			this.adjustorContexts = new ArrayList<>();
			this.eventsToSchedule = new ArrayList<>();
			this.subscribers = new ArrayList<>();
		}

		InterpretationResult(final List<SPDAdjustorContext> adjustorContexts,
				final List<SpdBasedEvent> eventsToSchedule,
				final List<Subscriber<? extends DESEvent>> subscribers) {
			this.adjustorContexts = new ArrayList<>(adjustorContexts);
			this.eventsToSchedule = new ArrayList<>(eventsToSchedule);
			this.subscribers = new ArrayList<>(subscribers);
		}

		/**
		 * Copies a result from another result.
		 *
		 * @param other
		 */
		InterpretationResult(final InterpretationResult other) {
			this(other.adjustorContexts, other.eventsToSchedule, other.subscribers);
		}

		public InterpretationResult adjustorContext(final SPDAdjustorContext adjustorContext) {
			this.adjustorContexts.add(adjustorContext);
			return this;
		}

		public InterpretationResult adjustorContext(final Collection<? extends SPDAdjustorContext> adjustorContexts) {
			this.adjustorContexts.addAll(adjustorContexts);
			return this;
		}

		public InterpretationResult eventsToSchedule(final Collection<? extends SpdBasedEvent> eventsToSchedule) {
			this.eventsToSchedule.addAll(eventsToSchedule);
			return this;
		}


		public List<SPDAdjustorContext> getAdjustorContexts() {
			return this.adjustorContexts;
		}

		public List<SpdBasedEvent> getEventsToSchedule() {
			return this.eventsToSchedule;
		}

		public List<Subscriber<? extends DESEvent>> getSubscribers() {
			return this.subscribers;
		}

		/**
		 * Adds the results from another interpretation result to this.
		 *
		 * @param other The other result.
		 * @return
		 */
		public InterpretationResult add(final InterpretationResult other) {
			this.adjustorContexts.addAll(other.adjustorContexts);
			this.eventsToSchedule.addAll(other.eventsToSchedule);
			return this;
		}
	}
}
