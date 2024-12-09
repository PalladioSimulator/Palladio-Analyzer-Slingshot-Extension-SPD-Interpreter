package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.functions;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.DataPoint;

/**
 * This aggregation calculates the defined percentile of the collected DataPoints using the
 * nearest-rank method
 *
 * @author Jens Berberich
 *
 */
public class PercentileAggregation implements Function<Collection<DataPoint>, Double> {

    private final double percentile;

    public PercentileAggregation(final double percentile) {
        super();
        this.percentile = percentile;
    }

    @Override
    public Double apply(final Collection<DataPoint> t) {
        if (t.isEmpty()) {
            return 0.0;
        }

        final Double[] values = t.stream()
            .map(DataPoint::getValue)
            .toList()
            .toArray(new Double[0]);
        Arrays.sort(values);
        double calculatedPercentile = values[(int) Math.ceil(values.length * this.percentile) - 1];

        // Use nearest-rank method to get the percentile
        return calculatedPercentile;
    }

}
