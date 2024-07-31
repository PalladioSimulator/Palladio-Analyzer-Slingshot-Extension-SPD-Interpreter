package org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment;

import org.eclipse.emf.ecore.EObject;

public abstract class ModelChange<T extends EObject> {

	private final T object;
	private final Class<T> objectType;
	private final double simulationTime;
	
	public ModelChange(T object, Class<T> objectType, double simulationTime) {
		super();
		this.object = object;
		this.objectType = objectType;
		this.simulationTime = simulationTime;
	}
	
	public ModelChange(T object, double simulationTime) {
		this(object, (Class<T>) object.getClass(), simulationTime);
	}

	public T getObject() {
		return object;
	}

	public double getSimulationTime() {
		return simulationTime;
	}
	
	public Class<T> getObjectType() {
		return this.objectType;
	}
	
}
