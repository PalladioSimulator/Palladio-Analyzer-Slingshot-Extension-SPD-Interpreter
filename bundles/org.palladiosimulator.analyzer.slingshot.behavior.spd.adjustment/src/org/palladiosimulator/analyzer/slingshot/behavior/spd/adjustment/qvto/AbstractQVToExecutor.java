package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.m2m.qvt.oml.BasicModelExtent;
import org.eclipse.m2m.qvt.oml.ExecutionContext;
import org.eclipse.m2m.qvt.oml.ExecutionContextImpl;
import org.eclipse.m2m.qvt.oml.ExecutionDiagnostic;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.m2m.qvt.oml.util.Log;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.ModelTransformationCache;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.QVToModelCache;

/**
 * This class is intended to be the base of all classes that wish to execute QVTo transformations.
 * The set of transformations that can be executed are passed to each instance upon construction in
 * terms of a {@link TransformationCache}, as well as the set of model that can serve as transformation
 * parameters. <br>
 *
 * Subclasses can re-implement the steps of the {@link #executeTransformation(TransformationData)}
 * template method to adapt the process of execution.
 *
 * @author Florian Rosenthal
 *
 */
public abstract class AbstractQVToExecutor {

	private static final Logger LOGGER = Logger.getLogger(AbstractQVToExecutor.class);

	private final QVToModelCache availableModels;
	private final ModelTransformationCache transformationCache;

	// TODO: Get Measurements somehow
	private static final Function<EObject, Collection<EObject>> CREATE_NON_EMPTY_MODEL_ELEMENTS_SWITCH = e -> Collections.singletonList(e);

	/**
	 * Accumulates the model elements and finally creates a basic model extent.
	 */
	private static final Collector<EObject, List<EObject>, ModelExtent> BASIC_MODEL_EXTENT_COLLECTOR
		= Collector.of(ArrayList<EObject>::new,
					   (acc, t) -> acc.add(t),
					   (l, r) -> {l.addAll(r); return l;},
					   BasicModelExtent::new);

	/**
	 * Initializes a new instance of the {@link AbstractQVToExecutor} class with the given
	 * parameters.
	 *
	 * @param knownTransformations
	 * 				An {@link TransformationCache} which contains all transformation that can be
	 * 				executed by this instance, might be empty.
	 * @param knownModels
	 * 				A {@link QVToModelCache} that contains all models that can serve as a
	 * 				transformation parameter.
	 * @throws NullPointerException If either parameter is {@code null}.
	 */
	protected AbstractQVToExecutor(final ModelTransformationCache knownTransformations,
								   final QVToModelCache knownModels) {
		this.transformationCache = Objects.requireNonNull(knownTransformations);
		this.availableModels = Objects.requireNonNull(knownModels);
	}

	/**
	 * Gets the underlying transformation cache used by this instance.
	 *
	 * @return The {@link TransformationCache} which contains all transformations that can be
	 * 		   executed by this instance.
	 */
	protected ModelTransformationCache getAvailableTransformations() {
		return this.transformationCache;
	}

	/**
	 * Gets the underlying model cache used by this instance.
	 *
	 * @return The {@link QVToModelCache} which contains all models that can serve as parameters.
	 */
	protected QVToModelCache getAvailableModels() {
		return this.availableModels;
	}

	/**
	 * Attempts to execute the transformations that corresponds to the given URI.
	 *
	 * @param transformationURI
	 * 				An {@link URI} that points to a QVTo transformation.
	 * @param resourceTableManager
	 * @return true iff the transformation succeeded.
	 * @throws NullPointerException
	 * 				In case the given URI is {@code null}.
	 * @throws IllegalArgumentException
	 * 				In case the transformation is not known, i.e., not stored in the internal cache.
	 * @see #executeTransformation(TransformationData)
	 */
	public boolean executeTransformation(final URI transformationURI) {
		final Optional<QVToModelTransformation> data = this.transformationCache.get(Objects.requireNonNull(transformationURI));
		return executeTransformation(
				data.orElseThrow(() -> new IllegalArgumentException("Given transformation not present in transformation cache."))
			   );
	}

	/**
	 * Template method to execute a QVTo transformation. Within this method, the following
	 * (primitive) steps are conducted:
	 *
	 * <ol>
	 *   <li>The required model {@link ModelExtent ModelExtents} are created:
	 *   	 {@link #setupModelExtents(TransformationData)}</li>
	 *   <li>The {@link ExecutionContext} is setup: {@link #setupExecutionContext()}</li>
	 *   <li>The transformation is executed:
	 *   	 {@link #doExecution(TransformationExecutor, ExecutionContext, ModelExtent[])}</li>
	 *   <li>The {@link ExecutionDiagnostic} that describes the execution result is processed:
	 *   	 {@link #handleExecutionResult(ExecutionDiagnostic)}</li>
	 * </ol>
	 *
	 * Note that all of the steps are implemented by this class, but are open to re-implementation
	 * by subclasses (apart from the execution step).
	 *
	 * @param modelTransformation
	 * @param resourceManager
	 * @return The result of the last step, i.e., a boolean that indicates whether the transformation succeeded.
	 */
	public final boolean executeTransformation(final QVToModelTransformation modelTransformation) {
		final ExecutionDiagnostic result = executeTransformationInternal(modelTransformation);
		return handleExecutionResult(result);
	}

