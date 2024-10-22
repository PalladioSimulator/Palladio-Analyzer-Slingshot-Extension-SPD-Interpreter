package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ModelInterpreter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;

public class FuzzyQLearningModelEvaluator extends LearningBasedModelEvaluator {

    private record State(Double utilization, Double responseTime) {

        private static double ALPHA = 0.2;
        private static double BETA = 0.4;
        private static double GAMMA = 0.65;
        private static double DELTA = 0.9;
        private static double LAMBDA;
        private static double MU;
        private static double NU;

        static State createFromModelAggregators(final FuzzyQLearningModelEvaluator fqlmeval)
                throws NotEmittableException {
            return new State(fqlmeval.workloadAggregator.getResult(), fqlmeval.responseTimeAggregator.getResult());
        }

        double[] getFuzzyUtil() {
            final double[] fuzzyValues = { 0.0, 0.0, 0.0 };
            if (this.utilization < State.ALPHA) {
                fuzzyValues[0] = 1;
            } else if (this.utilization < State.BETA) {
                fuzzyValues[0] = 1 - (this.utilization - State.ALPHA) / (State.BETA - State.ALPHA);
                fuzzyValues[1] = 1 - fuzzyValues[0];
            } else if (this.utilization < GAMMA) {
                fuzzyValues[1] = 1;
            } else if (this.utilization < State.DELTA) {
                fuzzyValues[1] = 1 - (this.utilization - State.GAMMA) / (State.DELTA - State.GAMMA);
                fuzzyValues[2] = 1 - fuzzyValues[1];
            } else {
                fuzzyValues[2] = 1;
            }
            return fuzzyValues;
        }

        double[] getFuzzyResponseTime() {
            final double[] fuzzyValues = { 0.0, 0.0, 0.0 };
            if (this.responseTime < State.LAMBDA) {
                fuzzyValues[0] = 1;
            } else if (this.responseTime < State.MU) {
                fuzzyValues[0] = 1 - (this.utilization - State.LAMBDA) / (State.MU - State.LAMBDA);
                fuzzyValues[1] = 1 - fuzzyValues[0];
            } else if (this.responseTime < NU) {
                fuzzyValues[1] = 1 - (this.utilization - State.MU) / (State.NU - State.MU);
                fuzzyValues[2] = 1 - fuzzyValues[1];
            } else {
                fuzzyValues[2] = 1;
            }
            return fuzzyValues;
        }

        /**
         * Returns the firing degree of the state for the given (fuzzy) workload and response time
         * (given as integers here)
         *
         * @param fuzzyWLState
         *            fuzzy workload
         * @param fuzzyRTState
         *            fuzzy response time
         * @return Firing degree, somewhere between 0.0 and 1.0
         */
        double getFiringDegree(final int fuzzyWLState, final int fuzzyRTState) {
            return this.getFuzzyResponseTime()[fuzzyRTState] * this.getFuzzyUtil()[fuzzyWLState];
        }

        double[][] getFiringDegrees() {
            final double[] fuzzyRT = this.getFuzzyResponseTime();
            final double[] fuzzyWL = this.getFuzzyUtil();
            final double[][] firingDegrees = new double[3][3];
            for (int wl = 0; wl < 3; wl++) {
                for (int rt = 0; rt < 3; rt++) {
                    firingDegrees[wl][rt] = fuzzyWL[wl] * fuzzyRT[rt];
                }
            }
            return firingDegrees;
        }
    }

    State previousState = null;
    Integer previousAction = null;
    State currentState = null;
    private final double discountFactor;
    private final double epsilon;
    private final double learningRate;
    private final double targetResponseTime;
    private final ModelAggregatorWrapper<OperationResponseTime> responseTimeAggregator;
    private final ModelAggregatorWrapper<?> workloadAggregator;
    private final double[][][] qValues;

    private int[][] ai;
    private double approximatedQValue;
    private static final Logger LOGGER = Logger.getLogger(FuzzyQLearningModelEvaluator.class);

