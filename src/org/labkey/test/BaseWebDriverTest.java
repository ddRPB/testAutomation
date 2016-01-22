/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

package org.labkey.test;

import com.google.common.base.Predicate;
import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.labkey.api.writer.PrintWriters;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.components.search.SearchSideWebPart;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.*;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isDevModeEnabled;
import static org.labkey.test.TestProperties.isInjectionCheckEnabled;
import static org.labkey.test.TestProperties.isLeakCheckSkipped;
import static org.labkey.test.TestProperties.isLinkCheckEnabled;
import static org.labkey.test.TestProperties.isQueryCheckSkipped;
import static org.labkey.test.TestProperties.isScriptCheckEnabled;
import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.TestProperties.isViewCheckSkipped;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.getHttpClientBuilder;
import static org.labkey.test.WebTestHelper.isLocalServer;

/**
 * This class should be used as the base for all functional test classes
 * Test cases should be non-destructive and should not depend on a particular execution order
 *
 * Shared setup steps should be in a public static void method annotated with org.junit.BeforeClass
 * The name of the method is not important. The JUnit runner finds the method solely based on the BeforeClass annotation
 *
 * @BeforeClass
 * public static void doSetup() throws Exception
 * {
 *     MyTestClass initTest = (MyTestClass)getCurrentTest();
 *     initTest.setupProject(); // Perform shared setup steps here
 * }
 *
 * org.junit.AfterClass is also supported, but should not be used to perform any destructive cleanup as it is executed
 * before the base test class can perform its final checks -- link check, leak check, etc.
 * The doCleanup method should be overridden for initial and final project cleanup
 */
