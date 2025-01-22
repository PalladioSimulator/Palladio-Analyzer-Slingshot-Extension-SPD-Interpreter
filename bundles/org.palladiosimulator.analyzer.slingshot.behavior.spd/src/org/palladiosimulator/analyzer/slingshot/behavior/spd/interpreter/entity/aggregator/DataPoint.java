package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

// Represents a data point with a timestamp and a double value
public class DataPoint {
    final private double timestamp;
    final private double value;

    public double getTimestamp() {
        return this.timestamp;
    }

    public double getValue() {
        return this.value;
    }

    DataPoint(double timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
