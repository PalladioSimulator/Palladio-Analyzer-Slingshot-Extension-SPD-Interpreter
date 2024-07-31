package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class MaxAggregation  implements Function<Collection<Double>, Double> {
	
	public MaxAggregation() {
		super();
	}

	@Override
	public Double apply(Collection<Double> t) {
		// TODO Auto-generated method stub
		return t.stream()
				.max(Double::compare)
				.orElseThrow(() -> new NoSuchElementException("There are no values to aggregate yet."));
	}

}
