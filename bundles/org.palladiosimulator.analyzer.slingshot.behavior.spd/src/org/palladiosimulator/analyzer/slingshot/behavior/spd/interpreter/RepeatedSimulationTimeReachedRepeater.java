package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import static org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality.SINGLE;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.RepeatedSimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SpdBasedEvent;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.spd.SPD;

@OnEvent(when = RepeatedSimulationTimeReached.class, then = RepeatedSimulationTimeReached.class, cardinality = SINGLE)
public class RepeatedSimulationTimeReachedRepeater implements SimulationBehaviorExtension {
    private static final Logger LOGGER = Logger.getLogger(RepeatedSimulationTimeReachedRepeater.class);

    @Inject
    public RepeatedSimulationTimeReachedRepeater(@Nullable final SPD spdModel) {
    }

    @Subscribe
    public Result<SpdBasedEvent> onRepeatedSimulationTimeReached(
            final RepeatedSimulationTimeReached repeatedSimulationTimeReached) {
        RepeatedSimulationTimeReached newRepeatedSimulationTimeReached = new RepeatedSimulationTimeReached(
                repeatedSimulationTimeReached.getTargetGroupId(),
                repeatedSimulationTimeReached.time() + repeatedSimulationTimeReached.getRepetitionTime(),
                repeatedSimulationTimeReached.delay(), repeatedSimulationTimeReached.getRepetitionTime());
        LOGGER.debug("Repeating the RepeatedSimulationTimeReached event: Received at "
                + repeatedSimulationTimeReached.time() + ", Repeated at " + newRepeatedSimulationTimeReached.time());
        return Result.of(newRepeatedSimulationTimeReached);
    }

}
