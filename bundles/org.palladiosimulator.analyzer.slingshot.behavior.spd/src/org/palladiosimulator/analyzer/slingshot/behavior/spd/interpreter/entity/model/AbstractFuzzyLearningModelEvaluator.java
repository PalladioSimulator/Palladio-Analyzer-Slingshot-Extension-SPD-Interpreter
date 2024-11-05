package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ModelInterpreter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
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
            final double lambda = 0.5 * this.targetResponseTime;
            final double mu = this.targetResponseTime;
            final double nu = 1.5 * this.targetResponseTime;
            if (this.responseTime < lambda) {
                fuzzyValues[0] = 1;
            } else if (this.responseTime < mu) {
                fuzzyValues[0] = 1 - (this.responseTime - lambda) / (mu - lambda);
                fuzzyValues[1] = 1 - fuzzyValues[0];
            } else if (this.responseTime < nu) {
                fuzzyValues[1] = 1 - (this.responseTime - mu) / (nu - mu);
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
    final double epsilon;
    protected final double learningRate;
    private final double targetResponseTime;
    private final ModelAggregatorWrapper<OperationResponseTime> responseTimeAggregator;
    private final ModelAggregatorWrapper<?> workloadAggregator;
    final NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en"));
    long containerCount;
    int maxContainerCount;

    protected int[][] partialActions;
    protected double approximatedQValue;

    AbstractFuzzyLearningModelEvaluator(final FuzzyLearningModel model) {
        super(false, true);
        final ModelInterpreter modelInterpreter = new ModelInterpreter();
        this.discountFactor = model.getDiscountFactor();
        this.epsilon = model.getEpsilon();
        this.learningRate = model.getLearningRate();
        this.targetResponseTime = model.getTargetResponseTime();
        this.responseTimeAggregator = modelInterpreter.getAggregatorForStimulus(model.getResponseTimeStimulus(), model,
                model.getResponseTimeAggregationMethod());
        this.workloadAggregator = modelInterpreter.getAggregatorForStimulus(model.getWorkloadStimulus(), model);
        this.nf.setMaximumFractionDigits(3);
        this.nf.setMinimumFractionDigits(3);
        this.nf.setRoundingMode(RoundingMode.UP);
        this.maxContainerCount = model.getMaximumContainers();
    }

    double calculateValueFunction(final double[][][] qValues) {
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

    double calculateControlAction(final State state, final int[][] partialActions) {
        double a = 0;
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                a += state.getFiringDegree(wl, rt) * (partialActions[wl][rt] - 2);
                if (state.getFiringDegree(wl, rt) > 0) {
                    System.out.println("Action " + (partialActions[wl][rt] - 2) + " Firing Degree "
                            + state.getFiringDegree(wl, rt));
                }
            }
        }
        System.out.println("Accumulated action: " + a);
        return a;
    }

    int[][] choosePartialActions(final double[][][] qValues, final double epsilon) {
        this.partialActions = new int[3][3];
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                if (Math.random() < epsilon) {
                    // Explore
                    this.partialActions[wl][rt] = ThreadLocalRandom.current()
                        .nextInt(0, 5);
                } else {
                    // Exploit
                    double bestValue = qValues[wl][rt][2];
                    this.partialActions[wl][rt] = 2;
                    for (int index = 0; index < 5; index++) {
                        if (qValues[wl][rt][index] > bestValue) {
                            bestValue = qValues[wl][rt][index];
                            this.partialActions[wl][rt] = index;
                        }
                    }
                }
            }
        }
        return this.partialActions;
    }

    /**
     * Function for approximating the q function by multiplying alphas with the given actions
     *
     * @param ai
     *            chosen actions for each state, should have size of state space
     * @return
     */
    double approximateQFunction(final State state, final int[][] ai, final double[][][] qValues) {
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

    String arrayToString(final double[] array) {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(this.nf.format(array[i]));
            if (i != array.length - 1) {
                sb.append(", ");
            }
        }
        return sb.append("]")
            .toString();
    }

    @Override
    public void recordUsage(final MeasurementMade measurement) {
        this.responseTimeAggregator.aggregateMeasurement(measurement);
        this.workloadAggregator.aggregateMeasurement(measurement);
        if (measurement.getEntity()
            .getMetricDesciption()
            .getId()
            .equals(MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS_OVER_TIME.getId())) {
            this.containerCount = (long) measurement.getEntity()
                .getMeasureForMetric(MetricDescriptionConstants.NUMBER_OF_RESOURCE_CONTAINERS)
                .getValue();
        }
    }

    @Override
    public int getDecision() throws NotEmittableException {
        return (int) Math.min(this.previousAction, this.maxContainerCount - this.containerCount);
    }

    double[][][] getQValuesWithKnowledge() {
        final double[][][] q = new double[3][3][5];
//        for (int wl = 0; wl < 3; wl += 1) {
//            for (int rt = 0; rt < 3; rt += 1) {
//                if (wl == 0 && rt != 2) {
//                    if (rt == 1) {
//                        q[wl][rt][1] = 0.2;
//                        q[wl][rt][0] = 0.1;
//                    } else {
//                        q[wl][rt][1] = 0.1;
//                        q[wl][rt][0] = 0.2;
//                    }
//                } else if (wl == 2 && rt != 1) {
//                    if (rt == 2) {
//                        q[wl][rt][4] = 0.2;
//                        q[wl][rt][3] = 0.1;
//                    } else {
//                        q[wl][rt][4] = 0.1;
//                        q[wl][rt][3] = 0.2;
//                    }
//                } else if (wl == 1 && rt == 0) {
//                    q[wl][rt][0] = 0.1;
//                    q[wl][rt][1] = 0.2;
//                    q[wl][rt][2] = 0.1;
//                } else {
//                    q[wl][rt][1] = 0.1;
//                    q[wl][rt][2] = 0.2;
//                    q[wl][rt][3] = 0.1;
//                }
//            }
//        }
        return q;

    }

}