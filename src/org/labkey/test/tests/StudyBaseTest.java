/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

package org.labkey.test.tests;

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Set;

/*
* User: adam
* Date: Aug 17, 2009
* Time: 1:42:07 PM
*/

// Provides some helpful utilities used in study-related tests.  Subclasses provide all study creation and
// verification steps.
public abstract class StudyBaseTest extends SimpleApiTest
{
    protected static final String ARCHIVE_TEMP_DIR = getStudySampleDataPath() + "drt_temp";
    protected static final String SPECIMEN_ARCHIVE_A = getStudySampleDataPath() + "specimens/sample_a.specimens";
    protected int datasetCount = 48;
    protected int visitCount = 65;

    private SpecimenImporter _specimenImporter;

    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps();


    protected void setupSpecimenManagement()
    {
        clickFolder(getStudyLabel());
        clickAndWait(Locator.linkWithText("Manage Study"));
        clickAndWait(Locator.linkWithText("Manage Request Statuses"));
        setFormElement("newLabel", "New Request");
        clickButton("Save");
        setFormElement("newLabel", "Processing");
        clickButton("Save");
        setFormElement("newLabel", "Completed");
        checkCheckbox("newFinalState");
        clickButton("Save");
        setFormElement("newLabel", "Rejected");
        checkCheckbox("newFinalState");
        uncheckCheckbox("newSpecimensLocked");
        clickButton("Done");
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    protected static String getStudySampleDataPath()
    {
        return "/sampledata/study/";
    }

    protected String getPipelinePath()
    {
        return getLabKeyRoot() + getStudySampleDataPath();
    }

    protected String getProjectName()
    {
        return "StudyVerifyProject";
    }

    protected String getStudyLabel()
    {
        return "Study 001";
    }

    protected String getFolderName()
    {
        return "My Study";
    }

    protected void initializeFolder()
    {
        hoverProjectBar();
        int response = -1;
        try{
            response = WebTestHelper.getHttpGetResponse(getBaseURL() + "/" + stripContextPath(getAttribute(Locator.linkWithText(getProjectName()), "href")));
        }
        catch(Exception e){/*No link or bad response*/}

        if (HttpStatus.SC_OK != response)
        {
            _containerHelper.createProject(getProjectName(), null);
        }

        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    // Start importing the specimen archive.  This can load in the background while executing the first set of
    // verification steps to speed up the test.  Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        startSpecimenImport(completeJobsExpected, SPECIMEN_ARCHIVE_A);
    }
    protected void startSpecimenImport(int completeJobsExpected, String specimenArchivePath)
    {
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), specimenArchivePath), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
        _specimenImporter.startImport();
    }

    protected void waitForSpecimenImport()
    {
        _specimenImporter.waitForComplete();
    }

    @Override
    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0]; 
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);

        deleteLogFiles(".");
        deleteLogFiles("datasets");
        deleteDir(new File(getPipelinePath(), "assaydata"));
        deleteDir(new File(getPipelinePath(), "reports_temp"));
        deleteDir(new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR));
    }

    private void deleteLogFiles(String directoryName)
    {
        File dataRoot = new File(getPipelinePath() + directoryName);
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
        for (File f : logFiles)
            if (!f.delete())
                log("WARNING: couldn't delete log file " + f.getAbsolutePath());
    }

    protected void importStudy(){importStudy(null);}

    protected void importStudy(String pipelinePath)
    {
        initializeFolder();
        initializePipeline(pipelinePath);

        // Start importing study.xml to create the study and load all the datasets.  We'll wait for this import to
        // complete before doing any further tests.
        clickFolder(getFolderName());

        log("Import new study with alt-ID");
        importFolderFromPipeline("AltIdStudy.folder.zip");
//        clickButton("Process and Import Data");
//        _extHelper.waitForImportDataEnabled();
//        _extHelper.clickFileBrowserFileCheckbox("study.xml");
//        selectImportDataAction("Import Study");
    }

    protected void exportStudy(boolean useXmlFormat, boolean zipFile)
    {
        exportStudy(useXmlFormat, zipFile, true);
    }

    protected void exportStudy(boolean useXmlFormat, boolean zipFile, boolean exportProtected)
    {
        exportStudy(useXmlFormat, zipFile, exportProtected, false, false, false, Collections.<String>emptySet());
    }

    @LogMethod protected void exportStudy(boolean useXmlFormat, boolean zipFile, boolean exportProtected,
                               boolean useAlternateIDs, boolean useAlternateDates, boolean maskClinic,
                               @Nullable Set<String> uncheckObjects)
    {
        clickFolder(getStudyLabel());
        clickTab("Manage");
        clickButton("Export Study");

        assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings");
        // TODO: these have moved to the folder archive, be sure to test there: "Queries", "Custom Views", "Reports", "Lists"

        if (uncheckObjects != null)
        {
            for (String uncheckObject : uncheckObjects)
                uncheckCheckbox(Locator.checkboxByNameAndValue("types", uncheckObject));
        }
        checkRadioButton(Locator.radioButtonByNameAndValue("format", useXmlFormat ? "new" : "old"));
        checkRadioButton(Locator.radioButtonByNameAndValue("location", zipFile ? "1" : "0"));  // zip file vs. individual files
        if(!exportProtected)
            checkCheckbox(Locator.name("removeProtected"));
        if(useAlternateIDs)
            checkCheckbox(Locator.name("alternateIds"));
        if(useAlternateDates)
            checkCheckbox(Locator.name("shiftDates"));
        if(maskClinic)
            checkCheckbox(Locator.name("maskClinic"));
        clickButton("Export");
    }

    protected void deleteStudy(String studyLabel)
    {
        clickFolder(studyLabel);
        clickTab("Manage");
        clickButton("Delete Study");
        checkCheckbox("confirm");
        clickButton("Delete", WAIT_FOR_PAGE);
    }

    protected void initializePipeline()
    {
        initializePipeline(null);
    }
    
    protected void initializePipeline(String pipelinePath)
    {
        if(pipelinePath==null)
            pipelinePath = getPipelinePath();

        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        addWebPart("Datasets");
        addWebPart("Specimens");
        addWebPart("Views");
        // Set a magic variable to prevent the data region from refreshing out from under us, which causes problems
        // in IE testing
        selenium.runScript("LABKEY.disablePipelineRefresh = true;");
        waitAndClickButton("Setup");
        setPipelineRoot(pipelinePath);
    }

    // Must be on study home page or "manage study" page
    protected void setDemographicsBit(String datasetName, boolean demographics)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(datasetName));
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (demographics)
            checkCheckbox("demographicData");
        else
            uncheckCheckbox("demographicData");

        clickButton("Save");
    }

    // Must be on study home page or "manage study" page
    protected void setVisibleBit(String datasetName, boolean showByDefault)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(datasetName));
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (showByDefault)
            checkCheckbox("showByDefault");
        else
            uncheckCheckbox("showByDefault");

        clickButton("Save");
    }

    protected void createReport(String reportType)
    {
        // click the create button dropdown
        String id = _extHelper.getExtElementId("btn_createView");
        click(Locator.id(id));

        id = _extHelper.getExtElementId(reportType);
        clickAndWait(Locator.id(id));
    }

    public void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        Assert.assertEquals(selenium.getSelectedValue(Locator.tagWithName("select", name).index(i).toString()), expected);
    }

    protected void goToManageStudyPage(String projectName, String studyName)
    {
        log("Going to Manage Study Page of: " + studyName);
        waitForElement(Locator.id("folderBar"));
        if (!getText(Locator.id("folderBar")).equals(projectName))
            clickProject(projectName);
        clickFolder(studyName);
        waitAndClick(Locator.linkWithText("Manage Study"));
        waitForElement(Locator.xpath("id('labkey-nav-trail-current-page')[text()='Manage Study']"));
    }

    //must be in folder whose designation you wish to change.
    protected void setStudyRedesign()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", "Study Redesign (ITN)");
        clickButton("Update Folder");
    }

    protected void enterStudySecurity()
    {
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Study Security");
        waitAndClickButton("Study Security");
    }

    public void goToManageDatasets()
    {
        goToManageStudy();
        waitForText("Manage Datasets");
        click(Locator.xpath("//a[text()='Manage Datasets']"));
    }
}
