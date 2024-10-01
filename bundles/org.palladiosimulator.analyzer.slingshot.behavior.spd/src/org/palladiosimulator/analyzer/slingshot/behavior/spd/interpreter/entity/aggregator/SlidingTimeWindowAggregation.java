package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MaxAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MeanAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MedianAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MinAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.PercentileAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.RateOfChangeAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.SumAggregation;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;

/***
 * An inefficient implementation of a sliding time window that allows the applying of
 * {@link #aggregationFunction} whenever the windows moves.
 * 
 * The implementation allows the specification of the {@link #durationNoEmit} to define the emitting
 * rate.
 * 
 * @author Floriment Klinaku
 *
 */
public class SlidingTimeWindowAggregation extends AbstractWindowAggregation {

    private Deque<DataPoint> window; // Data structure to store the sliding window
    private double windowSizeInSeconds; // Size of the sliding window in milliseconds
    private double currentSum; // Aggregate value of the current window
    private double emitTime;
    private final double durationNoEmit; // determines the emitting frequency
    private final Function<Collection<DataPoint>, Double> aggregationFunction;

    public SlidingTimeWindowAggregation(double winSizeSeconds, double noEmitDuration,
            Function<Collection<DataPoint>, Double> aggrFunction) {
        this.window = new LinkedList<>();
        this.windowSizeInSeconds = winSizeSeconds;
        this.currentSum = 0.0;
        this.durationNoEmit = noEmitDuration;
        this.emitTime = 0.0;
        this.aggregationFunction = aggrFunction;
    }

    /**
     * It is emittable whenever the last timestamp is larger than the window or the emit time
     * frequency has been reached.
     */
    @Override
    public boolean isEmittable() {
        return this.window.getLast()
            .getTimestamp() > windowSizeInSeconds
                && this.window.getLast()
                    .getTimestamp() - emitTime > durationNoEmit;
    }

    public static SlidingTimeWindowAggregation getFromAggregationMethod(final AGGREGATIONMETHOD aggregationMethod,
            final double winSizeSeconds, final double noEmitDuration) {
        return switch (aggregationMethod) {
        case MIN -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration, new MinAggregation());
        case AVERAGE -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration, new MeanAggregation());
        case MAX -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration, new MaxAggregation());
        case MEDIAN -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration, new MedianAggregation());
        case SUM -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration, new SumAggregation());
        case RATEOFCHANGE -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration,
                new RateOfChangeAggregation());
        case PERCENTILE95 -> new SlidingTimeWindowAggregation(winSizeSeconds, noEmitDuration,
                new PercentileAggregation(0.95));
        default -> throw new IllegalArgumentException("Unexpected value: " + aggregationMethod);
        };
    }

    @Override
    public double aggregate(double time, double newValue) {
        // TODO Auto-generated method stub
        // Remove old data points that fall outside the sliding window
        while (!window.isEmpty() && window.getFirst()
            .getTimestamp() <= time - windowSizeInSeconds) {
            DataPoint removed = window.removeFirst();
            currentSum -= removed.getValue();
        }

        // Add the new data point to the window
        DataPoint newPoint = new DataPoint(time, newValue);
        window.addLast(newPoint);
        currentSum += newValue;
        return currentSum / window.size();
    }

    @Override
    protected double getCurrentVal() {
        emitTime = this.window.getLast()
            .getTimestamp();
        return aggregationFunction.apply(window.stream()
            .toList());
    }

}
