package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.AnyStimulusAggregator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ManagedElementAggregator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.FuzzyQLearningModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.ModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.RandomModelEvaluator;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;
import org.palladiosimulator.spd.models.LearningBasedModel;
import org.palladiosimulator.spd.models.RandomModel;
import org.palladiosimulator.spd.models.util.ModelsSwitch;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;
import org.palladiosimulator.spd.triggers.stimuli.ManagedElementsStateStimulus;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

public class ModelInterpreter extends ModelsSwitch<ModelEvaluator> {

    @SuppressWarnings("rawtypes")
    public ModelAggregatorWrapper getAggregatorForStimulus(final Stimulus stimulus, final LearningBasedModel model) {
        final double windowSize = model.getInterval();
        if (stimulus instanceof final ManagedElementsStateStimulus managedElementsStateStimulus) {
            return new ManagedElementAggregator<>(managedElementsStateStimulus, windowSize);
        } else {
            // TODO currently using average aggregation by default for non-aggregated
            // stimuli, this might need to be changed
            return this.getAggregatorForStimulus(stimulus, model, AGGREGATIONMETHOD.AVERAGE);
        }
    }

    public <T extends Stimulus> ModelAggregatorWrapper<T> getAggregatorForStimulus(final T stimulus,
            final LearningBasedModel model, final AGGREGATIONMETHOD aggregationMethod) {
        final double windowSize = model.getInterval();
        return new AnyStimulusAggregator<>(stimulus, windowSize, aggregationMethod);
    }

    @Override
    public ModelEvaluator caseRandomModel(final RandomModel model) {
        return new RandomModelEvaluator(model);
    }

    @Override
    public ModelEvaluator caseFuzzyQLearningModel(FuzzyQLearningModel model) {
        return new FuzzyQLearningModelEvaluator(model);
    }
}
