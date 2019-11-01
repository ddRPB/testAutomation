/*
 * Copyright (c) 2010-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineConfig;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public class RReportHelper
{
    public static final String RDOCKER = "RDocker";

    public enum ReportOption {
        shareReport("Make this report available to all users", null, true),
        showSourceTab("Show source tab to all users", null, true),
        runInPipeline("Run this report in the background as a pipeline job", null, true),
        knitrNone("None" + Locator.NBSP, "Knitr Options", false),
        knitrHtml("Html" + Locator.NBSP, "Knitr Options", false),
        knitrMarkdown("Markdown" + Locator.NBSP, "Knitr Options", false);

        public String _label;
        public boolean _isCheckbox;
        public String _fieldSet;

        ReportOption(String label, String fieldSet, boolean isCheckbox)
        {
            _label = label;
            _fieldSet = fieldSet;
            _isCheckbox = isCheckbox;
        }
    }

    protected final BaseWebDriverTest _test;

    public RReportHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    private static File rExecutable = null;
    private static File rScriptExecutable = null;

    private static final String localEngineName = "R Scripting Engine";
    public static final String R_DOCKER_SCRIPTING_ENGINE = "R Docker Scripting Engine";
    private static final String REMOTE_R_SERVE ="Remote R Scripting Engine";

    private static final String INSTALL_RLABKEY = "install.packages(\"Rlabkey\", repos=\"http://cran.r-project.org\")";
    private static final String INSTALL_LOCAL_RLABKEY = "install.packages(\"%s\", repos=NULL)";

    public File getRLibraryPath()
    {
        String libPath = System.getenv("R_LIBS_USER");
        if (libPath != null)
            return new File(libPath);

        // default to sampledata/rlabkey path
        return new File(TestFileUtils.getLabKeyRoot(), "/sampledata/rlabkey");
    }

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    public boolean executeScript(String script, String verify)
    {
        return executeScript(script, verify, false);
    }

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    @LogMethod (quiet = true)
    public boolean executeScript(String script, String expectedLines, boolean failOnError)
    {
        // running a saved script
        _test.waitAndClick(Ext4Helper.Locators.tab("Source"));

        _test.setCodeEditorValue("script-report-editor", script);
        _test._ext4Helper.clickTabContainingText("Report");
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 5);

        Locator l = Locator.xpath("//div[@class='reportView']//pre");
        _test.waitForElement(l);
        String html = _test.getText(l).replaceAll(" +", " ");

        return checkScriptOutput(html, expectedLines, failOnError);
    }

    public boolean executeScriptDirectly(String script, String expectedLines) throws IOException
    {
        return executeScriptDirectly(script, expectedLines, false);
    }

    public boolean executeScriptDirectly(String script, String expectedLines, boolean failOnError) throws IOException
    {
        String scriptOutput = getRScriptOutput(script);

        return checkScriptOutput(scriptOutput, expectedLines, failOnError);
    }

    private boolean checkScriptOutput(String scriptOutput, String expectedLines, boolean failOnError)
    {
        if (failOnError && doesScriptProduceError(scriptOutput))
            return false;

        return doesScriptProduceOutput(expectedLines, scriptOutput);
    }

    private boolean doesScriptProduceOutput(String expectedLines, String scriptOutput)
    {
        if (!StringUtils.isEmpty(expectedLines))
        {
            // split string on newlines to make the comparison more reliable
            String[] parts = expectedLines.split("\n");

            for (String part : parts)
            {
                if (!scriptOutput.contains(part.trim()))
                {
                    TestLogger.error("Error: could not find expected text: " + part + ".\nfrom value:\n" + scriptOutput);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doesScriptProduceError(String scriptOutput)
    {
        if (scriptOutput.contains("javax.script.ScriptException"))
        {
            TestLogger.error("Error: the script failed with an error:\n" + scriptOutput);
            return true;
        }

        return false;
    }

    /**
     * Installs the latest version of the Rlabkey package that is either built or checked into the ../remoteapi/r/latest
     * directory. In order to install any dependent packages: (httr, rjson), an installation from the CRAN repository
     * is performed first, then if the local flag has been set, the local package is installed over the top.
     */
    @LogMethod
    public void installRlabkey(boolean local)
    {
        if (executeScript(INSTALL_RLABKEY, null, true))
        {
            if (local)
            {
                File rPackage = new File(TestFileUtils.getLabKeyRoot(), "/sampledata/rlabkey/Rlabkey.zip");

                if (!rPackage.exists())
                    fail("Unable to locate the local Rlabkey package: " + rPackage.getName());

                String cmd = String.format(INSTALL_LOCAL_RLABKEY, rPackage.getAbsolutePath());
                cmd = cmd.replaceAll("\\\\", "/");
                if (!executeScript(cmd, null, true))
                    fail("Unable to install the local Rlabkey package.");
            }
        }
        else
            fail("Unable to install the base Rlabkey package and dependencies.");
    }

    public String ensureRConfig()
    {
        return ensureRConfig(false);
    }

    /**
     * Ensure that an R script engine is configured
     * @param useDocker Specify whether the Dockerized R engine should be used
     * @return R version (e.g. 3.4.2); or "RDocker" for dockerized R engine
     */
    @LogMethod
    public String ensureRConfig(boolean useDocker)
    {
        _test.ensureAdminMode();

        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);

        _test.log("Check if R already is configured");

        String rVersion = null;
        if (useDocker)
        {
            rVersion = RDOCKER;
            if (!scripts.isEnginePresent(R_DOCKER_SCRIPTING_ENGINE))
                scripts.addEngineWithDefaults(EngineType.R_DOCKER);

            scripts.setSiteDefault(R_DOCKER_SCRIPTING_ENGINE);
        }
        else
        {
            _test.refresh(); // Avoid menu alignment issue on TeamCity

            if (scripts.isEnginePresent(localEngineName))
            {
                if (!TestProperties.isTestRunningOnTeamCity())
                {
                    scripts.editEngine(localEngineName);
                    rExecutable = new File(_test.getFormElement(Locator.id("editEngine_exePath-inputEl")));
                    TestLogger.log(localEngineName + " is already configured using: " + rExecutable.getAbsolutePath());
                    rVersion = getRVersion(rExecutable);
                    _test.clickButton("Cancel", 0);
                    scripts.setSiteDefault(localEngineName);
                    return rVersion;
                }
                else // Reset R scripting engine on TeamCity
                {
                    scripts.deleteAllREngines();
                    _test.refresh(); // Avoid menu alignment issue on TeamCity
                }
            }

            rVersion = getRVersion(getRExecutable());

            EngineConfig config = new EngineConfig(getRExecutable());
            config.setVersion(rVersion);
            scripts.addEngine(EngineType.R, config);
            scripts.setSiteDefault(localEngineName);
        }
        return rVersion;
    }

    public void configureRemoteRserve(String reports_temp,String data)
    {
        String username = "rserve";
        String password = "rserve";
        ConfigureReportsAndScriptsPage.RServeEngineConfig config = new ConfigureReportsAndScriptsPage.RServeEngineConfig(username,password,reports_temp,data);
        config.setMachine("127.0.0.1");
        config.setPortNumber("6311");

        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);
        if(!scripts.isEnginePresent(REMOTE_R_SERVE))
            scripts.addEngine(EngineType.REMOTE_R, config);

        scripts.setSiteDefault(REMOTE_R_SERVE);
    }

    public void ensureFolderREngine(String engineName)
    {
        ensureFolderREngine(engineName, engineName);
    }

    public void ensureFolderREngine(String reportEngineName, String pipelineEngineName)
    {
        _test.goToFolderManagement().goToRConfigTab();
        Locator inheritRadio = Locator.radioButtonByNameAndValue("overrideDefault", "parent");
        Locator overrideRadio = Locator.radioButtonByNameAndValue("overrideDefault", "override");

        if (_test.isChecked(inheritRadio))
            _test.checkRadioButton(overrideRadio);

        assertTrue(reportEngineName + " engine does not exist.", _test.isElementPresent(Locator.tagWithText("option", reportEngineName)));
        assertTrue(pipelineEngineName + " engine does not exist.", _test.isElementPresent(Locator.tagWithText("option", pipelineEngineName)));

        Locator overrideReportEngineSelect = Locator.name("reportEngine");
        Locator overridePipelineEngineSelect = Locator.name("pipelineEngine");
        boolean changed = false;
        if (reportEngineName.equals(_test.getSelectedOptionText(overrideReportEngineSelect).trim()))
        {
            _test.log(reportEngineName + "engine is already selected as default Report engine for folder");
        }
        else
        {
            changed = true;
            _test.log("Change folder's Report engine to " + reportEngineName);
            _test.selectOptionByText(overrideReportEngineSelect, reportEngineName);
        }

        if (pipelineEngineName.equals(_test.getSelectedOptionText(overridePipelineEngineSelect).trim()))
        {
            _test.log(pipelineEngineName + "engine is already selected as default Pipeline engine for folder");
        }
        else
        {
            changed = true;
            _test.log("Change folder's Pipeline engine to " + pipelineEngineName);
            _test.selectOptionByText(overridePipelineEngineSelect, pipelineEngineName);
        }

        if (changed)
        {
            _test.clickButton("Save", "Override Default R Configuration");
            _test.sleep(1000);
            _test.clickButton("Yes");
        }
    }

    public void resetFolderREngine()
    {
        _test.goToFolderManagement().goToRConfigTab();
        Locator inheritRadio = Locator.radioButtonByNameAndValue("overrideDefault", "parent");

        if (_test.isChecked(inheritRadio))
            return;

        _test.log("Remove folder's engine override.");
        _test.checkRadioButton(inheritRadio);

        _test.clickButton("Save", "Override Default R Configuration");
        _test.clickButton("Yes");
    }

    public void setPandocEnabled(Boolean enabled)
    {
        _test.goToProjectHome();
        _test.ensureAdminMode();

        _test.log("Check if R already is configured");
        _test.goToAdminConsole().clickViewsAndScripting();
        ConfigureReportsAndScriptsPage scripts = new ConfigureReportsAndScriptsPage(_test);

        String defaultScriptName = "R Scripting Engine";
        assertTrue("R Engine not setup", scripts.isEnginePresentForLanguage("R"));

        scripts.editEngine(defaultScriptName);

        Checkbox enabledCheckbox = Checkbox.Ext4Checkbox().withLabel("Use pandoc & rmarkdown:").find(_test.getDriver());
        if(enabled)
            enabledCheckbox.check();
        else
            enabledCheckbox.uncheck();

        _test.clickButton("Submit", 0);
        _test.waitForElementToDisappear(ConfigureReportsAndScriptsPage.Locators.editEngineWindow);
    }


    public File getRExecutable()
    {
        if (rExecutable != null)
            return rExecutable;

        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            _test.log("R_HOME is set to: " + rHome + " searching for the R application");
            File rHomeDir = new File(rHome);
            FileFilter rFilenameFilter =
                    file -> ("r.exe".equalsIgnoreCase(file.getName()) || "r".equalsIgnoreCase(file.getName()))
                            && file.canExecute();
            File[] files = rHomeDir.listFiles(rFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(rHome, "bin").listFiles(rFilenameFilter);
            }

            if (files != null && files.length > 0)
            {
                if (files.length > 1)
                {
                    TestLogger.log("WARNING: Found multiple R executables:");
                    for (File file : files)
                    {
                        _test.log("\t" + file.getAbsolutePath());
                    }
                }
                rExecutable = files[0];
                return rExecutable;
            }
        }

        TestLogger.error("Environment info: " + System.getenv());

        if (null == rHome)
        {
            TestLogger.error("");   // Blank line helps make the following message more readable
            TestLogger.error("R_HOME environment variable is not set.  Set R_HOME to your R bin directory to enable automatic configuration.");
        }
        throw new IllegalStateException("R is not configured on this system. Failed R tests.");
    }

    private File getRScriptExecutable()
    {
        if (rScriptExecutable == null)
            rScriptExecutable = new File(getRExecutable().getParentFile(), "rscript");
        return rScriptExecutable;
    }

    private String getRVersion(File r)
    {
        String versionOutput = "";
        try
        {
            versionOutput = new ProcessHelper(r, "--version").getProcessOutput().trim();

            Pattern versionPattern = Pattern.compile("R version ([1-9]\\.\\d+\\.\\d)");
            Matcher matcher = versionPattern.matcher(versionOutput);
            matcher.find();
            String versionNumber = matcher.group(1);

            _test.log("R --version > " + versionNumber);

            return versionNumber;
        }
        catch(IOException ex)
        {
            if (versionOutput.length() > 0)
                _test.log("R --version > " + versionOutput);
            throw new RuntimeException("Unable to determine R version: " + r.getAbsolutePath(), ex);
        }
    }

    public String getRScriptOutput(String scriptContents) throws IOException
    {
        return new ProcessHelper(getRScriptExecutable(), "-e", scriptContents).getProcessOutput().trim();
    }

    public void saveReport(String name, boolean isSaveAs, int wait)
    {
        WebElement saveButton = Ext4Helper.Locators.ext4Button(isSaveAs ? "Save As" : "Save").findElement(_test.getDriver());
        _test.scrollIntoView(saveButton, true);
        _test.clickAndWait(saveButton, wait);
        if (null != name)
        {
            saveReportWithName(name, isSaveAs);
        }
    }

    /**
     * Precondition: on save popup window
     */
    public void saveReportWithName(String name, boolean isSaveAs)
    {
        saveReportWithName(name, isSaveAs, false);
    }

    public void saveReportWithName(String name, boolean isSaveAs, boolean isExternal)
    {
        String windowTitle = isExternal ? "Create New Report" : isSaveAs ? "Save Report As" : "Save Report";
        Locator locator = Ext4Helper.Locators.window(windowTitle).append(Locator.xpath("//input[contains(@class, 'x4-form-field')]"));
        _test.waitForElement(locator);
        if (_test.isElementPresent(locator))
        {
            _test.setFormElement(locator, name);
            Window window = new Window(windowTitle, _test.getWrappedDriver());
            if (isExternal)
                window.clickButton("OK", 0);
            else
                window.clickButton("OK");
        }
    }

    public void saveReport(String name)
    {
        saveReport(name, false, 0);
    }

    public void saveAsReport(String name)
    {
        saveReport(name, true, 0);
    }

    public void selectOption(ReportOption option)
    {
        _selectOption(option, true);
    }

    public void clearOption(ReportOption option)
    {
        _selectOption(option, false);
    }

    private void _selectOption(ReportOption option, boolean checked)
    {
        ensureFieldSetExpanded(option._fieldSet);
        Checkbox checkbox;
        if (option._isCheckbox)
        {
            checkbox = Ext4Checkbox().withLabel(option._label).waitFor(_test.getDriver());
        }
        else
        {
            if (!checked)
                throw new IllegalArgumentException("Can't uncheck a radio button");
            checkbox = RadioButton().withLabel(option._label).waitFor(_test.getDriver());
        }
        checkbox.set(checked);
        _test.waitFor(() -> checkbox.isChecked() == checked, 1000);
    }

    public void clickReportTab()
    {
        _test.waitAndClick(Ext4Helper.Locators.tab("Report"));
        _test.waitForElement(Locator.tagWithClass("div", "reportView").notHidden().withPredicate("not(ancestor-or-self::*[contains(@class,'mask')])"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickSourceTab()
    {
        _test.waitAndClick(Ext4Helper.Locators.tab("Source"));
        _test.waitForElement(Locator.tagWithClass("div", "reportSource").notHidden(), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void ensureFieldSetExpanded(String name)
    {
        if (name != null)
        {
            Locator fieldSet = Locator.xpath("//fieldset").withClass("x4-fieldset-collapsed").withDescendant(Locator.xpath("//div").withClass("x4-fieldset-header-text").containing(name)).append("//div/img");

            if (_test.isElementPresent(fieldSet))
            {
                _test.click(fieldSet);
            }
        }
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     */
    public void createRReport(String name)
    {
        createRReport(name, false);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     * @param shareView should this view be available to all users
     */
    public void createRReport(String name, boolean shareView)
    {
        _test.waitForText(("Reports"));
        DataRegion(_test.getDriver()).find().goToReport("Create R Report");

        if (shareView)
            selectOption(RReportHelper.ReportOption.shareReport);

        saveReport(name);
    }

    /**
     * pre-condition: at folder that the report is to be created from
     * @param reportName
     * @param reportSource
     * @param shareView
     * @return labkey-output content of report
     */
    public String createAndRunRReport(String reportName, String reportSource, boolean shareView)
    {
        _test.goToManageViews();

        BootstrapMenu.find(_test.getDriver(), "Add Report")
                .clickSubMenu(true, "R Report");
        RReportHelper rReportHelper = new RReportHelper(_test);

        _test.setCodeEditorValue("script-report-editor", reportSource);

        if (shareView)
            selectOption(RReportHelper.ReportOption.shareReport);

        rReportHelper.saveReport(reportName);
        _test.waitForText(reportName);
        _test.log("Report created: " + reportName);

        Locator reportOutput = Locator.tagWithClass("table", "labkey-output");
        _test.waitForElement(reportOutput);
        return _test.getText(reportOutput);
    }


    /**
     * pre-conditions: at report's Report tab
     */
    public WebElement assertKnitrReportContents(Locator[] reportContains, String[] reportNotContains)
    {
        WebElement reportDiv = _test.waitForElement(Locator.css("div.reportView > div.labkey-knitr"), BaseWebDriverTest.WAIT_FOR_PAGE);

        for (Locator contains : reportContains)
        {
            contains.waitForElement(reportDiv, BaseWebDriverTest.WAIT_FOR_PAGE);
        }

        if (reportNotContains != null)
        {
            String reportText = reportDiv.getText();

            for (String text : reportNotContains)
            {
                assertFalse("Report contained undesired text : " + text, reportText.contains(text));
            }
        }


        return reportDiv;
    }

    /**
     * @deprecated Remove once RStudio branch is merged (May 2018)
     */
    @Deprecated
    public boolean isReportSourceLineCountMatch(int expectedLineCount)
    {
        Locator lastLineLoc = Locator.css(".CodeMirror-code > div:last-of-type .CodeMirror-linenumber");
        WebElement lastLine = lastLineLoc.findElement(_test.getDriver());
        int lineCount = Integer.parseInt(lastLine.getText());

        if (lineCount < expectedLineCount)
        {
            WebElement codeEditorDiv = Locator.css(".CodeMirror-scroll").findElement(_test.getDriver());
            _test.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", codeEditorDiv);
            _test.shortWait().until(ExpectedConditions.stalenessOf(lastLine));
            lastLine = lastLineLoc.findElement(_test.getDriver());
            lineCount = Integer.parseInt(lastLine.getText());
        }

        return lineCount == expectedLineCount;
    }
}
