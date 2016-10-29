/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartQueryDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.LookAndFeelPieChart;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;

import java.net.URL;

@Category({DailyC.class, Reports.class, Charting.class})
public class PieChartTest extends GenericChartsTest
{
    // TODO add test case for view base filters and user filters applied on create chart

    private final String PIE_CHART_SAVE_NAME = "Simple Pie Chart Test";
    private final String PIE_CHART_CATEGORY = "1.Adverse Experience (AE)";
    private final String PIE_CHART_MEASURE = "9. Visit Code reported";

    @LogMethod
    protected void testPlots()
    {
        doBasicPieChartTest();
        doColumnChartClickThrough();
        doExportOfPieChart();
    }

    @LogMethod
    private void doBasicPieChartTest()
    {

        final String AE_1_QUERY_NAME = "AE-1 (AE-1:(VTN) AE Log)";
        final String PLOT_TITLE = TRICKY_CHARACTERS;
        final String COLOR_WHITE = "FFFFFF";
        final String COLOR_RED = "FF0000";
        final String COLOR_BLUE = "0000FF";
        final String COLOR_BLACK = "000000";
        final String PIE_CHART_SAVE_DESC = "Pie chart created with the simple test.";

        ChartQueryDialog queryDialog;
        ChartTypeDialog chartTypeDialog;
        LookAndFeelPieChart pieChartLookAndFeel;
        String svgText, strTemp;
        int percentCount;

        log("Create a pie chart and then set different values using the dialogs.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickAddChart(ChartTypes.PIE);

        queryDialog = new ChartQueryDialog(getDriver());
        chartTypeDialog = queryDialog.selectSchema("study")
                .selectQuery(AE_1_QUERY_NAME)
                .clickOk();

        log("Set the minimal attributes necessary to create a pie chart.");
        chartTypeDialog.setCategories(PIE_CHART_CATEGORY);
        chartTypeDialog.clickApply();

        sleep(3000);  // TODO Is there a better trigger?

        log("Validate that the text values of the pie chart are as expected.");
        svgText = getSVGText();
        Assert.assertTrue("SVG did not contain expected title: '" + PIE_CHART_CATEGORY + "'", svgText.contains(PIE_CHART_CATEGORY.replace(" ", "")));
        Assert.assertTrue("SVG did not contain query text 'Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting'", svgText.contains("Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting"));

        log("Validate that the correct number of % values are shown.");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 6 '%' in the svg, found " + percentCount, 6, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected ')18%11%5%5%5%5%I'", svgText.contains(")18%11%5%5%5%5%I"));

        log("Now add a measure to the pie chart.");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setMeasure(PIE_CHART_MEASURE)
                .clickApply();

        sleep(3000);  // TODO Is there a better trigger?

        log("Again validate that the text on the pie chart is as expected.");
        svgText = getSVGText();

        Assert.assertTrue("SVG did not contain expected title: '" + PIE_CHART_CATEGORY + "'", svgText.contains(PIE_CHART_CATEGORY.replace(" ", "")));
        strTemp = "Sum of " + PIE_CHART_MEASURE;
        Assert.assertTrue("SVG did not contain expected footer: '" + strTemp + "'", svgText.contains(strTemp.replace(" ", "")));
        Assert.assertTrue("SVG did not contain query text 'Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting'", svgText.contains("Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting"));

        log("The percentage values displayed should have changed when a measure is applied.");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 6 '%' in the svg, found " + percentCount, 9, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%5%5%7%5%6%5%5%I'", svgText.contains("d25%9%5%5%7%5%6%5%5%I"));

        log("Now change the chart layout, also validate that the layout dialog is pre-populated as expected.");
        clickButton("Chart Layout", 0);

        pieChartLookAndFeel = new LookAndFeelPieChart(getDriver());
        strTemp = pieChartLookAndFeel.getSubTitle();
        Assert.assertTrue("Value in Subtitle text box not as expected. Expected '" + PIE_CHART_CATEGORY + "'", strTemp.equals(PIE_CHART_CATEGORY));
        strTemp = pieChartLookAndFeel.getFooter();
        Assert.assertTrue("Value in Footer text box not as expected. Expected 'Sum of " + PIE_CHART_MEASURE + "'", strTemp.equals("Sum of " + PIE_CHART_MEASURE));

        log("Remove the percentages, and change the gradient.");
        if(pieChartLookAndFeel.showPercentagesChecked())
            pieChartLookAndFeel.clickShowPercentages();

        // Changing gradient just to make sure no errors are generated. Didn't have time to validate that color had change in the pie chart.
        pieChartLookAndFeel.setGradientColor(COLOR_RED);

        pieChartLookAndFeel.clickApply();

        sleep(3000);  // TODO Is there a better trigger?

        svgText = getSVGText();
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should be no '%' values in the svg, found " + percentCount, 0, percentCount);

        clickButton("Chart Layout", 0);

        log("Now add percentages back and change the limit when they are visible.");
        pieChartLookAndFeel = new LookAndFeelPieChart(getDriver());

        if(!pieChartLookAndFeel.showPercentagesChecked())
            pieChartLookAndFeel.clickShowPercentages();

        pieChartLookAndFeel.setHidePercentageWhen("7");

        // Changing gradient, the radii and colors just to make sure no errors are generated.
        // It would be possible to easily validate the color of the text but validating the radii and gradients would need more work.
        pieChartLookAndFeel.setGradientColor(COLOR_WHITE)
                .setInnerRadiusPercentage(50)
                .setOuterRadiusPercentage(25)
                .setGradientColor(COLOR_BLUE)
                .setGradientPercentage(50)
                .setPercentagesColor(COLOR_BLACK)
                .clickApply();

        sleep(3000);  // TODO Is there a better trigger?

        log("Just a quick change.");

