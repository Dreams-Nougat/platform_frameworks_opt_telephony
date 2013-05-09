/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.telephony.runners;

import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

import com.android.internal.telephony.cdma.FCdmaPhoneCallTest;
import com.android.internal.telephony.FSMSNormalTest;
import com.android.internal.telephony.FSubmitDeliverTest;

import junit.framework.TestSuite;

public class FunctionalTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new InstrumentationTestSuite(this);
        suite.addTestSuite(FCdmaPhoneCallTest.class);
        suite.addTestSuite(FSMSNormalTest.class);
        suite.addTestSuite(FSubmitDeliverTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return FunctionalTestRunner.class.getClassLoader();
    }
}
