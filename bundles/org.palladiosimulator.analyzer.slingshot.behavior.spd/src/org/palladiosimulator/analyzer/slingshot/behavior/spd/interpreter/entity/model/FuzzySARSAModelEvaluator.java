package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.spd.models.FuzzySARSAModel;

public class FuzzySARSAModelEvaluator extends AbstractFuzzyLearningModelEvaluator {
    private static final Logger LOGGER = Logger.getLogger(FuzzySARSAModelEvaluator.class);
    private long resourceCount;

    private HashMap<Long, double[][][]> qValues;

    public FuzzySARSAModelEvaluator(final FuzzySARSAModel model) {
        super(model);
        // Step 1: Initialize Q-Values
        this.qValues = new HashMap<>();
    }

    @Override
    public void recordUsage(MeasurementMade measurement) {
        super.recordUsage(measurement);
        if (measurement.getEntity()
            .getMetricDesciption()
            .getId()
            .equals(MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS_OVER_TIME.getId())) {
            this.resourceCount = (long) measurement.getEntity()
                .getMeasureForMetric(MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS)
                .getValue();
        }
    }

    @Override
    public void update() throws NotEmittableException {
        this.currentState = State.createFromModelAggregators(this);
        this.qValues.putIfAbsent(this.resourceCount, new double[3][3][5]);
        double[][][] currentQValues = this.qValues.get(this.resourceCount);
        LOGGER.info("Utilization: " + this.currentState.utilization());
        LOGGER.info("Response time: " + this.currentState.responseTime());
        // Step 2: Select an action
        this.partialActions = choosePartialActions(currentQValues);
        // Step 3: Calculate control action a
        double a = calculateControlAction(this.partialActions);
        double previousQValue = this.approximatedQValue;
        // Step 4: Approximate the Q-function
        this.approximatedQValue = this.approximateQFunction(this.currentState, partialActions, currentQValues);
        if (this.previousState != null) {
            double[][][] previousQValues = this.qValues.get(this.resourceCount + this.previousAction);
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            // Step 7: Calculate the error signal
            double errorSignal = reward + this.discountFactor * this.approximatedQValue - previousQValue;
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    previousQValues[wl][rt][partialActions[wl][rt]] += this.learningRate * errorSignal
                            * this.previousState.getFiringDegree(wl, rt);
                }
            }
        }
        // Logging things + setting various things for next iteration
        LOGGER.info("Current Q-Values: ");
        for (int wl = 0; wl < 3; wl++) {
            for (int rt = 0; rt < 3; rt++) {
                LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                        + Arrays.toString(currentQValues[wl][rt]));
            }
        }
        // Step 5: Take action a and let system go to next state (-> in next iteration)
        this.previousAction = (int) Math.round(a);
        this.previousState = this.currentState;
    }

    @Override
    double calculateReward() {
        double reward = super.calculateReward();
        if (reward > 0) {
            // Additional factor discouraging too high container count
            reward /= this.resourceCount;
        }
        return reward;
    }
}
