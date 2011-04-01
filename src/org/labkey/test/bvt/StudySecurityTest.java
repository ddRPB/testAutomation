/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.drt.StudyBaseTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

/*
* User: dave
* Date: Oct 12, 2009
* Time: 2:39:18 PM
*/
public class StudySecurityTest extends StudyBaseTest
{
    protected static final String READER = "dsreader@studysecurity.test";
    protected static final String EDITOR = "dseditor@studysecurity.test";
    protected static final String LIMITED = "dslimited@studysecurity.test";
    protected static final String NONE = "dsnone@studysecurity.test";

    protected static final String GROUP_READERS = "Readers";
    protected static final String GROUP_EDITORS = "Editors";
    protected static final String GROUP_LIMITED = "The Limited";
    protected static final String GROUP_NONE = "No Access";

    protected enum GroupSetting
    {
        editAll("UPDATE"),
        readAll("READ"),
        perDataset("READOWN"),
        none("NONE");

        private String _radioValue;

        GroupSetting(String radioValue)
        {
            _radioValue = radioValue;
        }

        public String getRadioValue()
        {
            return _radioValue;
        }
    }

    protected enum PerDatasetPerm
    {
        Read,
        Edit
    }

    protected void doCreateSteps()
    {
        //start import--need to wait for completion after setting up security
        importStudy();

        createUser(READER, null);
        createUser(EDITOR, null);
        createUser(LIMITED, null);
        clickLinkWithText(getProjectName());

        //create groups for dataset reader, writer, and no-access
        createPermissionsGroup(GROUP_READERS, READER);
        createPermissionsGroup(GROUP_EDITORS, EDITOR);
        createPermissionsGroup(GROUP_LIMITED, LIMITED);
        createPermissionsGroup(GROUP_NONE, NONE);

        setPermissions(GROUP_READERS, "Reader");
        setPermissions(GROUP_EDITORS, "Editor");
        setPermissions(GROUP_LIMITED, "Reader");
        setPermissions(GROUP_NONE, "Reader");
        exitPermissionsUI();

        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Files");
        waitForPipelineJobsToComplete(1, "study import", false);
    }

    protected void doVerifySteps()
    {
        setupStudySecurity();

        //now verify that each group sees what it's supposed to see
        String[] all = new String[]{"CPF-1: Follow-up Chemistry Panel", "DEM-1: Demographics"};
        String[] none = new String[0];
        String[] limited = new String[]{"DEM-1: Demographics", "Types"};
        String[] unlimited = new String[]{"CPF-1: Follow-up Chemistry Panel"};

        //system admins (current user) should be able to edit everything and setup the pipeline
        verifyPerms(null, all, none, all, none, true);

        //GROUP_READERS should be able to see all datasets, but not edit anything
        //and not do anything with the pipeline
        verifyPerms(READER, all, none, none, all, false);

        //GROUP_EDITORS should be able to see all datasets and edit them
        //and import new data via the pipeline, but not set the pipeline path
        verifyPerms(EDITOR, all, none, all, none, false);

        //GROUP_LIMITED should be able to see only the few datasets we granted them
        //and not do anything with the pipeline
        verifyPerms(LIMITED, limited, unlimited, none, limited, false);

        //GROUP_NONE should not be able to see any datasets nor the pipeline
        verifyPerms(NONE, none, all, none, all, false);

        //revoke limited's permissions and verify
        adjustGroupDatasetPerms(GROUP_LIMITED, GroupSetting.none);
        clickLinkWithText(getFolderName());
        verifyPerms(LIMITED, none, all, none, all, false);

        //reinstate read to limited to verify that per-dataset settings were preserved
        adjustGroupDatasetPerms(GROUP_LIMITED, GroupSetting.perDataset);
        verifyPerms(LIMITED, limited, unlimited, none, limited, false);

        //move editors to per-dataset and grant edit only one of the datasets
        String[] edit = new String[]{"Types"};
        String[] noEdit = new String[]{"DEM-1: Demographics"};
        adjustGroupDatasetPerms(GROUP_EDITORS, GroupSetting.perDataset, edit, PerDatasetPerm.Edit);
        verifyPerms(EDITOR, edit, noEdit, edit, noEdit, false);

        //reset to general edit
        adjustGroupDatasetPerms(GROUP_EDITORS, GroupSetting.editAll);
        verifyPerms(EDITOR, all, none, all, none, false);
    }

