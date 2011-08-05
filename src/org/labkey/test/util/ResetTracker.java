package org.labkey.test.util;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/5/11
 * Time: 11:13 AM
 * To change this template use File | Settings | File Templates.
 */

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;


/**
 * This class tracks whether or not a page has been updated.
 * currently, it does so by populating the search bar and verifying
 * that it is unchanged at a specified point.  If this proves unsatisfactory
 * in the future, we could consider doing something with javascript.
 */


public class ResetTracker
{
    BaseSeleniumWebTest test = null;
    protected String searchBoxId = "query";
    protected String searchBoxEntry =  null;

    public ResetTracker(BaseSeleniumWebTest test)
    {
        this.test=test;
        test.addWebPart("Search");
    }

    protected int resetTrackingCounter = 0;

    public void startTrackingRefresh()
    {
        searchBoxEntry = BaseSeleniumWebTest.TRICKY_CHARACTERS + "this should not change" + resetTrackingCounter++;
        test.setFormElement(Locator.id(searchBoxId), searchBoxEntry);
    }

    public void stopTrackingRefresh()
    {
        searchBoxEntry = null;
        test.setFormElement(Locator.id(searchBoxId), searchBoxEntry);
    }

    public boolean wasPageRefreshed()
    {
        if(searchBoxEntry==null)
        {
            test.fail("search box was not iniitalized to wait for refresh");
        }
        String searchBoxContents = test.getFormElement(Locator.id(searchBoxId));
        return !searchBoxContents.equals(searchBoxEntry);
    }

    public void assertWasNotRefreshed()
    {
        test.assertFalse("Page was unexpectedly refreshed", wasPageRefreshed());
    }

}
