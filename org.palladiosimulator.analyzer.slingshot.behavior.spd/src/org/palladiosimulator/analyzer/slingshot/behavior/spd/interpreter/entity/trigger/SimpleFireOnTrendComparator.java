package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import org.palladiosimulator.spd.triggers.SimpleFireOnTrend;
import org.palladiosimulator.spd.triggers.TrendPattern;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTrend;
import org.palladiosimulator.spd.triggers.expectations.ExpectedValue;

/*
 * TODO: Compare values for simpleFireOnTrend! This class is just added
 * now for completeness, but is not implemented yet.
 */
public class SimpleFireOnTrendComparator implements ValueComparator {
	
	private final SimpleFireOnTrend simpleFireOnTrend;
	
	public SimpleFireOnTrendComparator(final SimpleFireOnTrend simpleFireOnTrend) {
		this.simpleFireOnTrend = simpleFireOnTrend;
	}
	
	@Override
	public ComparatorResult compare(double actualValue, ExpectedValue expectedValue) {
		// Pre-condition: The "actualValue" is already aggregated and a change
		if (!(expectedValue instanceof ExpectedTrend)) {
			throw new IllegalArgumentException("For the SimpleFireOnTrend, the ExpectedTrend must be used");
		}

		final ExpectedTrend expectedTrend = (ExpectedTrend) expectedValue;

		final boolean result = switch (expectedTrend.getTrend().getValue()) {
		case TrendPattern.INCREASING_VALUE -> actualValue > 0;
		case TrendPattern.DECREASING_VALUE -> actualValue < 0;
		case TrendPattern.NON_INCREASING_VALUE -> actualValue <= 0;
		case TrendPattern.NON_DECREASING_VALUE -> actualValue >= 0;
		default -> false;
		};

		return result ? ComparatorResult.IN_ACCORDANCE : ComparatorResult.DISREGARD;
	}

}
