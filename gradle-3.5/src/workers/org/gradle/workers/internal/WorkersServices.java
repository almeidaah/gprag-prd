/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.workers.internal;

import org.gradle.StartParameter;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.operations.BuildOperationWorkerRegistry;
import org.gradle.internal.progress.BuildOperationExecutor;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.PluginServiceRegistry;
import org.gradle.internal.work.AsyncWorkTracker;
import org.gradle.process.internal.health.memory.MemoryManager;
import org.gradle.process.internal.worker.WorkerProcessFactory;
import org.gradle.workers.WorkerExecutor;

public class WorkersServices implements PluginServiceRegistry {
    @Override
    public void registerGlobalServices(ServiceRegistration registration) {
    }

    @Override
    public void registerBuildSessionServices(ServiceRegistration registration) {
        registration.addProvider(new BuildSessionScopeServices());
    }

    @Override
    public void registerBuildServices(ServiceRegistration registration) {
    }

    @Override
    public void registerGradleServices(ServiceRegistration registration) {
    }

    @Override
    public void registerProjectServices(ServiceRegistration registration) {
    }

    private static class BuildSessionScopeServices {
        WorkerDaemonClientsManager createWorkerDaemonClientsManager(WorkerProcessFactory workerFactory,
                                                                    StartParameter startParameter,
                                                                    BuildOperationExecutor buildOperationExecutor) {
            return new WorkerDaemonClientsManager(new WorkerDaemonStarter(workerFactory, startParameter, buildOperationExecutor));
        }

        WorkerDaemonManager createWorkerDaemonManager(WorkerDaemonClientsManager workerDaemonClientsManager, MemoryManager memoryManager, BuildOperationWorkerRegistry buildOperationWorkerRegistry, BuildOperationExecutor buildOperationExecutor) {
            return new WorkerDaemonManager(workerDaemonClientsManager, memoryManager, buildOperationWorkerRegistry, buildOperationExecutor);
        }

        WorkerExecutor createWorkerDaemonExecutor(WorkerDaemonFactory workerDaemonFactory, FileResolver fileResolver, ExecutorFactory executorFactory, BuildOperationWorkerRegistry buildOperationWorkerRegistry, BuildOperationExecutor buildOperationExecutor, AsyncWorkTracker asyncWorkTracker) {
            return new DefaultWorkerExecutor(workerDaemonFactory, fileResolver, WorkerDaemonServer.class, executorFactory, buildOperationWorkerRegistry, buildOperationExecutor, asyncWorkTracker);
        }
    }
}
