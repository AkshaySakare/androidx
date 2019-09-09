/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ads.identifier;

import static androidx.ads.identifier.testing.MockPackageManagerHelper.createServiceResolveInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AdvertisingIdUtilsTest {

    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mPackageManager = mock(PackageManager.class);
    }

    @Test
    public void selectServiceByPriority() throws Exception {
        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createResolveInfo("c.normal.1", false, 1),
                createResolveInfo("y.normal.0", false, 0),
                createResolveInfo("x.normal.0", false, 0),
                createResolveInfo("z.high.2", true, 2));

        List<String> priorityList = getPriorityList(resolveInfos);

        assertThat(priorityList).containsExactly(
                "z.high.2",
                "x.normal.0",
                "y.normal.0",
                "c.normal.1"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_firstInstallTime() throws Exception {
        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createResolveInfo("com.a", false, 2),
                createResolveInfo("com.b", false, 9),
                createResolveInfo("com.c", false, 7),
                createResolveInfo("com.d", false, 10),
                createResolveInfo("com.e", false, 0));

        List<String> priorityList = getPriorityList(resolveInfos);

        assertThat(priorityList).containsExactly(
                "com.e",
                "com.a",
                "com.c",
                "com.b",
                "com.d"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_packageName() throws Exception {
        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createResolveInfo("com.abc.id", false, 0),
                createResolveInfo("com.abc", false, 0),
                createResolveInfo("org.example", false, 0),
                createResolveInfo("com.abcde", false, 0),
                createResolveInfo("com.abcde_id", false, 0));

        List<String> priorityList = getPriorityList(resolveInfos);

        assertThat(priorityList).containsExactly(
                "com.abc",
                "com.abc.id",
                "com.abcde",
                "com.abcde_id",
                "org.example"
        ).inOrder();
    }

    private List<String> getPriorityList(List<ResolveInfo> resolveInfos) {
        List<String> result = new ArrayList<>();
        while (resolveInfos.size() > 0) {
            final ServiceInfo serviceInfo =
                    AdvertisingIdUtils.selectServiceByPriority(resolveInfos, mPackageManager);

            result.add(serviceInfo.packageName);

            resolveInfos.removeIf(resolveInfo -> resolveInfo.serviceInfo == serviceInfo);
        }
        return result;
    }

    @Test
    public void selectServiceByPriority_inputNull() {
        assertThat(AdvertisingIdUtils.selectServiceByPriority(null, mPackageManager)).isNull();
    }

    @Test
    public void selectServiceByPriority_inputEmpty() {
        ServiceInfo serviceInfo = AdvertisingIdUtils.selectServiceByPriority(
                Collections.emptyList(), mPackageManager);

        assertThat(serviceInfo).isNull();
    }

    private ResolveInfo createResolveInfo(String packageName, boolean requestHighPriority,
            long firstInstallTime) throws Exception {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        if (requestHighPriority) {
            packageInfo.requestedPermissions =
                    new String[]{AdvertisingIdUtils.HIGH_PRIORITY_PERMISSION};
        }
        packageInfo.firstInstallTime = firstInstallTime;

        when(mPackageManager.getPackageInfo(eq(packageName), eq(PackageManager.GET_PERMISSIONS)))
                .thenReturn(packageInfo);

        return createServiceResolveInfo(packageName);
    }
}
