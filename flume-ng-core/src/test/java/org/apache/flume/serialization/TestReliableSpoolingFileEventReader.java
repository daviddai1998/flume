/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flume.serialization;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import junit.framework.Assert;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.client.avro.ReliableEventReader;
import org.apache.flume.client.avro.ReliableSpoolingFileEventReader;
import org.apache.flume.source.SpoolDirectorySourceConfigurationConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestReliableSpoolingFileEventReader {

  private static final Logger logger = LoggerFactory.getLogger
      (TestReliableSpoolingFileEventReader.class);

  private static final File WORK_DIR = new File("target/test/work/" +
      TestReliableSpoolingFileEventReader.class.getSimpleName());

  @Before
  public void setup() throws IOException, InterruptedException {
    if (!WORK_DIR.isDirectory()) {
      Files.createParentDirs(new File(WORK_DIR, "dummy"));
    }

    // write out a few files
    for (int i = 0; i < 4; i++) {
      File fileName = new File(WORK_DIR, "file"+i);
      StringBuilder sb = new StringBuilder();

      // write as many lines as the index of the file
      for (int j = 0; j < i; j++) {
        sb.append("file" + i + "line" + j + "\n");
      }
      Files.write(sb.toString(), fileName, Charsets.UTF_8);
    }
    Thread.sleep(1500L); // make sure timestamp is later
    Files.write("\n", new File(WORK_DIR, "emptylineFile"), Charsets.UTF_8);
  }

  @After
  public void tearDown() {

    // delete all the files & dirs we created
    File[] files = WORK_DIR.listFiles();
    for (File f : files) {
      if (f.isDirectory()) {
        File[] subDirFiles = f.listFiles();
        for (File sdf : subDirFiles) {
          if (!sdf.delete()) {
            logger.warn("Cannot delete file {}", sdf.getAbsolutePath());
          }
        }
        if (!f.delete()) {
          logger.warn("Cannot delete directory {}", f.getAbsolutePath());
        }
      } else {
        if (!f.delete()) {
          logger.warn("Cannot delete file {}", f.getAbsolutePath());
        }
      }
    }
    if (!WORK_DIR.delete()) {
      logger.warn("Cannot delete work directory {}", WORK_DIR.getAbsolutePath());
    }

  }

  // FIXME: implement ignore pattern test
  @Ignore
  @Test
  public void testIgnorePattern() {
    ReliableSpoolingFileEventReader parser;
  }

  @Test
  public void testRepeatedCallsWithCommitAlways() throws IOException {
    File trackerDir = new File(WORK_DIR,
        SpoolDirectorySourceConfigurationConstants.DEFAULT_META_DIR);
    ReliableEventReader reader = new ReliableSpoolingFileEventReader(WORK_DIR,
        SpoolDirectorySourceConfigurationConstants.DEFAULT_SPOOLED_FILE_SUFFIX,
        SpoolDirectorySourceConfigurationConstants.DFLT_IGNORE_PAT,
        trackerDir, false, "file",
        SpoolDirectorySourceConfigurationConstants.DEFAULT_DESERIALIZER,
        new Context());

    final int expectedLines = 0 + 1 + 2 + 3 + 1;
    int seenLines = 0;
    for (int i = 0; i < 10; i++) {
      List<Event> events = reader.readEvents(10);
      seenLines += events.size();
      reader.commit();
    }

    Assert.assertEquals(expectedLines, seenLines);
  }

  @Test
  public void testRepeatedCallsWithCommitOnSuccess() throws IOException {
    File trackerDir = new File(WORK_DIR,
        SpoolDirectorySourceConfigurationConstants.DEFAULT_META_DIR);
    ReliableEventReader reader = new ReliableSpoolingFileEventReader(WORK_DIR,
        SpoolDirectorySourceConfigurationConstants.DEFAULT_SPOOLED_FILE_SUFFIX,
        SpoolDirectorySourceConfigurationConstants.DFLT_IGNORE_PAT,
        trackerDir, false, "file",
        SpoolDirectorySourceConfigurationConstants.DEFAULT_DESERIALIZER,
        new Context());

    final int expectedLines = 0 + 1 + 2 + 3 + 1;
    int seenLines = 0;
    for (int i = 0; i < 10; i++) {
      List<Event> events = reader.readEvents(10);
      int numEvents = events.size();
      if (numEvents > 0) {
        seenLines += numEvents;
        reader.commit();

        // ensure that there are files in the trackerDir
        File[] files = trackerDir.listFiles();
        Assert.assertNotNull(files);
        Assert.assertTrue("Expected tracker files in tracker dir " + trackerDir
            .getAbsolutePath(), files.length > 0);
      }
    }

    Assert.assertEquals(expectedLines, seenLines);
  }

}
