package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.RepeatedSimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterObjectWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.FilterResult;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;

public final class SimulationTimeChecker extends TriggerChecker<SimulationTime> {

	public SimulationTimeChecker(final BaseTrigger trigger) {
		super(trigger, SimulationTime.class, Set.of(ExpectedTime.class));
	}

	@Override
	public FilterResult doProcess(final FilterObjectWrapper objectWrapper) {
		final DESEvent event = objectWrapper.getEventToFilter();
		final SimulationTimeReached simulationTimeReached;
		if (event instanceof RepeatedSimulationTimeReached repeatedSimulationTimeReached) {
			simulationTimeReached = new SimulationTimeReached(repeatedSimulationTimeReached.getTargetGroupId(),
			        repeatedSimulationTimeReached.time(), repeatedSimulationTimeReached.delay());
		} else if (event instanceof SimulationTimeReached) {
			simulationTimeReached = (SimulationTimeReached) event;
		} else {
			return FilterResult.disregard("");
		}
		final ComparatorResult comparatorResult = this.compareToTrigger(simulationTimeReached.time());
		if (comparatorResult == ComparatorResult.IN_ACCORDANCE) {
			return FilterResult.success(event);
		} else {
			if (this.trigger instanceof SimpleFireOnValue) {
				return FilterResult.disregard(String.format("Simulationtime of %f is not [%s] expectedValue. [[%s]]",
						simulationTimeReached.time(),
						((SimpleFireOnValue) this.trigger).getRelationalOperator().toString(),
						comparatorResult.toString()));
			} else {
				return FilterResult
						.disregard(String.format("Simulationtime of %f is not equal to expectedValue. [[%s]]",
								simulationTimeReached.time(), comparatorResult.toString()));
			}
		}

	}

}
