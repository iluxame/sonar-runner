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

import javax.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronously execute a native command line. It's much more limited than the Apache Commons Exec library.
 * For example it does not allow to run asynchronously or to automatically quote command-line arguments.
 *
 * @since 2.7
 */
class CommandExecutor {

  private static final CommandExecutor INSTANCE = new CommandExecutor();

  private CommandExecutor() {
  }

  static CommandExecutor create() {
    // stateless object, so a single singleton can be shared
    return INSTANCE;
  }

  int execute(Command command, StreamConsumer stdOut, StreamConsumer stdErr, long timeoutMilliseconds, @Nullable ProcessMonitor processMonitor) {
    ExecutorService executorService = null;
    Process process = null;
    StreamGobbler outputGobbler = null;
    StreamGobbler errorGobbler = null;
    try {
      ProcessBuilder builder = new ProcessBuilder(command.toStrings());
      builder.directory(command.directory());
      builder.environment().putAll(command.envVariables());
      process = builder.start();

      outputGobbler = new StreamGobbler(process.getInputStream(), stdOut);
      errorGobbler = new StreamGobbler(process.getErrorStream(), stdErr);
      outputGobbler.start();
      errorGobbler.start();

      executorService = Executors.newSingleThreadExecutor();
      final Future<Integer> futureTask = executeProcess(executorService, process);
      if (processMonitor != null) {
        monitorProcess(processMonitor, executorService, process);
      }

      int exitCode = futureTask.get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      verifyGobbler(command, outputGobbler, "stdOut");
      verifyGobbler(command, errorGobbler, "stdErr");
      return exitCode;

    } catch (TimeoutException te) {
      if (process != null) {
        process.destroy();
      }
      throw new CommandException("Timeout exceeded: " + timeoutMilliseconds + " ms", command, te);

    } catch (CommandException e) {
      throw e;

    } catch (Exception e) {
      throw new CommandException("Fail to execute command", command, e);

    } finally {
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      closeStreams(process);
      if (executorService != null) {
        executorService.shutdown();
      }
    }
  }

  private void monitorProcess(final ProcessMonitor processMonitor, final ExecutorService executor, final Process process) {
    new Thread() {
      @Override
      public void run() {
        while (!executor.isTerminated()) {
          if (processMonitor.stop()) {
            process.destroy();
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }
    }.start();
  }

  private Future<Integer> executeProcess(ExecutorService executorService, Process process) {
    final Process finalProcess = process;
    return executorService.submit(new Callable<Integer>() {
      @Override
      public Integer call() throws InterruptedException {
        return finalProcess.waitFor();
      }
    });
  }

  private void verifyGobbler(Command command, StreamGobbler gobbler, String type) {
    if (gobbler.getException() != null) {
      throw new CommandException("Error inside " + type + " stream", command, gobbler.getException());
    }
  }

  private void closeStreams(Process process) {
    if (process != null) {
      Utils.closeQuietly(process.getInputStream());
      Utils.closeQuietly(process.getInputStream());
      Utils.closeQuietly(process.getOutputStream());
      Utils.closeQuietly(process.getErrorStream());
    }
  }

  private void waitUntilFinish(StreamGobbler thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Logs.error("InterruptedException while waiting finish of " + thread.toString(), e);
      }
    }
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private final StreamConsumer consumer;
    private volatile Exception exception;

    StreamGobbler(InputStream is, StreamConsumer consumer) {
      super("ProcessStreamGobbler");
      this.is = is;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      try (InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr)) {
        String line;
        while ((line = br.readLine()) != null) {
          consumeLine(line);
        }
      } catch (IOException ioe) {
        exception = ioe;

      }
    }

    private void consumeLine(String line) {
      if (exception == null) {
        try {
          consumer.consumeLine(line);
        } catch (Exception e) {
          exception = e;
        }
      }
    }

    public Exception getException() {
      return exception;
    }
  }
}
