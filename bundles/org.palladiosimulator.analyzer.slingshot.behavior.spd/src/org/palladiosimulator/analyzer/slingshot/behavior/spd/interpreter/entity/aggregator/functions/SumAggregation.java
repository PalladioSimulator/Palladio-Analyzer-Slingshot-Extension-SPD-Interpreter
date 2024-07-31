package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.function.Function;

public class SumAggregation  implements Function<Collection<Double>, Double> {
	
	public SumAggregation() {
		super();
	}

	@Override
	public Double apply(Collection<Double> t) {
		// TODO Auto-generated method stub
		return t.stream().reduce(0.0d, Double::sum);
	}

}
