/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.runner.api;

import javax.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.nio.file.attribute.*;

class Utils {
  private Utils() {
    // only util static methods
  }

  /**
   * Similar to org.apache.commons.lang.StringUtils#join()
   */
  static String join(String[] array, String delimiter) {
    StringBuilder sb = new StringBuilder();
    Iterator<String> it = Arrays.asList(array).iterator();
    while (it.hasNext()) {
      sb.append(it.next());
      if (!it.hasNext()) {
        break;
      }
      sb.append(delimiter);
    }
    return sb.toString();
  }

  static boolean taskRequiresProject(Properties props) {
    Object task = props.get(RunnerProperties.TASK);
    return task == null || ScanProperties.SCAN_TASK.equals(task);
  }

  public static void deleteQuietly(File f) {
    try {
      Files.walkFileTree(f.toPath(), new DeleteFileVisitor());
    } catch (IOException e) {
      // ignore
    }
  }

  private static class DeleteFileVisitor extends SimpleFileVisitor<Path> {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (exc == null) {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      } else {
        // directory iteration failed; propagate exception
        throw exc;
      }
    }
  }

  public static void closeQuietly(@Nullable Closeable c) {
    if (c == null) {
      return;
    }

    try {
      c.close();
    } catch (IOException e) {
      // ignore
    }
  }
}
