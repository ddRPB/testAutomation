/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;

import java.util.List;

public class LabKeyPage<EC extends LabKeyPage.ElementCache> extends WebDriverWrapper
{
    @Deprecated
    protected BaseWebDriverTest _test;
    private WrapsDriver _wrapsDriver;
    private EC _elementCache;

    public LabKeyPage(WrapsDriver test)
    {
        if (test instanceof BaseWebDriverTest)
            _test = (BaseWebDriverTest)test;
        _wrapsDriver = test;
        waitForPage();
    }

    public LabKeyPage(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _wrapsDriver.getWrappedDriver();
    }

    protected void waitForPage() {}

    public static class Locators extends org.labkey.test.Locators // TODO: Stop extending Locators (leaving for feature branches)
    {
        public static Locator.XPathLocator bodyPanel = Locator.id("bodypanel");
    }

    protected final void clearCache()
    {
        _elementCache = null;
    }

    protected EC elementCache()
    {
        if (null == _elementCache)
            _elementCache = newElementCache();
        return _elementCache;
    }

    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    public class ElementCache implements SearchContext
    {
        @Override
        public List<WebElement> findElements(By by)
        {
            return getDriver().findElements(by);
        }

        @Override
        public WebElement findElement(By by)
        {
            return getDriver().findElement(by);
        }

        public WebElement headerBlock = Locator.css("body > div.labkey-main > header-block").findWhenNeeded(this);
        public WebElement bodyBlock = Locator.css("body > div.labkey-main > body-block").findWhenNeeded(this);
        public WebElement footerBlock = Locator.css("body > div.labkey-main > footer-block").findWhenNeeded(this);
    }
}
