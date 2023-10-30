package org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.utils;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyReference;
import org.palladiosimulator.pcmmeasuringpoint.LinkingResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.OperationReference;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ResourceEnvironmentMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.util.PcmmeasuringpointSwitch;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 * A switch based approach of checking whether the measuring point is inside the
 * given target group. It should return true if this is the case.
 * 
 * Per default, we currently return {@code false}.
 * 
 * TODO: Check for the remaining measuring points
 * 
 * @author Julijan Katic
 */
public class MeasuringPointInsideTargetGroup extends PcmmeasuringpointSwitch<Boolean> {

	private final TargetGroup targetGroup;

	public MeasuringPointInsideTargetGroup(final TargetGroup targetGroup) {
		this.targetGroup = targetGroup;
	}

	@Override
	public Boolean caseActiveResourceMeasuringPoint(final ActiveResourceMeasuringPoint object) {
		return TargetGroupUtils.isContainerInTargetGroup(
				object.getActiveResource().getResourceContainer_ProcessingResourceSpecification(), targetGroup);
	}

	@Override
	public Boolean caseResourceContainerMeasuringPoint(final ResourceContainerMeasuringPoint object) {
		return TargetGroupUtils.isContainerInTargetGroup(object.getResourceContainer(), targetGroup);
	}

	@Override
	public Boolean caseResourceEnvironmentMeasuringPoint(final ResourceEnvironmentMeasuringPoint object) {
		return object.getResourceEnvironment().getResourceContainer_ResourceEnvironment().stream()
				.anyMatch(rc -> TargetGroupUtils.isContainerInTargetGroup(rc, targetGroup));
	}

	@Override
	public Boolean caseAssemblyReference(final AssemblyReference object) {
		return TargetGroupUtils.isAssemblyInTargetGroup(object.getAssembly(), targetGroup);
	}

	@Override
	public Boolean caseOperationReference(final OperationReference object) {
		return TargetGroupUtils.isOperationSinatureRelatedToTargetGroup(object.getOperationSignature(), targetGroup);
	}

	@Override
	public Boolean defaultCase(final EObject object) {
		return false;
	}

	@Override
	public Boolean caseLinkingResourceMeasuringPoint(LinkingResourceMeasuringPoint object) {
		return TargetGroupUtils.isLinkingResourceInTargetGroup(object.getLinkingResource(), targetGroup);
	}

}
