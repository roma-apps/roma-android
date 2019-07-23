package tech.bigfig.roma.functional;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import tech.bigfig.roma.R;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Created on Mon 22 Jul 2019 @ 18:19
 * by Matt Fenlon (https://github.com/noln)
 * for project: Pleroma (tech.bigfig.roma.functional)
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class BasicFunctionalityTests {

    private static final String TEST_PACKAGE = "tech.bigfig.roma";

    private static final int LAUNCH_TIMEOUT = 5000;
    private static final int SHORT_WAIT = 1500;
    private static final int DEFAULT_TEST_TIMEOUT = 10000;

    private UiDevice mDevice;
    private boolean stayLoggedIn = false; // Arm-based emulators use inbuilt, inaccessible WebView

    @Before
    public void startMainActivityFromHomeScreen() throws UiObjectNotFoundException, InterruptedException {

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(getInstrumentation());

        // Start from the home screen
        mDevice.pressHome();

        // Wait for launcher
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the blueprint app
        Context context = getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(TEST_PACKAGE);
        assertThat(intent, notNullValue());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(TEST_PACKAGE).depth(0)), LAUNCH_TIMEOUT);

        // Login
        if (!stayLoggedIn) login();
    }

    @After
    public void resetState() throws UiObjectNotFoundException, InterruptedException {
        if (!stayLoggedIn) logoutFromMainActivity();
    }

    @Test
    public void checkPreconditions() {
        assertThat(mDevice, notNullValue());
    }

    @Test
    public void loginLogoutTest() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Test
    public void testAllTabsSelectable() throws UiObjectNotFoundException {

        // Switch to DM tab
        UiObject2 directMessageTabSelector = mDevice.wait(Until.findObject(By.desc(getApplicationContext().getString(R.string.title_direct_messages))),
                DEFAULT_TEST_TIMEOUT);
        directMessageTabSelector.click();

        // Switch to fediverse tab
        UiObject2 fediverseTabSelector = mDevice.wait(Until.findObject(By.desc(getApplicationContext().getString(R.string.title_public_federated))),
                DEFAULT_TEST_TIMEOUT);
        fediverseTabSelector.click();

        // Switch to public-local tab
        UiObject2 publicLocalTabSelector = mDevice.wait(Until.findObject(By.desc(getApplicationContext().getString(R.string.title_public_local))),
                DEFAULT_TEST_TIMEOUT);
        publicLocalTabSelector.click();

        // Switch to notifications tab
        UiObject2 notificationsTabSelector = mDevice.wait(Until.findObject(By.desc(getApplicationContext().getString(R.string.title_notifications))),
                DEFAULT_TEST_TIMEOUT);
        notificationsTabSelector.click();

        // Switch to home tab
        @SuppressWarnings("deprecation")
        UiObject homeTabSelector = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(ActionBar.Tab.class));
        homeTabSelector.waitForExists(SHORT_WAIT);
        homeTabSelector.click();
    }

    @Test
    public void composeMessageDialogTest() {

        // Open the compose dialog
        UiObject2 composeDialogFab = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "floating_btn")),
                DEFAULT_TEST_TIMEOUT);
        composeDialogFab.click();

        // Close the dialog
        UiObject2 closeDialog = mDevice.wait(Until.findObject(By.desc(("Navigate up"))),
                DEFAULT_TEST_TIMEOUT);
        closeDialog.click();
    }

    @Test
    public void showEditProfileTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_edit_profile);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showFavoritesTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_view_favourites);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showListsTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_lists);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showSearchTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_search);

        // Dismiss the keyboard
        mDevice.pressBack();

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showDraftsTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_access_saved_toot);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showAccountPreferencesTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_view_account_preferences);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showPreferencesTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.action_view_preferences);

        // Go back to main
        mDevice.pressBack();
    }

    @Test
    public void showAboutTest() {

        toggleDrawer();
        flingDrawerRecyclerViewDown();
        clickDrawerActionWithText(R.string.about_title_activity);

        // Go back to main
        mDevice.pressBack();
    }

    private void clickDrawerActionWithText(int actionTextInt) {
        UiObject2 actionButton = mDevice.wait(
                Until.findObject(By.text(getApplicationContext().getString(actionTextInt))), SHORT_WAIT);
        actionButton.click();
    }

    private void flingDrawerRecyclerViewDown() {

        UiObject2 drawerRecyclerView = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "material_drawer_recycler_view")),
                DEFAULT_TEST_TIMEOUT);
        drawerRecyclerView.fling(Direction.DOWN);
    }

    @Test
    public void flingUpAndDownTest() throws InterruptedException {

        // Switch to fediverse tab
        UiObject2 fediverseTabSelector = mDevice.wait(Until.findObject(By.desc(getApplicationContext().getString(R.string.title_public_federated))),
                DEFAULT_TEST_TIMEOUT);
        fediverseTabSelector.click();

        Thread.sleep(SHORT_WAIT);

        // Confirm that the drawer toggle is visible (therefore we're logged in)
        UiObject2 recyclerView = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "recyclerView")),
                DEFAULT_TEST_TIMEOUT);

        // Fling three times down.
        for (int i = 0; i < 3; i++) {
            recyclerView.fling(Direction.DOWN);
            Thread.sleep(SHORT_WAIT);
        }

        Thread.sleep(SHORT_WAIT);

        // Fling three times down.
        for (int i = 0; i < 3; i++) {
            recyclerView.fling(Direction.UP);
            Thread.sleep(SHORT_WAIT);
        }
    }

    private void logoutFromMainActivity() throws UiObjectNotFoundException, InterruptedException {

        toggleDrawer();

        // Click logout button
        UiObject logoutButton = mDevice.findObject(new UiSelector()
                .instance(9)
                .className(ViewGroup.class));
        logoutButton.waitForExists(SHORT_WAIT);
        logoutButton.click();

        // Dialog ok, check it's there, then click it
        UiObject confirmLogoutButton = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(Button.class));
        assertThat(confirmLogoutButton, notNullValue());
        confirmLogoutButton.click();

        Thread.sleep(SHORT_WAIT);

        // Confirm that we're back to square one
        UiObject2 domainEditText = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "domainEditText")),
                DEFAULT_TEST_TIMEOUT);
        assertThat(domainEditText, notNullValue());
    }

    private void login() throws UiObjectNotFoundException, InterruptedException {

        // Click on instance entry EditText
        UiObject2 domainEditText = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "domainEditText")),
                DEFAULT_TEST_TIMEOUT);
        domainEditText.click();

        // Enter name of instance to test against
        mDevice.findObject(By.res(TEST_PACKAGE, "domainEditText")).setText("pleroma.site");

        // Click on instance entry EditText
        mDevice.findObject(By.res(TEST_PACKAGE, "loginButton")).click();

        Thread.sleep(2000);

        // Open with Chrome dialog
        UiObject2 openWithBrowser = mDevice.findObject(By.res("android", "button_once"));
        openWithBrowser.click();

        mDevice.wait(Until.findObject(By.clazz(WebView.class)), SHORT_WAIT);

        // Set username
        UiObject usernameInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));
        usernameInput.waitForExists(SHORT_WAIT);
        usernameInput.setText(getApplicationContext().getString(R.string.test_username));

        // Set Password
        UiObject passwordInput = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(EditText.class));
        passwordInput.waitForExists(SHORT_WAIT);
        passwordInput.setText(getApplicationContext().getString(R.string.test_password));

        // Click submit
        UiObject submitButton = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));
        submitButton.click();
    }

    private String getLauncherPackageName() {

        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    private void toggleDrawer() {

        // Confirm that the drawer toggle is visible (therefore we're logged in)
        UiObject2 drawerToggle = mDevice.wait(Until.findObject(By.res(TEST_PACKAGE, "drawer_toggle")),
                DEFAULT_TEST_TIMEOUT);
        drawerToggle.click();
    }
}