	protected ExecutionDiagnostic executeTransformationInternal(final QVToModelTransformation modelTransformation) {
		final ModelExtent[] modelExtents = setupModelExtents(Objects.requireNonNull(modelTransformation));
		final ExecutionContext executionContext = setupExecutionContext();
		final ExecutionDiagnostic result = doExecution(modelTransformation, executionContext, modelExtents);

		return result;
	}

	/**
	 * Executes a transformation.
	 *
	 * @param modelTransformation
	 * @param executionContext
	 * @param params
	 * @return
	 */
	protected final ExecutionDiagnostic doExecution(final QVToModelTransformation modelTransformation,
												    final ExecutionContext executionContext,
												    final ModelExtent[] params) {
		return modelTransformation.getTransformationExecutor().execute(executionContext, params);
	}

	/**
     * Last step of the {@link #executeTransformation(TransformationData)} template method.
     * Processes the result status of the execution and transforms it into a boolean value.
     *
     * @param executionResult
     * @return {@code true} if the execution of the transformation is considered successful,
     *         {@code false} otherwise.
     * @see #doExecution(TransformationData, ExecutionContext, ModelExtent[])
     */
	protected boolean handleExecutionResult(final ExecutionDiagnostic executionResult) {
		final int severity = executionResult.getSeverity();
		if ((severity == Diagnostic.OK) || (severity == Diagnostic.INFO)) {
			LOGGER.debug("Succcesful rule application: " + executionResult.getMessage());
			return true;
		}

		final List<Diagnostic> details = executionResult.getChildren();
		final String chainedDetails = details.stream()
				.map(Object::toString)
				.collect(Collectors.joining(","));

		final Level level = (severity >= Diagnostic.ERROR) ? Level.ERROR : Level.WARN;

		LOGGER.log(level, String.format("%s; %s", executionResult.getMessage(), chainedDetails), executionResult.getException());
		return false;
	}

	 /**
     * This method is called prior to
     * {@link #doExecution(TransformationData, ExecutionContext, ModelExtent[])} within the
     * {@link #executeTransformation(TransformationData)} template method. It creates the execution
     * context to be used for execution of the transformation. In particular, {@link #createLog()}
     * is called to obtain the {@link Log} to be in use.
     *
     * @return A fully-fledged {@link ExecutionContext}.
     * @see #doExecution(TransformationData, ExecutionContext, ModelExtent[])
     */
	protected ExecutionContext setupExecutionContext() {
		// setup the execution environment details -> configuration properties, LOGGER, monitor object etc.
		final ExecutionContextImpl result = new ExecutionContextImpl();
		result.setLog(createLog());
		result.setConfigProperty("keepModeling", true);
		return result;
	}

	/**
     * Creates the Log that shall be used during execution of the transformation. This method is
     * called within {@link #setupExecutionContext()}.
     *
     * @return The {@link Log} to use during the execution of the transformation.<br>
     *         This default implementation always returns
     *         {@code new QVTOReconfigurationLogger(getClass())}.
     */
	protected Log createLog() {
		return new QVToReconfigurationLogger(getClass());
	}

	/**
     * First step of the {@link #executeTransformation(TransformationData)} template method.
     * Examines the required transformation parameters and creates appropriate model extents.
     *
     * @param transformation
     *            The {@link TransformationData} that represents the transformation to be executed.
     * @return An array of {@link ModelExtent ModelExtents}, one for each parameter, in order of
     *         appearance.
     * @throws IllegalStateException
     *             In case no fitting model could be found to create a model extent for an 'in' or
     *             'inout' parameter.
     * @see #doExecution(TransformationData, ExecutionContext, ModelExtent[])
     */
	protected ModelExtent[] setupModelExtents(final QVToModelTransformation transformation) {
		assert transformation != null && transformation.getTransformationExecutor() != null;

		final ModelExtent[] modelExtents = new ModelExtent[transformation.getParameterCount()];
		// prepare in/inout parameters : order probably?
		for (final TransformationParameterInformation inParams : transformation.getInParameters()) {
			final Collection<EObject> sourceModel = this.availableModels.getModelsByType(inParams.getParameterType());
			if (sourceModel.isEmpty()) {
				throw new IllegalStateException("No model in QVTo model cache for "
						+ (inParams.getParameterIndex() + 1) + " of parameter type " + inParams.getParameterType().getName() + " Parameter of transformation '"
						+ transformation.getTransformationName() + "'");
			}
			modelExtents[inParams.getParameterIndex()] = sourceModel.stream()
					.map(CREATE_NON_EMPTY_MODEL_ELEMENTS_SWITCH::apply)
					.flatMap(Collection::stream)
					.collect(BASIC_MODEL_EXTENT_COLLECTOR);
		}

		// now the pure out params, they need empty model extents
		transformation.getPureOutParameters().stream()
			.mapToInt(TransformationParameterInformation::getParameterIndex)
			.forEach(index -> modelExtents[index] = new BasicModelExtent());

		return modelExtents;
	}
}
