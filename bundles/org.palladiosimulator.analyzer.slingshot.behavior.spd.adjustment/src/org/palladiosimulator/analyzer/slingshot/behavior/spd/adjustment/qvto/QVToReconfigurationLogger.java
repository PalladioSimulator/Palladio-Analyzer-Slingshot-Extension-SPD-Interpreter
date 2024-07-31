package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.m2m.qvt.oml.util.Log;

/**
 * QVTo Reconfigurator Logging Class
 * 
 * @author Matthias Becker, Sebastian Krach
 *
 */
public class QVToReconfigurationLogger implements Log {
	
	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;
	
	private Logger logger;

	public QVToReconfigurationLogger(final Class<?> clazz) {
		this.logger = Logger.getLogger(clazz);
	}
	
	@Override
	public void log(int logLevel, String message, Object param) {
		final Level level = Level.toLevel(logLevel, DEFAULT_LOG_LEVEL);
		logger.log(level, String.format(message, param));
	}

	@Override
	public void log(int logLevel, String message) {
		final Level level = Level.toLevel(logLevel, DEFAULT_LOG_LEVEL);
		logger.log(level, message);
	}

	@Override
	public void log(String message, Object param) {
		logger.log(DEFAULT_LOG_LEVEL, String.format(message, param));
	}

	@Override
	public void log(String message) {
		logger.log(DEFAULT_LOG_LEVEL, message);
	}

}
