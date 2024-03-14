package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcmmeasuringpoint.OperationReference;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;

public final class OperationResponseTimeTriggerChecker extends TriggerChecker<OperationResponseTime> {

	public OperationResponseTimeTriggerChecker(final BaseTrigger trigger) {
		super(trigger, OperationResponseTime.class, Set.of(ExpectedTime.class));
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper objectWrapper) {
		final DESEvent event = objectWrapper.getEventToFilter();
		if (event instanceof final MeasurementMade measurementMade) {
			final SlingshotMeasuringValue measuringValue = measurementMade.getEntity();

			final MeasuringPoint point = measuringValue.getMeasuringPoint();
			if (point instanceof final OperationReference reference) {
				final OperationSignature referencedSignature = reference.getOperationSignature();
				final OperationSignature thisSignature = getStimulus().getOperationSignature();

				if(thisSignature.getId().equals(referencedSignature.getId())) {
					final Measure<Double,Duration> measure = measuringValue.getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
					final double operationTime = measure.doubleValue(SI.SECOND);

					if (this.compareToTrigger(operationTime) == ComparatorResult.IN_ACCORDANCE) {
						return FilterResult.success(event);
					}
				} else {
					return FilterResult.disregard("The signatures do not match");
				}
			}
		}
		
		
		return FilterResult.disregard();
	}
}