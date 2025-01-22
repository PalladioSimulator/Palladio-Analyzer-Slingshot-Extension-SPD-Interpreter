package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import java.util.Optional;

import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.targetgroup.TargetGroupChecker;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcmmeasuringpoint.OperationReference;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;
import org.palladiosimulator.spd.triggers.stimuli.ManagedElementsStateStimulus;
import org.palladiosimulator.spd.triggers.stimuli.NumberOfElements;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;
import org.palladiosimulator.spd.triggers.stimuli.QueueLength;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

/**
 * This class implements the base functionality for forwarding the latest measurement for
 * non-aggregated stimuli. Measurements for this elements need to be aggregated first, which is done
 * by {@link #aggregateMeasurement(MeasurementMade)}. Afterwards, {@link #getResult()} checks
 * whether enough measurements were made, and which value is aggregated.
 * 
 * The precondition is that the measuring point, where the measurements are coming from, are inside
 * the target group. This is done in the {@link TargetGroupChecker} filter, that should be placed
 * before this filter.
 * 
 * @author Jens Berberich, based on work by Julijan Katic
 *
 * @param <T>
 *            The concrete element the class is checking for.
 */
public class AnyStimulusAggregator<T extends Stimulus> extends ModelAggregatorWrapper<T> {
    abstract class StimulusChecker {
        abstract Optional<Measure<Double, ?>> checkStimulus(MeasurementMade measurementMade);
    }

    class OperationResponseTimeStimulusChecker extends StimulusChecker {
        private final OperationSignature operationSignature;

        OperationResponseTimeStimulusChecker(OperationResponseTime operationResponseTime) {
            this.operationSignature = operationResponseTime.getOperationSignature();
        }

        Optional<Measure<Double, ?>> checkStimulus(MeasurementMade measurementMade) {
            final SlingshotMeasuringValue measuringValue = measurementMade.getEntity();

            final MeasuringPoint point = measuringValue.getMeasuringPoint();
            if (point instanceof final OperationReference reference) {
                final OperationSignature referencedSignature = reference.getOperationSignature();
                final OperationSignature thisSignature = this.operationSignature;
                if (thisSignature.getId()
                    .equals(referencedSignature.getId())) {
                    return Optional
                        .of(measuringValue.getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));

                }
            }
            return Optional.empty();
        }
    }

    protected final WindowAggregation aggregator;
    private AnyStimulusAggregator<T>.StimulusChecker stimulusChecker;
    private static final Logger LOGGER = Logger.getLogger(AnyStimulusAggregator.class);

    public AnyStimulusAggregator(final T stimulus, double windowSize, AGGREGATIONMETHOD aggregationMethod) {
        if (stimulus instanceof ManagedElementsStateStimulus) {
            LOGGER.error("Function only for non-aggregated stimuli!");
            throw new IllegalArgumentException("Function only for non-aggregated stimuli!");
        }
        // QueueLength, NumberOfElements, OperationResponseTime
        else if (stimulus instanceof OperationResponseTime operationResponseTime) {
            this.stimulusChecker = new OperationResponseTimeStimulusChecker(operationResponseTime);
            this.baseMetricDescription = MetricDescriptionConstants.RESPONSE_TIME_METRIC;
            this.metricSetDescription = MetricDescriptionConstants.RESPONSE_TIME_METRIC_TUPLE;
        } else if (stimulus instanceof NumberOfElements numberOfElements) {
            LOGGER.info("Number of Elements stimuli are not yet supported");
            // TODO implement number of elements aggregator
            this.baseMetricDescription = MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS;
            this.metricSetDescription = MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS_OVER_TIME;
        } else if (stimulus instanceof QueueLength queueLength) {
            LOGGER.info("Queue Length stimuli are not yet supported");
            // TODO implement queue length aggregator
            this.baseMetricDescription = MetricDescriptionConstants.STATE_OF_PASSIVE_RESOURCE_METRIC;
            this.metricSetDescription = MetricDescriptionConstants.STATE_OF_PASSIVE_RESOURCE_METRIC_TUPLE;
        }
        if (aggregationMethod.equals(AGGREGATIONMETHOD.AVERAGE)) {
            this.aggregator = new SlidingTimeWindowAggregationBasedOnEMA(windowSize, windowSize / 2, 0.2);
        } else {
            this.aggregator = SlidingTimeWindowAggregation.getFromAggregationMethod(aggregationMethod, windowSize, 0.0);
        }
    }

    /**
     * Helper method to retrieve the filter result. If the aggregated value is in accordance with
     * the specified trigger, success is returned. If not all measurements were made yet, or if the
     * value was not in accordance, then disregard.
     * 
     * @throws NotEmittableException
     *             if {@link #aggregator} cannot currently emit a value
     */
    public double getResult() throws NotEmittableException {
        if (!this.aggregator.isEmittable()) {
            throw new NotEmittableException("Values for " + this.stimulusChecker.getClass()
                .getSimpleName() + " not emittable.");
        }
        return this.aggregator.getCurrentValue();
    }

    /**
     * Helper method to aggregate the measurement in case if the measurement comes from one of the
     * resource containers in the target group.
     */
    public void aggregateMeasurement(final MeasurementMade measurementMade) {
        if (measurementMade.getEntity()
            .getMetricDesciption()
            .getId()
            .equals(this.metricSetDescription.getId())) {
            Optional<Measure<Double, ?>> measure = this.stimulusChecker.checkStimulus(measurementMade);
            if (measure.isPresent()) {
                aggregator.aggregate(getPointInTime(measurementMade.getEntity()), measure.get()
                    .getValue());
            }
        }
    }

    protected double getPointInTime(final SlingshotMeasuringValue smv) {
        final Measure<Double, Duration> pointInTime = smv
            .getMeasureForMetric(MetricDescriptionConstants.POINT_IN_TIME_METRIC);
        return pointInTime.getValue();
    }

    protected double getValueForAggregation(final SlingshotMeasuringValue smv) {
        final Measure<Double, Dimensionless> measure = smv.getMeasureForMetric(this.baseMetricDescription);

        return measure.getValue();
    }
}
