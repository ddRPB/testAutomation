/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ElispotAssayTest extends AbstractPlateBasedAssayTest
{
    private final static String TEST_ASSAY_PRJ_ELISPOT = "Elispot Test Verify Project";

    protected static final String TEST_ASSAY_ELISPOT = "TestAssayElispot";
    protected static final String TEST_ASSAY_ELISPOT_DESC = "Description for Elispot assay";

    protected final String TEST_ASSAY_ELISPOT_FILE1 = getLabKeyRoot() + "/sampledata/Elispot/CTL_040A20042503-0001p.xls";
    protected final String TEST_ASSAY_ELISPOT_FILE2 = getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W4.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE3 = getLabKeyRoot() + "/sampledata/Elispot/Zeiss_datafile.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE4 = getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W5.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE5 = getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W8.txt";

    private static final String PLATE_TEMPLATE_NAME = "ElispotAssayTest Template";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/nab";
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_ELISPOT;
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    @LogMethod
    protected void runUITests()
    {
        log("Starting Elispot Assay BVT Test");

        //revert to the admin user
        revertToAdmin();

        log("Testing Elispot Assay Designer");

        // set up a scripting engine to run a java transform script
        prepareProgrammaticQC();

        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_ELISPOT, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_ELISPOT);

        //add the Assay List web part so we can create a new elispot assay
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        addWebPart("Assay List");

        //create a new elispot template
        createTemplate();

        //create a new elispot assay
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "ELISpot");
        clickButton("Next");

        log("Setting up Elispot assay");
        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("AssayDesignerName"), TEST_ASSAY_ELISPOT);

        selectOptionByValue(Locator.id("plateTemplate"), PLATE_TEMPLATE_NAME);
        setFormElement(Locator.id("AssayDesignerDescription"), TEST_ASSAY_ELISPOT_DESC);


        // set the specimenId field default value to be : last entered
        Locator specimenField = Locator.xpath("//td[@class='labkey-wp-title-left' and text() ='Sample Fields']/../..//div[@id='name1']");
        click(specimenField);
        click(Locator.xpath("//td[@class='labkey-wp-title-left' and text() ='Sample Fields']/../..//span[text()='Advanced']"));
        selectOptionByValue(Locator.xpath("//td[@class='labkey-wp-title-left' and text() ='Sample Fields']/../..//select[@class='gwt-ListBox']"), "LAST_ENTERED");

        sleep(2000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));

        log("Uploading Elispot Runs");
        clickButton("Import Data");
        clickButton("Next");

        selectOptionByText(Locator.name("plateReader"), "Cellular Technology Ltd. (CTL)");
        uploadFile(TEST_ASSAY_ELISPOT_FILE1, "A", "Save and Import Another Run", true);
        assertTextPresent("Upload successful.");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE2, "B", "Save and Import Another Run", false);
        assertTextPresent("Upload successful.");

        selectOptionByText(Locator.name("plateReader"), "Zeiss");
        uploadFile(TEST_ASSAY_ELISPOT_FILE3, "C", "Save and Finish", false);

        assertElispotData();
        runTransformTest();
        doBackgroundSubtractionTest();
    }

    protected void uploadFile(String filePath, String uniqueifier, String finalButton, boolean testPrepopulation)
    {
        uploadFile(filePath, uniqueifier, finalButton, testPrepopulation, false);
    }

    protected void uploadFile(String filePath, String uniqueifier, String finalButton, boolean testPrepopulation, boolean subtractBackground)
    {
        if(subtractBackground)
            checkCheckbox("subtractBackground");
        for (int i = 0; i < 4; i++)
        {
            Locator specimenLocator = Locator.name("specimen" + (i + 1) + "_ParticipantID");

            // test for prepopulation of specimen form element values
            if (testPrepopulation)
                assertFormElementEquals(specimenLocator, "Specimen " + (i+1));
            setFormElement(specimenLocator, "ptid " + (i + 1) + " " + uniqueifier);

            setFormElement(Locator.name("specimen" + (i + 1) + "_VisitID"), "" + (i + 1));
            setFormElement(Locator.name("specimen" + (i + 1) + "_SampleDescription"), "blood");
        }

        File file1 = new File(filePath);
        setFormElement("__primaryFile__", file1);
        clickButton("Next");

        for (int i = 0; i < 6; i++)
        {
            setFormElement(Locator.name("antigen" + (i + 1) + "_AntigenID"), "" + (i + 1));

            Locator antigenLocator = Locator.name("antigen" + (i + 1) + "_AntigenName");

            // test for prepopulation of antigen element values
            if (testPrepopulation)
                assertFormElementEquals(antigenLocator, "Antigen " + (i+1));

            setFormElement(antigenLocator, "atg_" + (i + 1) + uniqueifier);
            setFormElement(Locator.name("antigen" + (i + 1) + "_CellWell"), "150");
        }

        clickButton(finalButton);
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @LogMethod
    private void assertElispotData()
    {
        clickAndWait(Locator.linkWithText("Zeiss_datafile.txt"));
        
        assertTextPresent("ptid 1 C");
        assertTextPresent("ptid 2 C");
        assertTextPresent("ptid 3 C");
        assertTextPresent("ptid 4 C");

        assertTextPresent("atg_1C");
        assertTextPresent("atg_2C");
        assertTextPresent("atg_3C");
        assertTextPresent("atg_4C");

        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkWithText("AID_0161456 W4.txt"));

        assertTextPresent("ptid 1 B");
        assertTextPresent("ptid 2 B");
        assertTextPresent("ptid 3 B");
        assertTextPresent("ptid 4 B");

        assertTextPresent("atg_1B");
        assertTextPresent("atg_2B");
        assertTextPresent("atg_3B");
        assertTextPresent("atg_4B");

        // show the normalized spot count column and verify it is calculated correctly
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("NormalizedSpotCount");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable dataTable = new DataRegionTable("Data", this);
        List<String> cellWell = dataTable.getColumnDataAsText("CellWell");
        List<String> spotCount = dataTable.getColumnDataAsText("SpotCount");
        List<String> normalizedSpotCount = dataTable.getColumnDataAsText("NormalizedSpotCount");

        for (int i=0; i < cellWell.size(); i++)
        {
            int cpw = NumberUtils.toInt(cellWell.get(i), 0);
            Float sc = NumberUtils.toFloat(spotCount.get(i));
            Float nsc = NumberUtils.toFloat(normalizedSpotCount.get(i));
            Float computed = sc;

            if (cpw != 0)
                computed = sc / cpw * 1000000;

            Assert.assertEquals(computed.intValue(), nsc.intValue());
        }
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();

        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkContainingText("details"));

        assertTextPresent("Plate Summary Information");
        assertTextPresent("Antigen 7 Mean");
        assertTextPresent("Antigen 7 Median");
        assertTextPresent("Antigen 8 Mean");
        assertTextPresent("Antigen 8 Median");
        assertTextPresent("blood");

        waitForElement(Locator.xpath("//label[contains(@class, 'x4-form-item-label') and text() = 'Sample Well Groups']"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("//label[contains(@class, 'x4-form-item-label') and text() = 'Antigen Well Groups']"), WAIT_FOR_JAVASCRIPT);

        // test color hilighting of sample and antigen well groups
        click(Locator.xpath("//label[contains(@class, 'x4-form-cb-label') and text() = 'Specimen 2']"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1023.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1021.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1028.0"));

        // antigen well group
        click(Locator.xpath("//label[contains(@class, 'x4-form-cb-label') and contains(text(), 'Antigen 2')]"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "765.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "591.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "257.0"));

        // test the mean and median values
        DataRegionTable table = new DataRegionTable("AntigenStats", this);
        String[] expectedMeans = new String[]{"15555.6", "8888.9", "122222.2", "46666.7"};
        String[] expectedMedians = new String[]{"13333.3", "13333.3", "126666.7", "40000.0"};

        int row = 0;
        for (String mean : expectedMeans)
            Assert.assertEquals(mean, table.getDataAsText(row++, "Atg1CMean"));

        row = 0;
        for (String median : expectedMedians)
            Assert.assertEquals(median, table.getDataAsText(row++, "Atg1CMedian"));

        // verify customization of the run details view is possible
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Median");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 8_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 8_Median");
        _customizeViewsHelper.saveCustomView("Without Antigen7&8");

        clickMenuButton("Views", "default");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Median");
        _customizeViewsHelper.saveDefaultView();

        clickMenuButton("Views", "Without Antigen7&8");
        assertTextNotPresent("Antigen 7 Mean");
        assertTextNotPresent("Antigen 7 Median");
        assertTextNotPresent("Antigen 8 Mean");
        assertTextNotPresent("Antigen 8 Median");
        
        clickMenuButton("Views", "default");
        assertTextNotPresent("Antigen 7 Mean");
        assertTextNotPresent("Antigen 7 Median");
        assertTextPresent("Antigen 8 Mean");
        assertTextPresent("Antigen 8 Median");
    }

    private Locator getLocatorForHilightedWell(String className, String count)
    {
        String xpath = String.format("//div[contains(@class, '%s') and contains(@style, 'background-color: rgb(18, 100, 149);')]//a[contains(text(), %s)]",
                className, count);
        return Locator.xpath(xpath);
    }

    protected void createTemplate()
    {
        clickButton("Manage Assays");
        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 96 well (8x12) ELISpot default template"));
        Locator nameField = Locator.id("templateName");
        waitForElement(nameField, WAIT_FOR_JAVASCRIPT);

        Locator.css(".gwt-Label").withText("CONTROL").waitForElmement(_driver, WAIT_FOR_JAVASCRIPT).click();

        click(Locator.xpath("//div[contains(@class, 'x-form-trigger-arrow')]"));
        Locator.css(".x-combo-list-item").withText("Background Wells").waitForElmement(_driver, WAIT_FOR_JAVASCRIPT).click();
        clickButton("Create", 0);
        waitForElement(Locator.tagWithText("label", "Background Wells"));

        //TODO: Causes template creation to fail
//        log("Fail to create duplicate well group");
//        click(Locator.xpath("//div[contains(@class, 'x-form-trigger-arrow')]"));
//        Locator.css(".x-combo-list-item").contains("Background Wells").waitForElmement(_driver, WAIT_FOR_JAVASCRIPT).click();
//        clickButton("Create", 0);
//        assertAlert("Group : Background Wells already exists.");

        highlightWells("CONTROL", "Background Wells", "A1", "B3");
        highlightWells("CONTROL", "Background Wells", "C4", "D6");
        highlightWells("CONTROL", "Background Wells", "E7", "F9");
        highlightWells("CONTROL", "Background Wells", "G10", "H12");

        Locator groupField = Locator.xpath("//input[../div[contains(@class, 'x-form-trigger-arrow')]]");
        setFormElement(groupField, "other control group");

        setFormElement(nameField, PLATE_TEMPLATE_NAME);
        pressTab(nameField);
        clickButton("Create", 0);
        clickButton("Save & Close");
        waitForText(PLATE_TEMPLATE_NAME);
    }

    /**
     * Cleanup entry point.
     * @param afterTest
     */
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        revertToAdmin();
        super.doCleanup(afterTest);

        try
        {
            deleteEngine();
        }
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    } //doCleanup()

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading Elispot Runs with a transform script");

        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickEditAssayDesign();

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"), 0);
        clickButton("Save & Close");
        waitForElement(Locator.id("dataregion_Runs"));

        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), "transformed assayId");
        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE4, "D", "Save and Finish", false);

        // verify there is a spot count value of 747.747 and a custom column added by the transform
        clickAndWait(Locator.linkWithText("AID_0161456 W5.txt"));
        assertTextPresent("747.7");
        assertTextPresent("Custom Elispot Column");
        assertTextPresent("transformed!");
    }

    protected void doBackgroundSubtractionTest()
    {
        removeTransformScript();
        verifyBackgroundSubtractionOnExistingRun();
        verifyBackgroundSubtractionOnNewRun();
    }

    // Unable to apply background substitution to runs imported with a transform script.
    protected void removeTransformScript()
    {
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickEditAssayDesign();
        waitForElement(Locator.css("#partdelete_removeTransformScript0 img"));
        click(Locator.css("#partdelete_removeTransformScript0 img"));
        clickButton("Save & Close");
        waitForElement(Locator.id("dataregion_Runs"));
    }

    private final static String FILE4_PLATE_SUMMARY_POST_SUBTRACTION =
            "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n\n" +
            "A\n1.0\n0.0\n0.0\n0.0\n1.0\n0.0\n0.0\n4.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "B\n0.0\n1.0\n0.0\n0.0\n0.0\n0.0\n0.0\n6.0\n1.0\n2.0\n0.0\n0.0\n\n" +
            "C\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n20.0\n243.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "D\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n21.0\n264.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "E\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n277.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "F\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n3.0\n277.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "G\n6.0\n5.0\n7.0\n8.0\n4.0\n5.0\n38.0\n709.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "H\n14.0\n6.0\n6.0\n4.0\n17.0\n11.0\n49.0\n731.0\n0.0\n0.0\n0.0\n0.0";
    protected void verifyBackgroundSubtractionOnExistingRun()
    {
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        assertTextPresent("Background Subtraction");
        DataRegionTable runTable = new DataRegionTable("Runs", this, true, true);
        List<String> column = runTable.getColumnDataAsText("Background Subtraction");
        for(String item : column)
        {
            Assert.assertEquals("Background subtraction should be disabled by default.", "false", item);
        }

        runTable.checkAllOnPage();
        clickButton("Subtract Background");

        waitForTextWithRefresh("COMPLETE", WAIT_FOR_PAGE);

        // Check well counts for TEST_ASSAY_ELISPOT_FILE4
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickAndWait(Locator.linkWithText("run details", 3));
        waitForElement(Locator.css("#plate-summary-div-1 table"));

        DataRegionTable table = new DataRegionTable("AntigenStats", this, true, true);

        Iterator<String> means = Arrays.asList("0.0", "2271111.1", "1111.1", "4444.4").iterator();
        for (String mean : table.getColumnDataAsText("Atg1AMean"))
            Assert.assertEquals(means.next(), mean);

        Iterator<String> medians = Arrays.asList("0.0", "2376666.7", "3333.3", "6666.7").iterator();
        for (String median : table.getColumnDataAsText("Atg1AMedian"))
            Assert.assertEquals(medians.next(), median);

        //Assert.assertEquals("Incorrect spot counts after background subtraction.", FILE4_PLATE_SUMMARY_POST_SUBTRACTION, getText(Locator.css("#plate-summary-div-1 table")));

        // Check that all runs have been subtracted
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        column = runTable.getColumnDataAsText("Background Subtraction");
        for(String item : column)
        {
            Assert.assertEquals("Background subtraction should be true for all runs.", "true", item);
        }
    }

    private final static String FILE5_PLATE_SUMMARY_POST_SUBTRACTION =
            "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n\n" +
            "A\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n8.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "B\n0.0\n0.0\n0.0\n1.0\n1.0\n0.0\n0.0\n3.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "C\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n14.0\n149.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "D\n0.0\n2.0\n0.0\n0.0\n0.0\n0.0\n19.0\n195.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "E\n0.5\n0.0\n0.0\n0.5\n8.5\n0.0\n1.5\n234.5\n0.0\n0.0\n0.0\n0.0\n\n" +
            "F\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n0.0\n266.5\n0.0\n0.0\n0.0\n0.0\n\n" +
            "G\n15.0\n11.0\n12.0\n12.0\n9.0\n7.0\n61.0\n680.0\n0.0\n0.0\n0.0\n0.0\n\n" +
            "H\n6.0\n4.0\n11.0\n20.0\n26.0\n12.0\n46.0\n576.0\n0.0\n0.0\n0.0\n0.0";
    protected void verifyBackgroundSubtractionOnNewRun()
    {
        clickFolder(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickButton("Import Data");
        clickButton("Next");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE5, "E", "Save and Finish", false, true);
        DataRegionTable runTable = new DataRegionTable("Runs", this, true, true);
        assertTextPresent("AID_0161456 W8.txt");
        List<String> column = runTable.getColumnDataAsText("Background Subtraction");
        for(String item : column)
        {
            Assert.assertEquals("Background subtraction should be true for all runs.", "true", item);
        }

        clickAndWait(Locator.linkWithText("run details"));
        waitForElement(Locator.css("#plate-summary-div-1 table"));
        //Assert.assertEquals("Incorrect spot counts after background subtraction.", FILE5_PLATE_SUMMARY_POST_SUBTRACTION, getText(Locator.css("#plate-summary-div-1 table")));

        DataRegionTable detailsTable = new DataRegionTable("AntigenStats", this, true, true);
        column = detailsTable.getColumnDataAsText("BackgroundMedian");
        String[] expectedColumn = {"0.0","0.0","9.5","0.0"};
        for(int i = 0; i < 4; i++)
        {
            Assert.assertEquals("Incorrect background value for " + detailsTable.getDataAsText(i, "Participant ID"), expectedColumn[i], column.get(i));
        }
    }

    protected void highlightWell(String type, String group, String cell)
    {
        highlightWells(type, group, cell, cell);
    }

    protected void highlightWells(String type, String group, String startCell, String endCell)
    {
        Locator start = Locator.css(".Cell-"+startCell);
        Locator end = Locator.css(".Cell-"+endCell);
        if(group != null & !"".equals(group))
        {
            if(!getText(Locator.css(".gwt-TabBarItem-selected")).equals(type))
            {
                Locator.css(".gwt-Label").withText(type).findElement(_driver).click();
                //want for switch
            }
            if(!isChecked(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+group+"']]")))
                click(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+group+"']]"));
            if(!getAttribute(start, "style").contains("rgb(255, 255, 255)"))
                click(start);
        }
        else
        {
            Locator.css(".gwt-Label").withText(type).findElement(_driver).click();
            //select no group in order to clear area
        }
        dragAndDrop(start, end);
    }
}
