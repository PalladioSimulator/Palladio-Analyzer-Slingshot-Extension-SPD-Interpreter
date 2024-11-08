package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.AnyStimulusAggregator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ManagedElementAggregator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.FuzzyQLearningModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.FuzzySARSAModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.ModelEvaluator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model.RandomModelEvaluator;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;
import org.palladiosimulator.spd.models.FuzzySARSAModel;
import org.palladiosimulator.spd.models.LearningBasedModel;
import org.palladiosimulator.spd.models.RandomModel;
import org.palladiosimulator.spd.models.util.ModelsSwitch;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;
import org.palladiosimulator.spd.triggers.stimuli.ManagedElementsStateStimulus;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

public class ModelInterpreter extends ModelsSwitch<ModelEvaluator> {

    private double intervalDuration;
    private int maxContainerCount;
    private int minContainerCount;

    public ModelInterpreter(double intervalDuration, final int minContainerCount, final int maxContainerCount) {
        this.intervalDuration = intervalDuration;
        this.minContainerCount = minContainerCount;
        this.maxContainerCount = maxContainerCount;
    }

    @SuppressWarnings("rawtypes")
    public ModelAggregatorWrapper getAggregatorForStimulus(final Stimulus stimulus, final LearningBasedModel model) {
        if (stimulus instanceof final ManagedElementsStateStimulus managedElementsStateStimulus) {
            return new ManagedElementAggregator<>(managedElementsStateStimulus, this.intervalDuration);
        } else {
            // TODO currently using average aggregation by default for non-aggregated
            // stimuli, this might need to be changed
            return this.getAggregatorForStimulus(stimulus, model, AGGREGATIONMETHOD.AVERAGE);
        }
    }

    public <T extends Stimulus> ModelAggregatorWrapper<T> getAggregatorForStimulus(final T stimulus,
            final LearningBasedModel model, final AGGREGATIONMETHOD aggregationMethod) {
        return new AnyStimulusAggregator<>(stimulus, this.intervalDuration, aggregationMethod);
    }

    @Override
    public ModelEvaluator caseRandomModel(final RandomModel model) {
        return new RandomModelEvaluator(model);
    }

    @Override
    public ModelEvaluator caseFuzzySARSAModel(FuzzySARSAModel model) {
        return new FuzzySARSAModelEvaluator(model, this);
    }

    @Override
    public ModelEvaluator caseFuzzyQLearningModel(FuzzyQLearningModel model) {
        return new FuzzyQLearningModelEvaluator(model, this);
    }

    public int getMinContainerCount() {
        return minContainerCount;
    }

    public int getMaxContainerCount() {
        return maxContainerCount;
    }
}
