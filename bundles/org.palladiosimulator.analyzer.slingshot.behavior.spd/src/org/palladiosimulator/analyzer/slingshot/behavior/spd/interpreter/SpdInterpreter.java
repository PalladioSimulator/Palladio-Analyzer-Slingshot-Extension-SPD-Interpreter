package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.RepeatedSimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SpdBasedEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterChain;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.SPDAdjustorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.TargetGroupState;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.ModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.ModelBasedTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.Subscriber;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.ModelBasedScalingPolicy;
import org.palladiosimulator.spd.ReactiveScalingPolicy;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.constraints.policy.IntervalConstraint;
import org.palladiosimulator.spd.constraints.target.TargetGroupSizeConstraint;
import org.palladiosimulator.spd.models.BaseModel;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.util.SpdSwitch;

/**
 * A simple SPD interpreter that will build a {@link FilterChain} for each scaling policy.
 *
 * @author Julijan Katic
 */
class SpdInterpreter extends SpdSwitch<SpdInterpreter.InterpretationResult> {

    private static final Logger LOGGER = Logger.getLogger(SpdInterpreter.class);

    private final Map<TargetGroup, TargetGroupState> targetGroupStates = new HashMap<>();

    @Override
    public InterpretationResult caseSPD(final SPD spd) {
        LOGGER.debug("Interpreting SPD Model " + spd.getEntityName() + "[" + spd.getId() + "]");

        spd.getTargetGroups()
            .stream()
            .forEach(target -> this.targetGroupStates.put(target, new TargetGroupState(target)));

        return spd.getScalingPolicies()
            .stream()
            .map(this::doSwitch)
            .reduce(InterpretationResult::add)
            .orElseGet(() -> InterpretationResult.EMPTY_RESULT);
    }

    @Override
    public InterpretationResult caseReactiveScalingPolicy(final ReactiveScalingPolicy policy) {
        LOGGER.debug("Interpreting ReactiveScalingPolicy Model " + policy.getEntityName() + "[" + policy.getId() + "]");

        if (!policy.isActive()) {
            return new InterpretationResult();
        }

        final ScalingTriggerInterpreter.InterpretationResult intrResult = (new ScalingTriggerInterpreter(policy))
            .doSwitch(policy.getScalingTrigger());
        return (new InterpretationResult())
            .adjustorContext(new SPDAdjustorContext(policy, intrResult.getTriggerChecker(),
                    intrResult.getEventsToListen(), this.targetGroupStates.get(policy.getTargetGroup())))
            .eventsToSchedule(intrResult.getEventsToSchedule());
    }

    @Override
    public InterpretationResult caseModelBasedScalingPolicy(final ModelBasedScalingPolicy policy) {
        LOGGER
            .debug("Interpreting ModelBasedScalingPolicy Model " + policy.getEntityName() + "[" + policy.getId() + "]");

        if (!policy.isActive()) {
            return new InterpretationResult();
        }

        final BaseModel model = policy.getModel();
        final List<TargetGroupSizeConstraint> targetGroupSizeConstraints = policy.getTargetGroup()
            .getTargetConstraints()
            .stream()
            .filter(TargetGroupSizeConstraint.class::isInstance)
            .map(TargetGroupSizeConstraint.class::cast)
            .toList();
        int minContainerCount = 0;
        int maxContainerCount = Integer.MAX_VALUE;
        if (targetGroupSizeConstraints.isEmpty()) {
            LOGGER.warn(
                    "Using a model-based scaling policy without any target group size constraints is not recommended");
        } else {
            for (TargetGroupSizeConstraint targetGroupSizeConstraint : targetGroupSizeConstraints) {
                minContainerCount = Math.max(minContainerCount, targetGroupSizeConstraint.getMinSize());
                maxContainerCount = Math.min(maxContainerCount, targetGroupSizeConstraint.getMaxSize());
            }
        }

        final Optional<IntervalConstraint> intervalConstraint = policy.getPolicyConstraints()
            .stream()
            .filter(IntervalConstraint.class::isInstance)
            .map(IntervalConstraint.class::cast)
            .findFirst();

        if (intervalConstraint.isEmpty()) {
            LOGGER.error("Using a model-based scaling policy without an Interval Constraint is not supported.");
            return InterpretationResult.EMPTY_RESULT;
        }

        final RepeatedSimulationTimeReached event = new RepeatedSimulationTimeReached(policy.getTargetGroup()
            .getId(),
                intervalConstraint.get()
                    .getOffset()
                        + intervalConstraint.get()
                            .getIntervalDuration(),
                0.f, intervalConstraint.get()
                    .getIntervalDuration());

        final ModelInterpreter modelInterpreter = new ModelInterpreter(intervalConstraint.get()
            .getIntervalDuration(), minContainerCount, maxContainerCount);
        final ModelEvaluator modelEvaluator = modelInterpreter.doSwitch(model);

        final ScalingTriggerInterpreter.InterpretationResult intrResult = (new ScalingTriggerInterpreter.InterpretationResult())
            .scheduleEvent(event)
            .listenEvent(Subscriber.builder(RepeatedSimulationTimeReached.class))
            .listenEvent(Subscriber.builder(MeasurementMade.class))
            .listenEvent(Subscriber.builder(SimulationFinished.class))
            .triggerChecker(new ModelBasedTriggerChecker(modelEvaluator));

        return (new InterpretationResult())
            .adjustorContext(new SPDAdjustorContext(policy, intrResult.getTriggerChecker(),
                    intrResult.getEventsToListen(), this.targetGroupStates.get(policy.getTargetGroup())))
            .eventsToSchedule(intrResult.getEventsToSchedule());
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
                final List<SpdBasedEvent> eventsToSchedule, final List<Subscriber<? extends DESEvent>> subscribers) {
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
         * @param other
         *            The other result.
         * @return
         */
        public InterpretationResult add(final InterpretationResult other) {
            this.adjustorContexts.addAll(other.adjustorContexts);
            this.eventsToSchedule.addAll(other.eventsToSchedule);
            return this;
        }
    }
}
