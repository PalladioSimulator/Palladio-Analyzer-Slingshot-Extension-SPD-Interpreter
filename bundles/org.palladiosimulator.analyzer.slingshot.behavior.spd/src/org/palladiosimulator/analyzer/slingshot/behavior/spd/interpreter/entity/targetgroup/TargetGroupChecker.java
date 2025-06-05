package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.targetgroup;

import java.util.Objects;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.Filter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.utils.MeasuringPointInsideTargetGroup;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.targets.TargetGroup;

public class TargetGroupChecker implements Filter {

	private final TargetGroup targetGroup;
	private final MeasuringPointInsideTargetGroup measuringPointInsideTargetGroupSwitch;
	
	public TargetGroupChecker(final TargetGroup targetGroup) {
		this.targetGroup = Objects.requireNonNull(targetGroup);
		this.measuringPointInsideTargetGroupSwitch = new MeasuringPointInsideTargetGroup(targetGroup);
	}
	
	@Override
	public FilterResult doProcess(final FilterObjectWrapper objectWrapper) {
		final DESEvent event = objectWrapper.getEventToFilter();
		if (event instanceof final MeasurementMade mm) {
			if (this.measuringPointInsideTargetGroupSwitch.doSwitch(mm.getEntity().getMeasuringPoint())) {
				return FilterResult.success(event);
			}
			return FilterResult.disregard("The measurement is not inside this target group. Expected measurement for " + targetGroup.getEntityName() + " but received measurement for " + mm.getEntity().getMeasuringPoint().getStringRepresentation());
		}
		if (!(event instanceof SimulationTimeReached)) {
			return FilterResult.disregard("The event can only be checked if it is a MeasurementMade OR SimulationTimeReached at the moment.");
		}
		final SimulationTimeReached simulationTimeReached = (SimulationTimeReached) event;
		if (simulationTimeReached.getTargetGroupId().equals(targetGroup.getId())) {
			return FilterResult.success(event);
		}
		return FilterResult.disregard("The target group does not match the event.");
	}
}
