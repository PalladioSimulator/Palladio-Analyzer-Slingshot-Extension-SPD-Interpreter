package org.palladiosimulator.analyzer.slingshot.behavior.spd.data.adjustment;

import java.util.List;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;

public class AllocationChange extends ModelChange<Allocation> {

	private final List<AllocationContext> newAllocationContexts;
	private double simulationTime;

    // Private constructor to enforce the use of the builder
    private AllocationChange(final Builder builder) {
        super(builder.allocation, builder.simulationTime);
        this.newAllocationContexts = builder.newAllocationContexts;
    }

    public List<AllocationContext> getNewAllocationContexts() {
        return newAllocationContexts;
    }

	public static Builder builder() {
		return new Builder();
	}

	// Builder class for AllocationChange
    public static class Builder {

    	private Allocation allocation;
        private List<AllocationContext> newAllocationContexts;
    	private double simulationTime = -1;


		public Builder allocation(final Allocation allocation) {
			this.allocation = allocation;
			return this;
		}

        public Builder newAllocationContexts(final List<AllocationContext> newAllocationContexts) {
            this.newAllocationContexts = newAllocationContexts;
            return this;
        }

		public Builder simulationTime(final double simulationTime) {
			this.simulationTime = simulationTime;
			return this;
		}

        public AllocationChange build() {
            return new AllocationChange(this);
        }
    }


}
