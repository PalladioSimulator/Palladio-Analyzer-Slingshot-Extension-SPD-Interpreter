package org.palladiosimulator.analyzer.slingshot.behavior.spd.monitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.ProcessingType;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;

/**
 * A class that clones monitors (and measuring points) of a resource container
 * and its active resource specifications.
 * 
 * Everytime a cloning action is performed, the monitor is also saved directly
 * into the repository.
 * 
 * @author Julijan Katic
 */
public class ResourceContainerMonitorCloner {

	private final MeasuringPointRepository mpRepository;
	private final MonitorRepository monitorRepository;

	/** The list of monitors that are connected to the unit resource container */
	private final List<Monitor> unitResourceContainerMonitors;

	/**
	 * The list of monitors that are connected to the resource specs of the unit
	 * container.
	 */
	private final List<Monitor> unitActiveResourceMonitors;

	/**
	 * Constructs this class.
	 * 
	 * @param monitorRepository The monitor repository, needed to save the cloned
	 *                          monitors there.
	 * @param mpRepository      The measuring point repository, needed to save the
	 *                          cloned MPs there.
	 * @param unit              The resource container unit which is used to clone
	 *                          containers.
	 */
	public ResourceContainerMonitorCloner(final MonitorRepository monitorRepository,
			final MeasuringPointRepository mpRepository, final ResourceContainer unit) {
		this.mpRepository = mpRepository;
		this.monitorRepository = monitorRepository;

		final ResourceContainerMonitorFinder finder = new ResourceContainerMonitorFinder(monitorRepository);
		this.unitResourceContainerMonitors = finder.doSwitch(unit);
		this.unitActiveResourceMonitors = unit.getActiveResourceSpecifications_ResourceContainer().stream()
				.flatMap(ar -> finder.doSwitch(ar).stream()).toList();

	}

	/**
	 * Creates ("clones") new monitors for the resource container itself and its
	 * active resource specifications. For each monitor that reference the unit
	 * resource container, there will be a new monitor of the same type for the
	 * copied resource container. The same is true for the active resource specs.
	 * However, because it is not possible to track from where the copied spec is
	 * copied from, only the resource type will be considered.
	 * 
	 * For example, if there were monitors for each spec SPEC1 of type CPU and SPEC2
	 * of type HDD in the unit container, there are going to be new monitors for the
	 * copied specs. However, if the unit container contains two specs of the same
	 * type, only one of the copied specs gets a new monitor.
	 * 
	 * @param copiedResourceContainer
	 * @return
	 */
	public List<Monitor> createMonitorsForResourceContainer(final ResourceContainer copiedResourceContainer) {
		final List<Monitor> monitors = new ArrayList<>(
				unitResourceContainerMonitors.size() + unitActiveResourceMonitors.size());

		this.unitResourceContainerMonitors.stream()
				.map(unitMonitor -> createMonitor(copiedResourceContainer, unitMonitor)).forEach(monitors::add);

		this.unitActiveResourceMonitors.stream()
			.<Monitor>mapMulti((monitor, acceptor) -> {
				final ActiveResourceMeasuringPoint ap = (ActiveResourceMeasuringPoint) monitor.getMeasuringPoint();
				copiedResourceContainer.getActiveResourceSpecifications_ResourceContainer().stream()
						.filter(spec -> isSameProcessingType(spec, ap.getActiveResource()))
						.findAny()
						.ifPresent(spec -> acceptor.accept(createMonitor(spec, monitor)));
			}).forEach(monitors::add);

		return monitors;
	}

	private static boolean isSameProcessingType(final ProcessingResourceSpecification firstSpec,
			final ProcessingResourceSpecification secondSpec) {
		return firstSpec.getActiveResourceType_ActiveResourceSpecification().getId()
				.equals(secondSpec.getActiveResourceType_ActiveResourceSpecification().getId());
	}

	private Monitor createMonitor(final ResourceContainer resourceContainer, final Monitor originalMonitor) {
		final Monitor monitor = createMonitor(originalMonitor);
		monitor.setMeasuringPoint(createMeasuringPoint(resourceContainer, originalMonitor.getMeasuringPoint()));
		return monitor;
	}

