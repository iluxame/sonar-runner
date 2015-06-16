/*
 * SonarQube Runner - Implementation
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
package org.sonar.runner.impl;

import org.apache.commons.io.output.NullOutputStream;

import java.io.PrintStream;

public class Logs {
  private static PrintStream out = System.out;
  private static PrintStream err = System.err;
  private static boolean debugEnabled = false;

  private Logs() {
  }

  public static void setOutStream(PrintStream out) {
    if(out == null) {
      Logs.out = new PrintStream(new NullOutputStream());
    } else {
      Logs.out = out;
    }
  }

  public static void setErrStream(PrintStream err) {
    if(err == null) {
      Logs.err = new PrintStream(new NullOutputStream());
    } else {
      Logs.err = err;
    }
  }

  public static PrintStream getOutStream() {
    return Logs.out;
  }

  public static PrintStream getErrStream() {
    return Logs.err;
  }

  public static void setDebugEnabled(boolean debugEnabled) {
    Logs.debugEnabled = debugEnabled;
  }

  public static boolean isDebugEnabled() {
    return debugEnabled;
  }

  public static void debug(String message) {
    if (isDebugEnabled()) {
      out.println("DEBUG: " + message);
    }
  }

  public static void info(String message) {
    out.println("INFO: " + message);
  }

  public static void warn(String message) {
    out.println("WARN: " + message);
  }

  public static void error(String message) {
    err.println("ERROR: " + message);
  }

  public static void error(String message, Throwable t) {
    err.println("ERROR: " + message);
    if (t != null) {
      t.printStackTrace(err);
    }
  }
}
