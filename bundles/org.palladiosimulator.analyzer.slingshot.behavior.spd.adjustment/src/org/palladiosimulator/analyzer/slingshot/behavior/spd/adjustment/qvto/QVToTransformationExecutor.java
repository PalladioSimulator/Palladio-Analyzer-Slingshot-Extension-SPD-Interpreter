package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.qvt.oml.ExecutionContext;
import org.eclipse.m2m.qvt.oml.ExecutionDiagnostic;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;

public class QVToTransformationExecutor {
	private final AtomicBoolean inUse = new AtomicBoolean(false);
	protected TransformationExecutor internalExecutor = null;
	
	public QVToTransformationExecutor(final URI uri) {
		setupInternalExecutor(uri, null);
	}
	
	public QVToTransformationExecutor(final URI uri, EPackage.Registry registry) {
		setupInternalExecutor(uri, registry);
	}
	
	public Diagnostic loadTransformation() {
		return internalExecutor.loadTransformation();
	}
	
	public Diagnostic loadTransformation(final IProgressMonitor progressMonitor) {
		return internalExecutor.loadTransformation(progressMonitor);
	}
	
	public ExecutionDiagnostic execute(final ExecutionContext executionContext, final ModelExtent... modelParameters) {
		if (!this.inUse.compareAndSet(false, true)) {
			throw new IllegalStateException("This QVToTransformationExecutor instance is already in use.");
		}
		final ExecutionDiagnostic result = internalExecutor.execute(executionContext, modelParameters);
		internalExecutor.cleanup();
		inUse.set(false);
		return result;
	}
	
	public boolean isInUse() {
		return inUse.get();
	}
	
	protected void setupInternalExecutor(final URI uri, final EPackage.Registry registry) {
		this.internalExecutor = Optional.ofNullable(registry)
										.map(r -> new TransformationExecutor(uri, r))
										.orElseGet(() -> new TransformationExecutor(uri));
	}
}
