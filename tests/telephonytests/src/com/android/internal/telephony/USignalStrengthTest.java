/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.res.Resources;
import android.os.Parcel;
import android.telephony.SignalStrength;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Test case of to check signal strength of UMTS.
 *
 * This is the test case confirming that the number of antenna bars
 * is set according to the judgment of the value of RSCP.
 */
public class USignalStrengthTest extends AndroidTestCase {
    private static final String LOG_TAG = "USignalStrengthTest";

    private static final int[] sUmtsDbmThresholds =
            Resources.getSystem().getIntArray(
            com.android.internal.R.array.config_umtsDbmThresholds);

    private static final int mGreatSignalStrength         = Math.abs(sUmtsDbmThresholds[0]);
    private static final int mGoodMaxSignalStrength       = Math.abs(sUmtsDbmThresholds[0]-1);
    private static final int mGoodMinSignalStrength       = Math.abs(sUmtsDbmThresholds[1]);
    private static final int mModerateMaxSignalStrength   = Math.abs(sUmtsDbmThresholds[1]-1);
    private static final int mModerateMinSignalStrength   = Math.abs(sUmtsDbmThresholds[2]);
    private static final int mPoorMaxSignalStrength       = Math.abs(sUmtsDbmThresholds[2]-1);
    private static final int mPoorMinSignalStrength       = Math.abs(sUmtsDbmThresholds[3]);
    private static final int mNoneOrUnknownSignalStrength = Math.abs(sUmtsDbmThresholds[3]-1);

    private static SignalStrength createSignalStrengthReport(
            int gsmSignalStrength, int gsmBitErrorRate, int umtsRscp,
            int cdmaDbm, int cdmaEcio, int evdoDbm, int evdoEcio, int evdoSnr,
            int lteSignalStrength, int lteRsrp, int lteRsrq, int lteRssnr, int lteCqi,
            int tdScdmaRscp, boolean gsmFlag) {

        Parcel parcel = Parcel.obtain();
        parcel.writeInt(gsmSignalStrength);
        parcel.writeInt(gsmBitErrorRate);
        parcel.writeInt(umtsRscp);
        parcel.writeInt(cdmaDbm);
        parcel.writeInt(cdmaEcio);
        parcel.writeInt(evdoDbm);
        parcel.writeInt(evdoEcio);
        parcel.writeInt(evdoSnr);
        parcel.writeInt(lteSignalStrength);
        parcel.writeInt(lteRsrp);
        parcel.writeInt(lteRsrq);
        parcel.writeInt(lteRssnr);
        parcel.writeInt(lteCqi);
        parcel.writeInt(tdScdmaRscp);
        parcel.writeInt(gsmFlag ? 1 : 0);
        parcel.setDataPosition(0);
        SignalStrength signalStrength = new SignalStrength(parcel);
        signalStrength.validateInput();
        return signalStrength;
    }

    private static SignalStrength createSignalStrengthUmtsReport(int umtsRscp) {

        return createSignalStrengthReport(
                12, 0, umtsRscp,
                -1, -1, -1, -1, -1,
                99, SignalStrength.INVALID, SignalStrength.INVALID, SignalStrength.INVALID,
                SignalStrength.INVALID,SignalStrength.INVALID,
                true);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRscpGreat() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthUmtsReport(mGreatSignalStrength).getLevel());
    }

    public void testRscpGoodMax() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthUmtsReport(mGoodMaxSignalStrength).getLevel());
    }

    public void testRscpGoodMin() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthUmtsReport(mGoodMinSignalStrength).getLevel());
    }

    public void testRscpModerateMax() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthUmtsReport(mModerateMaxSignalStrength).getLevel());
    }

    public void testRscpModerateMin() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthUmtsReport(mModerateMinSignalStrength).getLevel());
    }

    public void testRscpPoorMax() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthUmtsReport(mPoorMaxSignalStrength).getLevel());
    }

    public void testRscpPoorMin() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthUmtsReport(mPoorMinSignalStrength).getLevel());
    }

    public void testRscpNoneOrUnknown() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthUmtsReport(mNoneOrUnknownSignalStrength).getLevel());
    }
}
