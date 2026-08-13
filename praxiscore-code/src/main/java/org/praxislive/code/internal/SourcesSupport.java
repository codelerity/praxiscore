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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.praxislive.core.Value;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;

/**
 * Support methods for sources commands.
 */
final class SourcesSupport {

    private SourcesSupport() {
        throw new Error();
    }

    static PMap readSources(URI base) throws IOException {
        Path baseDir = Path.of(base);
        try (Stream<Path> files = Files.walk(baseDir)) {
            Map<String, PString> sourceMap = new TreeMap<>();
            List<Path> sourceFiles = files
                    .filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                    .toList();
            for (Path source : sourceFiles) {
                String binaryName = base.relativize(source.toUri()).toString();
                binaryName = binaryName.substring(0, binaryName.lastIndexOf("."));
                binaryName = binaryName.replace("/", ".");
                sourceMap.put(binaryName, PString.of(Files.readString(source)));
            }
            return PMap.ofMap(sourceMap);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
    
    static List<String> writeSources(PMap sources, URI base) throws IOException {
        List<String> warnings = new ArrayList<>();
        Path basePath = Path.of(base);
        for (Map.Entry<String, Value> entry : sources.asMap().entrySet()) {
            String filePath = entry.getKey().replace(".", basePath.getFileSystem().getSeparator()) + ".java";
            Path file = basePath.resolve(filePath);
            if (Files.exists(file)) {
                String contents = Files.readString(file);
                if (!Objects.equals(contents, entry.getValue().toString())) {
                    warnings.add(MessageFormat.format(
                            CodeCommands.MESSAGES.getString("existing-file-mismatch"), filePath));
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue().toString());
            }
        }
        return List.copyOf(warnings);
    }

}
