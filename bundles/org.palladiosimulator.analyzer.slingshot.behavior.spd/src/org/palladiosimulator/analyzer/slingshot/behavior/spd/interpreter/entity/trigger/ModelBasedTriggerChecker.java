package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.RepeatedSimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.Filter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.LearningBasedModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.ModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.ModelBasedScalingPolicy;

/**
 * This class listens to events of the type {@link RepeatedSimulationTimeReached} and
 * {@link MeasurementMade} and handles them accordingly:
 *
 * - Events of type {@link RepeatedSimulationTimeReached} will be forwarded to the given
 * {@link ModelEvaluator} {@link #model} where a scaling decision is made and then returned. If
 * necessary, a new adjustment with the returned adjustment value is created.
 *
 * - Events of type {@link MeasurementMade} will also be forwarded to
 * {@link ModelEvaluator#recordUsage(MeasurementMade)}, where they will be recorded (both for input
 * aggregation and for the reward)
 *
 * @author Jens Berberich
 *
 */
public class ModelBasedTriggerChecker implements Filter {
    private final ModelEvaluator model;

    private static final Logger LOGGER = Logger.getLogger(ModelBasedTriggerChecker.class);

    public ModelBasedTriggerChecker(final ModelEvaluator model) {
        this.model = model;
    }

    @Override
    public FilterResult doProcess(final FilterObjectWrapper event) {
        final DESEvent filteredEvent = event.getEventToFilter();
        if (filteredEvent instanceof final MeasurementMade measurementMade) {
            LOGGER.debug("Received a datapoint collection event!");
            this.model.recordUsage(measurementMade);
        } else if (filteredEvent instanceof SimulationFinished
                && this.model instanceof final LearningBasedModelEvaluator learningBasedModelEvaluator) {
            learningBasedModelEvaluator.printTrainedModel();
        }
        if (filteredEvent instanceof RepeatedSimulationTimeReached
                && this.model instanceof final LearningBasedModelEvaluator learningBasedModelEvaluator) {
            try {
                LOGGER.info("Asking model for scaling action at time " + filteredEvent.time());
                learningBasedModelEvaluator.update();
            } catch (final NotEmittableException e) {
                LOGGER.warn(e.getMessage());
                return FilterResult.disregard(filteredEvent);
            }
        }
        if ((filteredEvent instanceof RepeatedSimulationTimeReached && this.model.getChangeOnInterval())
                || (filteredEvent instanceof MeasurementMade && this.model.getChangeOnStimulus())) {
            int value;
            try {
                value = this.model.getDecision();
            } catch (final NotEmittableException e) {
                LOGGER.warn(e.getMessage());
                return FilterResult.disregard(filteredEvent);
            }
            LOGGER.info("Model scaling decision: " + value);
            if (event.getState()
                .getScalingPolicy() instanceof final ModelBasedScalingPolicy modelBasedScalingPolicy) {
                modelBasedScalingPolicy.setAdjustment(value);
            }
            return FilterResult.success(filteredEvent);
        } else if (!(filteredEvent instanceof MeasurementMade
                || filteredEvent instanceof RepeatedSimulationTimeReached)) {
            LOGGER.debug("Received an unexpected event");
            return FilterResult.disregard(filteredEvent);
        }
        return FilterResult.disregard(filteredEvent);
    }
}
