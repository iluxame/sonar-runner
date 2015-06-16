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

import org.sonar.runner.impl.Logs;

import org.sonar.runner.impl.InternalProperties;

import javax.annotation.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

/**
 * @since 2.2
 */
public abstract class Runner<T extends Runner> {
  private final Properties globalProperties = new Properties();

  protected Runner() {
  }

  public Properties globalProperties() {
    Properties clone = new Properties();
    clone.putAll(globalProperties);
    return clone;
  }
  
  /**
   * Set stdout log stream. By default it is {@link System.out}.
   * If null, stdout logging will be suppressed.
   */
  public T setOutLogStream(PrintStream stdOut) {
    Logs.setOutStream(stdOut);
    return (T) this;
  }
  
  /**
   * Set stderr log stream. By default it is {@link System.err}.
   * If null, stderr logging will be suppressed.
   */
  public T setErrLogStream(PrintStream stdErr) {
    Logs.setErrStream(stdErr);
    return (T) this;
  }

  /**
   * Declare Sonar properties, for example sonar.projectKey=>foo.
   *
   * @see #setProperty(String, String)
   */
  public T addGlobalProperties(Properties p) {
    globalProperties.putAll(p);
    return (T) this;
  }

  /**
   * Declare a Sonar property.
   *
   * @see RunnerProperties
   * @see ScanProperties
   */
  public T setGlobalProperty(String key, String value) {
    globalProperties.setProperty(key, value);
    return (T) this;
  }

  public String globalProperty(String key, @Nullable String defaultValue) {
    return globalProperties.getProperty(key, defaultValue);
  }

  /**
   * User-agent used in the HTTP requests to the Sonar server
   */
  public T setApp(String app, String version) {
    setGlobalProperty(InternalProperties.RUNNER_APP, app);
    setGlobalProperty(InternalProperties.RUNNER_APP_VERSION, version);
    return (T) this;
  }

  public String app() {
    return globalProperty(InternalProperties.RUNNER_APP, null);
  }

  public String appVersion() {
    return globalProperty(InternalProperties.RUNNER_APP_VERSION, null);
  }

  public void runAnalysis(Properties analysisProperties) {
    Properties copy = new Properties();
    copy.putAll(analysisProperties);
    initAnalysisProperties(copy);

    String dumpToFile = copy.getProperty(InternalProperties.RUNNER_DUMP_TO_FILE);
    if (dumpToFile != null) {
      File dumpFile = new File(dumpToFile);
      Utils.writeProperties(dumpFile, copy);
      System.out.println("Simulation mode. Configuration written to " + dumpFile.getAbsolutePath());
    } else {
      doExecute(copy);
    }
  }

  public void start() {
    initGlobalDefaultValues();
    doStart();
  }

  public void stop() {
    doStop();
  }

  /**
   * @deprecated since 2.5 use {@link #start()}, {@link #runAnalysis(Properties)} and then {@link #stop()}
   */
  @Deprecated
  public final void execute() {
    start();
    runAnalysis(new Properties());
    stop();
  }

  protected abstract void doStart();

  protected abstract void doStop();

  protected abstract void doExecute(Properties analysisProperties);

  private void initGlobalDefaultValues() {
    setGlobalDefaultValue(RunnerProperties.HOST_URL, "http://localhost:9000");
    setGlobalDefaultValue(InternalProperties.RUNNER_APP, "SonarQubeRunner");
    setGlobalDefaultValue(InternalProperties.RUNNER_APP_VERSION, RunnerVersion.version());
  }

  private static void initAnalysisProperties(Properties p) {
    SourceEncoding.init(p);
    Dirs.init(p);
  }

  private void setGlobalDefaultValue(String key, String value) {
    if (!globalProperties.containsKey(key)) {
      setGlobalProperty(key, value);
    }
  }

}
