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

package org.gradle.internal.work;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gradle.internal.exceptions.DefaultMultiCauseException;
import org.gradle.internal.progress.BuildOperationExecutor.Operation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultAsyncWorkTracker implements AsyncWorkTracker {
    private final ListMultimap<Operation, AsyncWorkCompletion> items = ArrayListMultimap.create();
    private final Set<Operation> waiting = Sets.newHashSet();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void registerWork(Operation operation, AsyncWorkCompletion workCompletion) {
        lock.lock();
        try {
            if (waiting.contains(operation)) {
                throw new IllegalStateException("Another thread is currently waiting on the completion of work for the provided operation");
            }
            items.put(operation, workCompletion);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void waitForCompletion(Operation operation) {
        List<Throwable> failures = Lists.newArrayList();
        List<AsyncWorkCompletion> workItems;
        lock.lock();
        try {
            workItems = ImmutableList.copyOf(items.get(operation));
            items.removeAll(operation);
            startWaiting(operation);
        } finally {
            lock.unlock();
        }

        try {
            for (AsyncWorkCompletion item : workItems) {
                try {
                    item.waitForCompletion();
                } catch (Throwable t) {
                    failures.add(t);
                }
            }

            if (failures.size() > 0) {
                throw new DefaultMultiCauseException("There were failures while executing asynchronous work:", failures);
            }
        } finally {
            stopWaiting(operation);
        }
    }

    private void startWaiting(Operation operation) {
        lock.lock();
        try {
            waiting.add(operation);
        } finally {
            lock.unlock();
        }
    }

    private void stopWaiting(Operation operation) {
        lock.lock();
        try {
            waiting.remove(operation);
        } finally {
            lock.unlock();
        }
    }
}
