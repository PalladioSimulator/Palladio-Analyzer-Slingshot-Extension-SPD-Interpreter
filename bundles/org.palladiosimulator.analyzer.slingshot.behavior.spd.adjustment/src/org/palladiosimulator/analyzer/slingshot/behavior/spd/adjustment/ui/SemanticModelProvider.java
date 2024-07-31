package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.ui;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.extension.ModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.SemanticspdPackage;

public class SemanticModelProvider implements ModelProvider<Configuration> {

	private static final Logger LOGGER = Logger.getLogger(SemanticModelProvider.class);

	private final PCMResourceSetPartitionProvider resourceSet;

	@Inject
	public SemanticModelProvider(final PCMResourceSetPartitionProvider resourceSet) {
		this.resourceSet = resourceSet;
	}

	@Override
	public Configuration get() {
		final List<EObject> configurations = resourceSet.get().getElement(SemanticspdPackage.eINSTANCE.getConfiguration());
		if (configurations.size() == 0) {
			LOGGER.warn("Semantic model not present: List size is 0.");
			return null;
		}
		return (Configuration) configurations.get(0);
	}

}
