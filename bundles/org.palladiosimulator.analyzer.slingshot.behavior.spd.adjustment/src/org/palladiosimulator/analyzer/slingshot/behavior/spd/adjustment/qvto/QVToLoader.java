package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util.ModelTransformationCache;

/**
 *
 * @author Julijan Katic
 */
public class QVToLoader {

	private static final Logger LOGGER = Logger.getLogger(QVToLoader.class);

	public static Iterable<QVToModelTransformation> loadFromFiles(final String... files) {
		final URI[] uris = new URI[files.length];
		for (int i = 0; i < files.length; ++i) {
			uris[i] = getUriFromFile(files[i]);
			LOGGER.debug(String.format("Uri is: %s", uris[i].toFileString()));
		}
		return new ModelTransformationCache(uris).getAll();
	}

	private static URI getUriFromFile(final String path) {
		final URI uri = URI.createURI(path);
		return uri;
	}

}
