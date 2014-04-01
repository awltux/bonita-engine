/*******************************************************************************
 * Copyright (C) 2013-2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.business.data.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.lang3.ClassUtils;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.SDependencyNotFoundException;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.bonitasoft.engine.dependency.model.SDependencyMapping;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.dependency.model.builder.SDependencyBuilderFactory;
import org.bonitasoft.engine.dependency.model.builder.SDependencyMappingBuilderFactory;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.QueryOptions;

import com.bonitasoft.engine.bdm.BDMCompiler;
import com.bonitasoft.engine.bdm.BDMJarBuilder;
import com.bonitasoft.engine.bdm.Entity;
import com.bonitasoft.engine.business.data.BusinessDataRepository;
import com.bonitasoft.engine.business.data.NonUniqueResultException;
import com.bonitasoft.engine.business.data.SBusinessDataNotFoundException;
import com.bonitasoft.engine.business.data.SBusinessDataRepositoryDeploymentException;
import com.bonitasoft.engine.business.data.SBusinessDataRepositoryException;

/**
 * @author Matthieu Chaffotte
 * @author Romain Bioteau
 */
public class JPABusinessDataRepositoryImpl implements BusinessDataRepository {

	private static final String BDR = "BDR";

	private final Map<String, Object> configuration;

	private EntityManagerFactory entityManagerFactory;

	private final DependencyService dependencyService;

	private EntityManager entityManager;

	private final SchemaUpdater schemaUpdater;

	public JPABusinessDataRepositoryImpl(final DependencyService dependencyService, final SchemaUpdater schemaUpdater, final Map<String, Object> configuration) {
		this.dependencyService = dependencyService;
		this.schemaUpdater = schemaUpdater;
		this.configuration = new HashMap<String, Object>(configuration);
		this.configuration.put("hibernate.ejb.resource_scanner", InactiveScanner.class.getName());
	}

	@Override
	public void start() throws SBonitaException {
		if (isDBMDeployed()) {
			entityManagerFactory = Persistence.createEntityManagerFactory(BDR, configuration);
			updateSchema();
		}
	}

	@Override
	public void stop() {
		if (entityManagerFactory != null) {
			entityManagerFactory.close();
			entityManagerFactory = null;
		}
	}

	@Override
	public void pause() throws SBonitaException {
		stop();
	}

	@Override
	public void resume() throws SBonitaException {
		start();
	}

	protected void updateSchema() throws SBusinessDataRepositoryDeploymentException {
		schemaUpdater.execute(getAnnotatedClasses());

        final List<Exception> exceptions = schemaUpdater.getExceptions();
        if (!exceptions.isEmpty()) {
            throw new SBusinessDataRepositoryDeploymentException("Upating schema fails due to: " + exceptions);
        }
	}
	
	 private Set<Class<?>> getAnnotatedClasses() {
	        Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
	        for (final EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
	            annotatedClasses.add(entity.getJavaType());
	        }
	        return annotatedClasses;
	    }

	@Override
	public Set<String> getEntityClassNames() {
		if (entityManagerFactory == null) {
			return Collections.emptySet();
		}
		final EntityManager em = getEntityManager();
		final Set<EntityType<?>> entities = em.getMetamodel().getEntities();
		final Set<String> entityClassNames = new HashSet<String>();
		for (final EntityType<?> entity : entities) {
			entityClassNames.add(entity.getJavaType().getName());
		}
		return entityClassNames;
	}

	protected boolean isDBMDeployed() throws SBusinessDataRepositoryException {
		final byte[] dependency = getDeployedBDMDependency();
		return dependency != null && dependency.length > 0;
	}

	@Override
	public byte[] getDeployedBDMDependency() throws SBusinessDataRepositoryException {
		final FilterOption filterOption = new FilterOption(SDependency.class, "name", BDR);
		final List<FilterOption> filters = new ArrayList<FilterOption>();
		filters.add(filterOption);
		final QueryOptions queryOptions = new QueryOptions(filters, null);
		List<SDependency> dependencies;
		try {
			dependencies = dependencyService.getDependencies(queryOptions);
		} catch (SDependencyException e) {
			throw new SBusinessDataRepositoryException(e);
		}
		if (dependencies.isEmpty()) {
			return null;
		}
		return dependencies.get(0).getValue();
	}

	protected EntityManager getEntityManager() {
		if (entityManagerFactory == null) {
			throw new IllegalStateException("The BDR is not started");
		}

		if (entityManager == null || !entityManager.isOpen()) {
			entityManager = entityManagerFactory.createEntityManager();
		} else {
			entityManager.joinTransaction();
		}
		return entityManager;
	}

	protected SDependencyMapping createDependencyMapping(final long tenantId, final SDependency sDependency) {
		return BuilderFactory.get(SDependencyMappingBuilderFactory.class).createNewInstance(sDependency.getId(), tenantId, ScopeType.TENANT).done();
	}

	protected SDependency createSDependency(final long tenantId, final byte[] transformedBdrArchive) {
		return BuilderFactory.get(SDependencyBuilderFactory.class).createNewInstance(BDR, tenantId, ScopeType.TENANT, BDR + ".jar", transformedBdrArchive)
				.done();
	}

