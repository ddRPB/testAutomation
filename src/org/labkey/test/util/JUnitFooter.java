/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Runner;

import java.io.IOException;

 /**
 * User: tchad
 * Date: March 3, 2011
 * Time: 3:49:59 PM
 */

public class JUnitFooter extends BaseSeleniumWebTest
{
    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers created by the test.
    }

    @Override
    public void testSteps()
    {
        log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");

        log("** This test should follow JUnitTest.");
        log("** It will check for any errors or memory leaks caused by the tests therein");

        signIn();
        try{deleteFolder("Shared", "_junit");}catch(Throwable e){/*ignore*/}
        checkLeaksAndErrors();
        resetErrors();

        log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
    }

    public void doTestSteps() {}
}
