package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

/**
 * The {@link #SlidingTimeWindowAggregationBasedOnEMA(int, double, double)} implements an
 * exponential moving average. Similarly, to other WindowAggregation(s), it allows to define a
 * {@link #durationNoEmit} to control the emitting frequency.
 * 
 * In addition, it provides a smoothingFactor that defines the weight of a new value to the
 * aggregated value.
 *
 * This implementation does not allow the application of arbitrary aggregation functions. It
 * implements a moving average.
 * 
 * Further, the implementation makes the following assumption: It assumes that when no value is
 * added for a certain time period, then the utilization of the resource is 0 during that period.
 * This is happening because of the current implementation that updates the resource state only when
 * Jobs arrive or leave.
 * 
 * Implications:
 * 
 * To compensate, this implementation, corrects the EMA model by multiplying the resulting
 * utilization with an exponential factor that depends on the time difference between the last
 * update and the recent one. The farther the last update time the more zeros are assumed and the
 * more the overall utilization is decreased.
 * 
 * Caution: This EMA Aggregation method is suitable for how the utilization of active resources is
 * implemented in Slingshot. It may not be applicable to other types of measurements.
 * 
 * @author Floriment Klinaku, Sarah Stieß
 *
 */
public class SlidingTimeWindowAggregationBasedOnEMA extends AbstractWindowAggregation {

	private double windowSizeInSeconds; // Size of the sliding window in seconds
	private double currentValue; // Aggregate value of the current window
	private double emitTime;
	private final double durationNoEmit; // determines the emitting frequency
	private double alpha; // Smoothing factor for EMA
	private double lastUpdateTime;

	public SlidingTimeWindowAggregationBasedOnEMA(int winSizeSeconds, double noEmitDuration, double smoothingFactor) {
		this.windowSizeInSeconds = winSizeSeconds;
		this.durationNoEmit = noEmitDuration;
		this.emitTime = 0.0;
		this.alpha = smoothingFactor;
		this.currentValue = 0.0;
		this.lastUpdateTime = 0.0;
	}

	@Override
	public double aggregate(double currentTime, double newValue) {
		double deltaTime = currentTime - lastUpdateTime;

		// Update the EMA with the new data point
		currentValue = (1.0 - alpha) * currentValue + alpha * newValue;

		// Adjust the time-weighted factor based on the time elapsed since the last
		// update
		double timeFactor = Math.exp(-deltaTime / windowSizeInSeconds);

		// Apply the time factor to the current value
		currentValue *= timeFactor;

		// Update the last update time
		lastUpdateTime = currentTime;
		return currentValue;
	}

	// Get the current aggregate value of the sliding window
	public double getCurrentAggregate() {
		return currentValue;
	}

	/**
	 * It is emittable whenever the last timestamp is larger than the window or the
	 * emit time frequency has been reached.
	 */
	@Override
	public boolean isEmittable() {
		return lastUpdateTime > windowSizeInSeconds && lastUpdateTime - emitTime > durationNoEmit;
	}

	@Override
	protected double getCurrentVal() {
		assert lastUpdateTime - emitTime > durationNoEmit;
		emitTime = lastUpdateTime;
		return currentValue;
	}

}
