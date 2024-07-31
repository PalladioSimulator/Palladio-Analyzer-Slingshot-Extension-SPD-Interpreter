package org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment;

import java.util.Collections;
import java.util.List;

import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;

public class ResourceEnvironmentChange extends ModelChange<ResourceEnvironment> {

	private final List<ResourceContainer> oldResourceContainers;
	private final List<ResourceContainer> newResourceContainers;
	private final List<ResourceContainer> deletedResourceContainers;
	
	public ResourceEnvironmentChange(final Builder builder) {
		super(builder.environment, builder.simulationTime);
		this.oldResourceContainers = Collections.unmodifiableList(builder.oldResourceContainers);
		this.newResourceContainers = Collections.unmodifiableList(builder.newResourceContainers);
		this.deletedResourceContainers = Collections.unmodifiableList(builder.deletedResourceContainers);
	}

	public List<ResourceContainer> getOldResourceContainers() {
		return oldResourceContainers;
	}

	public List<ResourceContainer> getNewResourceContainers() {
		return newResourceContainers;
	}

	public List<ResourceContainer> getDeletedResourceContainers() {
		return deletedResourceContainers;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder {
		private ResourceEnvironment environment;
		private double simulationTime = -1;
		private List<ResourceContainer> oldResourceContainers;
		private List<ResourceContainer> newResourceContainers;
		private List<ResourceContainer> deletedResourceContainers;
		
		private Builder() {}
		
		public Builder resourceEnvironment(final ResourceEnvironment environment) {
			this.environment = environment;
			return this;
		}
		
		public Builder simulationTime(final double simulationTime) {
			this.simulationTime = simulationTime;
			return this;
		}
		
		public Builder oldResourceContainers(final List<ResourceContainer> oldResourceContainers) {
			this.oldResourceContainers = oldResourceContainers;
			return this;
		}
		
		public Builder newResourceContainers(final List<ResourceContainer> newResourceContainers) {
			this.newResourceContainers = newResourceContainers;
			return this;
		}
		
		public Builder deletedResourceContainers(final List<ResourceContainer> deletedResourceContainers) {
			this.deletedResourceContainers = deletedResourceContainers;
			return this;
		}
		
		public ResourceEnvironmentChange build() {
			return new ResourceEnvironmentChange(this);
		}
	}
}
