/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import org.labkey.test.util.ListHelper;

import java.io.File;

/*
* User: Jess Garms
* Date: Jan 16, 2009
*/
public class FieldLevelQcTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "FieldLevelQcVerifyProject";
    private static final String LIST_NAME = "QCList";
    private static final String ASSAY_NAME = "QCAssay";
    private static final String ASSAY_RUN_SINGLE_COLUMN = "QCAssayRunSingleColumn";
    private static final String ASSAY_RUN_TWO_COLUMN = "QCAssayRunTwoColumn";
    private static final String ASSAY_EXCEL_RUN_SINGLE_COLUMN = "QCAssayExcelRunSingleColumn";
    private static final String ASSAY_EXCEL_RUN_TWO_COLUMN = "QCAssayExcelRunTwoColumn";

    private static final String TEST_DATA_SINGLE_COLUMN_QC_LIST =
            "Name" + "\t" + "Age" + "\t"  + "Sex" + "\n" +
            "Ted" + "\t" + ".N" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "female" + "\n" +
            "Bob" + "\t" + ".Q" + "\t" + ".N" + "\n";

    private static final String TEST_DATA_TWO_COLUMN_QC_LIST =
            "Name" +    "\t" + "Age" +  "\t" + "AgeQCIndicator" +   "\t" + "Sex" +  "\t" + "SexQCIndicator" + "\n" +
            "Franny" +  "\t" + "" +     "\t" + ".N" +               "\t" + "male" + "\t" +  "" + "\n" +
            "Zoe" +     "\t" + "25" +   "\t" + ".Q" +               "\t" + "female" +     "\t" +  "" + "\n" +
            "J.D." +    "\t" + "50" +   "\t" + "" +                 "\t" + "male" + "\t" +  ".Q" + "\n";

    private static final String TEST_DATA_SINGLE_COLUMN_QC_DATASET = 
            "participantid\tSequenceNum\tAge\tSex\n" +
            "Ted\t1\t.N\tmale\n" +
            "Alice\t1\t17\tfemale\n" +
            "Bob\t1\t.Q\t.N";

    private static final String TEST_DATA_TWO_COLUMN_QC_DATASET =
            "participantid\tSequenceNum\tAge\tAgeQCIndicator\tSex\tSexQCIndicator\n" +
            "Franny\t1\t\t.N\tmale\t\n" +
            "Zoe\t1\t25\t.Q\tfemale\t\n" +
            "J.D.\t1\t50\t\tmale\t.Q";

    private static final String DATASET_SCHEMA_FILE = "/sampledata/fieldLevelQC/dataset_schema.tsv";

    private static final String TEST_DATA_SINGLE_COLUMN_QC_ASSAY =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                    "1\tTed\t1\t01-Jan-09\t.N\tmale\n" +
                    "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                    "3\tBob\t1\t01-Jan-09\t.Q\t.N";

    private static final String TEST_DATA_TWO_COLUMN_QC_ASSAY =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tageQCIndicator\tsex\tsexQCIndicator\n" +
                    "1\tFranny\t1\t01-Jan-09\t\t.N\tmale\t\n" +
                    "2\tZoe\t1\t01-Jan-09\t25\t.Q\tfemale\t\n" +
                    "3\tJ.D.\t1\t01-Jan-09\t50\t\tmale\t.Q";

    private final String ASSAY_SINGLE_COLUMN_EXCEL_FILE = getLabKeyRoot() + "/sampledata/fieldLevelQC/assay_single_column.xls";
    private final String ASSAY_TWO_COLUMN_EXCEL_FILE = getLabKeyRoot() + "/sampledata/fieldLevelQC/assay_two_column.xls";

    protected void doTestSteps() throws Exception
    {
        log("Create QC project");
        createProject(PROJECT_NAME, "Study");
        clickNavButton("Done");
        clickNavButton("Create Study");
        selectOptionByValue("securityString", "BASIC_WRITE");
        clickNavButton("Create Study");
        clickLinkWithText(PROJECT_NAME + " Study");
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getLabKeyRoot() + "/sampledata/fieldLevelQC");
        submit();

        checkListQc();
        checkDatasetQc();
        checkAssayQC();
    }

    private void checkListQc() throws Exception
    {
        log("Create list");

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "");
        listColumn.setAllowsQc(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setAllowsQc(true);
        columns[2] = listColumn;

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and QC column");
        clickLinkWithText("import data");
        setFormElement("ff_data", TEST_DATA_SINGLE_COLUMN_QC_LIST);
        submit();
        validateSingleColumnData();

        deleteListData();        

        log("Test inserting a single new row");
        clickNavButton("Insert New");
        setFormElement("quf_name", "Sid");
        setFormElement("quf_sex", "male");
        setFormElement("quf_age", ".N");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent(".N");

        deleteListData();

        log("Test separate QCIndicator column");
        clickNavButton("Import Data");
        setFormElement("ff_data", TEST_DATA_TWO_COLUMN_QC_LIST);
        submit();
        validateTwoColumnData();
    }

    private void deleteListData()
    {
        checkCheckbox(".toggle");
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
    }

    private void checkDatasetQc() throws Exception
    {
        log("Create dataset");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "datasetName");
        setFormElement("labelColumn", "datasetLabel");
        setFormElement("typeIdColumn", "datasetId");
        setLongTextField("tsv", getFileContents(DATASET_SCHEMA_FILE));
        clickNavButton("Submit", 180000);
        assertNoLabkeyErrors();
        assertTextPresent("QC Dataset");

        log("Import dataset data");
        clickLinkWithText("QC Dataset");
        clickNavButton("View Dataset Data");
        clickNavButton("Import Data");

        setFormElement("tsv", TEST_DATA_SINGLE_COLUMN_QC_DATASET);
        submit();
        validateSingleColumnData();

        deleteDatasetData();

        log("Test inserting a single row");
        clickNavButton("Insert New");
        setFormElement("quf_participantid", "Sid");
        setFormElement("quf_SequenceNum", "1");
        setFormElement("quf_Age", ".N");
        setFormElement("quf_Sex", "male");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent(".N");

        deleteDatasetData();

        log("Import dataset data with two qc columns");
        clickNavButton("Import Data");
        setFormElement("tsv", TEST_DATA_TWO_COLUMN_QC_DATASET);
        submit();
        validateTwoColumnData();
    }

    private void validateSingleColumnData()
    {
        assertNoLabkeyErrors();
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("17");
    }

    private void validateTwoColumnData()
    {
        assertNoLabkeyErrors();
        assertTextPresent("Franny");
        assertTextPresent("Zoe");
        assertTextPresent("J.D.");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("50");
        assertTextPresent("25");
    }

    private void checkAssayQC()
    {
        log("Create assay");
        defineAssay();

        log("Import single column QC data");
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        String targetStudyValue = "/" + PROJECT_NAME + " (" + PROJECT_NAME + " Study)";
        selenium.select("//select[@name='targetStudy']", targetStudyValue);

        clickNavButton("Next");
        selenium.type("name", ASSAY_RUN_SINGLE_COLUMN);
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_SINGLE_COLUMN_QC_ASSAY);
        clickNavButton("Save and Finish");
        assertNoLabkeyErrors();
        clickLinkWithText(ASSAY_RUN_SINGLE_COLUMN);
        validateSingleColumnData();

        log("Import two column QC data");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        selenium.select("//select[@name='targetStudy']", targetStudyValue);

        clickNavButton("Next");
        selenium.type("name", ASSAY_RUN_TWO_COLUMN);
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_TWO_COLUMN_QC_ASSAY);
        clickNavButton("Save and Finish");
        assertNoLabkeyErrors();
        clickLinkWithText(ASSAY_RUN_TWO_COLUMN);
        validateTwoColumnData();

        log("Copy to study");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickLinkWithText(ASSAY_RUN_SINGLE_COLUMN);
        validateSingleColumnData();
        checkCheckbox(".toggle");
        clickNavButton("Copy to Study");
        
        clickNavButton("Next");

        clickNavButton("Copy to Study");
        validateSingleColumnData();

        if (isFileUploadAvailable())
        {
            log("Import from Excel in single-column format");
            clickLinkWithText(PROJECT_NAME);
            clickLinkWithText(ASSAY_NAME);
            clickNavButton("Import Data");
            selenium.select("//select[@name='targetStudy']", targetStudyValue);

            clickNavButton("Next");
            selenium.type("name", ASSAY_EXCEL_RUN_SINGLE_COLUMN);
            checkCheckbox("dataCollectorName", "File upload", true);
            File file = new File(ASSAY_SINGLE_COLUMN_EXCEL_FILE);
            setFormElement("uploadedFile", file);
            clickNavButton("Save and Finish");
            assertNoLabkeyErrors();
            clickLinkWithText(ASSAY_EXCEL_RUN_SINGLE_COLUMN);
            validateSingleColumnData();

            log("Import from Excel in two-column format");
            clickLinkWithText(PROJECT_NAME);
            clickLinkWithText(ASSAY_NAME);
            clickNavButton("Import Data");
            selenium.select("//select[@name='targetStudy']", targetStudyValue);

            clickNavButton("Next");
            selenium.type("name", ASSAY_EXCEL_RUN_TWO_COLUMN);
            checkCheckbox("dataCollectorName", "File upload", true);
            file = new File(ASSAY_TWO_COLUMN_EXCEL_FILE);
            setFormElement("uploadedFile", file);
            clickNavButton("Save and Finish");
            assertNoLabkeyErrors();
            clickLinkWithText(ASSAY_EXCEL_RUN_TWO_COLUMN);
            validateTwoColumnData();
        }
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    /**
     * Defines an test assay at the project level for the security-related tests
     */
    @SuppressWarnings({"UnusedAssignment"})
    private void defineAssay()
    {
        log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Assay List");

        //copied from old test
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        selectOptionByText("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);

        int index = AssayTest.TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT;
        addField("Data Fields", index++, "age", "Age", "Integer");
        addField("Data Fields", index++, "sex", "Sex", "Text (String)");
        sleep(1000);

        log("setting fields to allow QC");
        selenium.click("//input[@id='ff_name4']");
        // This is gross, I know. I don't know why using just the name doesn't work
        //checkCheckbox("allowsQc");
        clickCheckboxById("gwt-uid-11", false);

        selenium.click("//input[@id='ff_name5']");
        // Yes, icky again. See above
        //checkCheckbox("allowsQc");
        clickCheckboxById("gwt-uid-11", false);

        clickNavButton("Save & Close");
        assertNoLabkeyErrors();

    }

    private void deleteDatasetData()
    {
        clickButton("Delete All Rows", defaultWaitForPage);
        selenium.getConfirmation();
    }

    protected void doCleanup() throws Exception
    {
        try {
            deleteProject(PROJECT_NAME);
        }
        catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }
}
