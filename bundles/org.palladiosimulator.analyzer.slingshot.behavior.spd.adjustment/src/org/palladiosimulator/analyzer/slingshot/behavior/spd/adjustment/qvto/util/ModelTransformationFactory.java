package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelParameter;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelType;
import org.eclipse.m2m.internal.qvt.oml.expressions.OperationalTransformation;
import org.eclipse.m2m.internal.qvt.oml.expressions.util.ExpressionsSwitch;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToPoolingModelTransformation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToTransformationExecutor;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToModelTransformation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.TransformationParameterInformation;

/**
 * Factory class to create {@link TransformationData} objects related to QVTo transformations.
 * 
 * @author Florian Rosenthal
 *
 */
@SuppressWarnings("restriction")
public class ModelTransformationFactory {

	private final ResourceSet resourceSet;
	
	/**
     * Initializes a new instance of the {@link ModelTransformationFactory} class.
     */
	public ModelTransformationFactory() {
		this.resourceSet = new ResourceSetImpl();
	}
	
	private static final ExpressionsSwitch<EPackage> PARAM_META_MODEL_SWITCH = new ExpressionsSwitch<>() {
		
		@Override
		public EPackage caseModelType(final ModelType modelType) {
			return modelType.getMetamodel().get(0);
		}
		
	};
	
	private static final ExpressionsSwitch<OperationalTransformation> OPERATIONAL_TRANSFORMATION_SWITCH = new ExpressionsSwitch<OperationalTransformation>() {
		
		@Override
		public OperationalTransformation caseOperationalTransformation(final OperationalTransformation transformation) {
			return transformation;
		}
		
	};
	
	/**
     * Factory method the create {@link TransformationData} for the QVTo transformation specified by
     * the given URI.
     * 
     * @param transformationUri
     *            A {@link URI} that points to a QVTo transformation.
     * @return A {@link TransformationData} instance that encapsulates all data required to deal
     *         with the transformation during Simulizar runs.
     * @throws IllegalArgumentException
     *             In case the given URI does not point to a {@link QVTo} transformation.
     * @throws NullPointerException
     *             In case the given {@code transformationURI} is {@code null}.
     */
	public QVToModelTransformation createModelTransformation(final URI transformationUri) {
		// the EObject transformation should be the first in the content list.
		final Resource transformationResource = this.resourceSet.getResource(Objects.requireNonNull(transformationUri), true);
		OperationalTransformation transformation = null;
		if (!transformationResource.getContents().isEmpty()) {
			transformation = OPERATIONAL_TRANSFORMATION_SWITCH.doSwitch(transformationResource.getContents().get(0));
		}
		if (transformation == null) {
			throw new IllegalArgumentException("OperationalTransformation instance could not be retrieved from resource contents.");
		}
		
		return new QVToPoolingModelTransformation(
				transformation,
				() -> new QVToTransformationExecutor(transformationUri),
				createTransformationParameterInformation(transformation)
			   );
	}
	
	private Collection<TransformationParameterInformation> createTransformationParameterInformation(final OperationalTransformation transformation) {
		assert transformation != null;
		
		final Collection<ModelParameter> parameters = transformation.getModelParameter();
		final List<TransformationParameterInformation> result = new ArrayList<>(parameters.size());
		int index = 0;
		
		for (final ModelParameter parameter : parameters) {
			result.add(new TransformationParameterInformation(
					PARAM_META_MODEL_SWITCH.doSwitch(parameter.getType()),
					parameter.getKind(),
					index++,
					parameter.getName()
			));
		}
		
		return result;
	}
}
