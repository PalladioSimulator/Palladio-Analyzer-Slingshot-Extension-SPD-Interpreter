package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MaxAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MeanAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MedianAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.MinAggregation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions.SumAggregation;
import org.palladiosimulator.spd.triggers.AGGREGATIONMETHOD;

/**
 * A window-based aggregator with a window size of the last {@code windowSize}
 * measurements.
 * 
 * This implementation allows for applying an aggregation function
 * {@link #aggrFunction} to the considered values in {@link #valuesToConsider}.
 * 
 * It is named simple while it simply collects values from as offered from the
 * {@link #aggregate(double, double)} method into a queue and allows the
 * application of an aggregate function on that structure.
 * 
 * In addition, it offers a helper method
 * {@link #getFromAggregationMethod(AGGREGATIONMETHOD)} to get a desired window
 * with a predefined aggregation function that matches the possible aggregations
 * specified in SPD {min, max, mean, median, sum}.
 * 
 * For simulation, this implementation is not advised while it has to keep all
 * measurements in the queue and is inefficient and slow. Therefore, more
 * efficient aggregations have to be employed, see
 * SlidingTimeWindowAggregationBasedOnEMA.
 * 
 * @author Julijan Katic, Floriment Klinaku
 */
public class FixedLengthWindowSimpleAggregation extends AbstractWindowAggregation {

	public static final int DEFAULT_WINDOW_SIZE = 10;

	protected final int windowSize;
	protected final Queue<Double> valuesToConsider;

	/** The aggregated value so far. */
	private double currentValue;
	private final Function<Collection<Double>, Double> aggrFunction;

	public FixedLengthWindowSimpleAggregation(final int windowSize,
			final Function<Collection<Double>, Double> aggrFunction) {
		this.windowSize = windowSize;
		this.valuesToConsider = new ArrayDeque<>(windowSize);
		this.aggrFunction = aggrFunction;
	}

	/**
	 * Aggregates the new value into the current value. This will also update
	 * {@link #getCurrentValue()}.
	 * 
	 * @param newValue
	 * @return
	 */
	@Override
	public final double aggregate(final double time, final double newValue) {
		this.consider(newValue);
		this.currentValue = aggrFunction.apply(valuesToConsider).doubleValue();
		return this.currentValue;
	}

	/**
	 * Returns whether the window is full or whether some measurements are still
	 * missing.
	 * 
	 * @return true iff the window is full.
	 */
	@Override
	public boolean isEmittable() {
		return this.valuesToConsider.size() == this.windowSize;
	}

	/**
	 * Adds the new value into the queue such that it contains at most
	 * {@code windowSize} values to consider. This means that if the queue is
	 * already "full", i.e. there are already {@code windowSize} element in it, the
	 * oldest element will be removed and the new value added.
	 * 
	 * @param newValue The new value to add.
	 */
	private void consider(final double newValue) {
		if (valuesToConsider.size() == windowSize) {
			valuesToConsider.poll();
		}
		valuesToConsider.offer(newValue);
	}


	public static FixedLengthWindowSimpleAggregation getFromAggregationMethod(final AGGREGATIONMETHOD aggregationMethod,
			final int windowSize) {
		return switch (aggregationMethod) {
		case MIN -> new FixedLengthWindowSimpleAggregation(windowSize, new MinAggregation());
		case AVERAGE -> new FixedLengthWindowSimpleAggregation(windowSize, new MeanAggregation());
		case MAX -> new FixedLengthWindowSimpleAggregation(windowSize, new MaxAggregation());
		case MEDIAN -> new FixedLengthWindowSimpleAggregation(windowSize, new MedianAggregation());
		case SUM -> new FixedLengthWindowSimpleAggregation(windowSize, new SumAggregation());
		default -> throw new IllegalArgumentException("Unexpected value: " + aggregationMethod);
		};
	}

	public static FixedLengthWindowSimpleAggregation getFromAggregationMethod(
			final AGGREGATIONMETHOD aggregationMethod) {
		return getFromAggregationMethod(aggregationMethod, DEFAULT_WINDOW_SIZE);
	}

	public final int getSize() {
		return this.valuesToConsider.size();
	}

	@Override
	protected double getCurrentVal() {
		return this.currentValue;
	}

}
