package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entity.aggregator;

public abstract non-sealed class AbstractWindowAggregation implements WindowAggregation {

	public AbstractWindowAggregation() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getCurrentValue() throws AssertionError {
		if (!isEmittable())
		    	throw new IllegalStateException("Cant get current value when it is non emittable.");
		
		return getCurrentVal();
	}
	
	protected abstract double getCurrentVal();

	@Override
	public abstract boolean isEmittable();

	@Override
	public abstract double aggregate(double time, double newValue);

}
