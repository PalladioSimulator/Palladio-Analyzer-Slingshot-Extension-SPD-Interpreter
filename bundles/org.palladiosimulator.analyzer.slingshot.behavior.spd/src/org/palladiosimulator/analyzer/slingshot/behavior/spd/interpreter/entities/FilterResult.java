package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public sealed interface FilterResult {
	
	public static Success success(final DESEvent nextEvent) {
		return new Success(nextEvent);
	}
	
	public static Disregard disregard(final Object reason) {
		return new Disregard(reason);
	}
	
	public static Disregard disregard() {
		return disregard("");
	}

	public static record Success(DESEvent nextEvent) implements FilterResult {}
	
	public static record Disregard(Object reason) implements FilterResult {}
	

}
