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

package com.android.internal.telephony.test;

import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.CellInfo;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;

import java.util.List;

class MockPhoneNotifier implements PhoneNotifier, NotificationRegister {

    private final String TAG = "MockPhoneNotifier";

    private RegistrantList mPhoneStateRegistrants = new RegistrantList();
    private RegistrantList mServiceStateRegistrants = new RegistrantList();
    private RegistrantList mCellLocationRegistrants = new RegistrantList();
    private RegistrantList mSignalStrengthRegistrants = new RegistrantList();
    private RegistrantList mMessageWaitingChangedRegistrants = new RegistrantList();
    private RegistrantList mCallForwardingChangedRegistrants = new RegistrantList();
    private RegistrantList mDataConnectionRegistrants = new RegistrantList();
    private RegistrantList mDataConnectionFailedRegistrants = new RegistrantList();
    private RegistrantList mDataActivityRegistrants = new RegistrantList();

    protected Object mStateMonitor = new Object();

    void dispose() {
        mPhoneStateRegistrants.removeCleared();
        mServiceStateRegistrants.removeCleared();
        mCellLocationRegistrants.removeCleared();
        mSignalStrengthRegistrants.removeCleared();
        mMessageWaitingChangedRegistrants.removeCleared();
        mCallForwardingChangedRegistrants.removeCleared();
        mDataConnectionRegistrants.removeCleared();
        mDataConnectionFailedRegistrants.removeCleared();
        mDataActivityRegistrants.removeCleared();
    }

    // **** Implements PhoneNotifier ****

    public void notifyPhoneState(Phone sender) {
        mPhoneStateRegistrants.notifyRegistrants();
    }

    public void notifyServiceState(Phone sender) {
        mServiceStateRegistrants.notifyRegistrants();
    }

    public void notifyCellLocation(Phone sender) {
        mCellLocationRegistrants.notifyRegistrants();
    }

    public void notifySignalStrength(Phone sender) {
        mSignalStrengthRegistrants.notifyRegistrants();
    }

    public void notifyMessageWaitingChanged(Phone sender) {
        mMessageWaitingChangedRegistrants.notifyRegistrants();
    }

    public void notifyCallForwardingChanged(Phone sender) {
        mCallForwardingChangedRegistrants.notifyRegistrants();
    }

    public void notifyDataConnection(Phone sender, String reason, String apnType,
                PhoneConstants.DataState state) {
        Log.d(TAG, "notifyDataConnection() is called - reason:" + reason + ", apnType:"
                + apnType+", PhoneConstants.DataState=" + state);
        mDataConnectionRegistrants.notifyRegistrants();
    }

    public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        Log.d(TAG, "notifyDataConnectionFailed() is called - reason:" + reason + ", apnType:"
                + apnType);
        mDataConnectionFailedRegistrants.notifyRegistrants();
    }

    public void notifyDataActivity(Phone sender) {
        mDataActivityRegistrants.notifyRegistrants();
    }

    public void notifyOtaspChanged(Phone sender, int otaspMode) {
    }

    public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
    }

    // **** Implements Notification Register ****

    public void registerForPhoneState(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPhoneStateRegistrants.add(r);
    }

    public void registerForServiceState(Handler h, int what, Object obj) {
        synchronized (mStateMonitor) {
            Registrant r = new Registrant(h, what, obj);
            mServiceStateRegistrants.add(r);
        }
    }

    public void registerForCellLocation(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCellLocationRegistrants.add(r);
    }

    public void registerForSignalStrength(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSignalStrengthRegistrants.add(r);
    }

    public void registerForMessageWaitingChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mMessageWaitingChangedRegistrants.add(r);
    }

    public void registerForCallForwardingChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCallForwardingChangedRegistrants.add(r);
    }

    public void registerForDataConnection(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataConnectionRegistrants.add(r);
    }

    public void unregisterForServiceState(Handler h) {
        synchronized (mStateMonitor) {
            mServiceStateRegistrants.remove(h);
        }
    }

    public void registerForDataConnectionFailed(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataConnectionFailedRegistrants.add(r);
    }

    public void registerForDataActivity(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataActivityRegistrants.add(r);
    }

}