        svgText = getSVGText();
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 3 '%' in the svg, found " + percentCount, 3, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%7%I'", svgText.contains("d25%9%7%I"));

        log("Ok last bit of changing for the Pie Chart.");
        clickButton("Chart Layout", 0);

        pieChartLookAndFeel = new LookAndFeelPieChart(getDriver());

        pieChartLookAndFeel.setPlotTitle(PLOT_TITLE)
                .setInnerRadiusPercentage(0)
                .setOuterRadiusPercentage(75)
                .setPlotWidth("500")
                .setPlotHeight("500")
                .setColorPalette(LookAndFeelPieChart.ColorPalette.Alternate)
                .clickApply();

        sleep(3000);  // Is there a better trigger?

        svgText = getSVGText();
        log("Last scgText: " + svgText);

        // There is one extra % because of the TRICKY_CHARACTERS used in the title.
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 4 '%' in the svg, found " + percentCount, 4, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%7%I'", svgText.contains("d25%9%7%I"));
        Assert.assertTrue("Expected Title '" + PLOT_TITLE + "' wasn't present.", svgText.contains(PLOT_TITLE.replace(" ", "")));
        String svgWidth = getAttribute(Locator.css("svg"), "width");
        String svgHeight= getAttribute(Locator.css("svg"), "height");
        Assert.assertEquals("Width of svg not expected.", "500", svgWidth);
        Assert.assertEquals("Height of svg not expected.", "500", svgHeight);

        savePlot(PIE_CHART_SAVE_NAME, PIE_CHART_SAVE_DESC);

    }

    @LogMethod
    private void doColumnChartClickThrough()
    {
        final String DATA_SOURCE_1 = "DEM-1: Demographics";
        final String COL_NAME_PIE = "DEMsex";
        final String COL_TEXT_PIE = "2.What is your sex?";

        ColumnChartRegion plotRegion;
        DataRegionTable dataRegionTable;
        ChartTypeDialog chartTypeDialog;
        LookAndFeelPieChart pieChartLookAndFeel;
        int expectedPlotCount = 0;
        String strTemp;

        log("Go to the '" + DATA_SOURCE_1 + "' grid to create a pie chart from a column.");

        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());

        log("Create a pie chart.");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        expectedPlotCount++;

        log("Refresh the reference to the plotRegion object and get a count of the plots.");
        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Click on the pie chart and validate that we are redirected to the plot wizard.");
        clickAndWait(plotRegion.getPlots().get(0), WAIT_FOR_PAGE);

        URL currentUrl = getURL();
        log("Current url path: " + currentUrl.getPath());
        Assert.assertTrue("It doesn't look like we navigated to the expected page.", currentUrl.getPath().toLowerCase().contains("visualization-genericchartwizard.view"));

        waitForElement(Locator.css("svg"));

        chartTypeDialog = clickChartTypeButton();

        strTemp = chartTypeDialog.getCategories();
        Assert.assertTrue("Categories field did not contain the expected value. Expected '" + COL_TEXT_PIE + "'. Found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        strTemp = chartTypeDialog.getMeasure();
        Assert.assertEquals("Measure field was not empty. Found '" + strTemp + "'", 0, strTemp.trim().length());

        chartTypeDialog.clickCancel();

        clickButton("Chart Layout", 0);
        pieChartLookAndFeel = new LookAndFeelPieChart(getDriver());

        strTemp = pieChartLookAndFeel.getPlotTitle();
        Assert.assertTrue("Value for plot title not as expected. Expected '" + DATA_SOURCE_1 + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(DATA_SOURCE_1.toLowerCase()));

        strTemp = pieChartLookAndFeel.getSubTitle();
        Assert.assertTrue("Value for plot sub title not as expected. Expected '" + COL_TEXT_PIE + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        pieChartLookAndFeel.clickCancel();
    }

    @LogMethod
    private void doExportOfPieChart()
    {
        final String EXPORTED_SCRIPT_CHECK_TYPE = "\"renderType\":\"pie_chart\"";
        final String EXPORTED_SCRIPT_CHECK_XAXIS = PIE_CHART_CATEGORY;
        final String EXPORTED_SCRIPT_CHECK_YAXIS = "Sum of " + PIE_CHART_MEASURE;

        log("Validate that export of the pie chart works.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(PIE_CHART_SAVE_NAME));
        clickAndWait(Locator.linkWithText(PIE_CHART_SAVE_NAME), WAIT_FOR_PAGE);

        waitForElement(Locator.css("svg"));

        log("Export as PDF");
        doAndWaitForDownload(() -> _ext4Helper.clickExt4MenuButton(false, Ext4Helper.Locators.ext4Button("Export"), false, "PDF"), 1);

        log("Export as PNG");
        doAndWaitForDownload(() -> _ext4Helper.clickExt4MenuButton(false, Ext4Helper.Locators.ext4Button("Export"), false, "PNG"), 1);

        log("Export to script.");
        Locator dlg = Locator.xpath("//div").withClass("chart-wizard-dialog").notHidden().withDescendant(Locator.xpath("//div").withClass("title-panel").withDescendant(Locator.xpath("//div")).withText("Export script"));
        _ext4Helper.clickExt4MenuButton(false, Ext4Helper.Locators.ext4Button("Export"), false, "Script");
        waitFor(() -> isElementPresent(dlg),
                "Ext Dialog with title '" + "Export script" + "' did not appear after " + WAIT_FOR_JAVASCRIPT + "ms", WAIT_FOR_JAVASCRIPT);
        String exportScript = _extHelper.getCodeMirrorValue("export-script-textarea");
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

        log("Validate that the script is as expected.");
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_TYPE + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_TYPE.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_XAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_XAXIS.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_YAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_YAXIS.toLowerCase()));

    }

}