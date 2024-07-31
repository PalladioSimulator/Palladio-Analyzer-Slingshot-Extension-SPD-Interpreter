package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.ModelTransformationCache;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.QVToModelCache;

/**
 * QVTo executor helper class that supports executing QVTo reconfiguration rules.
 * 
 * @author Matthias Becker
 * @author Sebastian Lehrig
 * @author Florian Rosenthal
 */
public class QVToExecutor extends AbstractQVToExecutor {
	
	private static final Logger LOGGER = Logger.getLogger(QVToExecutor.class);
	
	public QVToExecutor(final ModelTransformationCache knownTransformationCache,
						final QVToModelCache knownModels) {
		super(knownTransformationCache, knownModels);
	}
	
	public boolean executeTransformations(final Iterable<QVToModelTransformation> transformations) {
		boolean result = true;
		for (final QVToModelTransformation transformation : transformations) {

			result &= executeTransformation(transformation);
		}
		return result;
	}
	
}