    public FuzzyQLearningModelEvaluator(final FuzzyQLearningModel model) {
        super(false, true);
        final ModelInterpreter modelInterpreter = new ModelInterpreter();
        this.discountFactor = model.getDiscountFactor();
        this.epsilon = model.getEpsilon();
        this.learningRate = model.getLearningRate();
        this.targetResponseTime = model.getTargetResponseTime();
        this.responseTimeAggregator = modelInterpreter.getAggregatorForStimulus(model.getResponseTimeStimulus(), model,
                model.getResponseTimeAggregationMethod());
        this.workloadAggregator = modelInterpreter.getAggregatorForStimulus(model.getWorkloadStimulus(), model);
        // Step 1: Initialize Q-Values
        this.qValues = new double[3][3][5];
        // TODO this should probably be tuned
        State.LAMBDA = 0.5 * this.targetResponseTime;
        State.MU = this.targetResponseTime;
        State.NU = 1.5 * this.targetResponseTime;
    }

    @Override
    public void update() throws NotEmittableException {
        this.currentState = State.createFromModelAggregators(this);
        // TODO the following two lines are there only for debug purposes, remove them
        State currentState = this.currentState;
        double[][] firingDegrees = this.currentState.getFiringDegrees();
        LOGGER.info("Utilization: " + this.currentState.utilization);
        LOGGER.info("Response time: " + this.currentState.responseTime);
        if (this.previousState != null) {
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            double value = calculateValueFunction();
            // Step 7: Calculate the error signal
            double errorSignal = reward + this.discountFactor * value - this.approximatedQValue;
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    this.qValues[wl][rt][ai[wl][rt]] += this.learningRate * errorSignal
                            * this.previousState.getFiringDegree(wl, rt);
                }
            }
        }
        // Step 2: Select an action
        this.ai = choosePartialAction();
        // Step 3: Calculate control action a
        double a = calculateControlAction(this.ai);
        // Step 4: Approximate the Q-function
        this.approximatedQValue = this.approximateQFunction(this.currentState, ai);
        // Logging things + setting various things for next iteration
        LOGGER.info("Current Q-Values: ");
        for (int wl = 0; wl < 3; wl++) {
            for (int rt = 0; rt < 3; rt++) {
                LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                        + Arrays.toString(this.qValues[wl][rt]));
            }
        }
        // Step 5: Take action a and let system go to next state (-> in next iteration)
        this.previousAction = (int) Math.round(a);
        this.previousState = this.currentState;
    }

    double calculateValueFunction() {
        double value = 0;
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                value += this.currentState.getFiringDegree(wl, rt) * Arrays.stream(this.qValues[wl][rt])
                    .max()
                    .getAsDouble();
            }
        }
        return value;
    }

    double calculateControlAction(int[][] partialActions) {
        double a = 0;
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                a += this.currentState.getFiringDegree(wl, rt) * (partialActions[wl][rt] - 2);
            }
        }
        return a;
    }

    int[][] choosePartialAction() {
        ai = new int[3][3];
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                if (Math.random() < this.epsilon) {
                    // Explore
                    ai[wl][rt] = ThreadLocalRandom.current()
                        .nextInt(0, 5);
                } else {
                    // Exploit
                    double bestValue = this.qValues[wl][rt][2];
                    ai[wl][rt] = 2;
                    for (int index = 0; index < 5; index++) {
                        if (this.qValues[wl][rt][index] > bestValue) {
                            bestValue = this.qValues[wl][rt][index];
                            ai[wl][rt] = index;
                        }
                    }
                }
            }
        }
        return ai;
    }

    /**
     * Function for approximating the q function by multiplying alphas with the given actions
     * 
     * @param ai
     *            chosen actions for each state, should have size of state space
     * @return
     */
    double approximateQFunction(State state, int[][] ai) {
        double q = 0;
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j <= 3; j++) {
                q = q + state.getFiringDegree(i, j) * this.qValues[i][j][ai[i][j]];
            }
        }
        return q;
    }

    double calculateReward() {
        if (this.currentState.responseTime < this.targetResponseTime) {
            return Math.exp(this.currentState.utilization); // Higher utilization should yield
                                                            // higher rewards after all!
        } else if (this.currentState.responseTime < this.previousState.responseTime && this.previousAction > 0) {
            return 0;
        } else {
            return Math.exp((this.targetResponseTime - this.currentState.responseTime) / this.targetResponseTime) - 1;
        }
    }

    @Override
    void recordRewardMeasurement(final MeasurementMade measurement) {
        // Not needed as all aggregation is performed inside recordUsage
    }

    @Override
    public void recordUsage(final MeasurementMade measurement) {
        this.responseTimeAggregator.aggregateMeasurement(measurement);
        this.workloadAggregator.aggregateMeasurement(measurement);
    }

    @Override
    public void printTrainedModel() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getDecision() throws NotEmittableException {
        return this.previousAction;
    }

}