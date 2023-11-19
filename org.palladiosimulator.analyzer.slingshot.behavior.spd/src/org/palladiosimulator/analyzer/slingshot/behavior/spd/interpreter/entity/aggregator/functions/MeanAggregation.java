package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.function.Function;

public class MeanAggregation  implements Function<Collection<Double>, Double> {
	
	public MeanAggregation() {
		super();
	}

	@Override
	public Double apply(Collection<Double> t) {
		// TODO Auto-generated method stub
		return t.stream()
				 .mapToDouble(Double::doubleValue)
				 .average()
				 .orElse(0.0);
	}

}
