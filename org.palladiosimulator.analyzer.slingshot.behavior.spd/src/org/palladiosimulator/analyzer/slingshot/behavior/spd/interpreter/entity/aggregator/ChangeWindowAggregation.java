package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import java.util.Deque;

/**
 * This aggregator finds the average change of discrete values over the time.
 * The average change is defined to be {@code (f(x_0) - f(x_t)) / (x_0 - x_t)}.
 * 
 * Here, the latest value and the oldest value of the fixed window will be used.
 * 
 * @author Julijan Katic
 *
 */
public class ChangeWindowAggregation extends FixedLengthWindowAggregation {

	public ChangeWindowAggregation(int windowSize) {
		super(windowSize);
	}

	@Override
	protected double doAggregation() {
		final double firstValue = ((Deque<Double>) valuesToConsider).getFirst();
		final double secondValue = ((Deque<Double>) valuesToConsider).getLast();

		return (secondValue - firstValue) / valuesToConsider.size();
	}

}
