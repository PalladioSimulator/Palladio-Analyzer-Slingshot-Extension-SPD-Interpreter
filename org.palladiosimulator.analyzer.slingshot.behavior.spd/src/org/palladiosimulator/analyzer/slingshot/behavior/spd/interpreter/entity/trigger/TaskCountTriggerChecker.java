package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;

import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedCount;
import org.palladiosimulator.spd.triggers.stimuli.TaskCount;

public class TaskCountTriggerChecker extends AbstractManagedElementTriggerChecker<TaskCount> {

	public TaskCountTriggerChecker(final BaseTrigger trigger, final TaskCount stimulus, final TargetGroup targetGroup) {
		super(trigger, 
				stimulus,
				targetGroup, 
				Set.of(ExpectedCount.class), 
				MetricDescriptionConstants.STATE_OF_ACTIVE_RESOURCE_METRIC_TUPLE,
				MetricDescriptionConstants.STATE_OF_ACTIVE_RESOURCE_METRIC);
	}
	
	/* We need to retrieve the correct type (Long) instead of Double */
	@Override
	protected double getValueForAggregation(final SlingshotMeasuringValue smv) {
		final Measure<Long, Dimensionless> measure = smv.getMeasureForMetric(this.baseMetricDescription);
		final long value = measure.getValue();
		return value;
	}
}
