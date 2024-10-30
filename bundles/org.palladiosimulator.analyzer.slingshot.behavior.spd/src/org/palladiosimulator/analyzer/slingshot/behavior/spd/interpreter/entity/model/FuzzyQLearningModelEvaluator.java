package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;

public class FuzzyQLearningModelEvaluator extends AbstractFuzzyLearningModelEvaluator {

    private static final Logger LOGGER = Logger.getLogger(FuzzyQLearningModelEvaluator.class);
    private final double[][][] qValues;
    private int iterationCount;
    private long previousContainerCount;

    public FuzzyQLearningModelEvaluator(final FuzzyQLearningModel model) {
        super(model);
        // Step 1: Initialize Q-Values
        this.qValues = this.getQValuesWithKnowledge();
        this.iterationCount = 0;
    }

    @Override
    public void update() throws NotEmittableException {
        this.currentState = State.createFromModelAggregators(this);
        final double currentEpsilon = Math.max(Math.exp(-this.epsilon * this.iterationCount), 0.1);
        LOGGER.info("Utilization: " + this.nf.format(this.currentState.utilization()) + " ("
                + this.arrayToString(this.currentState.getFuzzyUtil()) + ")" + ", current Epsilon = "
                + this.nf.format(currentEpsilon));
        LOGGER.info("Response time: " + this.nf.format(this.currentState.responseTime()) + " ("
                + this.arrayToString(this.currentState.getFuzzyResponseTime()) + ")");
        if (this.previousState != null) {
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            final double value = this.calculateValueFunction(this.qValues);
            LOGGER.info("Approximated State Value: " + this.nf.format(value) + ", previously approximated Q-Value "
                    + this.nf.format(this.approximatedQValue));
            // Step 7: Calculate the error signal
            final double errorSignal = reward + this.discountFactor * value - this.approximatedQValue;
            LOGGER.info("Old Q-Values: ");
            for (int wl = 0; wl < 3; wl++) {
                for (int rt = 0; rt < 3; rt++) {
                    if (this.previousState.getFiringDegree(wl, rt) > 0) {
                        LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                                + this.arrayToString(this.qValues[wl][rt]) + ", firing degree "
                                + this.nf.format(this.previousState.getFiringDegree(wl, rt)) + ", partial action "
                                + (this.partialActions[wl][rt] - 2));
                    }
                }
            }
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    this.qValues[wl][rt][this.partialActions[wl][rt]] += this.learningRate * errorSignal
                            * this.previousState.getFiringDegree(wl, rt);
                }
            }
            LOGGER.info("Updated Q-Values: ");
            for (int wl = 0; wl < 3; wl++) {
                for (int rt = 0; rt < 3; rt++) {
                    if (this.previousState.getFiringDegree(wl, rt) > 0) {
                        LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                                + this.arrayToString(this.qValues[wl][rt]));
                    }
                }
            }
        }
        // Step 2: Select an action
        this.partialActions = this.choosePartialActions(this.qValues, currentEpsilon);
        this.iterationCount++;
        // Step 3: Calculate control action a
        final double a = this.calculateControlAction(this.currentState, this.partialActions);
        // Step 4: Approximate the Q-function
        this.approximatedQValue = this.approximateQFunction(this.currentState, this.partialActions, this.qValues);
        // Logging things + setting various things for next iteration
        // Step 5: Take action a and let system go to next state (-> in next iteration)
        this.previousAction = (int) Math.round(a);
        this.previousState = this.currentState;
        this.previousContainerCount = this.containerCount;
    }

    @Override
    double calculateReward() {
        double reward = super.calculateReward();
        if (reward > 0) {
            // Additional factor discouraging too high container count
            reward /= this.containerCount;
        }
        if (this.previousAction != null) {
            if (this.previousAction > 0) {
                // Small penalty for scaling up
                reward -= Math.min(this.previousAction, this.maxContainerCount - this.previousContainerCount) * 0.4;
            } else if (this.previousAction < 0) {
                // Small reward for scaling down
                reward += Math.min(-this.previousAction, this.previousContainerCount - 1) * 0.2;
            }
        }
        return reward;
    }

    @Override
    public void printTrainedModel() {
        // TODO Auto-generated method stub
    }
}