package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToModelTransformation;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToReconfigurator;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment.AllocationChange;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment.ModelChange;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment.MonitorChange;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.monitor.ResourceContainerMonitorCloner;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.SemanticspdFactory;
import org.palladiosimulator.spd.SPD;

@OnEvent(when = ModelAdjustmentRequested.class, then = ModelAdjusted.class, cardinality = EventCardinality.SINGLE)
public class SpdAdjustmentBehavior implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SpdAdjustmentBehavior.class);

	private final boolean activated;

	private final SPD spd;
	private final QVToReconfigurator reconfigurator;
	private final Iterable<QVToModelTransformation> transformations;
	private final Allocation allocation;
	private final Configuration semanticConfiguration;
	private final MonitorRepository monitorRepository;

	@Inject
	public SpdAdjustmentBehavior(
			final Allocation allocation,
			final @Nullable MonitorRepository monitorRepository,
			final @Nullable Configuration semanticConfiguration,
			final @Nullable SPD spd,
			final QVToReconfigurator reconfigurator,
			@Named(SpdAdjustorModule.MAIN_QVTO) final Iterable<QVToModelTransformation> transformations) {
		this.activated = monitorRepository != null && semanticConfiguration != null && spd != null;
		this.allocation = allocation;
		this.semanticConfiguration = semanticConfiguration;
		this.spd = spd;
		this.reconfigurator = reconfigurator;
		this.transformations = transformations;
		this.monitorRepository = monitorRepository;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	@Subscribe
	public Result<ModelAdjusted> onModelAdjustmentRequested(final ModelAdjustmentRequested event) {
		final ResourceEnvironment environment = allocation.getTargetResourceEnvironment_Allocation();

		/* Since the model is provided by the user, the model will be available in the cache already. */
		//final Configuration configuration = createConfiguration(event, environment);
		//this.reconfigurator.getModelCache().storeModel(configuration);

		// Set the enacted policy for the next transformation
		this.semanticConfiguration.setEnactedPolicy(event.getScalingPolicy());
		final List<ResourceContainer> oldContainers = new ArrayList<>(environment.getResourceContainer_ResourceEnvironment());
		final List<AllocationContext> oldAllocationContexts = new ArrayList<>(allocation.getAllocationContexts_Allocation());

		final boolean result = this.reconfigurator.execute(this.transformations);

		LOGGER.debug("RECONFIGURATION WAS " + result);

		if (result) {
			LOGGER.debug("Number of resource container is now: " + environment.getResourceContainer_ResourceEnvironment().size());

			/*
			 * Calculate what the new and deleted resource containers are for tracking.
			 */
			final List<ResourceContainer> newResourceContainers = new ArrayList<>(environment.getResourceContainer_ResourceEnvironment());
			newResourceContainers.removeAll(oldContainers);

			final List<ResourceContainer> deletedResourceContainers = new ArrayList<>(oldContainers);
			deletedResourceContainers.removeAll(environment.getResourceContainer_ResourceEnvironment());


			final List<AllocationContext> newAllocationContexts =  new ArrayList<>(allocation.getAllocationContexts_Allocation());
			newAllocationContexts.removeAll(oldAllocationContexts);


			final List<ModelChange<?>> changes = new ArrayList<>();



			changes.add(ResourceEnvironmentChange.builder()
					.resourceEnvironment(environment).simulationTime(event.time()).oldResourceContainers(oldContainers)
					.newResourceContainers(newResourceContainers).deletedResourceContainers(deletedResourceContainers)
					.build());


			changes.add(AllocationChange.builder().allocation(allocation).newAllocationContexts(newAllocationContexts).build());


			changes.addAll(this.createMonitors(newResourceContainers, event.time()));

			return Result.of(new ModelAdjusted(true, changes));
		} else {
			return Result.of(new ModelAdjusted(false, Collections.emptyList()));
		}

	}

	/**
	 * Create new monitors for all new containers created by the reconfiguration
	 * transformation.
	 *
	 * The news monitors match the monitors defined for the original container.
	 *
	 * @param newContainers  containers created by the reconfiguration
	 *                       transformation
	 * @param simulationTime time of the reconfiguration
	 * @return List of newly created monitors for all resource containers in
	 *         {@code newContainers}.
	 */
	private List<MonitorChange> createMonitors(final List<ResourceContainer> newContainers, final double simulationTime) {
		if (newContainers.isEmpty() || this.getUnitContainer(newContainers.get(0)) == null) {
			return Collections.emptyList();
		}

		final ResourceContainer unitContainer = getUnitContainer(newContainers.get(0));

		final ResourceContainerMonitorCloner cloner = new ResourceContainerMonitorCloner(this.monitorRepository,
				monitorRepository.getMonitors().get(0).getMeasuringPoint().getMeasuringPointRepository(),
				unitContainer);

		return newContainers.stream()
				.flatMap(container -> cloner.createMonitorsForResourceContainer(container).stream())
				.map(newMonitor -> new MonitorChange(newMonitor, null, simulationTime))
				.toList();
	}

	/**
	 * Get the resource container which {@code referenceContainer} is a replica of.
	 *
	 * I.e. get the {@code unit} of of the {@link ElasticInfrastructureCfg} that has
	 * {@code referenceContainer} in its elements.
	 *
	 * @param referenceContainer a replicated resource container, must not be null.
	 * @return Unit resource container which {@code referenceContainer} is a replica
	 *         of.
	 */
	private ResourceContainer getUnitContainer(final ResourceContainer referenceContainer) {
		assert referenceContainer != null : "Reference Container is null but must not be null.";

		return this.semanticConfiguration.getTargetCfgs().stream()
				.filter(ElasticInfrastructureCfg.class::isInstance)
				.map(ElasticInfrastructureCfg.class::cast)
				.filter(eicfg -> eicfg.getElements().contains(referenceContainer))
				.map(el -> el.getUnit())
				.findAny()
				.orElse(null);
	}

	/*
	 * We leave the following methods for now, as we will need to make the Configuration through
	 * a dedicated launch tab instead.
	 */

	private ElasticInfrastructureCfg createElasticInfrastructureCfg(final ResourceEnvironment environment) {
		final ElasticInfrastructureCfg targetGroupConfig = SemanticspdFactory.eINSTANCE.createElasticInfrastructureCfg();
		targetGroupConfig.setResourceEnvironment(environment);
		targetGroupConfig.setUnit(environment.getResourceContainer_ResourceEnvironment().stream().findAny().get());
		targetGroupConfig.getElements().addAll(environment.getResourceContainer_ResourceEnvironment());
		targetGroupConfig.setUnit(null);
		return targetGroupConfig;
	}

	/**
	 * Helper method for creating the {@link Configuration}
	 */
	private Configuration createConfiguration(final ModelAdjustmentRequested event,
			final ResourceEnvironment environment) {
		final Configuration configuration = SemanticspdFactory.eINSTANCE.createConfiguration();
		configuration.setAllocation(allocation);
		configuration.setResourceEnvironment(environment);
		configuration.setSpd(spd);
		configuration.setSystem(allocation.getSystem_Allocation());
		configuration.setRepository(allocation.getSystem_Allocation().getAssemblyContexts__ComposedStructure().get(0).getEncapsulatedComponent__AssemblyContext().getRepository__RepositoryComponent()); // TODO: What to do here?
		configuration.setEnactedPolicy(event.getScalingPolicy());

		final ElasticInfrastructureCfg targetGroupConfig = createElasticInfrastructureCfg(environment);
		configuration.getTargetCfgs().add(targetGroupConfig);



		return configuration;
	}

}
