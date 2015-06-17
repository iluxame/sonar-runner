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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;

public class LogsTest {
  private static final String EXPECTED_DEBUG = "DEBUG: debug\n";
  private static final String EXPECTED_INFO = "INFO: info\n";
  private static final String EXPECTED_ERROR = "ERROR: error\n";
  
  private ByteArrayOutputStream recordedSystemOut = new ByteArrayOutputStream();
  private ByteArrayOutputStream recordedSystemErr = new ByteArrayOutputStream();
  
  @Before
  public void restoreDefault() {
    recordedSystemOut = new ByteArrayOutputStream();
    recordedSystemErr = new ByteArrayOutputStream();
    
    System.setOut(new PrintStream(recordedSystemOut));
    System.setErr(new PrintStream(recordedSystemErr));
    
    Logs.setDebugEnabled(false);
    Logs.setOutStream(System.out);
    Logs.setErrStream(System.err);
  }
  
  @Test
  public void testNull() {
    Logs.setErrStream(null);
    Logs.setOutStream(null);
    
    writeTest();
    
    assertThat(recordedSystemOut.size()).isZero();
    assertThat(recordedSystemErr.size()).isZero();
  }
  
  @Test
  public void testDefault() throws UnsupportedEncodingException {
    writeTest();
    
    assertThat(recordedSystemOut.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_INFO);
    assertThat(recordedSystemErr.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_ERROR);
  }
  
  @Test
  public void testDebug() throws UnsupportedEncodingException {
    Logs.setDebugEnabled(true);
    writeTest();
    
    assertThat(recordedSystemOut.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_DEBUG + EXPECTED_INFO);
    assertThat(recordedSystemErr.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_ERROR);
  }
  
  @Test
  public void testChangeStdOut() throws UnsupportedEncodingException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Logs.setOutStream(new PrintStream(stream));
    
    writeTest();
    
    assertThat(recordedSystemOut.size()).isZero();
    assertThat(stream.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_INFO);
    assertThat(recordedSystemErr.toString(StandardCharsets.UTF_8.name())).isEqualTo(EXPECTED_ERROR);
  }
  
  private static void writeTest() {
    Logs.debug("debug");
    Logs.info("info");
    Logs.error("error");
  }
}