    protected void adjustGroupDatasetPerms(String groupName, GroupSetting setting)
    {
        adjustGroupDatasetPerms(groupName, setting, null, null);
    }

    protected void adjustGroupDatasetPerms(String groupName, GroupSetting setting, String[] datasets, PerDatasetPerm perm)
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickNavButton("Study Security");

        click(getRadioButtonLocator(groupName, setting));
        clickNavButtonByIndex("Update", 1);

        if (null != datasets && null != perm)
        {
            for (String dsName : datasets)
            {
                selectOptionByText(getPerDatasetSelectId(dsName), perm.name());
            }
            clickNavButton("Save");
        }

        clickLinkWithText(getFolderName());
    }

    protected String getPerDatasetSelectId(String dsName)
    {
        return selenium.getAttribute("//form[@id='datasetSecurityForm']/table/tbody/tr/td[text()='" + dsName + "']/../td/select/@name");
    }

    protected void verifyPerms(String userName, String[] dsCanRead, String[] dsCannotRead, String[] dsCanEdit, String[] dsCannotEdit, boolean canSetupPipeline)
    {
        if (null != userName)
            impersonate(userName);

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());

        if (canSetupPipeline)
            assertNavButtonPresent("Setup");
        else
            assertNavButtonNotPresent("Setup");

        for (String dsName : dsCanRead)
        {
            assertLinkPresentWithText(dsName);
        }

        for (String dsName : dsCannotRead)
        {
            assertLinkNotPresentWithText(dsName);
        }

        for (String dsName : dsCanEdit)
        {
            assertLinkPresentWithText(dsName);
            clickLinkWithText(dsName);
            assertTextPresent(dsName);
            assertLinkPresentWithText("edit");
            assertNavButtonPresent("Insert New");
            assertNavButtonPresent("Import Data");
            clickLinkWithText(getFolderName());
        }

        for (String dsName : dsCannotEdit)
        {
            if (isLinkPresentWithText(dsName))
            {
                clickLinkWithText(dsName);
                assertTextPresent(dsName);
                assertLinkNotPresentWithText("edit");
                assertNavButtonNotPresent("Insert New");
                assertNavButtonNotPresent("Import Data");
                clickLinkWithText(getFolderName());
            }
        }

        if (null != userName)
            stopImpersonating();
    }

    protected void setupStudySecurity()
    {
        //setup advanced dataset security
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickNavButton("Study Security");

        setFormElement("securityString", "ADVANCED_WRITE");
        waitForPageToLoad();

        //the radio buttons are named "group.<id>" and since we don't know the
        //group ids, we need to find them by name
        click(getRadioButtonLocator(GROUP_READERS, GroupSetting.readAll));
        click(getRadioButtonLocator(GROUP_EDITORS, GroupSetting.editAll));
        click(getRadioButtonLocator(GROUP_LIMITED, GroupSetting.perDataset));
        click(getRadioButtonLocator(GROUP_NONE, GroupSetting.none));
        clickNavButtonByIndex("Update", 1);

        //grant limited rights to read a couple of datasets
        selectOptionByText("dataset.1", "Read");
        selectOptionByText("dataset.2", "Read");
        clickNavButton("Save");

        clickLinkWithText(getFolderName());
    }

    protected Locator getRadioButtonLocator(String groupName, GroupSetting setting)
    {
        //not sure why the radios are in TH elements, but they are...
        return Locator.xpath("//form[@id='groupUpdateForm']/table/tbody/tr/td[text()='"
                + groupName + "']/../th/input[@value='" + setting.getRadioValue() + "']");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        super.doCleanup();
        deleteUser(READER);
        deleteUser(EDITOR);
        deleteUser(LIMITED);
        deleteUser(NONE);
    }
}
