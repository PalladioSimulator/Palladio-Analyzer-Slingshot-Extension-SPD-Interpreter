package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ManagedElementAggregator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.models.ModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.models.QThresholdsModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.models.RandomModelEvaluator;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.adjustments.models.QThresholdsModel;
import org.palladiosimulator.spd.adjustments.models.RandomModel;
import org.palladiosimulator.spd.adjustments.models.util.ModelsSwitch;
import org.palladiosimulator.spd.triggers.stimuli.AggregatedStimulus;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

public class ModelInterpreter extends ModelsSwitch<ModelEvaluator> {

    private EList<Stimulus> stimuli;
    private ScalingTriggerInterpreter triggerInterpreter;

    public ModelInterpreter(EList<Stimulus> stimuli, ScalingTriggerInterpreter triggerInterpreter) {
        this.stimuli = stimuli;
        this.triggerInterpreter = triggerInterpreter;
    }

    private List<ModelAggregatorWrapper> getStimuliListeners() {
        List<ModelAggregatorWrapper> stimuliListenerList = new ArrayList<>();
        for (Stimulus stimulus : this.stimuli) {
            if (stimulus instanceof AggregatedStimulus aggregatedStimulus) {
                stimuliListenerList.add(new ManagedElementAggregator<>(aggregatedStimulus,
                        this.triggerInterpreter.policy.getTargetGroup(),
                        MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE_TUPLE,
                        MetricDescriptionConstants.UTILIZATION_OF_ACTIVE_RESOURCE));
            }
        }
        return stimuliListenerList;
    }

    @Override
    public ModelEvaluator caseRandomModel(RandomModel object) {
        return new RandomModelEvaluator();
    }

    @Override
    public ModelEvaluator caseQThresholdsModel(QThresholdsModel object) {
        // TODO Auto-generated method stub
        // TODO IMPORTANT return a model
        return new QThresholdsModelEvaluator(object, getStimuliListeners());
    }
}