package com.termux.app;

import static org.junit.Assert.assertEquals;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import com.termux.BuildConfig;
import com.termux.shared.termux.TermuxConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CoexistenceIdentityTest {

    @Test
    public void runtimePackageConstantsMatchApplicationId() {
        assertEquals(BuildConfig.APPLICATION_ID, TermuxConstants.TERMUX_PACKAGE_NAME);
    }

    @Test
    public void componentNamesResolveInThePackagedManifest() throws Exception {
        PackageManager manager = RuntimeEnvironment.getApplication().getPackageManager();
        manager.getActivityInfo(new ComponentName(BuildConfig.APPLICATION_ID,
            TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY_NAME), 0);
        manager.getServiceInfo(new ComponentName(BuildConfig.APPLICATION_ID,
            TermuxConstants.TERMUX_APP.TERMUX_SERVICE_NAME), 0);
        manager.getServiceInfo(new ComponentName(BuildConfig.APPLICATION_ID,
            TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME), 0);
        assertEquals(BuildConfig.APPLICATION_ID,
            Class.forName(TermuxConstants.TERMUX_APP.BUILD_CONFIG_CLASS_NAME)
                .getField("APPLICATION_ID").get(null));
    }
}
