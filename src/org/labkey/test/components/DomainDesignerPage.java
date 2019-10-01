package org.labkey.test.components;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.UnsavedChangesModalDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class DomainDesignerPage extends LabKeyPage<DomainDesignerPage.ElementCache>
{
    public DomainDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static DomainDesignerPage beginAt(WebDriverWrapper driver, String containerPath, String schema, String query)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "domainDesigner", Maps.of("schemaName", schema, "queryName", query)));
        return new DomainDesignerPage(driver.getDriver());
    }

    public DomainDesignerPage clickSave()
    {
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveButton));
        String currentURL = getDriver().getCurrentUrl();
        elementCache().saveButton.click();

        waitFor(()-> !getDriver().getCurrentUrl().equals(currentURL)||
                anyAlert() != null,
                "expected either navigation or an alert with error or info to appear", 1000);

        if (isAlertVisible())
        {
            String msg = waitForAnyAlert();
            log("Clicking save.  Waited until alert with message [" + msg + "] appeared");
        }

        return this;
    }

    public UnsavedChangesModalDialog clickCancel()
    {
        elementCache().cancelBtn.click();
        UnsavedChangesModalDialog unsavedChangesModal = new UnsavedChangesModalDialog(
                new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Keep unsaved changes?"),
                getDriver());
        return unsavedChangesModal;
    }

    public DomainDesignerPage clickCancelAndDiscardChanges()
    {
        clickCancel().discardChanges();
        return new DomainDesignerPage(getDriver());
    }

    public boolean isAlertVisible()
    {
        return Locators.alert.findOptionalElement(getDriver()).map(WebElement::isDisplayed).orElse(false);
    }

    public WebElement saveButton()
    {
        return elementCache().saveButton;
    }

    public WebElement saveAndFinishButton()
    {
        return elementCache().saveButton;
    }

    public DomainFormPanel fieldProperties()
    {
        return elementCache().firstDomainFormPanel;
    }
    public DomainFormPanel fieldProperties(String queryName)
    {
        return elementCache().domainFormPanel(queryName);
    }

    public String waitForError()
    {
        waitFor(()-> BootstrapLocators.dangerAlert.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);
        return  errorAlert().getText();
    }
    public WebElement errorAlert()
    {
        return BootstrapLocators.dangerAlert.existsIn(getDriver()) ? BootstrapLocators.dangerAlert.findElement(getDriver()) : null;
    }

    public String waitForWarning()
    {
        waitFor(()-> BootstrapLocators.warningAlert.existsIn(getDriver()),
                "the warning alert did not appear as expected", 1000);
        return  warningAlert().getText();
    }
    public WebElement warningAlert()
    {
        return BootstrapLocators.warningAlert.existsIn(getDriver()) ? BootstrapLocators.warningAlert.findElement(getDriver()) : null;
    }

    public String waitForInfo()
    {
        waitFor(()-> BootstrapLocators.infoAlert.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);
        return  infoAlert().getText();
    }
    public WebElement infoAlert()
    {
        return BootstrapLocators.infoAlert.existsIn(getDriver()) ? BootstrapLocators.infoAlert.findElement(getDriver()) : null;
    }

    public String waitForAnyAlert()
    {
        WebElement alert = Locator.waitForAnyElement(shortWait(),
                BootstrapLocators.dangerAlert, BootstrapLocators.infoAlert, BootstrapLocators.warningAlert, BootstrapLocators.successAlert);
        return alert.getText();
    }
    public String anyAlert()
    {
        WebElement alert = Locator.findAnyElementOrNull(getDriver(),
                BootstrapLocators.dangerAlert, BootstrapLocators.infoAlert, BootstrapLocators.warningAlert, BootstrapLocators.successAlert);
        if (alert !=null)
            return alert.getText();
        else
            return null;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DomainFormPanel firstDomainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())   // for situations where there's only one on the page
                .findWhenNeeded(this);                                                          // and the caller is too lazy to specify which one they want

        DomainFormPanel domainFormPanel(String domainName)                                              // for situations with multiple domainformpanels on the same page
        {
            return new DomainFormPanel.DomainFormPanelFinder(getDriver())
                    .withTitle(domainName).findWhenNeeded(this);
        }
        WebElement saveButton = Locator.button("Save")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelBtn = Locator.button("Cancel")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    public static class Locators
    {
        static public Locator.XPathLocator domainDesignerButton(String text)
        {
            return Locator.tagWithClass("button", "btn-success-default").withText(text);
        }

        static public Locator alert = Locator.tagWithClass("div" , "alert");
    }
}