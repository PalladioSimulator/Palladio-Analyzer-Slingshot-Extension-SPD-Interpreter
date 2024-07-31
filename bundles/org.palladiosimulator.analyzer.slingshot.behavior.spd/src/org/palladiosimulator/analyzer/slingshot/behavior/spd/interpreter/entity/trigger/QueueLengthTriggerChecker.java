package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcmmeasuringpoint.PassiveResourceReference;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.ExpectedCount;
import org.palladiosimulator.spd.triggers.stimuli.QueueLength;

/**
 * The trigger checker for {@link QueueLength}. The measurements are only
 * considered if the measuring point references a passive resource (i.e. is of
 * type {@link PassiveResourceReference} and the referenced passive resource
 * corresponds to the passive resource returned by
 * {@link QueueLength#getPassiveResource()}.
 * 
 * @author Julijan Katic
 *
 */
public class QueueLengthTriggerChecker extends TriggerChecker<QueueLength> {

	private final QueueLength queueLength;

	public QueueLengthTriggerChecker(final SimpleFireOnValue trigger, final QueueLength queueLength) {
		super(trigger, QueueLength.class, Set.of(ExpectedCount.class));
		this.queueLength = queueLength;
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper event) {
		if (event.getEventToFilter() instanceof final MeasurementMade measurementMade
				&& isCorrectPassiveResource(measurementMade.getEntity().getMeasuringPoint())
				&& measurementMade.getEntity().getMetricDesciption()
						.equals(MetricDescriptionConstants.STATE_OF_PASSIVE_RESOURCE_METRIC_TUPLE)) {

			final Measure<Long, Dimensionless> value = measurementMade.getEntity()
					.getMeasureForMetric(MetricDescriptionConstants.STATE_OF_PASSIVE_RESOURCE_METRIC);
			final ComparatorResult result = this.compareToTrigger(value.getValue());

			if (result == ComparatorResult.IN_ACCORDANCE) {
				return FilterResult.success(measurementMade);
			}

			return FilterResult.disregard("Cannot trigger, since the result is not in accordance: " + result);
		}

		return FilterResult.disregard();
	}

	/**
	 * Checks whether the measuring point is of type
	 * {@link PassiveResourceReference} and the referenced passive resource matches
	 * with the passive resource by the {@link QueueLength}.
	 * 
	 * @return true iff the above holds.
	 */
	private boolean isCorrectPassiveResource(final MeasuringPoint measuringPoint) {
		if (measuringPoint instanceof final PassiveResourceReference prmp) {
			return prmp.getPassiveResource().getId().equals(queueLength.getPassiveResource().getId());
		}

		return false;
	}

}
