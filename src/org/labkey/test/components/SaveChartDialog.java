package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class SaveChartDialog<EC extends Component.ElementCache> extends Component<EC>
{
    private  final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'save-chart-panel')]";

    protected WebElement _saveChartDialog;
    protected BaseWebDriverTest _test;

    public SaveChartDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _saveChartDialog;
    }

    public boolean isDialogVisible()
    {
        return elements().dialog.isDisplayed();
    }

    public void waitForDialog()
    {
        waitForDialog(false);
    }

    public void waitForDialog(boolean saveAs)
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//div[text()='" + (saveAs ? "Save as" : "Save") + "']"));
    }

    public void setReportName(String name)
    {
        _test.setFormElement(elements().reportName, name);
    }

    public void setReportDescription(String description)
    {
        _test.setFormElement(elements().reportDescription, description);
    }

    public void clickCancel()
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("Cancel", 0);
    }

    public void clickSave()
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("Save", 0);
    }

    public Elements elements()
    {
        return new Elements();
    }

    class Elements extends ElementCache
    {
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement reportName = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//td//label[text()='Report Name:']/parent::td/following-sibling::td//input"), _test.getDriver());
        public WebElement reportDescription = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//td//label[text()='Report Description:']/parent::td/following-sibling::td//textarea"), _test.getDriver());
    }
}