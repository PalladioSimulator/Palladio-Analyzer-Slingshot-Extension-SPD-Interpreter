package org.palladiosimulator.analyzer.slingshot.behavior.spd.monitor;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.util.ResourceenvironmentSwitch;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;

/**
 * This class is used to find all the monitors of a certain element within a
 * resource environment. It returns a list of monitors that has a measuring
 * point to the queried element.
 * 
 * @author Julijan Katic
 */
public final class ResourceContainerMonitorFinder extends ResourceenvironmentSwitch<List<Monitor>> {

	private final MonitorRepository monitorRepository;

	public ResourceContainerMonitorFinder(final MonitorRepository monitorRepository) {
		this.monitorRepository = monitorRepository;
	}

	@Override
	public List<Monitor> caseResourceContainer(ResourceContainer object) {
		return monitorRepository.getMonitors().stream()
				.filter(monitor -> monitor.getMeasuringPoint() instanceof ResourceContainerMeasuringPoint)
				.filter(monitor -> ((ResourceContainerMeasuringPoint) monitor.getMeasuringPoint())
						.getResourceContainer().getId().equals(object.getId()))
				.toList();
	}

	@Override
	public List<Monitor> caseProcessingResourceSpecification(ProcessingResourceSpecification object) {
		return monitorRepository.getMonitors().stream()
				.filter(monitor -> monitor.getMeasuringPoint() instanceof ActiveResourceMeasuringPoint)
				.filter(monitor -> ((ActiveResourceMeasuringPoint) monitor.getMeasuringPoint()).getActiveResource()
						.getId().equals(object.getId()))
				.toList();
	}

	@Override
	public List<Monitor> defaultCase(EObject object) {
		return Collections.emptyList();
	}


}
