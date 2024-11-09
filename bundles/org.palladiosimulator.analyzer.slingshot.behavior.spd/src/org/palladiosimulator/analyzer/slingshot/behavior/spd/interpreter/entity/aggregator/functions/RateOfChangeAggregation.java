package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Collection;
import java.util.function.Function;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.DataPoint;

public class RateOfChangeAggregation implements Function<Collection<DataPoint>, Double> {

    public RateOfChangeAggregation() {
        super();
    }

    @Override
    public Double apply(Collection<DataPoint> t) {

        if (t.isEmpty()) {
            return 0.0;
        }

        SimpleRegression regression = new SimpleRegression();
        t.stream()
            .forEach(dataPoint -> regression.addData(dataPoint.getTimestamp(), dataPoint.getValue()));
        return regression.getSlope();
    }
}
