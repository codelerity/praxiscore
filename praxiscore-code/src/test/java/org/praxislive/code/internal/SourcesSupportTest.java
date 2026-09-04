/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.code.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.praxislive.core.types.PMap;

import static org.junit.jupiter.api.Assertions.*;

public class SourcesSupportTest {

    private static final boolean VERBOSE = Boolean.getBoolean("praxis.test.verbose");

    private static final String JAVA_SRC_ONE
            = """
            package SHARED;
            
            public class One {}
            """;

    private static final String JAVA_SRC_TWO
            = """
            package SHARED;
            
            public class Two {}
            """;

    private static final String JAVA_SRC_TWO_EDITED
            = """
            package SHARED;
            
            public class Two {
                // made some changes
            }
            """;

    private static final PMap SHARED_SOURCE_MAP = PMap.of(
            "SHARED.One", JAVA_SRC_ONE,
            "SHARED.Two", JAVA_SRC_TWO);

    @BeforeEach
    public void beforeEach(TestInfo info) {
        if (VERBOSE) {
            System.out.println("START TEST : " + info.getDisplayName());
        }
    }

    @AfterEach
    public void afterEach(TestInfo info) {
        if (VERBOSE) {
            System.out.println("END TEST : " + info.getDisplayName());
            System.out.println("=====================================");
        }
    }

    @Test
    public void testReadSources(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tmp)
            throws IOException {
        Path pkg = tmp.resolve("code").resolve("root").resolve("SHARED");
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("Two.java"), JAVA_SRC_TWO);
        Files.writeString(pkg.resolve("One.java"), JAVA_SRC_ONE);
        PMap sources = SourcesSupport.readSources(pkg.getParent().toUri());
        logResult("Loaded source map", sources);
        assertEquals(SHARED_SOURCE_MAP, sources);
    }

    @Test
    public void testWriteSources(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tmp)
            throws IOException {
        Path pkg = tmp.resolve("code").resolve("root").resolve("SHARED");
        List<String> warnings = SourcesSupport.writeSources(SHARED_SOURCE_MAP,
                pkg.getParent().toUri());
        assertTrue(warnings.isEmpty());
        Path file1 = pkg.resolve("One.java");
        Path file2 = pkg.resolve("Two.java");
        String contents1 = Files.readString(file1);
        String contents2 = Files.readString(file2);
        logResult("File One.java content", contents1);
        logResult("File Two.java content", contents2);
        assertEquals(JAVA_SRC_ONE, contents1);
        assertEquals(JAVA_SRC_TWO, contents2);
        Files.delete(file1);
        warnings = SourcesSupport.writeSources(SHARED_SOURCE_MAP,
                pkg.getParent().toUri());
        assertTrue(warnings.isEmpty());
        contents1 = Files.readString(file1);
        assertEquals(JAVA_SRC_ONE, contents1);
        Files.delete(file1);
        warnings = SourcesSupport.writeSources(PMap.of(
                "SHARED.One", JAVA_SRC_ONE,
                "SHARED.Two", JAVA_SRC_TWO_EDITED),
                pkg.getParent().toUri());
        logResult("Expected warnings", warnings);
        assertEquals(1, warnings.size());
        contents2 = Files.readString(file2);
        assertEquals(JAVA_SRC_TWO, contents2);
    }

    static void logResult(String description, Object value) {
        if (VERBOSE) {
            System.out.println(description);
            System.out.println(value);
            System.out.println();
        }
    }

}
