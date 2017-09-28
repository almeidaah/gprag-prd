/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.tasks.execution;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.internal.changedetection.TaskArtifactState;
import org.gradle.api.internal.tasks.TaskExecuter;
import org.gradle.api.internal.tasks.TaskExecutionContext;
import org.gradle.api.internal.tasks.TaskExecutionOutcome;
import org.gradle.api.internal.tasks.TaskStateInternal;
import org.gradle.caching.BuildCacheEntryReader;
import org.gradle.caching.BuildCacheEntryWriter;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.internal.tasks.TaskOutputCachingBuildCacheKey;
import org.gradle.caching.internal.tasks.TaskOutputPacker;
import org.gradle.caching.internal.tasks.origin.TaskOutputOriginFactory;
import org.gradle.internal.time.Timer;
import org.gradle.internal.time.Timers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

public class SkipCachedTaskExecuter implements TaskExecuter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkipCachedTaskExecuter.class);

    private final BuildCacheService buildCache;
    private final TaskOutputPacker packer;
    private final TaskExecuter delegate;
    private final TaskOutputsGenerationListener taskOutputsGenerationListener;
    private final TaskOutputOriginFactory taskOutputOriginFactory;

    public SkipCachedTaskExecuter(TaskOutputOriginFactory taskOutputOriginFactory,
                                  BuildCacheService buildCache,
                                  TaskOutputPacker packer,
                                  TaskOutputsGenerationListener taskOutputsGenerationListener,
                                  TaskExecuter delegate) {
        this.taskOutputOriginFactory = taskOutputOriginFactory;
        this.buildCache = buildCache;
        this.packer = packer;
        this.taskOutputsGenerationListener = taskOutputsGenerationListener;
        this.delegate = delegate;
    }

    @Override
    public void execute(final TaskInternal task, TaskStateInternal state, TaskExecutionContext context) {
        final Timer clock = Timers.startTimer();

        LOGGER.debug("Determining if {} is cached already", task);

        final TaskOutputsInternal taskOutputs = task.getOutputs();
        TaskOutputCachingBuildCacheKey cacheKey = context.getBuildCacheKey();
        boolean taskOutputCachingEnabled = state.getTaskOutputCaching().isEnabled();
        if (taskOutputCachingEnabled) {
            if (cacheKey.isValid()) {
                TaskArtifactState taskState = context.getTaskArtifactState();
                if (taskState.isAllowedToUseCachedResults()) {
                    boolean found = buildCache.load(cacheKey, new BuildCacheEntryReader() {
                        @Override
                        public void readFrom(final InputStream input) {
                            taskOutputsGenerationListener.beforeTaskOutputsGenerated();
                            packer.unpack(taskOutputs, input, taskOutputOriginFactory.createReader(task));
                            LOGGER.info("Unpacked output for {} from cache (took {}).", task, clock.getElapsed());
                        }
                    });
                    if (found) {
                        state.setOutcome(TaskExecutionOutcome.FROM_CACHE);
                        return;
                    }
                } else {
                    LOGGER.info("Not loading {} from cache because pulling from cache is disabled for this task", task);
                }
            } else {
                LOGGER.info("Not caching {} because no valid cache key was generated", task);
            }
        }

        delegate.execute(task, state, context);

        if (taskOutputCachingEnabled) {
            if (cacheKey.isValid()) {
                if (state.getFailure() == null) {
                    buildCache.store(cacheKey, new BuildCacheEntryWriter() {
                        @Override
                        public void writeTo(OutputStream output) {
                            LOGGER.info("Packing {}", task.getPath());
                            packer.pack(taskOutputs, output, taskOutputOriginFactory.createWriter(task, clock.getElapsedMillis()));
                        }
                    });
                } else {
                    LOGGER.debug("Not pushing result from {} to cache because the task failed", task);
                }
            } else {
                LOGGER.info("Not pushing results from {} to cache because no valid cache key was generated", task);
            }
        }
    }
}
