package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedPercentage;
import org.palladiosimulator.spd.triggers.stimuli.NetworkUtilization;

public class NetworkUtilizationTriggerChecker extends AbstractManagedElementTriggerChecker<NetworkUtilization> {

	public NetworkUtilizationTriggerChecker(BaseTrigger trigger, NetworkUtilization stimulus, TargetGroup targetGroup) {
		super(trigger, stimulus, targetGroup, Set.of(ExpectedPercentage.class),
				MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE_TUPLE,
				MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE);
		// TODO Auto-generated constructor stub
	}


}
