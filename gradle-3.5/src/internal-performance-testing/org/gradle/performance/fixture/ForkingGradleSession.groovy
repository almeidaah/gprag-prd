/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.performance.fixture

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.integtests.fixtures.executer.IntegrationTestBuildContext
import org.gradle.internal.os.OperatingSystem
import org.gradle.performance.measure.MeasuredOperation

import java.lang.ProcessBuilder.Redirect

/**
 * A performance test session that runs Gradle from the command line.
 *
 * This deliberate does not use our integration test fixtures, because they are not optimized
 * for low overhead.
 */
@CompileStatic
class ForkingGradleSession implements GradleSession {

    final GradleInvocationSpec invocation
    private final IntegrationTestBuildContext integrationTestBuildContext

    private ProcessBuilder stop

    ForkingGradleSession(GradleInvocationSpec invocation, IntegrationTestBuildContext integrationTestBuildContext) {
        this.invocation = invocation
        this.integrationTestBuildContext = integrationTestBuildContext
    }

    @Override
    void prepare() {
        cleanup()
    }

    Action<MeasuredOperation> runner(BuildExperimentInvocationInfo invocationInfo, InvocationCustomizer invocationCustomizer) {
        return { MeasuredOperation measuredOperation ->
            DurationMeasurementImpl.measure(measuredOperation) {
                run(invocationInfo, invocationCustomizer)
            }
        } as Action<MeasuredOperation>
    }

    @Override
    void cleanup() {
        if (stop != null) {
            stop.start().waitFor()
            stop = null
        }
    }

    private void run(BuildExperimentInvocationInfo invocationInfo, InvocationCustomizer invocationCustomizer) {
        def invocation = invocationCustomizer ? invocationCustomizer.customize(invocationInfo, this.invocation) : this.invocation

        List<String> args = []
        if (OperatingSystem.current().isWindows()) {
            args << "cmd.exe" << "/C"
        }
        args << new File(invocation.gradleDistribution.gradleHomeDir, "bin/gradle").absolutePath
        args << "--gradle-user-home" << new File(invocationInfo.projectDir, "gradle-user-home").absolutePath
        args << "--no-search-upward"
        args << "--stacktrace"
        args << "-Dorg.gradle.jvmargs=${invocation.jvmOpts.collect { '\'' + it + '\'' }.join(' ')}".toString()
        args += invocation.args
        if (invocation.useDaemon) {
            args << "--daemon"
        } else {
            args << "--no-daemon"
        }

        ProcessBuilder run = newProcessBuilder(invocationInfo, args + invocation.tasksToRun)
        stop = newProcessBuilder(invocationInfo, args + "--stop")

        def exitCode = run.start().waitFor()
        if (exitCode != 0 && !invocation.expectFailure) {
            throw new IllegalStateException("Build failed, see ${getBuildLog(invocationInfo)} for details")
        }
    }

    private ProcessBuilder newProcessBuilder(BuildExperimentInvocationInfo invocationInfo, List<String> args) {
        new ProcessBuilder()
            .directory(invocationInfo.projectDir)
            .redirectOutput(Redirect.appendTo(getBuildLog(invocationInfo)))
            .redirectError(Redirect.appendTo(getBuildLog(invocationInfo)))
            .command(args)
    }

    private File getBuildLog(BuildExperimentInvocationInfo invocationInfo) {
        new File(invocationInfo.projectDir, "log.txt")
    }
}
