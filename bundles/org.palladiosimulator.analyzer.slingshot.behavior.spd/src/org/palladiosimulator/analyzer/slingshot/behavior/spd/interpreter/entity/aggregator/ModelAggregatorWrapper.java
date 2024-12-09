package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.MetricSetDescription;
import org.palladiosimulator.spd.triggers.stimuli.Stimulus;

public abstract class ModelAggregatorWrapper<T extends Stimulus> {

    protected MetricSetDescription metricSetDescription;
    protected BaseMetricDescription baseMetricDescription;

    public abstract double getResult() throws NotEmittableException;

    public abstract void aggregateMeasurement(final MeasurementMade measurementMade);
}
