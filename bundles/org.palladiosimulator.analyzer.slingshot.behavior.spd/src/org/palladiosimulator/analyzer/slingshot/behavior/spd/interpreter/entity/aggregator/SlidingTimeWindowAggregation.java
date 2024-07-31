package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

/***
 * An inefficient implementation of a sliding time window that allows the
 * applying of {@link #aggregationFunction} whenever the windows moves.
 * 
 * The implementation allows the specification of the {@link #durationNoEmit} to
 * define the emitting rate.
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
	private final Function<Collection<Double>, Double> aggregationFunction;

	// Represents a data point with a timestamp and a double value
	private class DataPoint {
		double timestamp;
		double value;

		DataPoint(double timestamp, double value) {
			this.timestamp = timestamp;
			this.value = value;
		}
	}

	public SlidingTimeWindowAggregation(int winSizeSeconds, double noEmitDuration,
			Function<Collection<Double>, Double> aggrFunction) {
		this.window = new LinkedList<>();
		this.windowSizeInSeconds = winSizeSeconds;
		this.currentSum = 0.0;
		this.durationNoEmit = noEmitDuration;
		this.emitTime = 0.0;
		this.aggregationFunction = aggrFunction;
	}
	/**
	 * It is emittable whenever the last timestamp is larger than the window or the
	 * emit time frequency has been reached.
	 */
	@Override
	public boolean isEmittable() {
		return this.window.getLast().timestamp > windowSizeInSeconds
				&& this.window.getLast().timestamp - emitTime > durationNoEmit;
	}

	@Override
	public double aggregate(double time, double newValue) {
		// TODO Auto-generated method stub
		// Remove old data points that fall outside the sliding window
		while (!window.isEmpty() && window.getFirst().timestamp <= time - windowSizeInSeconds) {
			DataPoint removed = window.removeFirst();
			currentSum -= removed.value;
		}

		// Add the new data point to the window
		DataPoint newPoint = new DataPoint(time, newValue);
		window.addLast(newPoint);
		currentSum += newValue;
		return currentSum / window.size();
	}

	@Override
	protected double getCurrentVal() {
		emitTime = this.window.getLast().timestamp;
		return aggregationFunction.apply(window.stream().map(dp -> dp.value).toList());
	}

}
