package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.ChangeWindowAggregation;
import org.palladiosimulator.spd.triggers.SimpleFireOnTrend;
import org.palladiosimulator.spd.triggers.TrendPattern;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTrend;
import org.palladiosimulator.spd.triggers.expectations.ExpectedValue;

/**
 * This class is used for calculating the trend and then checking whether the
 * trend is in accordance of the trigger. It automatically aggregates the values
 * so that the trend can be calculated. If there are too few values to find the
 * trend, {@link ComparatorResult.WAIT} will be returned.
 * 
 * @author Julijan Katic
 */
public class SimpleFireOnTrendComparator implements ValueComparator {
	
	private static final int WINDOW_SIZE = 10;

	private final SimpleFireOnTrend simpleFireOnTrend;
	private final ChangeWindowAggregation aggregator = new ChangeWindowAggregation(WINDOW_SIZE);
	
	public SimpleFireOnTrendComparator(final SimpleFireOnTrend simpleFireOnTrend) {
		this.simpleFireOnTrend = simpleFireOnTrend;
	}
	
	@Override
	public ComparatorResult compare(double actualValue, ExpectedValue expectedValue) {
		if (!(expectedValue instanceof ExpectedTrend)) {
			throw new IllegalArgumentException("For the SimpleFireOnTrend, the ExpectedTrend must be used");
		}

		final ExpectedTrend expectedTrend = (ExpectedTrend) expectedValue;
		final double aggregatedValue = aggregator.aggregate(actualValue);

		if (!aggregator.isWindowFull()) {
			// In case the trend cannot be inferred yet, wait for the next values
			return ComparatorResult.WAIT;
		}

		final boolean result = switch (expectedTrend.getTrend().getValue()) {
		case TrendPattern.INCREASING_VALUE -> aggregatedValue > 0;
		case TrendPattern.DECREASING_VALUE -> aggregatedValue < 0;
		case TrendPattern.NON_INCREASING_VALUE -> aggregatedValue <= 0;
		case TrendPattern.NON_DECREASING_VALUE -> aggregatedValue >= 0;
		default -> false;
		};

		return result ? ComparatorResult.IN_ACCORDANCE : ComparatorResult.DISREGARD;
	}


}
