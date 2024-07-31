package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class MinAggregation implements Function<Collection<Double>, Double> {
	
	public MinAggregation() {
		super();
	}

	@Override
	public Double apply(Collection<Double> t) {
		// TODO Auto-generated method stub
		return t.stream()
				.min(Double::compare)
				.orElseThrow(() -> new NoSuchElementException("There are no values to aggregate yet."));
	}

}
