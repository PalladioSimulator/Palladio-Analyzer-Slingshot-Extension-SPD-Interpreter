package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ModelInterpreter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.spd.models.FuzzySARSAModel;

public class FuzzySARSAModelEvaluator extends AbstractFuzzyLearningModelEvaluator {
    private static final Logger LOGGER = Logger.getLogger(FuzzySARSAModelEvaluator.class);

    private final HashMap<Long, double[][][]> qValues;
    private int iterationCount;

    public FuzzySARSAModelEvaluator(final FuzzySARSAModel model, final ModelInterpreter modelInterpreter) {
        super(model, modelInterpreter);
        // Step 1: Initialize Q-Values
        this.qValues = new HashMap<>();
        this.iterationCount = 0;
    }

    @Override
    public void update() throws NotEmittableException {
        this.currentState = State.createFromModelAggregators(this);
        this.qValues.putIfAbsent(this.containerCount, this.getQValuesWithKnowledge());
        this.iterationCount += 1;
        final double currentEpsilon = Math.max(Math.exp(-this.epsilon * this.iterationCount), 0.1);
        final double[][][] currentQValues = this.qValues.get(this.containerCount);
        LOGGER.info("Utilization: " + this.nf.format(this.currentState.utilization()) + " ("
                + this.arrayToString(this.currentState.getFuzzyUtil()) + ")" + ", current Epsilon = "
                + this.nf.format(currentEpsilon));
        LOGGER.info("Response time: " + this.nf.format(this.currentState.responseTime()) + " ("
                + this.arrayToString(this.currentState.getFuzzyResponseTime()) + ")");
        // Step 2: Select an action
        final int[][] previousPartialActions = this.partialActions;
        this.partialActions = this.choosePartialActions(currentQValues, currentEpsilon);
        // Step 3: Calculate control action a
        final double a = this.calculateControlAction(this.currentState, this.partialActions);
        final double previousQValue = this.approximatedQValue;
        // Step 4: Approximate the Q-function
        this.approximatedQValue = this.approximateQFunction(this.currentState, this.partialActions, currentQValues);
        if (this.previousState != null) {
            final double[][][] previousQValues = this.qValues.get(this.previousContainerCount);
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            // Step 7: Calculate the error signal
            final double errorSignal = reward + this.discountFactor * this.approximatedQValue - previousQValue;
            LOGGER.info("Approximated Q-Values: " + this.nf.format(this.approximatedQValue) + ", previously "
                    + this.nf.format(previousQValue));
            LOGGER.info("Error signal: " + errorSignal);
            LOGGER.info("Old Q-Values for state " + this.previousContainerCount + ": ");
            for (int wl = 0; wl < 3; wl++) {
                for (int rt = 0; rt < 3; rt++) {
                    if (this.previousState.getFiringDegree(wl, rt) > 0) {
                        LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                                + this.arrayToString(previousQValues[wl][rt]) + ", firing degree "
                                + this.nf.format(this.previousState.getFiringDegree(wl, rt)) + ", partial action "
                                + (previousPartialActions[wl][rt] - 2));
                    }
                }
            }
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    previousQValues[wl][rt][previousPartialActions[wl][rt]] += this.learningRate * errorSignal
                            * this.previousState.getFiringDegree(wl, rt);
                }
            }
            LOGGER.info("Updated Q-Values for state " + this.previousContainerCount + ": ");
            for (int wl = 0; wl < 3; wl++) {
                for (int rt = 0; rt < 3; rt++) {
                    if (this.previousState.getFiringDegree(wl, rt) > 0) {
                        LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                                + this.arrayToString(previousQValues[wl][rt]));
                    }
                }
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
            reward /= this.containerCount;
        }
        if (this.previousAction != null) {
            if (this.previousAction > 0) {
                // Small penalty for scaling up
                reward -= (this.containerCount - this.previousContainerCount) * 4;
            } else if (this.previousAction < 0) {
                // Small reward for scaling down
                reward += (this.containerCount - this.previousContainerCount) * 2;
            }
        }
        return reward;
    }

    @Override
    public void printTrainedModel() {
        // TODO Auto-generated method stub
    }
}
