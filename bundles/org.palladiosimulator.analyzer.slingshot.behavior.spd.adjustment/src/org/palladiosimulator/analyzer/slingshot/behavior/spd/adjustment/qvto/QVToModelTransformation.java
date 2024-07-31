package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.m2m.internal.qvt.oml.expressions.OperationalTransformation;


@SuppressWarnings("restriction")
public class QVToModelTransformation {

	private static final Predicate<TransformationParameterInformation> PURE_OUT_PARAM_PREDICATE =
			transformationParameterInformation -> transformationParameterInformation.isOutParameter();

	private static final Predicate<TransformationParameterInformation> IN_INOUT_PARAM_PREDICATE
		= Predicate.not(PURE_OUT_PARAM_PREDICATE);

	private final QVToTransformationExecutor transformationExecutor;
	private final Collection<TransformationParameterInformation> inParams;
	private final Collection<TransformationParameterInformation> outParams;

	private final OperationalTransformation modelTransformation;

	public QVToModelTransformation(final OperationalTransformation transformation,
								   final QVToTransformationExecutor executor,
								   final Collection<TransformationParameterInformation> paramInfo) {
		this.modelTransformation = transformation;
		this.transformationExecutor = executor;
		this.inParams = paramInfo.stream().filter(IN_INOUT_PARAM_PREDICATE).collect(Collectors.toUnmodifiableList());
		this.outParams = paramInfo.stream().filter(PURE_OUT_PARAM_PREDICATE).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Gets the number of parameters the associated QVTo transformation has.
	 *
	 * @return A nonnegative integer indicating the number of parameters.
	 */
	public int getParameterCount() {
		return this.inParams.size() + this.outParams.size();
	}

	/**
	 * Gets all transformation parameters that are marked as 'out' params, i.e., all
	 * parameters that are preceded by the out keyword.
	 *
	 * @return An UNMODIFIABLE {@link Collection} of the 'out' params, in order of
	 *         appearance.
	 */
    public Collection<TransformationParameterInformation> getPureOutParameters() {
        return this.outParams;
    }

    /**
     * Gets all transformation parameters that are marked as 'in' or 'inout' params, i.e., all
     * parameters that are preceded by either the 'in' or the 'inout' keyword.
     *
     * @return An UNMODIFIABLE {@link Collection} of the 'in'/'inout' params, in order of
     *         appearance.
     */
    public Collection<TransformationParameterInformation> getInParameters() {
        return this.inParams;
    }

    /**
     * Gets the name of the associated QVTo transformation.
     *
     * @return A {@link String} that contains the name of the transformation.
     */
    public String getTransformationName() {
        return this.modelTransformation.getName();
    }

    /**
     * Gets the transformation executor that will be used to execute the associated QVT0
     * transformation.
     *
     * @return The {@link QVTOTransformationExecutor} that will execute the transformation.
     */
    public QVToTransformationExecutor getTransformationExecutor() {
        return this.transformationExecutor;
    }

    @Override
    public String toString() {
    	final StringBuilder builder = new StringBuilder();
    	builder.append("QVToTransformation[name = ");
    	builder.append(this.modelTransformation.getName());
    	builder.append(", params = {\n");
    	this.inParams.forEach(inf -> {
    		builder.append("\t[");
    		builder.append(inf.toString());
    		builder.append("]\n");
    	});
    	this.outParams.forEach(inf -> {
    		builder.append("\t[");
    		builder.append(inf.toString());
    		builder.append("]\n");
    	});
    	builder.append("}1]");

    	return builder.toString();
    }
}
