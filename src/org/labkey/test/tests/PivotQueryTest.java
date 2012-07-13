/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.util.List;

/**
 * User: kevink
 * Date: 6/27/12
 */
public class PivotQueryTest extends BaseSeleniumWebTest
{
    private static final String STUDY_ZIP = "/sampledata/study/LabkeyDemoStudy.zip";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(getProjectName()); } catch (Throwable t) {}
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void setupProject()
    {
        createProject(getProjectName(), "Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();
        verifyPivotQuery();

        // UNDONE: customize view (remove columns, reorder columns)
        // UNDONE: export to excel
        // UNDONE: LABKEY.Query.selectRows(), check pivot metadata
    }

    private void verifyPivotQuery()
    {
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=study&query.queryName=LuminexPivot");
        setSort("query", "ParticipantId", SortDirection.ASC);

        log("** Verifing pivot table headers");
        Locator AnalyteName_header = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[3]/td[2]");
        assertElementContains(AnalyteName_header, "Analyte Name");

        Locator IL_10_header = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[4]/td[1]");
        assertElementContains(IL_10_header, "IL-10 (23)");

        Locator ConcInRange_MIN_header = Locator.xpath("//*[@id=\"query:IL-10 (23)::ConcInRange_MIN:header\"]");
        assertElementContains(ConcInRange_MIN_header, "Conc In Range MIN");

        log("** Verifing pivot table contents");
        // First "Participant" data cell
        Locator Participant_cell = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[7]/td[1]");
        assertElementContains(Participant_cell, "249318596");

        // First "ParticipantCount" data cell
        Locator ParticipantCount_cell = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[7]/td[2]");
        assertElementContains(ParticipantCount_cell, "5");

        // First "ConcInRange_MIN" data cell
        Locator ConcInRange_MIN_cell = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[7]/td[3]");
        // NOTE: Remove "<missing column..." when the OOR column metadata works
        // Issue 15299: QueryPivot/QAggregate: MV and OOR suggested columns
        assertElementContains(ConcInRange_MIN_cell, "<missing column IL-10 (23)::ConcInRange_MINOORIndicator>7.99");

        // XXX: SQLServer dialect doesn't support group_concat yet: reenable the GROUP_CONCAT aggregate in LuminexPivot query when it is supported.
        // First "ConcInRange_CONCAT" data cell
        //Locator ConcInRange_CONCAT_cell = Locator.xpath("//*[@id=\"dataregion_query\"]/tbody/tr[7]/td[6]");
        //String contents = selenium.getText(ConcInRange_CONCAT_cell.toString());
        //assertNotNull("The GROUP_CONCAT cell is empty", contents);
        //String[] concats = contents.split(", *");
        //assertTrue("Expected 5 GROUP_CONCAT values", concats.length == 5);
    }
}
