package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.DataPoint;

public class MaxAggregation implements Function<Collection<DataPoint>, Double> {

    public MaxAggregation() {
        super();
    }

    @Override
    public Double apply(Collection<DataPoint> t) {
        // TODO Auto-generated method stub
        return t.stream()
            .map(DataPoint::getValue)
            .max(Double::compare)
            .orElseThrow(() -> new NoSuchElementException("There are no values to aggregate yet."));
    }

}
