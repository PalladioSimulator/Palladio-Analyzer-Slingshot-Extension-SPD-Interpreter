package org.palladiosimulator.analyzer.slingshot.behavior.spd.data;


import java.util.List;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

public final class ModelAdjusted extends AbstractSimulationEvent implements SpdBasedEvent {
	
	private final boolean wasSuccessful;
	private final List<ModelChange<?>> changes;
	
	public ModelAdjusted(boolean wasSuccessful, List<ModelChange<?>> changes) {
		super();
		this.wasSuccessful = wasSuccessful;
		this.changes = changes;
	}

	public boolean isWasSuccessful() {
		return wasSuccessful;
	}

	public List<ModelChange<?>> getChanges() {
		return changes;
	}
	
	
}
