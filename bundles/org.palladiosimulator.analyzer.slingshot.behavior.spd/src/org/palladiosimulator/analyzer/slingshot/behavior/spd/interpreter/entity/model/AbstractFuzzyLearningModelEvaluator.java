package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ModelInterpreter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.models.FuzzyLearningModel;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;

public abstract class AbstractFuzzyLearningModelEvaluator extends LearningBasedModelEvaluator {

    record State(Double utilization, Double responseTime, double targetResponseTime) {

        private static double ALPHA = 0.3;
        private static double BETA = 0.5;
        private static double GAMMA = 0.65;
        private static double DELTA = 0.9;

        static State createFromModelAggregators(final AbstractFuzzyLearningModelEvaluator fqlmeval)
                throws NotEmittableException {
            return new State(fqlmeval.workloadAggregator.getResult(), fqlmeval.responseTimeAggregator.getResult(),
                    fqlmeval.targetResponseTime);
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
            // TODO these factors should probably be tuned
            double lambda = 0.5 * this.targetResponseTime;
            double mu = this.targetResponseTime;
            double nu = 1.5 * this.targetResponseTime;
            if (this.responseTime < lambda) {
                fuzzyValues[0] = 1;
            } else if (this.responseTime < mu) {
                fuzzyValues[0] = 1 - (this.utilization - lambda) / (mu - lambda);
                fuzzyValues[1] = 1 - fuzzyValues[0];
            } else if (this.responseTime < nu) {
                fuzzyValues[1] = 1 - (this.utilization - mu) / (nu - mu);
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
    final double discountFactor;
    private final double epsilon;
    protected final double learningRate;
    private final double targetResponseTime;
    private final ModelAggregatorWrapper<OperationResponseTime> responseTimeAggregator;
    private final ModelAggregatorWrapper<?> workloadAggregator;

    protected int[][] partialActions;
    protected double approximatedQValue;

    AbstractFuzzyLearningModelEvaluator(FuzzyLearningModel model) {
        super(false, true);
        final ModelInterpreter modelInterpreter = new ModelInterpreter();
        this.discountFactor = model.getDiscountFactor();
        this.epsilon = model.getEpsilon();
        this.learningRate = model.getLearningRate();
        this.targetResponseTime = model.getTargetResponseTime();
        this.responseTimeAggregator = modelInterpreter.getAggregatorForStimulus(model.getResponseTimeStimulus(), model,
                model.getResponseTimeAggregationMethod());
        this.workloadAggregator = modelInterpreter.getAggregatorForStimulus(model.getWorkloadStimulus(), model);
    }

    double calculateValueFunction(double[][][] qValues) {
        double value = 0;
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                value += this.currentState.getFiringDegree(wl, rt) * Arrays.stream(qValues[wl][rt])
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

    int[][] choosePartialActions(double[][][] qValues) {
        partialActions = new int[3][3];
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                if (Math.random() < this.epsilon) {
                    // Explore
                    partialActions[wl][rt] = ThreadLocalRandom.current()
                        .nextInt(0, 5);
                } else {
                    // Exploit
                    double bestValue = qValues[wl][rt][2];
                    partialActions[wl][rt] = 2;
                    for (int index = 0; index < 5; index++) {
                        if (qValues[wl][rt][index] > bestValue) {
                            bestValue = qValues[wl][rt][index];
                            partialActions[wl][rt] = index;
                        }
                    }
                }
            }
        }
        return partialActions;
    }

    /**
     * Function for approximating the q function by multiplying alphas with the given actions
     * 
     * @param ai
     *            chosen actions for each state, should have size of state space
     * @return
     */
    double approximateQFunction(State state, int[][] ai, double[][][] qValues) {
        double q = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                q = q + state.getFiringDegree(i, j) * qValues[i][j][ai[i][j]];
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