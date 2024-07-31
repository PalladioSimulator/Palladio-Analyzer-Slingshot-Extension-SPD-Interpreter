package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.ExpectedPercentage;
import org.palladiosimulator.spd.triggers.stimuli.CPUUtilization;

public class CPUUtilizationTriggerChecker extends AbstractManagedElementTriggerChecker<CPUUtilization> {

	public CPUUtilizationTriggerChecker(final SimpleFireOnValue trigger,
										final CPUUtilization stimulus,
								 		final TargetGroup targetGroup) {
		super(trigger, 
				stimulus, 
				targetGroup,
				Set.of(ExpectedPercentage.class),
				MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE_TUPLE,
				MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE);
	}

}
