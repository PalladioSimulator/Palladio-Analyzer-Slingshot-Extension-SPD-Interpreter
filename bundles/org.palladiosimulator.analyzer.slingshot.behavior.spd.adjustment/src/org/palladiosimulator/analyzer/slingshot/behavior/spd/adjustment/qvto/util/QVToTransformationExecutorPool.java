package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Supplier;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.QVToTransformationExecutor;

public class QVToTransformationExecutorPool {
	private final Collection<SoftReference<QVToTransformationExecutor>> availableExecutors;
	private final ReferenceQueue<QVToTransformationExecutor> collectedExecutors;
	protected final Supplier<QVToTransformationExecutor> executorSupplier;
	
	public QVToTransformationExecutorPool(final Supplier<QVToTransformationExecutor> executorSupplier) {
		this.availableExecutors = new LinkedList<>();
		this.collectedExecutors = new ReferenceQueue<>();
		this.executorSupplier = executorSupplier;
	}
	
	public QVToTransformationExecutor getExecutor() {
		//Reference<? extends QVToTransformationExecutor> ref;
		
		for (Reference<? extends QVToTransformationExecutor> ref = collectedExecutors.poll();
			 ref != null;
			 ref = collectedExecutors.poll()) {
			availableExecutors.remove(ref);
		}
		
		return availableExecutors.stream()
								 .map(ref -> ref.get())
								 .filter(ref -> ref != null)
								 .filter(exec -> !exec.isInUse())
								 .findFirst()
								 .orElseGet(() -> {
									final QVToTransformationExecutor exec = executorSupplier.get();
									availableExecutors.add(new SoftReference<>(exec, collectedExecutors));
									return exec;
								 });
	}
}
