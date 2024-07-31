package org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment;

import org.palladiosimulator.monitorrepository.Monitor;

public final class MonitorChange extends ModelChange<Monitor> {

	private final Monitor copiedFrom;
	private final Monitor newMonitor;

	public MonitorChange(Monitor object, Monitor copiedFrom, double simulationTime) {
		super(object, Monitor.class, simulationTime);
		this.newMonitor = object;
		this.copiedFrom = copiedFrom;
	}

	public Monitor getCopiedFrom() {
		return copiedFrom;
	}

	/**
	 * Synonym for {@link #getObject()}.
	 * 
	 * @see #getObject()
	 */
	public Monitor getNewMonitor() {
		return newMonitor;
	}

}
