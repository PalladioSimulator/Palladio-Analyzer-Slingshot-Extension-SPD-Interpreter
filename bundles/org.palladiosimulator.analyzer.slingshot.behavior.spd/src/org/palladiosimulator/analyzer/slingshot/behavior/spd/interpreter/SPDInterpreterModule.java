package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ui.SPDModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.ui.SPDModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.spd.SPD;

public class SPDInterpreterModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(SPDModelConfiguration.class);
		install(SpdBehavior.class);
		provideModel(SPD.class, SPDModelProvider.class);
	}

}
