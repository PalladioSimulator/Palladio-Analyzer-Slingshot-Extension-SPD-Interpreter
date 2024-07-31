package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment;

import javax.inject.Named;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToLoader;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToModelTransformation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToReconfigurator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.ui.SemanticModelLaunchConfig;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.ui.SemanticModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.semanticspd.Configuration;

import com.google.inject.Provides;

public class SpdAdjustorModule extends AbstractSlingshotExtension {

	private static final String MAIN_QVTO_FILE = "platform:/plugin/org.palladiosimulator.spd.semantic.transformations/transformations/spd/MainTransformation.qvto";
	public static final String MAIN_QVTO = "mainqvto";
	
	@Override
	protected void configure() {
		install(SpdAdjustmentBehavior.class);
		install(SemanticModelLaunchConfig.class);
		provideModel(Configuration.class, SemanticModelProvider.class);

		bind(QVToReconfigurator.class);
	}

	@Provides
	@Named(MAIN_QVTO)
	public String mainQvtoFile() {
		return MAIN_QVTO_FILE;
	}
	
	@Provides
	@Named(MAIN_QVTO)
	public Iterable<QVToModelTransformation> getTransformations() {
		return QVToLoader.loadFromFiles(MAIN_QVTO_FILE);
	}
	
}