public abstract class BaseWebDriverTest extends LabKeySiteWrapper implements Cleanable, WebTest
{
    private static BaseWebDriverTest currentTest;
    private static WebDriver _driver;

    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    protected static boolean _testFailed = false;
    protected static boolean _anyTestCaseFailed = false;
    public final static int WAIT_FOR_PAGE = 30000;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    private final ArtifactCollector _artifactCollector;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public final CustomizeView _customizeViewsHelper;
    public StudyHelper _studyHelper = new StudyHelper(this);
    public final ListHelper _listHelper;
    public AbstractUserHelper _userHelper = new APIUserHelper(this);
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);
    public SecurityHelper _securityHelper = new SecurityHelper(this);
    public FileBrowserHelper _fileBrowserHelper = new FileBrowserHelper(this);
    public PermissionsHelper _permissionsHelper = new PermissionsHelper(this);
    private static File _downloadDir;

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public static final double DELTA = 10E-10;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#\u00E4\u00F6\u00FC\u00C5";
    public static final String INJECT_CHARS_1 = "\"'>--><script>alert('8(');</script>;P";
    public static final String INJECT_CHARS_2 = "\"'>--><img src=xss onerror=alert(\"8(\")>\u2639";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "actions";

    private final BrowserType BROWSER_TYPE;

    protected static final String PERMISSION_ERROR = "User does not have permission to perform this operation";

    protected boolean isPerfTest = false;

    public BaseWebDriverTest()
    {
        _artifactCollector = new ArtifactCollector(this);
        _listHelper = new ListHelper(this);
        _customizeViewsHelper = new CustomizeViewsHelper(this);
//        _customizeViewsHelper = new CustomizeView(this);
        _downloadDir = new File(getArtifactCollector().ensureDumpDir(), "downloads");

        String seleniumBrowser = System.getProperty("selenium.browser");
        if (seleniumBrowser == null || seleniumBrowser.length() == 0)
        {
            if (isTestRunningOnTeamCity() || (bestBrowser() == BrowserType.CHROME))
                BROWSER_TYPE = BrowserType.FIREFOX;
            else
                BROWSER_TYPE = bestBrowser();
        }
        else if (seleniumBrowser.toLowerCase().contains("best"))
        {
            BROWSER_TYPE = bestBrowser();
        }
        else
        {
            for (BrowserType bt : BrowserType.values())
            {
                if (seleniumBrowser.toLowerCase().contains(bt.name().toLowerCase()))
                {
                    BROWSER_TYPE = bt;
                    return;
                }
            }
            BROWSER_TYPE = bestBrowser();
            log("Unknown browser [" + seleniumBrowser + "]; Using best compatible browser [" + BROWSER_TYPE + "]");
        }
    }

    public static BaseWebDriverTest getCurrentTest()
    {
        return currentTest;
    }

    public WebDriver getWrappedDriver()
    {
        return _driver;
    }

    public void pauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
    }


    public void unpauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("unpause crawler"))
            clickButton("unpause crawler");
    }

    protected void setIsPerfTest(boolean isPerfTest)
    {
        this.isPerfTest = isPerfTest;
    }

    protected abstract @Nullable String getProjectName();

    @LogMethod
    public void setUp()
    {
        if (_testFailed)
        {
            // In case the previous test failed so catastrophically that it couldn't clean up after itself
            doTearDown();
        }

        _driver = createNewWebDriver(getDriver(), BROWSER_TYPE, getDownloadDir());

        getDriver().manage().timeouts().setScriptTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
        getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        getDriver().manage().window().setSize(new Dimension(1280, 1024));
    }

    public ArtifactCollector getArtifactCollector()
    {
        return _artifactCollector;
    }

    /**
     * The browser that can run the test fastest.
     * Firefox by default unless a faster browser (probably Chrome) has been verified.
     */
    protected BrowserType bestBrowser()
    {
        return BrowserType.FIREFOX;
    }

    public BrowserType getBrowserType()
    {
        return BROWSER_TYPE;
    }

    private static void doTearDown()
    {
        try
        {
            boolean skipTearDown = _testFailed && "false".equalsIgnoreCase(System.getProperty("close.on.fail"));
            if ((!skipTearDown || isTestRunningOnTeamCity()) && _driver != null)
            {
                _driver.quit();
            }
        }
        catch (UnreachableBrowserException ignore) {}
        finally
        {
            _driver = null;
        }
    }

    @LogMethod
    public void signIn()
    {
        if ( isGuestModeTest() )
        {
            waitForStartup();
            log("Skipping sign in.  Test runs as guest.");
            beginAt("/login/logout.view");
            return;
        }

        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to ensure credentials", e);
        }
        waitForStartup();
        log("Signing in");
        //
        beginAt("/login/logout.view");
        checkForUpgrade();
        simpleSignIn();
        ensureAdminMode();
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password)
    {
        attemptSignIn(email, password);
        waitForElementToDisappear(Locator.lkButton("Sign In"));
        Assert.assertEquals("Logged in as wrong user", email, getCurrentUser());
    }

    public void attemptSignIn(String email, String password)
    {
        if (!getDriver().getTitle().equals("Sign In"))
        {
            try
            {
                clickAndWait(Locator.linkWithText("Sign In"));
            }
            catch (NoSuchElementException error)
            {
                throw new IllegalStateException("You need to be logged out to log in.  Please log out to log in.", error);
            }
        }

        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));
        setFormElement(Locator.id("email"), email);
        setFormElement(Locator.id("password"), password);
        clickButton("Sign In", 0);
    }

    public void signInShouldFail(String email, String password, String... expectedMessages)
    {
        attemptSignIn(email, password);
        String errorText = waitForElement(Locator.id("errors").withText()).getText();
        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));

        List<String> missingErrors = getMissingTexts(new TextSearcher(() -> errorText).setSourceTransformer(text -> text), expectedMessages);
        assertTrue(String.format("Wrong errors.\nExpected: ['%s']\nActual: '%s'", String.join("',\n'", expectedMessages), errorText), missingErrors.isEmpty());
    }

    protected void setInitialPassword(String user, String password)
    {
        // Get setPassword URL from notification email.
        beginAt("/dumbster/begin.view?");

        //the name of the installation can vary, so we need to infer the email subject
        WebElement link = null;
        String linkPrefix = user + " : Welcome to the ";
        String linkSuffix = "new user registration";
        for (WebElement el : getDriver().findElements(By.partialLinkText(linkPrefix)))
        {
            String text = el.getText();
            if (text.startsWith(linkPrefix) && text.endsWith(linkSuffix))
            {
                link = el;
                break;
            }
        }
        assertNotNull("Link for '" + user + "' not found", link);

        String emailSubject = link.getText();
        link.click();

        WebElement resetLink = shortWait().until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//a[text() = '" + emailSubject + "']/..//a[contains(@href, 'setPassword.view')]")));
        clickAndWait(resetLink, WAIT_FOR_PAGE);

        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    protected String getPasswordResetUrl(String username)
    {
        goToHome();
        goToModule("Dumbster");
        String emailSubject = "Reset Password Notification";
        WebElement email = getDriver().findElement(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[starts-with(text(), '" + emailSubject + "')]"));
        email.click();
        WebElement resetLink = shortWait().until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[contains(@href, 'setPassword.view')]")));
        return resetLink.getText();
    }

    protected void resetPassword(String resetUrl, String username, String newPassword)
    {
        if (PasswordUtil.getUsername().equals(username))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        if(resetUrl!=null)
            beginAt(resetUrl);

        assertTextPresent(username, "Choose a password you'll use to access this server", "six non-whitespace characters or more, cannot match email address");

        setFormElement(Locator.id("password"), newPassword);
        setFormElement(Locator.id("password2"), newPassword);

        clickButton("Set Password");

        if(!isElementPresent(Locator.id("userMenuPopupLink")))
        {
            clickButtonContainingText("Submit", defaultWaitForPage*3);
            clickButton("Done");

            signOut();
            signIn(username, newPassword);
        }
    }

    @LogMethod protected void changePassword(String oldPassword, @LoggedParam String password)
    {
        if (PasswordUtil.getUsername().equals(getCurrentUser()))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        goToMyAccount();
        clickButton("Change Password");

        setFormElement(Locator.id("oldPassword"), oldPassword);
        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    /**
     * change user's e-mail from userEmail to newUserEmail from admin console
     */
    protected void changeUserEmail(String userEmail, String newUserEmail)
    {
        log("Attempting to change user email from " + userEmail + " to " + newUserEmail);


        goToSiteUsers();
        clickAndWait(Locator.linkContainingText(displayNameFromEmail(userEmail)));

        clickButton("Change Email");

        setFormElement(Locator.name("newEmail"), newUserEmail);
        clickButton("Submit");
    }


    protected void setSystemMaintenance(boolean enable)
    {
        // Not available in production mode
        if (isDevModeEnabled())
        {
            goToAdminConsole();
            clickAndWait(Locator.linkWithText("system maintenance"));

            if (enable)
                checkCheckbox(Locator.name("enableSystemMaintenance"));
            else
                uncheckCheckbox(Locator.name("enableSystemMaintenance"));

            clickButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        if (!isElementPresent(Locator.css("#adminMenuPopupText")))
            stopImpersonating();
        if (!isElementPresent(Locators.projectBar))
        {
            goToHome();
            waitForElement(Locators.projectBar, WAIT_FOR_PAGE);
        }
    }

    public void goToAdminConsole()
    {
        ensureAdminMode();
        clickAdminMenuItem("Site", "Admin Console");
    }

    public void goToSiteSettings()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("site settings"));
    }

    public void goToAuditLog()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("audit log"));
    }

    protected void createDefaultStudy()
    {
        clickButton("Create Study");
        clickButton("Create Study");
    }

    private void waitForStartup()
    {
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        long ms = System.currentTimeMillis();
        while (!hitFirstPage && ((System.currentTimeMillis() - ms)/1000) < MAX_SERVER_STARTUP_WAIT_SECONDS)
        {
            try
            {
                getDriver().manage().timeouts().pageLoadTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
                getDriver().get(buildURL("login", "logout"));

                if (isElementPresent(Locator.css("table.labkey-main")) || isElementPresent(Locator.id("permalink")) || isElementPresent(Locator.id("headerpanel")))
                {
                    hitFirstPage = true;
                }
                else
                {
                    long elapsedMs = System.currentTimeMillis() - ms;
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            (elapsedMs / 1000)) + " more seconds...");
                }
            }
            catch (SeleniumException | TimeoutException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Ignoring selenium exception: " + e.getMessage());
            }
            finally
            {
                if (!hitFirstPage)
                {
                    sleep(1000);
                }
            }

        }
        if (!hitFirstPage)
        {
            throw new RuntimeException("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
    }

    @LogMethod
    private void checkForUpgrade()
    {
        boolean bootstrapped = false;

        // check to see if we're the first user:
        if (isTextPresent("Welcome! We see that this is your first time logging in."))
        {
            bootstrapped = true;
            assertTitleEquals("Account Setup");
            log("Need to bootstrap");
            verifyInitialUserRedirects();

            log("Testing bad email addresses");
            verifyInitialUserError(null, null, null, "Invalid email address:");
            verifyInitialUserError("bogus@bogus@bogus", null, null, "Invalid email address: bogus@bogus@bogus");

            log("Testing bad passwords");
            String email = PasswordUtil.getUsername();
            verifyInitialUserError(email, null, null, "You must enter a password.");
            verifyInitialUserError(email, "LongEnough", null, "You must enter a password.");
            verifyInitialUserError(email, null, "LongEnough", "You must enter a password.");
            verifyInitialUserError(email, "short", "short", "Your password must be six non-whitespace characters or more.");
            verifyInitialUserError(email, email, email, "Your password must not match your email address.");
            verifyInitialUserError(email, "LongEnough", "ButDontMatch", "Your password entries didn't match.");

            log("Register the first user");
            pushLocation();
            assertTextPresent("Retype Password");
            verifyInitialUserError(email, PasswordUtil.getPassword(), PasswordUtil.getPassword(), null);

            log("Attempting to register another initial user");
            popLocation();
            // Make sure we got redirected to the module status page, since we already have a user
            assertTextNotPresent("Retype Password");
            assertTextPresent("Please wait, this page will automatically update with progress information");
            goToHome();
        }

        if (bootstrapped || isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if (isTitleEqual("Sign In"))
                simpleSignIn();

            String upgradeText = "Please wait, this page will automatically update with progress information.";
            boolean performingUpgrade = isTextPresent(upgradeText);

            if (performingUpgrade)
            {
                try
                {
                    verifyRedirectBehavior(upgradeText);
                }
                catch (IOException fail)
                {
                    throw new RuntimeException(fail);
                }

                int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes

                while (waitMs > 0 && (!(isButtonPresent("Next") || isElementPresent(Locator.linkWithText("Home")))))
                {
                    try
                    {
                        // Pound the server aggressively with requests for the home page to test synchronization
                        // in the sql script runner.
                        for (int i = 0; i < 5; i++)
                        {
                            goToHome();
                            sleep(200);
                            waitMs -= 200;
                        }
                        sleep(2000);
                        waitMs -= 2000;
                        if (isTextPresent("error occurred") || isTextPresent("failure occurred"))
                            throw new RuntimeException("A startup failure occurred.");
                    }
                    catch (SeleniumException e)
                    {
                        // Do nothing -- this page will sometimes auto-navigate out from under selenium
                    }
                }

                if (waitMs <= 0)
                    throw new TestTimeoutException("Script runner took more than 10 minutes to complete.");

                if (isButtonPresent("Next"))
                {
                    clickButton("Next");

                    // check for any additional upgrade pages inserted after module upgrade
                    if (isButtonPresent("Next"))
                        clickButton("Next");
                }

                if (isElementPresent(Locator.linkContainingText("Go directly to the server's Home page")))
                {
                    clickAndWait(Locator.linkContainingText("Go directly to the server's Home page"));
                }
            }

            // Tests hit this page a lot. Make it load as fast as possible
            PortalHelper portalHelper = new PortalHelper(this);
            for (BodyWebPart webPart : portalHelper.getBodyWebParts())
                webPart.delete();
            for (SideWebPart webPart : portalHelper.getSideWebParts())
                webPart.delete();
        }
    }


    private void verifyInitialUserError(@Nullable String email, @Nullable String password1, @Nullable String password2, @Nullable String expectedText)
    {
        if (null != email)
            setFormElement(Locator.id("email"), email);

        if (null != password1)
            setFormElement(Locator.id("password"), password1);

        if (null != password2)
            setFormElement(Locator.id("password2"), password2);

        clickAndWait(Locator.linkWithText("Next"));

        if (null != expectedText)
            assertEquals("Wrong error message.", expectedText, Locator.css(".labkey-error").findElement(getDriver()).getText());
    }


    private void verifyInitialUserRedirects()
    {
        String initialText = "Welcome! We see that this is your first time logging in.";

        // These requests should redirect to the initial user page
        beginAt("/login/resetPassword.view");
        assertTextPresent(initialText);
        beginAt("/admin/maintenance.view");
        assertTextPresent(initialText);
    }

    @LogMethod
    private void verifyRedirectBehavior(String upgradeText) throws IOException
    {
        // Do these checks via direct http requests the primary upgrade window seems to interfere with this test, #15853

        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;
        HttpUriRequest method;
        int status;

        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            // These requests should NOT redirect to the upgrade page

            method = new HttpGet(getBaseURL() + "/login/resetPassword.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());

            method = new HttpGet(getBaseURL() + "/admin/maintenance.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());


            // Check that sign out and sign in work properly during upgrade/install (once initial user is configured)

            DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy()
            {
                @Override
                public boolean isRedirected(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
                {
                    boolean isRedirect = false;
                    try
                    {
                        isRedirect = super.isRedirected(httpRequest, httpResponse, httpContext);
                    }
                    catch (ProtocolException ignore)
                    {
                    }
                    if (!isRedirect)
                    {
                        int responseCode = httpResponse.getStatusLine().getStatusCode();
                        if (responseCode == 301 || responseCode == 302)
                            return true;
//                        if (WebTestHelper.getHttpResponseBody(httpResponse).contains("http-equiv=\"Refresh\""))
//                            return true;
                    }
                    return isRedirect;
                }

                //TODO: Generate HttpRequest for 'http-equiv' redirect
//                @Override
//                public HttpUriRequest getRedirect(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
//                {
//                    HttpUriRequest redirectRequest = null;
//                    ProtocolException ex = null;
//                    try
//                    {
//                        return super.getRedirect(httpRequest, httpResponse, httpContext);
//                    }
//                    catch (ProtocolException e){ex = e;}
//                    redirectRequest = httpRequest.;
//
//                    if (redirectRequest == null)
//                        throw ex;
//                    else
//                        return redirectRequest;
//                }
            };
            try (CloseableHttpClient redirectClient = getHttpClientBuilder().setRedirectStrategy(redirectStrategy).build())
            {
                method = new HttpPost(getBaseURL() + "/login/logout.view");
                List<NameValuePair> args = new ArrayList<>();
                args.add(new BasicNameValuePair("login", PasswordUtil.getUsername()));
                args.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
                ((HttpPost) method).setEntity(new UrlEncodedFormEntity(args));
                response = redirectClient.execute(method, context);
                status = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected response", HttpStatus.SC_OK, status);
                // TODO: check login, once http-equiv redirect is sorted out
                assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            }
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    @LogMethod
    public void disableMaintenance()
    {
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/customizeSite.view");
        click(Locator.radioButtonByNameAndValue("systemMaintenanceInterval", "never"));
        clickButton("Save");
    }

    private static long smStart = 0;
    private static String smUrl = null;

    public void startSystemMaintenance()
    {
        startSystemMaintenance("");
    }

    public void startSystemMaintenance(String taskName)
    {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("test", "true");
        if (!taskName.isEmpty())
            urlParams.put("taskName", taskName);
        String maintenanceTriggerUrl = WebTestHelper.buildURL("admin", "systemMaintenance", urlParams);

        smStart = System.currentTimeMillis();
        SimpleHttpRequest request = new SimpleHttpRequest(maintenanceTriggerUrl);
        request.setRequestMethod("POST");
        request.copySession(getDriver());
        SimpleHttpResponse response = request.getResponse();
        assertEquals("Failed to start system maintenance", HttpStatus.SC_OK, response.getResponseCode());
        smUrl = response.getResponseBody();
    }

    public void waitForSystemMaintenanceCompletion()
    {
        assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        // Navigate to pipeline details page, then refresh page and check for system maintenance complete, up to 10 minutes from the start of the test
        beginAt(smUrl);
        int timeLeft = 10 * 60 * 1000 - ((Long)elapsed).intValue();
        waitForTextWithRefresh(timeLeft > 0 ? timeLeft : 0, "System maintenance complete");
    }

    private void populateLastPageInfo()
    {
        _lastPageTitle = getLastPageTitle();
        _lastPageURL = getLastPageURL();
        _lastPageText = getLastPageText();
    }

    public String getLastPageTitle()
    {
        if (_lastPageTitle == null)
        {
            if (null != getDriver().getTitle())
                return getDriver().getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : getDriver().getPageSource();
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(getDriver().getCurrentUrl());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    public void resetErrors()
    {
        if (isGuestModeTest())
            return;
        if (isLocalServer())
            beginAt("/admin/resetErrorMark.view");
    }

    private static final String BEFORE_CLASS = "BeforeClass";
    private static final String AFTER_CLASS = "AfterClass";
    private static boolean beforeClassSucceeded = false;
    private static boolean reenableMiniProfiler = false;
    private static long testClassStartTime;
    private static Class testClass;
    private static int testCount;
    private static int currentTestNumber;

    @ClassRule
    public static TestWatcher testClassWatcher() {return new TestWatcher()
    {
        @Override
        public Statement apply(Statement base, Description description)
        {
            testClassStartTime = System.currentTimeMillis();
            testClass = description.getTestClass();
            _driver = null;
            testCount = description.getChildren().size();
            currentTestNumber = 0;
            beforeClassSucceeded = false;
            _anyTestCaseFailed = false;

            return super.apply(base, description);
        }

        @Override
        public void starting(Description description)
        {
            TestLogger.resetLogger();
            TestLogger.log("// BeforeClass \\\\");
            TestLogger.increaseIndent();

            ArtifactCollector.init();

            try
            {
                currentTest = (BaseWebDriverTest)testClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }

            currentTest.setUp();
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            String pseudoTestName = beforeClassSucceeded ? AFTER_CLASS : BEFORE_CLASS;

            if (currentTest != null)
            {
                try
                {
                    currentTest.handleFailure(e, pseudoTestName);
                }
                catch (RuntimeException | Error secondary)
                {
                    TestLogger.log("Error while collecting failure data");
                    secondary.printStackTrace(System.out);
                }
            }
        }

        @Override
        protected void finished(Description description)
        {
            doTearDown();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ AfterClass Complete //");
        }
    };}

    public static Class getCurrentTestClass()
    {
        return testClass;
    }

    @BeforeClass
    public static void preamble()
    {
        if (getDownloadDir().exists())
        {
            try{
                FileUtils.deleteDirectory(getDownloadDir());
            }
            catch (IOException ignore) { }
        }

        currentTest.getContainerHelper().clearCreatedProjects();
        currentTest.doPreamble();
    }

    private void doPreamble()
    {
        signIn();
        enableEmailRecorder();
        reenableMiniProfiler = disableMiniProfiler();
        resetErrors();

        if (isSystemMaintenanceDisabled())
        {
            // Disable scheduled system maintenance to prevent timeouts during nightly tests.
            disableMaintenance();
        }

        // Only do this as part of test startup if we haven't already checked. Since we do this as the last
        // step in the test, there's no reason to bother doing it again at the beginning of the next test
        if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
        {
            checkLeaksAndErrors();
        }

        cleanup(false);
    }

    @Before
    public final void beforeTest() throws Exception
    {
        ensureNotImpersonating();
        simpleSignIn();
    }

    @After
    public final void afterTest()
    {
        checkJsErrors();
    }

    @AfterClass
    public static void postamble() throws Exception
    {
        if (beforeClassSucceeded)
        {
            if (reenableMiniProfiler)
                getCurrentTest().setMiniProfilerEnabled(true);

            getCurrentTest().doPostamble();
        }
    }

    @ClassRule
    public static Timeout globalTimeout()
    {
        return new Timeout(2400000); // 40 minutes
    }

    @Rule
    public Timeout testTimeout = new Timeout(1800000); // 30 minutes

    private TestWatcher _watcher = new TestWatcher()
    {
        @Override
        protected void starting(Description description)
        {
            // We know that @BeforeClass methods are done now that we are in a non-static context
            beforeClassSucceeded = true;

            if (TestProperties.isNewWebDriverForEachTest())
                doTearDown();

            setUp(); // Instantiate new WebDriver if needed
            _testFailed = false;
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            Ext4Helper.resetCssPrefix();
            try
            {
                handleFailure(e, description.getMethodName());
            }
            catch (RuntimeException | Error secondary)
            {
                log("Error while collecting failure data");
                secondary.printStackTrace(System.out);
            }
        }

        @Override
        protected void finished(Description description)
        {
            Ext4Helper.resetCssPrefix();
        }
    };

    private TestWatcher _logger = new TestWatcher()
    {
        private long testCaseStartTimeStamp;

        @Override
        protected void skipped(AssumptionViolatedException e, Description description)
        {
            if (currentTestNumber == 0)
            {
                TestLogger.resetLogger();
                TestLogger.log("\\\\ BeforeClass Complete //");
            }

            currentTestNumber++;

            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("<< Test Case Skipped - " + testCaseName + " >>");
        }

        @Override
        protected void starting(Description description)
        {
            if (currentTestNumber == 0)
            {
                TestLogger.resetLogger();
                TestLogger.log("\\\\ BeforeClass Complete //");
            }

            currentTestNumber++;
            testCaseStartTimeStamp = System.currentTimeMillis();
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("// Begin Test Case - " + testCaseName + " \\\\");
            TestLogger.increaseIndent();
        }

        @Override
        protected void succeeded(Description description)
        {
            Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ Test Case Complete - " + testCaseName + " [" + getElapsedString(elapsed) + "] //");
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ Failed Test Case - " + testCaseName + " [" + getElapsedString(elapsed) + "] //");
        }

        @Override
        protected void finished(Description description)
        {
            if (currentTestNumber == testCount)
            {
                TestLogger.resetLogger();
                TestLogger.log("// AfterClass \\\\");
                TestLogger.increaseIndent();
            }

        }

        private String getElapsedString(long elapsed)
        {
            return String.format("%dm %d.%ds",
                    TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                    elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
        }
    };

    @Rule
    public RuleChain _ruleChain = RuleChain.outerRule(_logger).around(_watcher);

    /**
     * Collect additional information about test failures and publish build artifacts for TeamCity
     */
    @LogMethod
    public void handleFailure(Throwable error, @LoggedParam String testName)
    {
        _testFailed = true;
        _anyTestCaseFailed = true;

        error.printStackTrace(System.out);

        if (error instanceof InterruptedException || error.getCause() != null && error.getCause() instanceof InterruptedException)
            return;

        try
        {
            try
            {
                if (isTestRunningOnTeamCity())
                {
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "sampledata"));
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "build/deploy/files"));
                    getArtifactCollector().dumpPipelineFiles();
                }
                if (_testTimeout)
                    getArtifactCollector().dumpThreads();
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump pipeline files");
                e.printStackTrace(System.out);
            }

            if (error instanceof UnreachableBrowserException || getDriver() == null)
            {
                return;
            }
            if (error instanceof TestTimeoutException || error instanceof TimeoutException)
            {
                _testTimeout = true;
            }
            else if (error instanceof UnhandledAlertException)
            {
                dismissAllAlerts();
            }

            try
            {
                populateLastPageInfo();

                if (_lastPageTitle != null && !_lastPageTitle.startsWith("404") && _lastPageURL != null)
                {
                    try
                    {
                        // On failure, re-invoke the last action with _debug paramter set, which lets the action log additional debugging information
                        String lastPage = _lastPageURL.toString();
                        URL url = new URL(lastPage + (lastPage.contains("?") ? "&" : "?") + "_debug=1");
                        log("Re-invoking last action with _debug parameter set: " + url.toString());
                        url.getContent();
                    }
                    catch (IOException t)
                    {
                        log("Unable to re-invoke last page");
                        t.printStackTrace(System.out);
                    }
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to determine information about the last page");
                e.printStackTrace(System.out);
            }

            try
            {

                getArtifactCollector().dumpPageSnapshot(testName, null);
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump screenshots");
                e.printStackTrace(System.out);
            }

            dismissAllAlerts();
            checkJsErrors();
        }
        finally
        {
            if (!isTestCleanupSkipped())
            {
                try (TestScrubber scrubber = new TestScrubber(BROWSER_TYPE, getDownloadDir()))
                {
                    scrubber.cleanSiteSettings();
                }
            }

            doTearDown();
        }
    }

    private void doPostamble()
    {
        if (!_anyTestCaseFailed)
        {
            //make sure you're signed in as admin, because this won't work otherwise
            ensureSignedInAsAdmin();

            checkQueries();

            checkViews();

            if(!isPerfTest)
                checkActionCoverage();

            checkLinks();

            if (!isTestCleanupSkipped())
            {
                goToHome();
                cleanup(true);

                if (getDownloadDir().exists())
                {
                    try{
                        FileUtils.deleteDirectory(getDownloadDir());
                    }
                    catch (IOException ignore) { }
                }
            }
            else
            {
                log("Skipping test cleanup as requested.");
            }

            if (!"DRT".equals(System.getProperty("suite")) || Runner.isFinalTest())
            {
                checkLeaksAndErrors();
            }

            checkJsErrors();
        }
        else
        {
            log("Skipping post-test checks because a test case failed.");
        }
    }

    @LogMethod
    public void ensureSignedInAsAdmin()
    {
        signOut();
        simpleSignIn();
    }

    private void cleanup(boolean afterTest) throws TestTimeoutException
    {
        if (!ClassUtils.getAllInterfaces(getCurrentTestClass()).contains(ReadOnlyTest.class) || ((ReadOnlyTest) this).needsSetup())
            doCleanup(afterTest);
    }

    // Standard cleanup: delete the project
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        String projectName = getProjectName();

        if (null != projectName)
            deleteProject(projectName, afterTest);
    }

    public void cleanup() throws Exception
    {
        try
        {
            log("========= Cleaning up " + getClass().getSimpleName() + " =========");

            // explicitly go back to the site, just in case we're on a 404 or crash page:
            beginAt("");
            signIn();
            doCleanup(false);   // User requested cleanup... could be before or after tests have run (or some intermediate state). False generally means ignore errors.

            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            doTearDown();
        }
    }

    public static File getDownloadDir()
    {
        return _downloadDir;
    }

    @LogMethod
    public void checkLeaksAndErrors()
    {
        if ( isGuestModeTest() )
            return;
        checkErrors();
        checkLeaks();
        _checkedLeaksAndErrors = true;
    }

    public void checkLeaks()
    {
        if (!isLocalServer())
            return;
        if (isLeakCheckSkipped())
            return;
        if (isGuestModeTest())
            return;

        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;
        long msSinceTestStart = Long.MAX_VALUE;

        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC. ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");

                // If another thread (e.g., SearchService) is doing work then give it 10 seconds before trying again
                if (isElementPresent(Locators.labkeyError.containing("Active thread(s) may have objects in use:")))
                {
                    log("Pausing 10 seconds to wait for active thread");
                    sleep(10000);
                }
            }
            msSinceTestStart = System.currentTimeMillis() - testClassStartTime;
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                throw new IllegalStateException("Asserts must be enabled to track memory leaks; add -ea to your server VM params and restart or add -DmemCheck=false to your test VM params.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String newLeak = null;
            List<WebElement> errorRows = Locator.css("#leaks tr:not(:first-child)").findElements(getDriver());
            for (WebElement errorRow : errorRows)
            {
                String ageStr = errorRow.findElement(By.cssSelector(".age")).getText();
                Duration leakAge = Duration.parse("PT" + ageStr);
                if (msSinceTestStart > leakAge.toMillis())
                {
                    newLeak = errorRow.findElement(By.cssSelector(".allocationStack")).getText();
                    break;
                }
            }

            if (newLeak != null)
            {
                getArtifactCollector().dumpHeap();
                getArtifactCollector().dumpThreads();
                fail(String.format("Found memory leak: %s [1 of %d, MAX:%d]\nSee test artifacts for more information.", newLeak, leakCount, MAX_LEAK_LIMIT));
            }

            log("Found " + leakCount + " in-use objects.  They appear to be from a previous test.");
        }
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }

    public void checkErrors()
    {
        if (!isLocalServer())
            return;
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/showErrorsSinceMark.view");

        assertTrue("There were server-side errors during the test run. Check labkey.log and/or labkey-errors.log for details.", isPageEmpty());
        log("No new errors found.");
        goToHome();         // Don't leave on an empty page
    }

    @LogMethod
    public void checkExpectedErrors(@LoggedParam int expectedErrors)
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        String text = getBodyText();
        Pattern errorPattern = Pattern.compile("^ERROR", Pattern.MULTILINE);
        Matcher errorMatcher = errorPattern.matcher(text);
        int count = 0;
        while (errorMatcher.find())
        {
            count++;
        }
        assertEquals("Expected error count does not match actual count for this run.", expectedErrors, count);

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }

    @LogMethod
    protected void checkQueries()
    {
        if (isQueryCheckSkipped())
            return;
        if(getProjectName() != null)
        {
            clickProject(getProjectName());
            if(!"Query Schema Browser".equals(getDriver().getTitle()))
                goToSchemaBrowser();
            validateQueries(true);
//            validateLabAuditTrail();
        }
    }

    @LogMethod
    protected void checkViews()
    {
        if (isViewCheckSkipped())
            return;

        List<String> checked = new ArrayList<>();

        for (String projectName : _containerHelper.getCreatedProjects())
        {
            clickProject(projectName);

            doViewCheck(projectName);
            checked.add(projectName);
        }

        for (WebTestHelper.FolderIdentifier folderId : _containerHelper.getCreatedFolders())
        {
            String project = folderId.getProjectName();
            String folder = folderId.getFolderName();
            if(!checked.contains(project))
            {
                clickProject(project);

                doViewCheck(project);
                checked.add(project);
            }
            if(!checked.contains(folder))
            {
                if (!getText(Locator.id("folderBar")).equals(project))
                    clickProject(project);
                clickFolder(folder);

                doViewCheck(folder);
                checked.add(folder);
            }
        }
    }

    /**
     * TODO: 7695: Custom views are not deleted when list is deleted
     * @return List of view names which are no longer valid
     */
    protected Set<String> getOrphanedViews()
    {
        return new HashSet<>();
    }

    /**
     * To be overloaded by tests
     * @return The Set of folder names to be excluded from the view check
     */
    protected Set<String> excludeFromViewCheck()
    {
        return new HashSet<>();
    }

    @LogMethod
    private void doViewCheck(@LoggedParam String folder)
    {
        if (excludeFromViewCheck().contains(folder))
        {
            log ("Skipping view check for folder");
            return;
        }

        try{
            goToManageViews();
        }
        catch (SeleniumException e)
        {
            log("No manage views option");
            return;
        }

        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator view = Locator.xpath("//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]");
        int viewCount = getElementCount(view);
        for (int i = 0; i < viewCount; i++)
        {
            Locator.XPathLocator thisView = view.index(i);
            waitForElement(thisView);
            String viewName = getText(thisView.append("//td[contains(@class, 'x-grid3-cell-first')]"));
            if (!getOrphanedViews().contains(viewName))
            {
                pushLocation();
                click(thisView);

                Locator.XPathLocator expandedReport = Locator.tag("div").withClass("x-grid3-row-expanded");

                //String reportType = getAttribute(expandedReport.append("//div").withClass("x-grid3-col-1").append("/img"), "alt");
                String schemaName = getText(expandedReport.append("//td[normalize-space()='schema name']/following-sibling::td"));
                String queryName = getText(expandedReport.append("//td[normalize-space()='query name']/following-sibling::td"));
                String viewString = viewName + " of " + schemaName + "." + queryName;

                if ("Stand-alone views".equals(queryName))
                {
                    log("Checking view: " + viewName);
                    waitAndClick(Locator.linkWithText("VIEW"));
                    Set<String> windows = getDriver().getWindowHandles();
                    if (windows.size() > 1)
                    {
                        getDriver().switchTo().window((String)windows.toArray()[1]);
                        assertEquals(200, getResponseCode());
                        getDriver().close();
                        getDriver().switchTo().window((String)windows.toArray()[0]);
                    }
                    else
                    {
                        assertEquals(200, getResponseCode());
                    }
                }
                else
                {
                    log("Checking view: " + viewString);
                    waitAndClickAndWait(Locator.linkWithText("VIEW"));
                    waitForText(viewName);
                }

                popLocation();
                _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
            }
            else
                log("Skipping manually excluded view: " + viewName);
        }
    }

    @LogMethod
    protected void checkJsErrors()
    {
        if (isScriptCheckEnabled() && getDriver() != null && getJsErrorChecker() != null)
        {
            List<LogEntry> jsErrors = getJsErrorChecker().getErrors();

            List<LogEntry> validErrors = new ArrayList<>();
            Set<LogEntry> ignoredErrors = new HashSet<>();

            for (LogEntry error : jsErrors)
            {
                if (!getJsErrorChecker().isErrorIgnored(error))
                    validErrors.add(error);
                else
                    ignoredErrors.add(error);
            }
            if (ignoredErrors.size() + validErrors.size() > 0)
            {
                log("<<<<<<<<<<<<<<<JAVASCRIPT ERRORS>>>>>>>>>>>>>>>");
                for (LogEntry error : validErrors)
                    log(error.toString());

                if (!ignoredErrors.isEmpty())
                {
                    log("<<<<<<<<<<<<<<<IGNORED ERRORS>>>>>>>>>>>>>>>>>>");
                    for (LogEntry error : ignoredErrors)
                        log("[Ignored] " + error.toString());
                }

                log("<<<<<<<<<<<<<<<JAVASCRIPT ERRORS>>>>>>>>>>>>>>>");
            }

            if (validErrors.size() > 0)
            {
                String errorCtStr = "";
                if (validErrors.size() > 1)
                    errorCtStr = " (1 of " + validErrors.size() + ") ";
                if (!_testFailed) // Don't clobber existing failures. Just log them.
                    fail("JavaScript error" + errorCtStr + ": " + validErrors.get(0));
            }
        }
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName)
    {
        return executeSelectRowCommand(schemaName, queryName, null);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName,  @Nullable List<Filter> filters)
    {
        return executeSelectRowCommand(schemaName, queryName, filters, false);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, @Nullable List<Filter> filters, boolean reuseSession)
    {
        return executeSelectRowCommand(schemaName, queryName, ContainerFilter.CurrentAndSubfolders, "/" + getProjectName(), filters, reuseSession);
    }

    @LogMethod
    protected void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;

        pushLocation();
        int rowCount, coveredActions, totalActions;
        Double actionCoveragePercent;
        String actionCoveragePercentString;
        beginAt("/admin/actions.view");

        rowCount = getTableRowCount(ACTION_SUMMARY_TABLE_NAME);
        if (getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 0).equals("Total"))
        {
            totalActions = Integer.parseInt(getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 1));
            coveredActions = Integer.parseInt(getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 2));
            actionCoveragePercentString = getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 3);
            actionCoveragePercent =  Double.parseDouble(actionCoveragePercentString.substring(0, actionCoveragePercentString.length() - 1 ));
            writeActionStatistics(totalActions, coveredActions, actionCoveragePercent);
        }

        // Download full action coverage table and add to TeamCity artifacts.
        beginAt("/admin/exportActions.view?asWebPage=true");
        getArtifactCollector().publishArtifact(saveTsv(TestProperties.getDumpDir(), "ActionCoverage"));
        popLocation();
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            pauseJsErrorChecker();
            Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
            crawler.addExcludedActions(getUncrawlableActions());
            crawler.setInjectionCheckEnabled(isInjectionCheckEnabled());
            crawler.crawlAllLinks();
            resumeJsErrorChecker();
        }
    }

    protected List<Crawler.ControllerActionId> getUncrawlableActions()
    {
        return Collections.emptyList();
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        File xmlFile = new File(TestFileUtils.getLabKeyRoot(), "teamcity-info.xml");
        try (Writer writer = PrintWriters.getPrintWriter(xmlFile))
        {
            xmlFile.createNewFile();

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + totalActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException ignore){}
    }

    public File saveTsv(File dir, String baseName)
    {
        return saveFile(dir, baseName + ".tsv");
    }

    public File saveFile(File dir, String fileName)
    {
        return saveFile(dir, fileName, getBodyText());
    }

    public File saveFile(File dir, String fileName, String contents)
    {
        File tsvFile = new File(dir, fileName);

        try(Writer writer = PrintWriters.getPrintWriter(tsvFile))
        {
            writer.write(contents);
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace(System.out);
            return null;
        }
    }

    public String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
    }

    public String getProjectUrl()
    {
        return "/project/" + EscapeUtil.encode(getProjectName()) + "/begin.view?";
    }

    public void openProjectMenu()
    {
        waitForHoverNavigationReady();
        shortWait().until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(@Nullable WebDriver driver)
            {
                click(Locators.projectBar);
                return ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#projectBar_menu .project-nav")).apply(driver);
            }
        });
    }

    public void clickProject(String project)
    {
        clickProject(project, true);
    }

    public void clickProject(String project, boolean assertDestination)
    {
        openProjectMenu();
        WebElement projectLink = Locator.linkWithText(project).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        clickAt(projectLink, 1, 1, WAIT_FOR_PAGE); // Don't click hidden portion of long links
        if (assertDestination)
        {
            acceptTermsOfUse(null, true);
            waitForElement(Locator.id("folderBar").withText(project));
        }
    }

    public void openFolderMenu()
    {
        waitForElement(Locators.folderMenu.withText());
        waitForFolderNavigationReady();
        shortWait().until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(@Nullable WebDriver driver)
            {
                click(Locators.folderMenu);
                return ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#folderBar_menu .folder-nav")).apply(driver);
            }
        });
    }

    public void clickFolder(String folder)
    {
        openFolderMenu();
        expandFolderTree(folder);
        waitAndClickAndWait(Locator.linkWithText(folder));
    }

    public void waitForFolderNavigationReady()
    {
        waitForHoverNavigationReady();
        waitFor(() -> (boolean) executeScript("if (HoverNavigation._folder.webPartName == 'foldernav') return true; else return false;"),
                "HoverNavigation._folder not ready", WAIT_FOR_JAVASCRIPT);
    }

    public void waitForHoverNavigationReady()
    {
        waitFor(() -> (boolean) executeScript("if (window.HoverNavigation) return true; else return false;"),
                "HoverNavigation not ready", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.AbstractContainerHelper#deleteProject(String, boolean)}
     */
    @Deprecated
    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        _containerHelper.deleteProject(project, failIfFail);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.PortalHelper#addWebPart(String)}
     */
    @Deprecated public void addWebPart(String webPartName)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart(webPartName);
    }

    protected void setSelectedFields(String containerPath, String schema, String query, String viewName, String[] fields)
    {
        pushLocation();
        beginAt("/query" + containerPath + "/internalNewView.view");
        setFormElement(Locator.name("ff_schemaName"), schema);
        setFormElement(Locator.name("ff_queryName"), query);
        if (viewName != null)
            setFormElement(Locator.name("ff_viewName"), viewName);
        clickButton("Create");
        StringBuilder strFields = new StringBuilder(fields[0]);
        for (int i = 1; i < fields.length; i ++)
        {
            strFields.append("&");
            strFields.append(fields[i]);
        }
        setFormElement(Locator.name("ff_columnList"), strFields.toString());
        clickButton("Save");
        popLocation();
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionExportHelper}
     */
    @Deprecated
    protected void clickExportToText()
    {
        clickButton("Export", 0);
        shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(DataRegionTable.findDataRegion(this)));
        _extHelper.clickSideTab("Text");
        clickButton("Export to Text");
    }

    protected void selectSecurityGroupsExport()
    {
        Locator checkbox = Locator.checkboxByNameAndValue("types", "Project-level groups and members");
        waitForElement(checkbox);
        checkCheckbox(checkbox);
    }

    protected void selectRoleAssignmentsExport()
    {
        Locator checkbox = Locator.checkboxByNameAndValue("types", "Role assignments for users and groups");
        waitForElement(checkbox);
        checkCheckbox(checkbox);
    }

    @LogMethod
    protected void prepareForFolderExport(@Nullable String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders, int locationIndex)
    {

        if (folderName != null)
            clickFolder(folderName);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        waitForElement(Locator.name("location"));

        if (exportSecurityGroups)
            selectSecurityGroupsExport();
        if (exportRoleAssignments)
            selectRoleAssignmentsExport();
        if (includeSubfolders)
            click(Locator.name("includeSubfolders"));
        checkRadioButton(Locator.name("location").index(locationIndex)); // first locator with this name is "Pipeline root export directory, as individual files
    }

    @LogMethod
    protected void exportFolderAsIndividualFiles(String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders)
    {
        // first locator with this name is "Pipeline root export directory, as individual files
        prepareForFolderExport(folderName, exportSecurityGroups, exportRoleAssignments, includeSubfolders, 0);
        clickButton("Export");
    }


    protected void exportFolderAsZip(boolean exportSecurityGroups, boolean exportRoleAssignments)
    {
        prepareForFolderExport(null, exportSecurityGroups, exportRoleAssignments, false, 1);
        clickButton("Export");
    }

    protected File exportFolderToBrowserAsZip()
    {
        prepareForFolderExport(null, false, false, false, 2);
        return clickAndWaitForDownload(Locator.extButton("Export"));
    }

    public void setModuleProperties(List<ModulePropertyValue> values)
    {
        goToFolderManagement();
        log("setting module properties");
        clickAndWait(Locator.linkWithText("Module Properties"));
        waitForText("Save Changes");
        waitForText("Property:");  //proxy for the panel actually loading

        boolean changed = false;
        for (ModulePropertyValue value : values)
        {
            log("setting property: " + value.getPropertyName() + " for container: " + value.getContainerPath() + " to value: " + value.getValue());
            Map<String, String> map = new HashMap<>();
            map.put("moduleName", value.getModuleName());
            map.put("containerPath", value.getContainerPath());
            map.put("propName", value.getPropertyName());
            waitForText(value.getPropertyName()); //wait for the property name to appear
            String query = ComponentQuery.fromAttributes("field", map);
            Ext4FieldRef ref = _ext4Helper.queryOne(query, Ext4FieldRef.class);
            String val = (String)ref.getValue();
            if((StringUtils.isEmpty(val) != StringUtils.isEmpty(value.getValue())) || !val.equals(value.getValue()))
            {
                changed = true;
                ref.setValue(value.getValue());
            }
        }
        if (changed)
        {
            clickButton("Save Changes", 0);
            waitForElement(Ext4Helper.Locators.window("Success"));
            clickButton("OK", 0);
            _ext4Helper.waitForMaskToDisappear();
        }
        else
        {
            log("properties were already set, no changed needed");
        }
    }

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("403: Error Page -- User does not have permission to perform this operation");
    }

    public void assertNavTrail(String... links)
    {
        ///TODO:  Would like this to be more sophisitcated
        assertTextPresentInThisOrder(links);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    public void verifyTabSelected(String caption)
    {
        assertTrue("Tab not selected: " + caption, isElementPresent(Locator.xpath("//li[contains(@class, labkey-tab-active)]/a[text() = '" + caption + "']")));
    }

    public int getImageWithAltTextCount(String altText)
    {
        String js = "function countImagesWithAlt(txt) {" +
                "var doc=document;" +
                "var count = 0;" +
                "for (var i = 0; i < doc.images.length; i++) {" +
                "if (doc.images[i].alt == txt) " +
                "count++;" +
                "}" +
                "return count;" +
                "};" +
                "return countImagesWithAlt('" + altText + "');";
        return Integer.parseInt(executeScript(js).toString());
    }

    // Returns the text contents of every "Status" cell in the pipeline StatusFiles grid
    public List<String> getPipelineStatusValues()
    {
        DataRegionTable status = new DataRegionTable("StatusFiles", this, true, false);
        return status.getColumnDataAsText("Status");
    }

    public void setPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath, false);
    }

    public void setPipelineRoot(String rootPath, boolean inherit)
    {
        log("Set pipeline to: " + rootPath);
        goToModule("Pipeline");
        clickButton("Setup");

        if (isElementPresent(Locator.linkWithText("override")))
        {
            if (inherit)
                clickAndWait(Locator.linkWithText("modify the setting for all folders"));
            else
                clickAndWait(Locator.linkWithText("override"));
        }
        checkRadioButton(Locator.radioButtonById("pipeOptionProjectSpecified"));
        setFormElement(Locator.id("pipeProjectRootPath"), rootPath);

        clickButton("Save");

        getArtifactCollector().addArtifactLocation(new File(rootPath));

        log("Finished setting pipeline to: " + rootPath);
    }

    public void setPipelineRootToDefault()
    {
        log("Set pipeline to default based on the site-level root");
        goToModule("Pipeline");
        clickButton("Setup");
        checkRadioButton(Locator.radioButtonById("pipeOptionSiteDefault"));
        clickButton("Save");
        log("Finished setting pipeline to default based on the site-level root");
    }

    // Returns count of "COMPLETE"
    public int getCompleteCount(List<String> statusValues)
    {
        int complete = 0;

        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                complete++;

        return complete;
    }

    // Returns count of "COMPLETE" and "ERROR"
    public int getFinishedCount(List<String> statusValues)
    {
        int finsihed = 0;
        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "ERROR".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                finsihed++;
        return finsihed;
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#getFullColumnValues(String...)}
     */
    @Deprecated
    public List<List<String>> getColumnValues(String tableName, String... columnNames)
    {
        DataRegionTable table = new DataRegionTable(tableName, this);
        return table.getFullColumnValues(columnNames);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType)
    {
        setUpFilter(regionName, columnName, filterType, null);
        clickButton("OK");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        setFilter(regionName, columnName, filterType, filter, WAIT_FOR_PAGE);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String, int)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType, String filter, int waitMillis)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", waitMillis);
    }

    public void setUpFilter(String regionName, String columnName, String filterType, @Nullable String filter)
    {
        setUpFilter(regionName, columnName, filterType, filter, null, null);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String, int)}
     */
    @Deprecated
    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", milliSeconds);
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setUpFilter(String regionName, String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable String filter2)
    {
        String log = "Setting filter in " + regionName + " for " + columnName + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        log(log);

        openFilter(regionName, columnName);

        if (isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
        {
            log("Switching to advanced filter UI");
            _extHelper.clickExtTab("Choose Filters");
            waitForElement(Locator.xpath("//span["+Locator.NOT_HIDDEN+" and text()='Filter Type:']"), WAIT_FOR_JAVASCRIPT);
        }
        _extHelper.selectComboBoxItem("Filter Type:", filter1Type); //Select combo box item.
        if(filter1 != null && !filter1Type.contains("Blank"))
            setFormElement(Locator.id("value_1"), filter1);
        if(filter2Type!=null && !filter2Type.contains("Blank"))
        {
            _extHelper.selectComboBoxItem("and:", filter2Type); //Select combo box item.
            if(filter2 != null) setFormElement(Locator.id("value_2"), filter2);
        }
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        setUpFilter(regionName, columnName, filter1Type, filter1, filter2Type, filter2);
        clickButton("OK");
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setUpFacetedFilter(String regionName, String columnName, String... values)
    {
        String log;
        if (values.length > 0)
        {
            log = "Setting filter in " + regionName + " for " + columnName + " to one of: [";
            for(String v : values)
            {
                log += v + ", ";
            }
            log = log.substring(0, log.length() - 2) + "]";
        }
        else
        {
            log = "Clear filter in " + regionName + " for " + columnName;
        }

        log(log);

        openFilter(regionName, columnName);
        String columnLabel = getText(DataRegionTable.Locators.columnHeader(regionName, columnName));

        sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", getText(Locator.css(".x-tab-strip-active")));
        if(!isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
            click(Locator.linkWithText("[All]"));
        click(Locator.linkWithText("[All]"));

        if(values.length > 1)
        {
            for(String v : values)
            {
                click(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...") +
                        "//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            click(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where "+columnLabel+"...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='"+values[0]+"']"));
        }
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setFacetedFilter(String regionName, String columnName, String... values)
    {
        setUpFacetedFilter(regionName, columnName, values);
        clickButton("OK");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#openFilterDialog(String)}
     */
    @Deprecated
    public void openFilter(String regionName, String columnName)
    {
        (new DataRegionTable(regionName, this)).openFilterDialog(columnName);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearAllFilters(String)}
     */
    @Deprecated
    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        openFilter(regionName, columnName);
        clickButton("CLEAR ALL FILTERS");
    }

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[text() = '" + propertyHeading + "']/../..";
    }

    public String getPropertyXPathContains(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    /**
     *
     * @param feature  the enable link will have an id of the form "labkey-experimental-feature-[feature]
     */
    public void enableExperimentalFeature(String feature)
    {
        log("Attempting to enable feature: " + feature);
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("experimental features"));

        String xpath = "//div[div[text()='" + feature + "']]/a";
        if (!isElementPresent(Locator.xpath(xpath)))
            fail("No such feature found: " + feature);
        else
        {
            Locator link = Locator.xpath(xpath + "[text()='Enable']");
            if(isElementPresent(link))
            {
                click(link);
                log("Enable link found, enabling " + feature);
            }
            else
            {
                log("Link not found, presumed enabled: " + feature);
            }
        }
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#checkAll()}
     */
    @Deprecated
    public void checkAllOnPage(String dataRegionName)
    {
        new DataRegionTable(dataRegionName, this).checkAll();
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#checkCheckbox(int)}
     */
    @Deprecated
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        new DataRegionTable(dataRegionName, this).checkCheckbox(index);
    }

    public void addUserToGroupFromGroupScreen(String userName)
    {
        waitForElement(Locator.name("names"));
        setFormElement(Locator.name("names"), userName);
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");

    }

    protected void setDisplayName(String email, String newDisplayName)
    {
        String previousDisplayName = usersAndDisplayNames.get(email);
        String defaultDisplayName = getDefaultDisplayName(email);
        usersAndDisplayNames.remove(email);

        if (previousDisplayName == null && newDisplayName.equals(defaultDisplayName))
            return;
        else
        {
            if (!newDisplayName.equals(previousDisplayName))
            {
                goToSiteUsers();

                DataRegionTable users = new DataRegionTable("Users", this, true, true);
                int userRow = users.getRow("Email", email);
                assertFalse("No such user: " + email, userRow == -1);
                clickAndWait(users.detailsLink(userRow));

                clickButton("Edit");
                setFormElement(Locator.name("quf_DisplayName"), newDisplayName);
                clickButton("Submit");
            }
        }

        if (!newDisplayName.equals(defaultDisplayName))
            usersAndDisplayNames.put(email, newDisplayName);
    }

    /**
     * Create a user with the specified permissions for the specified project
     */
    public void createUserWithPermissions(String userName, String projectName, String permissions)
    {
        createUser(userName, null);
        if(projectName==null)
            goToProjectHome();
        else
            clickProject(projectName);
        _permissionsHelper.setUserPermissions(userName, permissions);
    }

    public CreateUserResponse createUser(String userName, @Nullable String cloneUserName)
    {
        return createUser(userName, cloneUserName, true);
    }

    public CreateUserResponse createUser(String userName, @Nullable String cloneUserName, boolean verifySuccess)
    {
        if(cloneUserName == null)
        {
            return _userHelper.createUser(userName, verifySuccess);
        }
        else
        {
            throw new IllegalArgumentException("cloneUserName support has been removed"); //not in use, so was not implemented in new user helpers
        }
    }

    public void createUserAndNotify(String userName, String cloneUserName)
    {
        createUserAndNotify(userName, cloneUserName, true);
    }

    public void createUserAndNotify(String userName, String cloneUserName, boolean verifySuccess)
    {
        ensureAdminMode();
        goToSiteUsers();
        clickButton("Add Users");

        setFormElement(Locator.name("newUsers"), userName);
        if (cloneUserName != null)
        {
            checkCheckbox(Locator.id("cloneUserCheck"));
            setFormElement(Locator.name("cloneUser"), cloneUserName);
        }
        clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createSiteDeveloper(String userEmail)
    {
        ensureAdminMode();
        goToSiteDevelopers();

        if (!isElementPresent(Locator.xpath("//input[@value='" + userEmail + "']")))
        {
            setFormElement(Locator.name("names"), userEmail);
            uncheckCheckbox(Locator.name("sendEmail"));
            clickButton("Update Group Membership");
        }
    }

    public void deleteUsersIfPresent(String... userEmails)
    {
        deleteUsers(false, userEmails);
    }

    @LogMethod
    public void deleteUsers(boolean failIfNotFound, @LoggedParam String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<>();
        ensureAdminMode();
        goToSiteUsers();

        if(isElementPresent(Locator.linkWithText("INCLUDE INACTIVE USERS")))
            clickAndWait(Locator.linkWithText("INCLUDE INACTIVE USERS"));

        DataRegionTable usersTable = new DataRegionTable("Users", this, true, true);

        for(String userEmail : userEmails)
        {
            int row = usersTable.getRow("Email", userEmail);

            boolean isPresent = row != -1;

            // If we didn't find the user and we have more than one page, then show all pages and try again
            if (!isPresent && isElementPresent(Locator.linkContainingText("Next")) && isElementPresent(Locator.linkContainingText("Last")))
            {
                clickButton("Page Size", 0);
                clickAndWait(Locator.linkWithText("Show All"));
                row = usersTable.getRow("Email", userEmail);
                isPresent = row != -1;
            }

            if (failIfNotFound)
                assertTrue(userEmail + " was not present", isPresent);
            else if (!isPresent)
                log("Unable to delete non-existent user: " + userEmail);

            if (isPresent)
            {
                usersTable.checkCheckbox(row);
                checked++;
                displayNames.add(usersTable.getDataAsText(row, "Display Name"));
            }
        }

        if(checked > 0)
        {
            clickButton("Delete");
            assertTextPresent(displayNames);
            clickButton("Permanently Delete");
            assertTextNotPresent(userEmails);
        }
    }

    public void assertUserExists(String email)
    {
        log("asserting that user " + email + " exists...");
        ensureAdminMode();
        goToSiteUsers();
        assertTextPresent(email);
        log("user " + email + " exists.");
    }

    private long start = 0;

    protected void startTimer()
    {
        start = System.currentTimeMillis();
    }

    protected long elapsedSeconds()
    {
        return (System.currentTimeMillis() - start) / 1000;
    }

    protected long elapsedMilliseconds()
    {
        return System.currentTimeMillis() - start;
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Returns the data region for the the cohort table to enable setting
     * or verifying the enrolled status of the cohort
     */
    public DataRegionTable getCohortDataRegionTable()
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Cohorts"));
        return new DataRegionTable("Cohort", this, false);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Verifies the enrolled status of a cohort
     */
    public void verifyCohortStatus(DataRegionTable table, String cohort, boolean  enrolled)
    {
        int row = getCohortRow(table, cohort);
        String s = table.getDataAsText(row, "Enrolled");
        assertTrue("Enrolled column should be " + String.valueOf(enrolled), (0 == s.compareToIgnoreCase(String.valueOf(enrolled))));
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Retrieves the row for the cohort matching the label passed in
     */
    public int getCohortRow(DataRegionTable cohortTable, String cohort)
    {
        int row;
        for (row = 0; row < cohortTable.getDataRowCount(); row++)
        {
            String s = cohortTable.getDataAsText(row, "Label");
            if (0 == s.compareToIgnoreCase(cohort))
            {
                break;
            }
        }
        return row;
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Changes the enrolled status of the passed in cohort name
     */
    public void changeCohortStatus(DataRegionTable cohortTable, String cohort, boolean enroll)
    {
        int row = getCohortRow(cohortTable, cohort);
        // if the row does not exist then most likely the cohort passed in is incorrect
        clickAndWait(cohortTable.link(row, 0));

        if (!enroll)
        {
            uncheckCheckbox(Locator.name("quf_enrolled"));
        }
        else
        {
            checkCheckbox(Locator.name("quf_enrolled"));
        }

        clickButton("Submit");
    }

    public void goToProjectHome()
    {
        goToProjectHome(getProjectName());
    }

    public void goToProjectHome(String projectName)
    {
        if(!isElementPresent(Locators.projectBar))
            goToHome();
        clickProject(projectName);
    }

    /**
     * go to the project settings page of a project
     * @param project project name
     */
    public void goToProjectSettings(String project)
    {
        goToProjectHome(project);
        goToProjectSettings();
    }

    public void goToAdmin()
    {
        beginAt("/admin/showAdmin.view");
    }

    public void goToMyAccount()
    {
        clickUserMenuItem("My Account");
    }

    protected void startImportStudyFromZip(File studyFile)
    {
        startImportStudyFromZip(studyFile, false);
    }

    protected void startImportStudyFromZip(File studyFile, boolean ignoreQueryValidation)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation, false);
    }

    protected void startImportStudyFromZip(File studyFile, boolean ignoreQueryValidation, boolean createSharedDatasets)
    {
        clickButton("Import Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        if (ignoreQueryValidation)
        {
            click(Locator.checkboxByName("validateQueries"));
        }
        Locator createSharedDatasetsCheckbox = Locator.name("createSharedDatasets");
        List<WebElement> webElements = createSharedDatasetsCheckbox.findElements(getDriver());
        if (!webElements.isEmpty())
        {
            if (createSharedDatasets)
                checkCheckbox(createSharedDatasetsCheckbox);
            else
                uncheckCheckbox(createSharedDatasetsCheckbox);
        }

        clickButton("Import Study From Local Zip Archive");
        if (isElementPresent(Locator.css(".labkey-error")))
        {
            String errorText = Locator.css(".labkey-error").findElement(getDriver()).getText();
            assertTrue("Error present: " + errorText, errorText.trim().length() == 0);
        }
    }

    protected void importStudyFromZip(File studyFile)
    {
        importStudyFromZip(studyFile, false);
    }

    protected void importFolderFromZip(File folderFile)
    {
        importFolderFromZip(folderFile, true, 1);
    }

    protected void importStudyFromZip(File studyFile, boolean ignoreQueryValidation)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    public void importStudyFromZip(File studyFile, boolean ignoreQueryValidation, boolean createSharedDataset)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation, createSharedDataset);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs)
    {
        importFolderFromZip(folderFile, validateQueries, completedJobs, false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs, boolean expectErrors)
    {
        importFolderFromZip(folderFile, validateQueries, completedJobs, expectErrors, MAX_WAIT_SECONDS * 1000);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs, boolean expectErrors, int wait)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        waitForElement(Locator.name("folderZip"));
        setFormElement(Locator.name("folderZip"), folderFile);
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButtonContainingText("Import Folder From Local Zip Archive");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(completedJobs, "Folder import", expectErrors, wait);
    }

    public void importFolderFromPipeline(String folderFile)
    {
        importFolderFromPipeline(folderFile, 1, true);
    }

    public void importFolderFromPipeline(String folderFile, int completedJobsExpected)
    {
        importFolderFromPipeline(folderFile, completedJobsExpected, true);
    }

    public void importFolderFromPipeline(String folderFile, int completedJobsExpected, boolean validateQueries)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        clickButtonContainingText("Import Folder Using Pipeline");
        _fileBrowserHelper.importFile(folderFile, "Import Folder");

        waitForText("Import Folder from Pipeline");
        Locator validateQuriesCheckbox = Locator.name("validateQueries");
        waitForElement(validateQuriesCheckbox);
        if (!validateQueries)
            uncheckCheckbox(validateQuriesCheckbox);
        clickButton("Start Import");

        waitForPipelineJobsToComplete(completedJobsExpected, "Folder import", false);
    }

    @LogMethod
    public void signOut(@Nullable String termsText)
    {
        log("Signing out");
        beginAt("/login/logout.view");

        acceptTermsOfUse(termsText, true);

        if (!isElementPresent(Locators.signInButtonOrLink)) // Sign-out action stopped impersonation
            beginAt("/login/logout.view");
        waitForElement(Locators.signInButtonOrLink);
    }

    @LogMethod
    public void signOut()
    {
        signOut(null);
    }

    public void signOutHTTP()
    {
        String logOutUrl = WebTestHelper.buildURL("login", "logout");
        SimpleHttpRequest logOutRequest = new SimpleHttpRequest(logOutUrl, "POST");
        logOutRequest.copySession(getDriver());

        SimpleHttpResponse response = logOutRequest.getResponse();
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(final String projectName, String searchFor, final Integer expectedResults, @Nullable final String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickProject(projectName);
        final SearchResultsPage searchResults = new SearchSideWebPart(this).searchForm().searchFor(searchFor);

        try
        {
            longWait().until(new Predicate<WebDriver>()
            {
                @Override
                public boolean apply(@Nullable WebDriver webDriver)
                {
                    if (titleName == null && searchResults.getResultCount().equals(expectedResults) ||
                            titleName != null && isElementPresent(Locator.linkContainingText(titleName)))
                        return true;
                    else
                        refresh();
                    return false;
                }
            });
        }
        catch (TimeoutException ignore) {}

        assertEquals(String.format("Found wrong number of search results for '%s'", searchFor), expectedResults, searchResults.getResultCount());
        if (titleName != null)
        {
            clickAndWait(Locator.linkContainingText(titleName));
            if (searchFor.startsWith("\""))
                searchFor = searchFor.substring(1, searchFor.length() - 1);
            assertTextPresent(searchFor);
        }
    }

    // Helper methods for interacting with the query schema browser
    public void selectSchema(String schemaName)
    {
        String[] schemaParts = schemaName.split("\\.");
        if (isExtTreeNodeSelected(schemaParts[schemaParts.length - 1]))
            return;

        String schemaWithParents = "";
        String separator = "";
        for (String schemaPart : schemaParts)
        {
            schemaWithParents += separator + schemaPart;
            separator = ".";

            Locator.XPathLocator loc = Locator.schemaTreeNode(schemaPart);

            //first load of schemas might a few seconds
            shortWait().until(ExpectedConditions.elementToBeClickable(loc.toBy()));
            Locator.XPathLocator selectedSchema = Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span").withText(schemaPart);

            if (getDriver().getCurrentUrl().endsWith("schemaName=" + schemaPart))
                waitForElement(selectedSchema);
            if (isElementPresent(selectedSchema))
                continue; // already selected
            log("Selecting schema " + schemaWithParents + " in the schema browser...");
            click(Locator.css("body")); // Dismiss tooltip
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
            // select/expand tree node
            try{
                scrollIntoView(loc);
            }
            catch (StaleElementReferenceException ignore) {}
            doAndWaitForPageSignal(() -> click(loc), "queryTreeSelectionChange");
            waitForElement(selectedSchema, 60000);
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        // wait for tool tip to disappear, in case it is covering the element we want to click on
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-tip') and contains(@style, 'display: none')]//div[contains(@class, 'x4-tip-body')]"));
        Locator loc = Locator.queryTreeNode(queryName);
        shortWait().until(ExpectedConditions.elementToBeClickable(loc.toBy()));

        // NOTE: consider abstracting this.
        waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
        // select/expand tree node
        try{
            scrollIntoView(loc);
        }
        catch (StaleElementReferenceException ignore) {}
        clickAt(loc, 5, 5, 0);

        waitForElement(Locator.xpath("//div[contains(./@class,'lk-qd-name')]/a[contains(text(), '" + schemaName + "." + queryName + "')]/.."), 30000);
    }

    public void clickFkExpando(String schemaName, String queryName, String columnName)
    {
        String queryLabel = schemaName + "." + queryName;
        click(Locator.xpath("//div/a[text()='" + queryLabel + "']/../../../table/tbody/tr/td/img[(contains(@src, 'plus.gif') or contains(@src, 'minus.gif')) and ../../td[text()='" + columnName + "']]"));
    }

    public void viewQueryData(String schemaName, String queryName)
    {
        viewQueryData(schemaName, queryName, null);
    }

    public void viewQueryData(String schemaName, String queryName, @Nullable String moduleName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.xpath("//div[contains(@class, 'lk-qd-name')]/a[text()='" + schemaName + "." + queryName + "']");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(loc, "href");
        if (moduleName != null) // 12474
            assertTextPresent("Defined in " + moduleName + " module");
        log("Navigating to " + href);
        beginAt(href);
    }

    public void editQueryProperties(String schemaName, String queryName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.tagWithText("a", "edit properties");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        clickAndWait(loc);
    }

    public void createNewQuery(String schemaName)
    {
        createNewQuery(schemaName, null);
    }

    // Careful: If baseQueryName isn't provided, the first table in the schema will be used as the base query.
    public void createNewQuery(@NotNull String schemaName, @Nullable String baseQueryName)
    {
        if (baseQueryName != null)
            selectQuery(schemaName, baseQueryName);
        else
            selectSchema(schemaName);
        clickAndWait(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Create New Query')]"));
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
        beginAt(queryURL);
        createNewQuery(schemaName);
        waitForElement(Locator.name("ff_newQueryName"));
        setFormElement(Locator.name("ff_newQueryName"), name);
        clickButton("Create and Edit Source", 0);
        waitForElement(Locator.id("labkey-nav-trail-current-page").withText("Edit " + name));
        setCodeEditorValue("queryText", sql);
        if (xml != null)
        {
            _extHelper.clickExtTab("XML Metadata");
            setCodeEditorValue("metadataText", xml);
        }
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties("flow", name);
            selectOptionByValue(Locator.name("inheritable"), "true");
            clickButton("Save");
        }
    }

    public void validateQueries(boolean validateSubfolders)
    {
        click(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Validate Queries')]"));
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(Locator.tagWithClass("div", "qbrowser-validate"), WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
        {
            shortWait().until(ExpectedConditions.elementToBeClickable(By.className("lk-vq-subfolders")));
            checkCheckbox(Locator.tagWithClass("table", "lk-vq-subfolders"));
        }
        checkCheckbox(Locator.tagWithClass("table", "lk-vq-systemqueries"));
        clickButton("Start Validation", 0);
        waitForElement(locFinishMsg, 120000);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            fail("Some queries did not pass validation. See error log for more details.");
        }
    }

    // This class makes it easier to start a specimen import early in a test and wait for completion later.
    public class SpecimenImporter
    {
        private final File _pipelineRoot;
        private final File[] _specimenArchives;
        private final File _tempDir;
        private final String _studyFolderName;
        private final int _completeJobsExpected;
        private final File[] _copiedArchives;
        private boolean _expectError = false;

        public SpecimenImporter(File pipelineRoot, File specimenArchive, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            this(pipelineRoot, new File[] { specimenArchive }, tempDir, studyFolderName, completeJobsExpected);
        }

        public SpecimenImporter(File pipelineRoot, File[] specimenArchives, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            _pipelineRoot = pipelineRoot;
            _specimenArchives = specimenArchives;
            _tempDir = tempDir;
            _studyFolderName = studyFolderName;
            _completeJobsExpected = completeJobsExpected;

            _copiedArchives = new File[_specimenArchives.length];
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                String baseName = specimenArchive.getName();
                baseName = baseName.substring(0, baseName.length() - ".specimens".length());
                _copiedArchives[i] = new File(_tempDir, baseName + "_" + FastDateFormat.getInstance("MMddHHmmss").format(new Date()) + ".specimens");
            }
        }

        public void setExpectError(boolean expectError)
        {
            _expectError = expectError;
        }

        public void importAndWaitForComplete()
        {
            startImport();
            waitForComplete();
        }


        @LogMethod
        public void startImport()
        {
            log("Starting import of specimen archive(s):");
            for (File specimenArchive : _specimenArchives)
                log("  " + specimenArchive);

            // copy the file into its own directory
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                copyFile(specimenArchive, _copiedArchives[i]);
            }

            clickFolder(_studyFolderName);

            int total = 0;
            while( !isElementPresent(Locator.linkWithText("Manage Files")) && total < WAIT_FOR_PAGE)
            {
                // Loop in case test is outrunning the study creator
                sleep(250);
                total += 250;
                refresh();
            }

            clickAndWait(Locator.linkWithText("Manage Files"));
            clickButton("Process and Import Data", defaultWaitForPage);

            // TempDir is somewhere underneath the pipeline root.  Determine each subdirectory we need to navigate to reach it.
            File testDir = _tempDir;
            List<String> dirNames = new ArrayList<>();

            while (!_pipelineRoot.equals(testDir))
            {
                dirNames.add(0, testDir.getName());
                testDir = testDir.getParentFile();
            }

            //Build folder path.
            String path = "/";
            for (String dir : dirNames)
                path += dir + "/";

            _fileBrowserHelper.selectFileBrowserItem(path);

            for (File copiedArchive : _copiedArchives)
                _fileBrowserHelper.checkFileBrowserFileCheckbox(copiedArchive.getName());
            _fileBrowserHelper.selectImportDataAction("Import Specimen Data");
            clickButton("Start Import");
        }

        @LogMethod
        public void waitForComplete()
        {
            log("Waiting for completion of specimen archives");

            clickFolder(_studyFolderName);
            clickAndWait(Locator.linkWithText("Manage Files"));

            if (_expectError)
                waitForPipelineJobsToFinish(_completeJobsExpected);
            else
                waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    throw new RuntimeException("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
    }


    public void waitForPipelineJobsToComplete(@LoggedParam final int completeJobsExpected, @LoggedParam final String description, final boolean expectError)
    {
        waitForPipelineJobsToComplete(completeJobsExpected, description, expectError, MAX_WAIT_SECONDS * 1000);
    }

    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    @LogMethod
    public void waitForPipelineJobsToComplete(@LoggedParam final int completeJobsExpected, @LoggedParam final String description, final boolean expectError, int wait)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");

        waitFor(() -> {
                    log("Waiting for " + description);
                    List<String> statusValues = getPipelineStatusValues();
                    log("[" + StringUtils.join(statusValues,",") + "]");
                    if (!expectError)
                    {
                        assertElementNotPresent(Locator.linkWithText("ERROR"));
                    }
                    if (statusValues.size() < completeJobsExpected || statusValues.size() != getFinishedCount(statusValues))
                    {
                        refresh();
                        return false;
                    }
                    return true;
                },
                "Pipeline jobs did not complete.", wait);

        assertEquals("Did not find correct number of completed pipeline jobs.", completeJobsExpected, expectError ? getFinishedCount(getPipelineStatusValues()) : getCompleteCount(getPipelineStatusValues()));
    }

    // wait until pipeline UI shows that all jobs have finished (either COMPLETE or ERROR status)
    @LogMethod
    protected void waitForPipelineJobsToFinish(@LoggedParam int jobsExpected)
    {
        log("Waiting for " + jobsExpected + " pipeline jobs to finish");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (getFinishedCount(statusValues) < jobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }
        assertEquals("Did not find correct number of finished pipeline jobs.", jobsExpected, getFinishedCount(statusValues));
    }

    @LogMethod
    protected void waitForRunningPipelineJobs(int wait)
    {
        log("Waiting for running pipeline jobs list to be empty.");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (statusValues.size() > 0 && elapsedSeconds() < wait)
        {
            log("[" + StringUtils.join(statusValues,",") + "]");
            log("Waiting for " + statusValues.size() + " jobs to complete...");
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
            statusValues.removeAll(Arrays.asList("COMPLETE", "ERROR"));
        }

        assertTrue("Running pipeline jobs were found.  Timeout:" + wait, statusValues.size() == 0);
    }

    @LogMethod
    protected void deletePipelineJob(@LoggedParam String jobDescription, @LoggedParam boolean deleteRuns)
    {
        deletePipelineJob(jobDescription, deleteRuns, false);
    }

    @LogMethod
    protected void deletePipelineJob(@LoggedParam String jobDescription, @LoggedParam boolean deleteRuns, @LoggedParam boolean descriptionStartsWith)
    {
        goToModule("Pipeline");

        PipelineStatusTable table = new PipelineStatusTable(this, true, false);
        int tableJobRow = table.getJobRow(jobDescription, descriptionStartsWith);
        assertNotEquals("Failed to find job rowid", -1, tableJobRow);
        table.checkCheckbox(tableJobRow);

        clickButton("Delete");
        assertElementPresent(Locator.linkContainingText(jobDescription));
        if (deleteRuns)
            checkCheckbox(Locator.id("deleteRuns"));
        clickButton("Confirm Delete");
    }

    public void ensureSignedOut()
    {
        if(isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();
    }

    protected void reloadStudyFromZip(File studyFile)
    {
        reloadStudyFromZip(studyFile, true, 2);
    }

    protected void reloadStudyFromZip(File studyFile, boolean validateQueries, int pipelineJobs)
    {
        goToManageStudy();
        clickButton("Reload Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        if(! validateQueries) {uncheckCheckbox(Locator.checkboxByName("validateQueries"));}
        clickButton("Reload Study From Local Zip Archive");
        waitForPipelineJobsToComplete(pipelineJobs, "Study Reload", false);
    }

    public AbstractContainerHelper getContainerHelper()
    {
        return _containerHelper;
    }

    public void setContainerHelper(AbstractContainerHelper containerHelper)
    {
        _containerHelper = containerHelper;
    }

    //hopefully we'll come up with a better solution soon
    public void waitForSaveAssay()
    {
        sleep(5000);
    }

    public void assertSVG(final String expectedSvgText)
    {
        assertSVG(expectedSvgText, 0);
    }

    /**
     * Wait for an SVG with the specified text (Ignores thumbnails)
     * @param expectedSvgText exact text expected. Whitespace will be ignored on Firefox due to inconsistencies in getText results. Use getText value from Chrome.
     * @param svgIndex the zero-based index of the svg which is expected to match
     */
    public void assertSVG(final String expectedSvgText, final int svgIndex)
    {
        final String expectedText = prepareSvgText(expectedSvgText);
        final Locator svgLoc = Locator.css("div:not(.thumbnail) > svg").index(svgIndex);

        if (!isDumpSvgs())
        {
            waitFor(() -> {
                if (isElementPresent(svgLoc))
                {
                    String svgText = prepareSvgText(getText(svgLoc));
                    return expectedText.equals(svgText);
                }
                else
                    return false;
            }, WAIT_FOR_JAVASCRIPT);

            String svgText = prepareSvgText(getText(svgLoc));
            assertEquals("SVG did not look as expected", expectedText, svgText);
        }
        else
        {
            waitForElement(svgLoc);
            scrollIntoView(svgLoc);
            String svgText = getText(svgLoc);

            File svgDir = new File(getArtifactCollector().ensureDumpDir(), "svgs");
            String baseName;
            File svgFile;

            int i = 0;
            do
            {
                i++;
                baseName = String.format("%2d-svg[%d]", i, svgIndex);
                svgFile = new File(svgDir, baseName + ".txt");
            }while (svgFile.exists());

            getArtifactCollector().dumpScreen(svgDir, baseName);

            try(Writer writer = PrintWriters.getPrintWriter(svgFile))
            {
                writer.write("Expected:\n");
                writer.write(expectedSvgText.replace("\\", "\\\\").replace("\n", "\\n"));
                writer.write("\n\nActual:\n");
                writer.write(svgText.replace("\\", "\\\\").replace("\n", "\\n"));
            }
            catch (IOException e){
                log("Failed to dump svg: " + svgFile.getName() + "Reason: " + e.getMessage());
            }
        }
    }

    public String waitForWikiDivPopulation(String testDivName, int waitSeconds)
    {
        while (waitSeconds-- > 0)
        {
            log("Waiting for " + testDivName + " div to render...");
            if (isElementPresent(Locator.id(testDivName)))
            {
                String divHtml = (String)executeScript("return document.getElementById('" + testDivName + "').innerHTML;");
                if (divHtml.length() > 0)
                    return divHtml;
            }
            sleep(1000);
        }
        fail("Div failed to render.");
        return null;
    }

    private String prepareSvgText(String svgText)
    {
        final boolean isFirefox = getDriver() instanceof FirefoxDriver;

        // Remove raphael credits to make this function work with Raphael and d3 renderers.
        final String ignoredRaphaelText = "Created with Rapha\u00ebl 2.1.0";
        svgText = svgText.replace(ignoredRaphaelText, "");
        svgText = svgText.trim();

        // Strip out all the whitespace on Firefox to deal with different return of getText from svgs
        return isFirefox ?
                svgText.replaceAll("[\n ]", "") :
                svgText;
    }

    private boolean isDumpSvgs()
    {
        return "true".equals(System.getProperty("dump.svgs"));
    }

    public void goToSvgAxisTab(String axisLabel)
    {
        // Workaround: (Selenium 2.33) Unable to click axis labels reliably for some reason. Use javascript
        fireEvent(Locator.css("svg text").containing(axisLabel).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), SeleniumEvent.click);
        waitForElement(Ext4Helper.Locators.ext4Button("Cancel")); // Axis label windows always have a cancel button. It should be the only one on the page
    }

    public void createSurveyDesign(String label, @Nullable String description, String schemaName, String queryName, @Nullable File metadataFile)
    {
        clickButton("Create Survey Design");
        waitForElement(Locator.name("label"));
        setFormElement(Locator.name("label"), label);
        if (description != null) setFormElement(Locator.name("description"), description);
        _ext4Helper.selectComboBoxItem("Schema", schemaName);
        // the schema selection enables the query combo, so wait for it to enable
        waitForElementToDisappear(Locator.xpath("//table[contains(@class,'item-disabled')]//label[text() = 'Query']"), WAIT_FOR_JAVASCRIPT);
        sleep(1000); // give it a second to get the queries for the selected schema
        _ext4Helper.selectComboBoxItem("Query", queryName);
        clickButton("Generate Survey Questions", 0);
        sleep(1000); // give it a second to generate the metadata
        String metadataValue = _extHelper.getCodeMirrorValue("metadata");
        assertNotNull("No generate survey question metadata available", metadataValue);
        if (metadataFile != null)
        {
            assertTrue(metadataFile.exists());
            String json = TestFileUtils.getFileContents(metadataFile);
            _extHelper.setCodeMirrorValue("metadata", json);
        }

        clickButton("Save Survey");
        waitForElement(Locator.tagWithText("td", label));
    }
}
