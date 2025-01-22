package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.model;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator.NotEmittableException;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;

public abstract class ModelEvaluator {
    boolean changeOnInterval = true;
    boolean changeOnStimulus = false;

    public abstract int getDecision() throws NotEmittableException;

    public abstract void recordUsage(MeasurementMade measurement);

    public boolean getChangeOnInterval() {
        return changeOnInterval;
    }

    public boolean getChangeOnStimulus() {
        return changeOnStimulus;
    }
}
