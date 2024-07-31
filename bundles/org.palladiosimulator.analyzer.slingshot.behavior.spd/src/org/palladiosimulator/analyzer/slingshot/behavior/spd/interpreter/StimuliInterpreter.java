package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ScalingTriggerInterpreter.InterpretationResult;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.CPUUtilizationTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.OperationResponseTimeTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.QueueLengthTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.SimulationTimeChecker;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.TaskCountTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.Subscriber;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.targets.CompetingConsumersGroup;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.expectations.ExpectedCount;
import org.palladiosimulator.spd.triggers.expectations.ExpectedPercentage;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.expectations.ExpectedValue;
import org.palladiosimulator.spd.triggers.stimuli.CPUUtilization;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;
import org.palladiosimulator.spd.triggers.stimuli.QueueLength;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;
import org.palladiosimulator.spd.triggers.stimuli.TaskCount;
import org.palladiosimulator.spd.triggers.stimuli.util.StimuliSwitch;

import com.google.common.base.Preconditions;

final class StimuliInterpreter extends StimuliSwitch<InterpretationResult> {

	private final ScalingTriggerInterpreter scalingTriggerInterpreter;
	private final SimpleFireOnValue trigger;

	public StimuliInterpreter(final ScalingTriggerInterpreter scalingTriggerInterpreter, final SimpleFireOnValue trigger) {
		super();
		this.scalingTriggerInterpreter = scalingTriggerInterpreter;
		this.trigger = trigger;
	}



	@Override
	public InterpretationResult caseSimulationTime(final SimulationTime object) {
		final ExpectedTime expectedTime = this.checkExpectedValue(ExpectedTime.class);

		final SimulationTimeReached event = new SimulationTimeReached(this.scalingTriggerInterpreter.policy.getTargetGroup().getId(), expectedTime.getValue());

		return (new InterpretationResult()).scheduleEvent(event)
										   .listenEvent(Subscriber.builder(SimulationTimeReached.class)
												   				  .name("something"))
										   .triggerChecker(new SimulationTimeChecker(this.trigger));
	}



	@Override
	public InterpretationResult caseOperationResponseTime(final OperationResponseTime object) {
		this.checkExpectedValue(ExpectedTime.class);
		return (new InterpretationResult()).listenEvent(Subscriber.builder(MeasurementMade.class)
																  .name("measurementMade"))
									       .triggerChecker(new OperationResponseTimeTriggerChecker(this.trigger));
	}
	
	@Override
	public InterpretationResult caseCPUUtilization(final CPUUtilization object) {
		final ExpectedPercentage expectedPercentage = this.checkExpectedValue(ExpectedPercentage.class);
		Preconditions.checkArgument(0 <= expectedPercentage.getValue() && expectedPercentage.getValue() <= 100, 
									"The expected percentage must be between 0 and 100");
		
		
		return (new InterpretationResult()).listenEvent(Subscriber.builder(MeasurementMade.class)
																  .name("cpuUtilizationMade"))
										   .triggerChecker(new CPUUtilizationTriggerChecker(
												   				   this.trigger, 
																   object, 
																   this.scalingTriggerInterpreter.policy.getTargetGroup())
												   		  );
	}

	@Override
	public InterpretationResult caseTaskCount(final TaskCount object) {
		this.checkExpectedValue(ExpectedCount.class);
		
		return (new InterpretationResult()).listenEvent(Subscriber.builder(MeasurementMade.class)
																  .name("taskCount"))
										   .triggerChecker(new TaskCountTriggerChecker(this.trigger, object, this.scalingTriggerInterpreter.policy.getTargetGroup()));
	}


	@Override
	public InterpretationResult caseQueueLength(final QueueLength object) {
		if (!(this.scalingTriggerInterpreter.policy.getTargetGroup() instanceof CompetingConsumersGroup)) {
			throw new IllegalArgumentException("The QueueLength trigger is only for CompetingConsumersGroup");
		}

		return (new InterpretationResult()).listenEvent(Subscriber.builder(MeasurementMade.class).name("queueLength"))
				.triggerChecker(new QueueLengthTriggerChecker(this.trigger, object));
	}

	@SuppressWarnings("unchecked")
	private <T extends ExpectedValue> T checkExpectedValue(final Class<T> expectedType) {
		if (!(expectedType.isAssignableFrom(this.trigger.getExpectedValue().getClass()))) {
			throw new IllegalArgumentException(String.format("It is only possible "
					+ "that the trigger is of type %s, but the given type was %s",
					expectedType.getSimpleName(),
					this.trigger.getExpectedValue().getClass().getSimpleName()));
		}
		
		return (T) this.trigger.getExpectedValue();
	}
}