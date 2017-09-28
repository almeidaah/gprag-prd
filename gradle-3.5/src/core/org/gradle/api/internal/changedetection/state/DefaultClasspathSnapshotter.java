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

package org.gradle.api.internal.changedetection.state;

import com.google.common.hash.HashCode;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory;
import org.gradle.api.internal.hash.FileHasher;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.nativeintegration.filesystem.FileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DefaultClasspathSnapshotter extends AbstractFileCollectionSnapshotter implements ClasspathSnapshotter {
    private static final Comparator<FileDetails> FILE_DETAILS_COMPARATOR = new Comparator<FileDetails>() {
        @Override
        public int compare(FileDetails o1, FileDetails o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    };

    private final ClasspathEntryHasher classpathEntryHasher;

    public DefaultClasspathSnapshotter(FileHasher hasher, StringInterner stringInterner, FileSystem fileSystem, DirectoryFileTreeFactory directoryFileTreeFactory, FileSystemMirror fileSystemMirror, ClasspathEntryHasher classpathEntryHasher) {
        super(hasher, stringInterner, fileSystem, directoryFileTreeFactory, fileSystemMirror);
        this.classpathEntryHasher = classpathEntryHasher;
    }

    @Override
    public Class<? extends FileCollectionSnapshotter> getRegisteredType() {
        return ClasspathSnapshotter.class;
    }

    @Override
    protected List<FileDetails> normaliseTreeElements(List<FileDetails> fileDetails) {
        // TODO: We could rework this to produce a FileDetails for the directory that
        // has a hash for the contents of this directory vs returning a list of the contents
        // of the directory with their hashes
        // Collect the signatures of each class file
        List<FileDetails> sorted = new ArrayList<FileDetails>(fileDetails.size());
        for (FileDetails details : fileDetails) {
            if (details.getType() == FileType.RegularFile) {
                HashCode signatureForClass = classpathEntryHasher.hash(details);
                if (signatureForClass == null) {
                    // Should be excluded
                    continue;
                }
                sorted.add(details.withContentHash(signatureForClass));
            }
        }

        // Sort as their order is not important
        Collections.sort(sorted, FILE_DETAILS_COMPARATOR);
        return sorted;
    }

    @Override
    protected FileDetails normaliseFileElement(FileDetails details) {
        HashCode signature = classpathEntryHasher.hash(details);
        if (signature!=null) {
            return details.withContentHash(signature);
        }
        return details;
    }
}
