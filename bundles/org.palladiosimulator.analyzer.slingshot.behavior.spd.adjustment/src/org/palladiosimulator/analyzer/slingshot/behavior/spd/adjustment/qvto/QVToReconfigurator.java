package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import javax.inject.Inject;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.ModelTransformationCache;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.QVToModelCache;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class QVToReconfigurator {

	private final MDSDBlackboard blackboard;
	private QVToExecutor qvtoExecutor;
	private QVToModelCache modelCache;
	private ModelTransformationCache transformationCache;
	
	@Inject
	public QVToReconfigurator(
			final MDSDBlackboard pcmPartitionManager
	) {
		this.blackboard = pcmPartitionManager;
		this.getQVToExecutor();
	}
	
	private QVToExecutor getQVToExecutor() {
		if (this.qvtoExecutor == null) {
			this.modelCache = new QVToModelCache(this.blackboard);
			this.transformationCache = new ModelTransformationCache();
			this.qvtoExecutor = new QVToExecutor(this.transformationCache, this.modelCache);
		}
		return this.qvtoExecutor;
	}
	
	public boolean execute(final Iterable<QVToModelTransformation> actions) {
		return getQVToExecutor().executeTransformations(actions);
	}

	public QVToModelCache getModelCache() {
		return modelCache;
	}

	public void setModelCache(final QVToModelCache modelCache) {
		this.modelCache = modelCache;
	}

	public ModelTransformationCache getTransformationCache() {
		return transformationCache;
	}

	public void setTransformationCache(final ModelTransformationCache transformationCache) {
		this.transformationCache = transformationCache;
	}
	
	
}
