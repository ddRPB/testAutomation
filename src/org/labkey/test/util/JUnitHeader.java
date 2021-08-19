/*
 * Copyright (c) 2011-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.tests.JUnitTest;

import static org.labkey.test.WebTestHelper.logToServer;

/**
 * This test is inserted before server-side tests are run. It cleans up the default test container ('Shared/_junit') and
 * sets up some configurations that server-side tests might need.
 */
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class JUnitHeader extends JUnitTest.BaseJUnitTestWrapper
{
    @BeforeClass
    public static void beforeClass()
    {
        TestLogger.log("Preparing server to execute server-side tests");
    }

    @Test
    public void configureR()
    {
        Assume.assumeTrue(extraSetup);
        RReportHelper reportHelper = new RReportHelper(this);
        reportHelper.ensureRConfig(); // reportTest.js (via RhinoService) executes an R script
    }

    @Test
    public void configurePipeline()
    {
        Assume.assumeTrue(extraSetup);
        PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
        pipelineToolsHelper.setToolsDirToTestDefault(); // Point to extra tools if present (currently only sequeneanalysis tools)
    }

    @Test
    public void cleanTestContainer()
    {
        if (_containerHelper.doesContainerExist("Shared/_junit"))
            _containerHelper.deleteFolder("Shared", "_junit");
    }

    @Override
    @Test
    public void startSystemMaintenance()
    {
        Assume.assumeTrue(extraSetup);
        super.startSystemMaintenance();
    }

    @AfterClass
    public static void logStart()
    {
        startTime = System.currentTimeMillis();
        logToServer("=== Starting Server-side JUnit Tests ===");
    }
}
