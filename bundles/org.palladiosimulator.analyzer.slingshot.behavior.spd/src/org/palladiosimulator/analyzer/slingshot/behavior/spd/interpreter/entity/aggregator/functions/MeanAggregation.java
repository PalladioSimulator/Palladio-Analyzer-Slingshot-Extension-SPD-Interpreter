package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.DataPoint;

public class MeanAggregation implements Function<Collection<DataPoint>, Double> {

    public MeanAggregation() {
        super();
    }

    @Override
    public Double apply(Collection<DataPoint> t) {
        // TODO Auto-generated method stub
        return t.stream()
            .map(DataPoint::getValue)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElseThrow();
    }

}
