package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import java.util.Collection;
import java.util.function.Supplier;

import org.eclipse.m2m.internal.qvt.oml.expressions.OperationalTransformation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.QVToTransformationExecutorPool;

@SuppressWarnings("restriction")
public class QVToPoolingModelTransformation extends QVToModelTransformation {

	protected final QVToTransformationExecutorPool executorPool;
	
	public QVToPoolingModelTransformation(
			final OperationalTransformation transformation,
			final Supplier<QVToTransformationExecutor> executorSupplier,
			final Collection<TransformationParameterInformation> paramInfo
	) {
		super(transformation, null, paramInfo);
		this.executorPool = new QVToTransformationExecutorPool(executorSupplier);
	}
	
	@Override
	public QVToTransformationExecutor getTransformationExecutor() {
		return executorPool.getExecutor();
	}
	
}
