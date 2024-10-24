package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;

public class FuzzyQLearningModelEvaluator extends AbstractFuzzyLearningModelEvaluator {

    private static final Logger LOGGER = Logger.getLogger(FuzzyQLearningModelEvaluator.class);
    private final double[][][] qValues;
    private int iterationCount;

    public FuzzyQLearningModelEvaluator(final FuzzyQLearningModel model) {
        super(model);
        // Step 1: Initialize Q-Values
        this.qValues = this.getQValuesWithKnowledge();
        this.iterationCount = 0;
    }

    @Override
    public void update() throws NotEmittableException {
        this.currentState = State.createFromModelAggregators(this);
        LOGGER.info("Utilization: " + this.currentState.utilization());
        LOGGER.info("Response time: " + this.currentState.responseTime());
        if (this.previousState != null) {
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            final double value = this.calculateValueFunction(this.qValues);
            // Step 7: Calculate the error signal
            final double errorSignal = reward + this.discountFactor * value - this.approximatedQValue;
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    this.qValues[wl][rt][this.partialActions[wl][rt]] += this.learningRate * errorSignal
                            * this.previousState.getFiringDegree(wl, rt);
                }
            }
        }
        // Step 2: Select an action
        this.partialActions = this.choosePartialActions(this.qValues,
                Math.max(Math.exp(-this.epsilon * this.iterationCount), 0.01));
        this.iterationCount++;
        // Step 3: Calculate control action a
        final double a = this.calculateControlAction(this.currentState, this.partialActions);
        // Step 4: Approximate the Q-function
        this.approximatedQValue = this.approximateQFunction(this.currentState, this.partialActions, this.qValues);
        // Logging things + setting various things for next iteration
        LOGGER.info("Current Q-Values: ");
        for (int wl = 0; wl < 3; wl++) {
            for (int rt = 0; rt < 3; rt++) {
                LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                        + this.arrayToString(this.qValues[wl][rt]));
            }
        }
        // Step 5: Take action a and let system go to next state (-> in next iteration)
        this.previousAction = (int) Math.round(a);
        this.previousState = this.currentState;
    }

    @Override
    public void printTrainedModel() {
        // TODO Auto-generated method stub
    }
}