package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.FixedLengthWindowSimpleAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.SlidingTimeWindowAggregationBasedOnEMA;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.WindowAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.targetgroup.TargetGroupChecker;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.MetricSetDescription;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedPrimitive;
import org.palladiosimulator.spd.triggers.stimuli.ManagedElementsStateStimulus;

/**
 * This abstract class implements the base functionality for
 * {@link ManagedElementsStateStimulus} elements. Measurements for this elements
 * need to be aggregated first, which is done by
 * {@link #aggregateMeasurement(MeasurementMade, MeasuringPoint)}. Afterwards,
 * {@link #getResult(DESEvent)} checks whether enough measurements were made,
 * and whether the calculated value triggers the policy.
 * 
 * The precondition is that the measuring point, where the measurements are
 * coming from, are inside the target group. This is done in the
 * {@link TargetGroupChecker} filter, that should be placed before this filter.
 * 
 * @author Julijan Katic
 *
 * @param <T> The concrete element the class is checking for.
 */
public abstract class AbstractManagedElementTriggerChecker<T extends ManagedElementsStateStimulus>
		extends TriggerChecker<T> {
	
	protected final TargetGroup targetGroup;
	protected final T managedElementsStateStimulus;
	protected final MetricSetDescription metricSetDescription;
	protected final BaseMetricDescription baseMetricDescription;
	protected final WindowAggregation aggregator;
	
	@SuppressWarnings("unchecked")
	public AbstractManagedElementTriggerChecker(final BaseTrigger trigger, 
												final T stimulus,
												final TargetGroup targetGroup,
												final Set<Class<? extends ExpectedPrimitive>> allowedExpectedPrimitives,
												final MetricSetDescription metricSetDescription,
												final BaseMetricDescription baseMetricDescription) {
		super(trigger, (Class<T>) stimulus.getClass(), allowedExpectedPrimitives);
		
		this.targetGroup = targetGroup;
		this.managedElementsStateStimulus = stimulus;
		this.metricSetDescription = metricSetDescription;
		this.baseMetricDescription = baseMetricDescription;
		
		if(stimulus.getAggregationOverElements().equals(AGGREGATIONMETHOD.AVERAGE)) {
		    this.aggregator = new SlidingTimeWindowAggregationBasedOnEMA(60,10,0.2);		    
		}else {
		    this.aggregator = FixedLengthWindowSimpleAggregation.getFromAggregationMethod(stimulus.getAggregationOverElements());
		}
		
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		if (event.getEventToFilter() instanceof final MeasurementMade measurementMade) {
			/*
			 * Check that the measuring point points to one of the resource container's
			 * specifications, and count them only once!
			 */
			aggregateMeasurement(measurementMade);


			/*
			 * If all the values are collected, then check whether the expected percentage
			 * matches, otherwise disregard.
			 */
			return getResult(measurementMade);

		}


		return FilterResult.disregard("Not a measurement made event.");
	}

	/**
	 * Helper method to retrieve the filter result. If the aggregated value is in
	 * accordance with the specified trigger, success is returned. If not all
	 * measurements were made yet, or if the value was not in accordance, then
	 * disregard.
	 */
	protected FilterResult getResult(final DESEvent event) {
		if (!this.aggregator.isEmittable()) {
			return FilterResult.disregard("Values not emittable.");
		}
		
		final double aggregatedValue = this.aggregator.getCurrentValue();
		if (this.compareToTrigger(aggregatedValue) == ComparatorResult.IN_ACCORDANCE) {
			return FilterResult.success(event);
		}
		return FilterResult.disregard("Value and Expectation not in accordance.");
	}

	/**
	 * Helper method to aggregate the measurement in case if the measurement comes 
	 * from one of the resource containers in the target group.
	 */
	protected void aggregateMeasurement(final MeasurementMade measurementMade) {
		if (measurementMade.getEntity().getMetricDesciption().getId().equals(this.metricSetDescription.getId())) {
			aggregator.aggregate(getPointInTime(measurementMade.getEntity()),getValueForAggregation(measurementMade.getEntity()));
		}
	}
	
	protected double getPointInTime(final SlingshotMeasuringValue smv) {
		final Measure<Double, Duration>  pointInTime = smv.getMeasureForMetric(MetricDescriptionConstants.POINT_IN_TIME_METRIC);
		final double value = pointInTime.getValue();
		return value;		
	}
	
	protected double getValueForAggregation(final SlingshotMeasuringValue smv) {
		final Measure<Double, Dimensionless> measure = smv.getMeasureForMetric(this.baseMetricDescription);
		final double value = measure.getValue();
		return value;
	}
	
}