	private MeasuringPoint createMeasuringPoint(final ResourceContainer resourceContainer,
			final MeasuringPoint originalMeasuringPoint) {
		if (originalMeasuringPoint instanceof ResourceContainerMeasuringPoint) {
			final ResourceContainerMeasuringPoint copy = PcmmeasuringpointFactory.eINSTANCE
					.createResourceContainerMeasuringPoint();
			copy.setMeasuringPointRepository(mpRepository);
			copy.setResourceContainer(resourceContainer);
			mpRepository.getMeasuringPoints().add(copy);
			return copy;
		}

		return null;
	}

	/**
	 * Helper method that creates a generic monitor without a reference to a
	 * measuring point. Every attribute is copied from the original monitor, except:
	 * - The reference to the old measuring point - The entity name will contain the
	 * old name, but with "-copy" appended - The id will be newly generated
	 * 
	 * @param originalMonitor The original monitor to copy from
	 * @return A new monitor
	 */
	private Monitor createMonitor(final Monitor originalMonitor) {
		final Monitor monitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();

		monitor.setActivated(originalMonitor.isActivated());
		monitor.setEntityName(originalMonitor.getEntityName() + "-copy");
		monitor.setId(EcoreUtil.generateUUID());
		originalMonitor.getMeasurementSpecifications().stream().map(spec -> createMeasurementSpec(spec, monitor))
				.forEach(monitor.getMeasurementSpecifications()::add);

		monitor.setMonitorRepository(monitorRepository);
		monitorRepository.getMonitors().add(monitor);

		return monitor;
	}

	private MeasurementSpecification createMeasurementSpec(final MeasurementSpecification originalSpec,
			final Monitor newMonitor) {
		final MeasurementSpecification newSpecification = MonitorRepositoryFactory.eINSTANCE
				.createMeasurementSpecification();
		newSpecification.setMetricDescription(originalSpec.getMetricDescription());
		newSpecification.setId(EcoreUtil.generateUUID());
		newSpecification.setMonitor(newMonitor);
		newSpecification.setProcessingType(createProcessingType(originalSpec.getProcessingType(), newSpecification));

		// IMPORTANT: For some very weird reason you cannot .getName()?
		// final String originalName = originalSpec.getName();
		// newSpecification.setName(originalName + "-copy");

		newSpecification.setTriggersSelfAdaptations(originalSpec.isTriggersSelfAdaptations());
		return newSpecification;
	}

	/**
	 * This copies the processing type as well. Since ProcessingType is abstract, we
	 * need to use E-Attributes and E-References to copy the actual attributes in
	 * the class.
	 * 
	 * @param original
	 * @return
	 */
	private ProcessingType createProcessingType(final ProcessingType original,
			final MeasurementSpecification newSpecification) {
		final EcoreUtil.Copier copier = new EcoreUtil.Copier();
		final ProcessingType pt = (ProcessingType) copier.copy(original);
		copier.copyReferences();

		pt.setMeasurementSpecification(newSpecification);
		pt.setId(EcoreUtil.generateUUID());

		return pt;
	}

	private Monitor createMonitor(final ProcessingResourceSpecification spec, final Monitor originalMonitor) {
		final Monitor monitor = createMonitor(originalMonitor);
		monitor.setMeasuringPoint(createMeasuringPoint(spec, originalMonitor.getMeasuringPoint()));
		return monitor;
	}

	private MeasuringPoint createMeasuringPoint(final ProcessingResourceSpecification spec,
			final MeasuringPoint originalMeasuringPoint) {
		if (originalMeasuringPoint instanceof final ActiveResourceMeasuringPoint acMP) {
			final ActiveResourceMeasuringPoint copy = PcmmeasuringpointFactory.eINSTANCE
					.createActiveResourceMeasuringPoint();
			copy.setMeasuringPointRepository(mpRepository);
			copy.setActiveResource(spec);
			copy.setReplicaID(acMP.getReplicaID());
			mpRepository.getMeasuringPoints().add(copy);
			return copy;
		}

		return null;
	}

}
