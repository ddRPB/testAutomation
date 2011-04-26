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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;

public class TimeChartTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "TimeChartTest Project";
    private static final String FOLDER_NAME = "Demo Study";
    private static final String STUDY_ZIP = "/sampledata/study/LabkeyDemoStudy.zip";

    private static final String REPORT_NAME_1 = "TimeChartTest Report";
    private static final String REPORT_NAME_2 = "TimeChartTest 2Report";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartTest";
    private static final String X_AXIS_LABEL = "New X-Axis Label";
    private static final String Y_AXIS_LABEL = "New Y-Axis Label";
    private static final String CHART_TITLE = "New Chart Title";

    private static final String USER1 = "user1@timechart.test";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Study", null);
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Time Chart");
        waitForElement(Locator.button("Choose a Measure"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Choose a Measure", 0);
        String extDialogTitle = "Choose a Measure...";
        ExtHelper.waitForExtDialog(this, extDialogTitle);
        waitForText("NAbAssay", WAIT_FOR_JAVASCRIPT);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        log("Test measure search.");
        ExtHelper.setExtFormElementByType(this, extDialogTitle, "text", "nab");
        pressEnter(ExtHelper.getExtDialogXPath(extDialogTitle)+"//input[contains(@class, 'x-form-field') and @type='text']");
        // Count search results (11 in study's NAb assay)
        assertEquals("", 11, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(extDialogTitle)+"//div[contains(@class, 'x-list-body-inner')]/dl")));

        log("Check for appropriate message for measure with no data.");
        click(Locator.xpath(ExtHelper.getExtDialogXPath(extDialogTitle)+"//dl[./dt/em[text()='Cutoff Percentage (3)']]"));
        clickNavButton("Select", 0);
        waitForText("No data found", WAIT_FOR_JAVASCRIPT);

        // Regression test for "11764: Time Chart Wizard raises QueryParseException on 'StdDev' measure"
        log("StdDev regression check");
        clickNavButton("Change", 0);
        extDialogTitle = "Change Measure...";
        ExtHelper.waitForExtDialog(this, extDialogTitle);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(ExtHelper.getExtDialogXPath(extDialogTitle)+"//dl[./dt/em[text()='StdDev']]"));
        clickNavButtonByIndex("Select", 1, 0); // Need to index button because previous dialog is present (but hidden)
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT);

        log("Check visualization");
        clickNavButton("Change", 0);
        extDialogTitle = "Change Measure...";
        ExtHelper.waitForExtDialog(this, extDialogTitle);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByType(this, extDialogTitle, "text", "viral");
        pressEnter(ExtHelper.getExtDialogXPath(extDialogTitle)+"//input[contains(@class, 'x-form-text') and @type='text']");
        assertEquals("", 1, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(extDialogTitle)+"//div[contains(@class, 'x-list-body-inner')]/dl")));
        click(Locator.xpath(ExtHelper.getExtDialogXPath(extDialogTitle)+"//dl[./dt/em[text()='HIV Test Results']]"));
        clickNavButtonByIndex("Select", 2, 0); // Need to index button because previous dialog is present (but hidden)
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("No data found");

        clickNavButton("View Data", 0);
        waitForText("1 - 33 of 33", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker')]/div")); // Select all participants checkbox
        waitForText("1 - 38 of 38", WAIT_FOR_JAVASCRIPT);
        ExtHelper.prevClickFileBrowserFileCheckbox(this, "249320127"); // de-select one participant
        waitForText("1 - 31 of 31", WAIT_FOR_JAVASCRIPT);

        log("Test X-Axis");
        clickNavButton("View Chart(s)", 0);
        ExtHelper.clickExtTab(this, "X-Axis");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='x-axis-interval-combo']/.."), "Weeks");
        waitForText("Weeks Since Start Date", WAIT_FOR_JAVASCRIPT);
        setFormElement("x-axis-label-textfield", X_AXIS_LABEL);
        fireEvent(Locator.name("x-axis-label-textfield"), SeleniumEvent.blur);
        waitForText(X_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='x-axis-interval-combo']/.."), "Days");
        assertTextNotPresent("Days Since Start Date"); // Label shouldn't change automatically once it has been set manually
        checkRadioButton("xaxis_range", "manual");
        setFormElement(Locator.xpath("//input[@name='xaxis_range']/../../input[1]"), "20");
        fireEvent(Locator.xpath("//input[@name='xaxis_range']/../../input[1]"), SeleniumEvent.blur);
        setFormElement(Locator.xpath("//input[@name='xaxis_range']/../../input[2]"), "40");
        fireEvent(Locator.xpath("//input[@name='xaxis_range']/../../input[2]"), SeleniumEvent.blur);
        mouseDown(Locator.xpath("/html/body"));
        assertTextNotPresent("15");
        assertTextNotPresent("45");

        log("Test Y-Axis");
        ExtHelper.clickExtTab(this, "Y-Axis");
        setFormElement("y-axis-label-textfield", Y_AXIS_LABEL);
        fireEvent(Locator.name("y-axis-label-textfield"), SeleniumEvent.blur);
        waitForText(Y_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
        checkRadioButton("yaxis_range", "manual");
        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), "200000");
        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), SeleniumEvent.blur);
        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), "400000");
        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), SeleniumEvent.blur);
        waitForText("400,000", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("500,000");
        assertTextNotPresent("200,000");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./label[text()='Scale:']]/div/div"), "Log");
        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), "10000");
        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), SeleniumEvent.blur);
        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), "1000000");
        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), SeleniumEvent.blur);
        waitForText("1,000,000", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("10,000", "100,000", "1,000,000");
        assertTextNotPresent("500,000");

        ExtHelper.clickExtTab(this, "Chart(s)");
        checkRadioButton("chart_layout", "per_subject");
        setFormElement("chart-title-textfield", CHART_TITLE);
        fireEvent(Locator.name("chart-title-textfield"), SeleniumEvent.blur);
        assertTextPresent(CHART_TITLE, 5);
        ExtHelper.prevClickFileBrowserFileCheckbox(this, "249320127"); // re-select participant
        waitForText("New Chart Title: 249320127", WAIT_FOR_JAVASCRIPT);
        assertTextPresent(CHART_TITLE, 6);

        ExtHelper.clickExtTab(this, "Overview");
        setFormElement("reportName", REPORT_NAME_1);
        setFormElement("reportDescription", REPORT_DESCRIPTION);
        clickNavButton("Save", 0);
        ExtHelper.waitForExtDialog(this, "Success");
        clickNavButton("OK", 0);

        clickNavButton("Save As", 0);
        ExtHelper.waitForExtDialog(this, "Save As");
        setFormElement(Locator.id("reportNameSaveAs"), REPORT_NAME_2);
        setFormElement(Locator.id("reportDescriptionSaveAs"), "This is another report generated by the TimeChartTest");
        click(Locator.id("reportSharedMeSaveAs"));
        clickNavButtonByIndex("Save", 1, 0);
        ExtHelper.waitForExtDialog(this, "Success");
        clickNavButton("OK", 0);

        log("Verify saved report");
        clickLinkWithText("Manage Views");
        waitForText(REPORT_NAME_1, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(REPORT_NAME_2);
        click(Locator.tagWithText("div", REPORT_NAME_1));
        assertTextPresent(REPORT_DESCRIPTION);
        clickAndWait(Locator.xpath("//a[text()='view' and contains(@href, '"+REPORT_NAME_1.replace(" ", "%20")+"')]"));
        waitForText(Y_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(X_AXIS_LABEL);
        assertTextPresent(CHART_TITLE, 6);
        pushLocation();
        pushLocation();

        log("Check Time Chart Permissions");
        createUser(USER1, null);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        setUserPermissions(USER1, "Reader");
        setSiteGroupPermissions("Guests", "Reader");
        clickNavButton("Save and Finish");
        impersonate(USER1);
        popLocation(); // Saved chart
        waitForText("New Chart Title", WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.xpath("//table[contains(@class, 'x-item-disabled')]//button[text()='Save']"));
        clickLinkWithText(FOLDER_NAME);
        assertTextNotPresent(REPORT_NAME_2);
        stopImpersonating();
        signOut();
        popLocation(); // Saved chart
        waitForText("New Chart Title", WAIT_FOR_JAVASCRIPT);
        assertElementNotVisible(Locator.button("Save As"));
        assertElementPresent(Locator.xpath("//table[contains(@class, 'x-item-disabled')]//button[text()='Save']"));
        simpleSignIn();
    }

    @Override
    public void doCleanup()
    {
        try {deleteUser(USER1);} catch (Throwable T) {}
        try {deleteProject(PROJECT_NAME);} catch (Throwable T) {}
    }
}
