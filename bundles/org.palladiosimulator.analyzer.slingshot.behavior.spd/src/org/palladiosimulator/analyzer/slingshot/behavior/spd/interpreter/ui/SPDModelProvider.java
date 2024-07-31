package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ui;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.extension.ModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;

@Singleton
public class SPDModelProvider implements ModelProvider<SPD> {
	
	private static final Logger LOGGER = Logger.getLogger(SPDModelProvider.class);

	private final PCMResourceSetPartitionProvider provider;
	
	@Inject
	public SPDModelProvider(final PCMResourceSetPartitionProvider provider) {
		this.provider = provider;
	}
	
	@Override
	public SPD get() {
		final List<EObject> spds = provider.get().getElement(SpdPackage.eINSTANCE.getSPD());
		if (spds.size() == 0) {
			// It is important that for optional model, the corresponding classes using the model
			// should be able to handle nullable!
			LOGGER.warn("An SPD model was not provided. Null will be returned");
			return null;
		}
		return (SPD) spds.get(0);
	}

}
