package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.expressions.DirectionKind;

/**
 * Convenience class to store required information (type, kind, index and name) about parameters
 * of a QVTo transformation.
 * 
 * @author Florian Rosenthal
 *
 */
@SuppressWarnings("restriction")
public final class TransformationParameterInformation {

	private static final String FORMAT_STRING = "param: index = %d, name = %s, type = %s, direction = %s";
	
	private final EPackage parameterType;
	private final DirectionKind parameterDirectionKind;
	private final int parameterIndex;
	private final String parameterName;
	
	public TransformationParameterInformation(
			final EPackage parameterType, 
			final DirectionKind parameterDirectionKind,
			final int parameterIndex, 
			final String parameterName) {
		super();
		this.parameterType = parameterType;
		this.parameterDirectionKind = parameterDirectionKind;
		this.parameterIndex = parameterIndex;
		this.parameterName = parameterName;
	}

	/**
     * Gets the type of the parameter, i.e., its corresponding meta-model package.
     * 
     * @return The {@link EPackage} which denotes the type of the transformation parameter.
     */
	public EPackage getParameterType() {
		return parameterType;
	}

	public DirectionKind getParameterDirectionKind() {
		return parameterDirectionKind;
	}
	
	/**
     * Indicates whether the parameter is marked with the 'out' keyword, i.e, has implicit return
     * type semantics.
     * 
     * @return {@code true} in case the parameter is an 'out' parameter, {@code false} otherwise.
     */
	public boolean isOutParameter() {
		return getParameterDirectionKind() == DirectionKind.OUT;
	}

	/**
     * Gets the index of the parameter in the corresponding QVTo transformation. <br>
     * Note, that the first parameter has index {@code 0} and so forth.
     * 
     * @return The index of the parameter, expressed by a nonnegative integer.
     */
	public int getParameterIndex() {
		return parameterIndex;
	}

	/**
     * Gets the name of the parameter.
     * 
     * @return A {@link String} that contains the parameter's name.
     */
	public String getParameterName() {
		return parameterName;
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT_STRING, this.parameterIndex, this.parameterName, this.parameterType.getName(), this.parameterDirectionKind.getName());
	}
}
