/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.LogMethod;

import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexExcludedTitrationTest extends LuminexTest
{
    protected static final Locator AVAILABLE_ANALYTES_CHECKBOX = Locator.xpath("//div[@class='x-grid3-hd-inner x-grid3-hd-checker']/div[@class='x-grid3-hd-checker']");
    protected static final Locator COMMENT_LOCATOR = Locator.xpath("//input[@id='comment']") ;
    protected void ensureConfigured()
    {

        setUseXarImport(true);
        super.ensureConfigured();
    }

    @LogMethod @Override
    protected void runUITests()
    {
        runTitrationExclusionTest();
    }

    /**
     * test of titration exclusion- the ability to exclude certain titrations and add a comment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, wells for the given titration will be marked excluded
     */
    @LogMethod
    private void runTitrationExclusionTest()
    {
        ensureMultipleCurveDataPresent();

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();
        excludeTitration("Sample 1");
        excludeAnalyteWithinTitration("Sample 2", "ENV6");
    }

    private void excludeTitration(String Titration)
    {
        clickButton("Exclude Titration","Analytes excluded for a replicate group or at the assay level will not be re-included by changes in titration exclusions" );
        waitForElement(Locator.xpath("//td/div").withText(Titration));
        mouseDown(Locator.xpath("//td/div").withText(Titration));
        waitForElement(AVAILABLE_ANALYTES_CHECKBOX);
        mouseDown(AVAILABLE_ANALYTES_CHECKBOX);
        String exclusionMessage =  "excluding all analytes for titration " + Titration;
        setFormElement(COMMENT_LOCATOR, exclusionMessage);
        sleep(10000);
        waitAndClickButton("Save", 0);
        _extHelper.waitForExtDialog("Confirm Exclusions", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes");
        verifyTitrationExclusion(Titration, exclusionMessage);
    }

    private void excludeAnalyteWithinTitration(String Titration, String Analyte)
    {
        clickButton("Exclude Titration","Analytes excluded for a replicate group or at the assay level will not be re-included by changes in titration exclusions" );
        waitForElement(Locator.xpath("//td/div").withText(Titration));
        click(Locator.xpath("//td/div").withText(Titration));
        waitForElement(Locator.xpath("//td/div[@class='x-grid3-cell-inner x-grid3-col-1 x-unselectable']").containing(Analyte));
        click(Locator.xpath("//td/div[@class='x-grid3-cell-inner x-grid3-col-1 x-unselectable']").containing(Analyte));
        String exclusionMessage =  "excluding " + Analyte + " analyte for titration " + Titration;
        setFormElement(COMMENT_LOCATOR, exclusionMessage);
        sleep(10000) ;
        waitAndClickButton("Save", 0);
        _extHelper.waitForExtDialog("Confirm Exclusions", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes");
        verifyTitrationAnalyteExclusion(Titration, Analyte, exclusionMessage);
    }

    private void verifyTitrationAnalyteExclusion(String excludedTitration, String excludedAnalyte, String exclusionMessage)
    {
        setFilter("Data", "Description", "Equals", excludedTitration);
        setFilter("Data", "Analyte", "Contains", excludedAnalyte);
        waitForElement(Locator.paginationText(1, 12, 12));
        List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for (int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.substring(0, 4);
            log("Analyte: " + analyte);

            if (analyte.contains(excludedAnalyte) && description.equals(excludedTitration))
            {
                assertTrue(comment.contains(exclusionMessage));
            }

            if (comment.contains(exclusionMessage))
            {
                assertTrue(analyte.contains(excludedAnalyte) && description.equals(excludedTitration));
            }
        }

        setFilter("Data", "Analyte", "Does Not Contain", excludedAnalyte);
        setFilter("Data", "ExclusionComment", "Is Not Blank");
        waitForText("No data to show.");

        clearFilter("Data", "ExclusionComment");
        clearFilter("Data", "Analyte");
        clearFilter("Data", "Description");
    }

    private void verifyTitrationExclusion(String excludedTitration, String exclusionMessage)
    {
        setFilter("Data", "Description", "Equals", excludedTitration);
        waitForElement(Locator.paginationText(1, 60, 60));
        List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for(int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.substring(0, 4);
            log("Analyte: " + analyte);

            if(description.equals(excludedTitration))
            {
                assertTrue(comment.contains(exclusionMessage));
            }
        }

        setFilter("Data", "Description", "Does Not Equal", excludedTitration);
        setFilter("Data", "ExclusionComment", "Is Not Blank");
        waitForText("No data to show.");

        clearFilter("Data", "ExclusionComment");
        clearFilter("Data", "Description");
    }

}
