package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.trigger;

import org.palladiosimulator.spd.triggers.SimpleFireOnTrend;
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
		throw new UnsupportedOperationException("This is not yet implemented and only exist as a placeholder right now");
	}

}
