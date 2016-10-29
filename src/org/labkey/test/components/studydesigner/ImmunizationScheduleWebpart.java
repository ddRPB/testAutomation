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
package org.labkey.test.components.studydesigner;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ImmunizationScheduleWebpart extends BodyWebPart<ImmunizationScheduleWebpart.ElementCache>
{
    public ImmunizationScheduleWebpart(WebDriver driver)
    {
        super(driver, "Immunization Schedule");
    }

    @Override
    protected void waitForReady()
    {
        getWrapper().waitForElement(elementCache().getCohortTableLocator());
    }

    public boolean isEmpty()
    {
        return elementCache().isCohortTableEmpty();
    }

    public int getCohortRowCount()
    {
        return elementCache().getCohortRowCount();
    }

    public String getCohortCellDisplayValue(String column, int rowIndex)
    {
        return elementCache().getCohortCell(column, rowIndex).getText();
    }

    public void manage()
    {
        elementCache().manageLink.click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends WebPart.ElementCache
    {
        private int wait = BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
        private Locator.XPathLocator tableOuterLoc = Locator.tagWithClass("table", "outer");
        private Locator.XPathLocator tableRowLoc = Locator.tagWithClass("tr", "row-outer");
        private Locator.XPathLocator cellDisplayLoc = Locator.tagWithClass("td", "cell-display");
        private Locator.XPathLocator emptyLoc = Locator.tagWithClassContaining("td", "empty").withText("No data to show.");
        private Locator.XPathLocator manageLoc = Locator.linkWithText("Manage Treatments");
        private Locator.XPathLocator cohortsLoc = Locator.tagWithClass("div", "immunization-schedule-cohorts");

        Locator.XPathLocator getCohortTableLocator()
        {
            return cohortsLoc.append(elementCache().tableOuterLoc);
        }

        WebElement cohortsTable = cohortsLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement manageLink = manageLoc.findWhenNeeded(this).withTimeout(wait);

        WebElement getCohortCell(String column, int rowIndex)
        {
            Locator.XPathLocator rowLoc = elementCache().cohortsLoc.append(elementCache().tableRowLoc.withAttribute("outer-index", rowIndex+""));
            Locator.XPathLocator cellLoc = rowLoc.append(elementCache().cellDisplayLoc.withAttribute("data-index", column));
            return cellLoc.findElement(getDriver());
        }

        int getCohortRowCount()
        {
            return cohortsLoc.append(elementCache().tableRowLoc).findElements(getDriver()).size();
        }

        boolean isCohortTableEmpty()
        {
            cohortsTable.findElement(By.xpath(elementCache().tableOuterLoc.getLoc()));
            return getWrapper().isElementPresent(elementCache().cohortsLoc.append(elementCache().tableOuterLoc).append(elementCache().emptyLoc));
        }
    }
}