package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.Collections;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;

public abstract class LearningBasedModelEvaluator extends ModelEvaluator {

    final List<ModelAggregatorWrapper<?>> aggregatorList;

    /**
     * @param stimuliListeners
     *            For all contained ModelAggregaatorWrappers, received measurements will be
     *            aggregated (if the {@link #recordUsage(MeasurementMade)} method isn't overwritten)
     */
    LearningBasedModelEvaluator(List<ModelAggregatorWrapper<?>> stimuliListeners, boolean changeOnStimulus,
            boolean changeOnInterval) {
        super();
        this.aggregatorList = stimuliListeners;
        this.changeOnStimulus = changeOnStimulus;
        this.changeOnInterval = changeOnInterval;
    }

    /**
     * If this method is used, the {@link #recordUsage(MeasurementMade)} method should be
     * overwritten by a child class, as no Stimuli Listeners are given. If this is not intended (and
     * some stimulus aggregation needs to happen) use the constructor
     * {@link #LearningBasedModelEvaluator(List, boolean, boolean)
     */
    protected LearningBasedModelEvaluator(boolean changeOnStimulus, boolean changeOnInterval) {
        super();
        this.aggregatorList = Collections.emptyList();
        this.changeOnStimulus = changeOnStimulus;
        this.changeOnInterval = changeOnInterval;
    }

    @Override
    public void recordUsage(MeasurementMade measurement) {
        for (ModelAggregatorWrapper<?> modelAggregatorWrapper : aggregatorList) {
            modelAggregatorWrapper.aggregateMeasurement(measurement);
        }
    }

    public abstract void update() throws NotEmittableException;

    abstract void recordRewardMeasurement(MeasurementMade measurement);

    /**
     * Print the values that were obtained during the training phase of the model (so far). Which
     * values depend on the model, this could i.e. be the Q-Values, some determined thresholds or
     * similar.
     */
    public abstract void printTrainedModel();
}