	protected byte[] generateBDMJar(final byte[] bdmZip) throws SBusinessDataRepositoryDeploymentException {
		final BDMJarBuilder builder = new BDMJarBuilder(BDMCompiler.create());
		return builder.build(bdmZip);
	}

	@Override
	public void deploy(final byte[] bdmZip, final long tenantId) throws SBusinessDataRepositoryDeploymentException {
		final byte[] bdmJar = generateBDMJar(bdmZip);
		final SDependency sDependency = createSDependency(tenantId, bdmJar);
		try {
			dependencyService.createDependency(sDependency);
			final SDependencyMapping sDependencyMapping = createDependencyMapping(tenantId, sDependency);
			dependencyService.createDependencyMapping(sDependencyMapping);
		} catch (final SDependencyException e) {
			throw new SBusinessDataRepositoryDeploymentException(e);
		}
	}

	@Override
	public void undeploy(final long tenantId) throws SBusinessDataRepositoryException {
		try {
			dependencyService.deleteDependency(BDR);
		} catch (final SDependencyNotFoundException sde) {
			// do nothing
		} catch (final SDependencyException sde) {
			throw new SBusinessDataRepositoryException(sde);
		}
	}

	@Override
	public <T extends Entity> T findById(final Class<T> entityClass, final Long primaryKey) throws SBusinessDataNotFoundException {
		if (primaryKey == null) {
			throw new SBusinessDataNotFoundException("Impossible to get data with a null identifier");
		}
		final EntityManager em = getEntityManager();
		final T entity = em.find(entityClass, primaryKey);
		if (entity == null) {
			throw new SBusinessDataNotFoundException("Impossible to get data with id: " + primaryKey);
		}
		em.detach(entity);
		return entity;
	}


	protected <T extends Serializable> T find(final Class<T> resultClass, final TypedQuery<T> query, final Map<String, Serializable> parameters) throws NonUniqueResultException {
		if(query == null){
			throw new IllegalArgumentException("query is null");
		}
		if (parameters != null) {
			for (final Entry<String, Serializable> parameter : parameters.entrySet()) {
				query.setParameter(parameter.getKey(), parameter.getValue());
			}
		}
		final EntityManager em = getEntityManager();
		try {
			final T entity = query.getSingleResult();
			detachEntity(em, resultClass, entity);
			return entity;
		} catch (final javax.persistence.NonUniqueResultException nure) {
			throw new NonUniqueResultException(nure);
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public <T extends Serializable> T find(Class<T> resultClass,
			String jpqlQuery, Map<String, Serializable> parameters)
					throws NonUniqueResultException {
		TypedQuery<T> typedQuery = createTypedQuery(jpqlQuery, resultClass);
		return find(resultClass, typedQuery, parameters);
	}

	@Override
	public <T extends Serializable> List<T> findList(Class<T> resultClass,
			String jpqlQuery, Map<String, Serializable> parameters) {
		TypedQuery<T> typedQuery = createTypedQuery(jpqlQuery, resultClass);
		return findList(resultClass, typedQuery, parameters);
	}

	@Override
	public <T extends Serializable> T findByNamedQuery(final String queryName, final Class<T> resultClass, final Map<String, Serializable> parameters)
			throws NonUniqueResultException {
		final EntityManager em = getEntityManager();
		TypedQuery<T> query = em.createNamedQuery(queryName, resultClass);
		return find(resultClass, query, parameters);
	}

	@Override
	public <T extends Serializable> List<T> findListByNamedQuery(final String queryName, final Class<T> resultClass, final Map<String, Serializable> parameters) {
		final EntityManager em = getEntityManager();
		TypedQuery<T> query = em.createNamedQuery(queryName, resultClass);
		return findList(resultClass, query, parameters);
	}


	private  <T extends Serializable> TypedQuery<T> createTypedQuery(String jpqlQuery,Class<T> resultClass){
		return getEntityManager().createQuery(jpqlQuery, resultClass);
	}


	protected <T extends Serializable> List<T> findList(final Class<T> resultClass, final TypedQuery<T> query, final Map<String, Serializable> parameters) {
		if(query == null){
			throw new IllegalArgumentException("query is null");
		}
		if (parameters != null) {
			for (final Entry<String, Serializable> parameter : parameters.entrySet()) {
				query.setParameter(parameter.getKey(), parameter.getValue());
			}
		}
		final EntityManager em = getEntityManager();
		final List<T> resultList = query.getResultList();
		for (final T entity : resultList) {
			detachEntity(em, resultClass, entity);
		}
		return resultList;
	}

	private <T> void detachEntity(final EntityManager em, final Class<T> resultClass, final T entity) {
		if (!ClassUtils.isPrimitiveOrWrapper(resultClass)) {
			em.detach(entity);
		}
	}

	@Override
	public <T extends Entity> T merge(final T entity) {
		if (entity != null) {
			final EntityManager em = getEntityManager();
			return em.merge(entity);
		}
		return null;
	}

	@Override
	public void remove(final Entity entity) {
		if (entity != null && entity.getPersistenceId() != null) {
			final EntityManager em = getEntityManager();
			final Entity attachedEntity = em.find(entity.getClass(), entity.getPersistenceId());
			if (attachedEntity != null) {
				em.remove(attachedEntity);
			}
		}
	}



}
