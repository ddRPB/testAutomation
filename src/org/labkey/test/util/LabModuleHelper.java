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
package org.labkey.test.util;

import junit.framework.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/28/12
 * Time: 8:56 PM
 */
public class LabModuleHelper
{
    private BaseSeleniumWebTest _test;

    public LabModuleHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    public void defineAssay(String provider, String label)
    {
        _test.log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        _test.goToProjectHome();

        //copied from old test
        _test.goToManageAssays();
        _test.clickNavButton("New Assay Design");
        _test.checkRadioButton("providerName", provider);
        _test.clickNavButton("Next");
        _test.waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), _test.WAIT_FOR_JAVASCRIPT);
        _test.getWrapper().type("//input[@id='AssayDesignerName']", label);

        _test.sleep(1000);
        _test.clickNavButton("Save", 0);
        _test.waitForText("Save successful.", 20000);
    }

    public static Locator getNavPanelItem(String label, String itemText)
    {
        //NOTE: this should return only visible items
        return Locator.xpath("//span[text() = '" + label + "']/../../../div[not(contains(@style, 'display: none'))]//span[text() = '" + itemText + "']");
    }

    public void clickNavPanelItem(String label, String itemText)
    {
        Locator l = getNavPanelItem(label, itemText);
        _test.waitForElement(l);
        _test.click(l);
    }

    public static Locator getNavPanelRow(String label)
    {
        return Locator.xpath("//div[descendant::span[text() = '" + label + "']]");
    }

    public void goToLabHome()
    {
        _test.goToProjectHome();
        _test.waitForText("Types of Data:");
    }

    public void verifyNavPanelRowItemPresent(String label)
    {
        _test.log("Verifying NavPanel row present with label: " + label);
        Assert.assertTrue("Row missing: " + label, _test.isElementPresent(getNavPanelRow(label)));
    }

    public static Locator webpartTitle(String title)
    {
        return Locator.xpath("//span[contains(@class, 'labkey-wp-title-text') and text() = '" + title + "']");
    }
}
