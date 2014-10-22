package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * Created by davebradlee on 10/21/14.
 */
@Category({DailyA.class})
public class StudyDatasetReloadTest extends StudyBaseTest
{
    protected String getProjectName()
    {
        return "StudyDatasetReloadProject";
    }

    protected String getFolderName()
    {
        return "Study Dataset Reload";
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        initializeFolder();
        initializePipeline(null);
        clickFolder(getFolderName());

        log("Import study with Demographics bit set on dataset");
        importFolderFromZip(new File(getPipelinePath(), "StudyWithDemoBit.folder.zip"));
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        reloadStudyFromZip(new File(getPipelinePath(), "StudyWithoutDemoBit.folder.zip"));

        // Check changes
        clickFolder(getFolderName());
        clickAndWait(Locator.linkContainingText("DEM: Demographics"));
        clickButton("Manage Dataset");
        assertTextPresent("StaffCode", "DoubleNum");
        assertElementPresent(Locator.tagWithText("td", "Staff Code").append("/../td/input[last()][@checked]"));     // MV
        assertElementPresent(Locator.tagWithText("td", "VisitDay").append("/../td/input[last()][not(@checked)]"));  // MV

        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        assertNotChecked(Locator.checkboxByName("demographicData"));

        goToManageStudy();
        clickButton("Reload Study");
        setFormElement(Locator.name("folderZip"), new File(getPipelinePath(), "StudyWithDemoBit.folder.zip"));
        clickButton("Reload Study From Local Zip Archive");
        waitForPipelineJobsToComplete(3, "Study Reload", false);

        // Check changes
        clickFolder(getFolderName());
        clickAndWait(Locator.linkContainingText("DEM-1: Demographics"));
        clickButton("Manage Dataset");
        assertTextNotPresent("StaffCode", "DoubleNum");
        assertElementPresent(Locator.tagWithText("td", "VisitDay").append("/../td/input[last()][@checked]"));  // MV

        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        assertChecked(Locator.checkboxByName("demographicData"));
    }


}
