package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ModelInterpreter;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ModelAggregatorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger.ModelBasedTriggerChecker;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.spd.models.FuzzyQLearningModel;
import org.palladiosimulator.spd.triggers.stimuli.OperationResponseTime;

public class FuzzyQLearningModelEvaluator extends LearningBasedModelEvaluator {

    private record State(Double utilization, Double responseTime) {

        private static double ALPHA = 0;
        private static double BETA = 0;
        private static double GAMMA = 0;
        private static double DELTA = 0;
        private static double LAMBDA = 0;
        private static double MU = 0;
        private static double NU = 0;

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
            if (this.utilization < State.LAMBDA) {
                fuzzyValues[0] = 1;
            } else if (this.utilization < State.MU) {
                fuzzyValues[0] = 1 - (this.utilization - State.LAMBDA) / (State.MU - State.LAMBDA);
                fuzzyValues[1] = 1 - fuzzyValues[0];
            } else if (this.utilization < NU) {
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
    }

    State old_state = null;
    Integer old_action = null;
    State current_state = null;
    private final double discountFactor;
    private final double epsilon;
    private final double learningRate;
    private final double targetResponseTime;
    private final ModelAggregatorWrapper<OperationResponseTime> responseTimeAggregator;
    private final ModelAggregatorWrapper workloadAggregator;
    private final double[][][] qValues;

    private final Map<Double, Map<Double, Map<Long, Double>>> Q;
    private int[][] ai;
    private double approximatedQValue;
    private static final Logger LOGGER = Logger.getLogger(ModelBasedTriggerChecker.class);

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
        this.qValues = new double[3][3][5];
        Q = new HashMap<>();
    }

    private int fuzzy_util(final double util) {
        return 0;
    }

    private int fuzzy_response(final double response) {
        return 0;
    }

    /**
     * This function first calculates the firing degree (for the current state in
     * {@link #current_state} and then multiplies it with the corresponding action and sums these
     * multiplications up to get something that is called "control action" in the original paper
     * 
     * @return
     */
    private double[][] calculateFuzzyOutput() {
        final double[][] alpha = new double[3][3];
        for (int wl = 0; wl <= 2; wl += 1) {
            for (int rt = 0; rt <= 2; rt += 1) {
                alpha[wl][rt] = 1.0;
            }
        }
        // final double consequent = 0; // TODO Where to get the consequent from?
        for (int wl = 0; wl <= 2; wl += 1) {
            for (int rt = 0; rt <= 2; rt += 1) {
                alpha[wl][rt] *= this.current_state.getFiringDegree(wl, rt);
            }
        }
        return alpha;
    }

    @Override
    public void update() throws NotEmittableException {
        this.current_state = State.createFromModelAggregators(this);
        LOGGER.info("Utilization: " + this.current_state.utilization);
        if (this.old_state != null) {
            // Step 6: Observe the reinforcement signal r(t + 1) + calculate value for new state
            // TODO currently using reward from MathLab implementation, not from paper!
            final double reward = this.calculateReward();
            LOGGER.info("Reward (for the last period): " + reward);
            double value = 0;
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    value += this.current_state.getFiringDegree(wl, rt) * Arrays.stream(this.qValues[wl][rt])
                        .max()
                        .getAsDouble();
                }
            }
            // Step 7: Calculate the error signal
            double errorSignal = reward + this.discountFactor * value - this.approximatedQValue;
            // Step 8: Update q-Values
            for (int wl = 0; wl < 3; wl += 1) {
                for (int rt = 0; rt < 3; rt += 1) {
                    this.qValues[wl][rt][ai[wl][rt]] += this.learningRate * errorSignal
                            * this.old_state.getFiringDegree(wl, rt);
                }
            }
        }
        // this below basically copies the implementation from `fqlearn`
        this.ai = new int[3][3];
        double a = 0;
        // fuzzy_action_selector
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
        double qValueChange = 0;
        for (int wl = 0; wl < 3; wl += 1) {
            for (int rt = 0; rt < 3; rt += 1) {
                qValueChange += this.current_state.getFiringDegree(wl, rt) * this.qValues[wl][rt][ai[wl][rt]];
                a += this.current_state.getFiringDegree(wl, rt) * (ai[wl][rt] - 2);
            }
        }
        LOGGER.info("Current Q-Values: ");
        for (int wl = 0; wl < 3; wl++) {
            for (int rt = 0; rt < 3; rt++) {
                LOGGER.info("q-Values for workload " + wl + " and response time " + rt + ": "
                        + Arrays.toString(this.qValues[wl][rt]));
            }
        }
        this.approximatedQValue = qValueChange;
        this.old_action = (int) Math.round(a);
        this.old_state = this.current_state;
    }

    /**
     * @deprecated Function for approximating the q function by multiplying alphas with the given
     *             actions
     * @param ai
     *            chosen actions for each state, should have size of state space
     * @return
     */
    private double approximate_q_function(int[][] ai) {
        double q = 0;
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j <= 3; j++) {
                q = q + this.current_state.getFiringDegree(i, j) * this.qValues[i][j][ai[i][j]];
            }
        }
        return q;
    }

    private double calculate_value_function(int i, int j) {
        // TODO Auto-generated method stub
        return 0;
    }

    private double calculateReward() {
        if (this.current_state.responseTime < this.targetResponseTime) {
            return 1;
        } else if (this.current_state.responseTime < this.old_state.responseTime && this.old_action > 0) {
            return 0;
        } else {
            return Math.exp((this.targetResponseTime - this.current_state.responseTime) / this.targetResponseTime) - 1;
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
        // TODO Auto-generated method stub
        return this.old_action;
    }

}
