package org.palladiosimulator.analyzer.slingshot.behavior.spd.adjustment.qvto.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreSwitch;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.modelversioning.emfprofile.EMFProfilePackage;
import org.modelversioning.emfprofileapplication.EMFProfileApplicationPackage;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.jobs.LoadSharedPCMLibrariesIntoBlackboard;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.ResourceSetPartition;

/**
 * This cache implementation is used to store models (e.g., PCM models or runtime measurement
 * models) that can be parameters of QVTo transformations. To store a model in the cache, its
 * corresponding {@link EPackage} (its meta-model) is used as tag.
 * 
 * @author Florian Rosenthal, Sebastian Krach, Julijan Katic
 *
 */
public class QVToModelCache {
	
	private static final Logger LOGGER = Logger.getLogger(QVToModelCache.class);
	
	// Put (statically) EClass objects of blackboard models that are not intended to be
	// transformation parameters here
	private static final EClass[] MODEL_ECLASS_BLACKLIST = {
			EMFProfileApplicationPackage.eINSTANCE.getProfileApplication(), EMFProfilePackage.eINSTANCE.getProfile(),
			NotationPackage.eINSTANCE.getDiagram() };
	
	// switch to determine the meta-model/EPackage of a model
	private static final EcoreSwitch<EPackage> MODELTYPE_RETRIEVER = new EcoreSwitch<EPackage>() {
		
		@Override
		public EPackage caseEPackage(final EPackage ePackage) {
			return ePackage; // we found the model type, just return
		}
		
		@Override
		public EPackage caseEClass(final EClass eClass) {
			return doSwitch(eClass.eContainer());
		}
		
		@Override
		public EPackage defaultCase(final EObject eObject) {
			return doSwitch(eObject.eClass());
		}
		
	};
	
	private final Map<EPackage, Set<EObject>> cache;
	private final MDSDBlackboard blackboard;
	
	public QVToModelCache(final MDSDBlackboard blackboard) {
		this.cache = new HashMap<>();
		this.blackboard = Objects.requireNonNull(blackboard);
		storeBlackboardModels();
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param from The instance to copy from.
	 */
	private QVToModelCache(final QVToModelCache from) {
		this.cache = new HashMap<>();
		this.blackboard = from.blackboard;
		from.cache.values().stream()
						   .flatMap(Collection::stream)
						   .forEach(this::storeModel);
	}
	
	/**
	 * Stores the given model in the cache. If a model of the same type (denoted by the meta-model,
	 * i.e., the {@link EPackage} that corresponds to the model) is already present, it will be
	 * overwritten. <br>
	 * In case {@code null} is passed, this method does nothing.
	 * 
	 * @param modelInstance representing a model.
	 */
	public void storeModel(final EObject modelInstance) {
		if (modelInstance != null) {
			final EPackage metaModel = MODELTYPE_RETRIEVER.doSwitch(modelInstance);
			
			// We do not want to transform meta models
			if (modelInstance.equals(metaModel)) {
				return;
			}
			
			this.cache.computeIfAbsent(metaModel, mm -> new HashSet<>())
					  .add(modelInstance);
		}
	}
	
	/**
	 * Stores the model found in the partition of the blackboard that is identified by the given id.
	 * If a model of the same type (denoted by meta-model, i.e., the {@link EPackage} that
	 * corresponds to the model) is already present, it will be overwritten. <br>
	 * 
	 * If the given id does not identify the partition, or the requested partition is empty, nothing
	 * happens.
	 * 
	 * @param partitionId A string which identifies a {@link ResourceSetPartition} of the global {@link MDSDBlackboard}
	 * @throws NullPointerException In case {@code partitionId == null}
	 * @see #storeModel(EObject)
	 */
	public final void storeModelFromBlackboardPartition(final String partitionId) {
		if (blackboard.hasPartition(Objects.requireNonNull(partitionId))) {
			final ResourceSetPartition partition = blackboard.getPartition(partitionId);

			partition.getResourceSet().getResources().stream()
												     .map(Resource::getContents)
					.flatMap(contents -> contents.stream()).filter(Predicate.not(QVToModelCache::isBlacklisted))
					.forEach(this::storeModel);
		}
	}
	
	/**
	 * Removes all of the currently stored models which are instances of the meta-model represented by the
	 * given ePackage. <br>
	 * In case {@code null} is passed, this method does nothing.
	 * 
	 * @param metaModel describes a meta-model
	 */
	public void removeModelOfType(final EPackage metaModel) {
		if (metaModel != null) {
			this.cache.remove(metaModel);
		}
	}
	
	/**
     * Removes the given model from the cache, if present.<br>
     * This method does nothing, if {@code null} is passed or the given model is not cached.
     * 
     * @param model
     *            The {@link EObject} to remove from the cache.
     */
	public void removeModel(final EObject model) {
		if (model != null) {
			cache.get(MODELTYPE_RETRIEVER.doSwitch(model))
				 .remove(model);
		}
	}
	
	/**
     * Clears the cache, that is, all models are removed.
     */
    public void clear() {
        this.cache.clear();
    }

    /**
     * Creates a snapshot of the current state of the cache.<br>
     * More precisely, this method creates an instance that contains the same models as this one.
     * 
     * @return A {@link QVToModelCache} which is a snapshot of the current state of this instance.
     */
    public QVToModelCache snapshot() {
        return new QVToModelCache(this);
    }
    
    /**
     * Gets the currently stored model that is an instance of the meta-model represented by the
     * given ePackage.
     * 
     * @param ePackage
     *            An {@link EPackage} that describes a meta-model.
     * @return The model, contained in an {@link Optional} and represented as an {@link EObject},
     *         that is an instance of the given meta-model, or an empty {@link Optional} if none
     *         could be found.
     * @throws NullPointerException
     *             In case {@code ePackage == null}.
     */
    public Collection<EObject> getModelsByType(final EPackage ePackage) {
    	final String namespace = Objects.requireNonNull(ePackage.getNsURI());
    	final Collection<EPackage> res = cache.keySet().stream()
    												   .filter(key -> key.getNsURI().equals(namespace))
    												   .collect(Collectors.toList());
    	final Collection<Set<EObject>> result = res.stream()
    											   .map(cache::get)
    											   .collect(Collectors.toList());
    	return result.stream()
    				 .flatMap(Collection::stream)
    				 .collect(Collectors.toList());
    }
    
    /**
     * Gets whether a model of the meta-model represented by the given ePackage is currently in
     * store.
     * 
     * @param ePackage
     *            An {@link EPackage} that describes a meta-model.
     * @return {@code true} if a model of the given type is stored, {@code false} otherwise.
     * @throws NullPointerException
     *             In case {@code ePackage == null}.
     * 
     * @see #getModelsByType(EPackage)
     */
    public boolean containsModelOfType(final EPackage ePackage) {
    	return this.cache.containsKey(Objects.requireNonNull(ePackage));
    }
    
    private void storeBlackboardModels() {
    	assert this.blackboard != null;
    	storeModelFromBlackboardPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
    }
    
    private static boolean isBlacklisted(final EObject model) {
    	assert model != null;
    	
    	return model.eResource().getURI().equals(LoadSharedPCMLibrariesIntoBlackboard.PCM_PALLADIO_PRIMITIVE_TYPE_REPOSITORY_URI)
    			|| Arrays.stream(MODEL_ECLASS_BLACKLIST).anyMatch(bannedEClass -> bannedEClass == model.eClass());
    }
}
