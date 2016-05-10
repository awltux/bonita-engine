/**
 * Copyright (C) 2016 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

package org.bonitasoft.engine.dependency.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.commons.NullCheckingUtil;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyDeletionException;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.SDependencyNotFoundException;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.bonitasoft.engine.dependency.model.SDependencyMapping;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.service.BroadcastService;
import org.bonitasoft.engine.sessionaccessor.STenantIdNotSetException;
import org.bonitasoft.engine.transaction.STransactionNotFoundException;
import org.bonitasoft.engine.transaction.UserTransactionService;

/**
 * @author Baptiste Mesta
 */
public abstract class AbstractDependencyService implements DependencyService {
    protected static final int BATCH_SIZE = 100;
    private BroadcastService broadcastService;
    private UserTransactionService userTransactionService;
    private ReadPersistenceService persistenceService;

    public AbstractDependencyService(BroadcastService broadcastService, UserTransactionService userTransactionService, ReadPersistenceService persistenceService) {
        this.broadcastService = broadcastService;
        this.userTransactionService = userTransactionService;
        this.persistenceService = persistenceService;
    }

    protected abstract void delete(SDependency dependency) throws SDependencyDeletionException;

    protected abstract List<SDependency> getDependencies(QueryOptions queryOptions) throws SDependencyException;

    protected abstract SDependency getDependency(String name) throws SDependencyNotFoundException, SDependencyDeletionException;

    @Override
    public void deleteAllDependencies() throws SDependencyException {
        final QueryOptions queryOptions1 = getDefaultQueryOptionForDependencyMapping();
        List<SDependencyMapping> dependencyMappings;
        do {
            dependencyMappings = getDependencyMappings(queryOptions1);
            for (final SDependencyMapping dependencyMapping : dependencyMappings) {
                deleteDependencyMapping(dependencyMapping);
            }
        } while (dependencyMappings.size() == queryOptions1.getNumberOfResults());
        final QueryOptions queryOptions = getDefaultQueryOptionForDependency();
        List<SDependency> dependencies;
        do {
            dependencies = getDependencies(queryOptions);
            for (final SDependency dependency : dependencies) {
                delete(dependency);
            }
        } while (dependencies.size() == queryOptions.getNumberOfResults());
    }

    @Override
    public void deleteDependency(final String name) throws SDependencyException {
        final SDependency sDependency = getDependency(name);
        deleteDependency(sDependency);
    }

    /**
     * register a synchronization that will refresh the classloader everywhere
     * is called when a dependency change is triggered
     *
     * @param type
     * @param id
     * @throws SDependencyException
     */
    void registerRefreshOnAllNodes(final ScopeType type, final long id) throws SDependencyException {
        refreshClassLoader(type, id);
        try {
            registerRefreshOnOtherNodes(type, id);
        } catch (Exception e) {
            throw new SDependencyException(e);
        }
    }

    private void registerRefreshOnOtherNodes(ScopeType type, long id) throws STenantIdNotSetException, STransactionNotFoundException {
        final AbstractRefreshClassLoaderTask callable = getRefreshClassLoaderTask(type, id);
        userTransactionService.registerBonitaSynchronization(new RefreshClassloaderSynchronization(broadcastService, callable, getTenantId()));
    }

    protected abstract AbstractRefreshClassLoaderTask getRefreshClassLoaderTask(final ScopeType type, final long id);

    protected Map<String, byte[]> getDependenciesResources(final ScopeType type, final long id) throws SDependencyException {
        final Map<String, byte[]> resources = new HashMap<>();
        int fromIndex = 0;
        List<Long> dependencyIds;
        do {
            dependencyIds = getDependencyIds(id, type, fromIndex, BATCH_SIZE);
            if (dependencyIds != null && dependencyIds.size() > 0) {
                final List<SDependency> dependencies = getDependencies(dependencyIds);
                for (final SDependency dependency : dependencies) {
                    resources.put(dependency.getFileName(), dependency.getValue());
                }
            }
            fromIndex = fromIndex + BATCH_SIZE;
        } while (dependencyIds != null && dependencyIds.size() == BATCH_SIZE);
        return resources;
    }

    protected abstract Long getTenantId() throws STenantIdNotSetException;

    protected abstract void createDependencyMapping(SDependencyMapping dependencyMapping) throws SDependencyException;

    protected abstract void deleteDependencyMapping(SDependencyMapping dependencyMapping) throws SDependencyException;

    protected abstract List<SDependencyMapping> getDependencyMappings(long dependencyId, QueryOptions queryOptions) throws SDependencyException;

    @Override
    public List<Long> getDependencyIds(final long artifactId, final ScopeType artifactType, final int startIndex, final int maxResult)
            throws SDependencyException {
        NullCheckingUtil.checkArgsNotNull(artifactId, artifactType, startIndex, maxResult);
        final QueryOptions queryOptions = new QueryOptions(startIndex, maxResult);
        try {
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("artifactId", artifactId);
            parameters.put("artifactType", artifactType);
            final SelectListDescriptor<Long> desc = getSelectDescriptorForDependencyIds(queryOptions, parameters);
            return persistenceService.selectList(desc);
        } catch (final SBonitaReadException e) {
            throw new SDependencyException("Can't get dependencies", e);
        }
    }

    protected abstract SelectListDescriptor<Long> getSelectDescriptorForDependencyIds(QueryOptions queryOptions, Map<String, Object> parameters);

    protected abstract QueryOptions getDefaultQueryOptionForDependency();

    protected abstract QueryOptions getDefaultQueryOptionForDependencyMapping();

    @Override
    public void deleteDependency(final SDependency dependency) throws SDependencyException {
        for (SDependencyMapping dependencyMapping : getDependencyMappings(dependency.getId(), getDefaultQueryOptionForDependencyMapping())) {
            deleteDependencyMapping(dependencyMapping);
        }
        delete(dependency);
    }

    @Override
    public void deleteDependencies(final long id, final ScopeType type) throws SDependencyException {
        int fromIndex = 0;
        List<Long> dependencyIds = getDependencyIds(id, type, fromIndex, BATCH_SIZE);
        while (!dependencyIds.isEmpty()) {
            for (final Long dependencyId : dependencyIds) {
                final List<SDependencyMapping> dependencyMappings = getDependencyMappings(dependencyId, getDefaultQueryOptionForDependencyMapping());
                if (dependencyMappings.size() == 1) {// only when the dependency is linked only to on element
                    final SDependencyMapping dependencyMapping = dependencyMappings.get(0);
                    deleteDependencyMapping(dependencyMapping);
                    deleteDependency(getDependency(dependencyId));
                } else {
                    fromIndex++;
                }
            }
            dependencyIds = getDependencyIds(id, type, fromIndex, BATCH_SIZE);
        }
    }
}
