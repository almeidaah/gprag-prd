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

package org.gradle.jvm.internal;

import org.gradle.api.Transformer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.internal.artifacts.ArtifactDependencyResolver;
import org.gradle.api.internal.artifacts.GlobalDependencyResolutionRules;
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory;
import org.gradle.api.internal.artifacts.ResolveContext;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactVisitor;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.DefaultResolvedArtifactsBuilder;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.DependencyArtifactsVisitor;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvedArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvedVariant;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.SelectedArtifactResults;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.ModuleExclusions;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphEdge;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphNode;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphSelector;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphVisitor;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.api.internal.tasks.TaskDependencies;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.resolve.ModuleVersionResolveException;
import org.gradle.language.base.internal.resolve.LibraryResolveException;
import org.gradle.platform.base.internal.BinarySpecInternal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DependencyResolvingClasspath extends AbstractFileCollection {
    private final GlobalDependencyResolutionRules globalRules = GlobalDependencyResolutionRules.NO_OP;
    private final List<ResolutionAwareRepository> remoteRepositories;
    private final BinarySpecInternal binary;
    private final ArtifactDependencyResolver dependencyResolver;
    private final ResolveContext resolveContext;
    private final AttributesSchema attributesSchema;
    private final ImmutableModuleIdentifierFactory moduleIdentifierFactory;
    private final ModuleExclusions moduleExclusions;

    private final String descriptor;
    private ResolveResult resolveResult;

    public DependencyResolvingClasspath(
        BinarySpecInternal binarySpec,
        String descriptor,
        ArtifactDependencyResolver dependencyResolver,
        List<ResolutionAwareRepository> remoteRepositories,
        ResolveContext resolveContext,
        AttributesSchema attributesSchema, ImmutableModuleIdentifierFactory moduleIdentifierFactory, ModuleExclusions moduleExclusions) {
        this.binary = binarySpec;
        this.descriptor = descriptor;
        this.dependencyResolver = dependencyResolver;
        this.remoteRepositories = remoteRepositories;
        this.resolveContext = resolveContext;
        this.attributesSchema = attributesSchema;
        this.moduleIdentifierFactory = moduleIdentifierFactory;
        this.moduleExclusions = moduleExclusions;
    }

    @Override
    public String getDisplayName() {
        return "Classpath for " + descriptor;
    }

    @Override
    public Set<File> getFiles() {
        ensureResolved(true);
        final Set<File> result = new LinkedHashSet<File>();
        resolveResult.artifactsResults.getArtifacts().visit(new ArtifactVisitor() {
            @Override
            public void visitArtifact(AttributeContainer variant, ResolvedArtifact artifact) {
                result.add(artifact.getFile());
            }

            @Override
            public void visitFailure(Throwable failure) {
                throw UncheckedException.throwAsUncheckedException(failure);
            }

            @Override
            public boolean includeFiles() {
                return true;
            }

            @Override
            public void visitFile(ComponentArtifactIdentifier artifactIdentifier, AttributeContainer variant, File file) {
                result.add(file);
            }
        });
        return result;
    }

    @Override
    public TaskDependency getBuildDependencies() {
        ensureResolved(false);

        List<TaskDependency> taskDependencies = new ArrayList<TaskDependency>();
        resolveResult.artifactsResults.getArtifacts().collectBuildDependencies(taskDependencies);
        return TaskDependencies.of(taskDependencies);
    }

    private void ensureResolved(boolean failFast) {
        if (resolveResult == null) {
            resolveResult = resolve();
        }
        if (failFast) {
            failOnUnresolvedDependency(resolveResult.notFound);
        }
    }

    private ResolveResult resolve() {
        ResolveResult result = new ResolveResult();
        dependencyResolver.resolve(resolveContext, remoteRepositories, globalRules, Specs.<DependencyMetadata>satisfyAll(), result, result, attributesSchema, moduleIdentifierFactory, moduleExclusions);
        return result;
    }

    private void failOnUnresolvedDependency(List<Throwable> notFound) {
        if (!notFound.isEmpty()) {
            throw new LibraryResolveException(String.format("Could not resolve all dependencies for '%s' %s", binary.getDisplayName(), descriptor), notFound);
        }
    }

    class ResolveResult implements DependencyGraphVisitor, DependencyArtifactsVisitor {
        public final List<Throwable> notFound = new LinkedList<Throwable>();
        public DefaultResolvedArtifactsBuilder artifactsBuilder = new DefaultResolvedArtifactsBuilder(true);
        public SelectedArtifactResults artifactsResults;

        @Override
        public void start(DependencyGraphNode root) {
        }

        @Override
        public void visitNode(DependencyGraphNode resolvedConfiguration) {
            for (DependencyGraphEdge dependency : resolvedConfiguration.getOutgoingEdges()) {
                ModuleVersionResolveException failure = dependency.getFailure();
                if (failure != null) {
                    notFound.add(failure);
                }
            }
        }

        @Override
        public void visitSelector(DependencyGraphSelector selector) {
        }

        @Override
        public void visitEdges(DependencyGraphNode resolvedConfiguration) {
        }

        @Override
        public void finish(DependencyGraphNode root) {
        }

        @Override
        public void startArtifacts(DependencyGraphNode root) {
        }

        @Override
        public void visitArtifacts(DependencyGraphNode from, DependencyGraphNode to, ArtifactSet artifacts) {
            artifactsBuilder.visitArtifacts(from, to, artifacts);
        }

        @Override
        public void finishArtifacts() {
            artifactsResults = artifactsBuilder.complete().select(Specs.<ComponentIdentifier>satisfyAll(), new Transformer<ResolvedArtifactSet, Collection<? extends ResolvedVariant>>() {
                @Override
                public ResolvedArtifactSet transform(Collection<? extends ResolvedVariant> variants) {
                    // Select the first variant
                    return variants.iterator().next().getArtifacts();
                }
            });
        }
    }
}
