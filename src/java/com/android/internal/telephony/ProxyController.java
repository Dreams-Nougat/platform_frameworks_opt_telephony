/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import android.telephony.Rlog;
import android.telephony.ServiceState;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.uicc.UiccController;

public class ProxyController {
    static final String LOG_TAG = "ProxyController";

    //***** Class Variables
    private static ProxyController sProxyController;

    private Phone[] mProxyPhones;

    private UiccController mUiccController;

    private CommandsInterface[] mCi;

    private Context mContext;

    //UiccPhoneBookController to use proper IccPhoneBookInterfaceManagerProxy object
    private UiccPhoneBookController mUiccPhoneBookController;

    //PhoneSubInfoController to use proper PhoneSubInfoProxy object
    private PhoneSubInfoController mPhoneSubInfoController;

    //UiccSmsController to use proper IccSmsInterfaceManager object
    private UiccSmsController mUiccSmsController;

    private CardSubscriptionManager mCardSubscriptionManager;

    private SubscriptionManager mSubscriptionManager;

    //***** Class Methods
    public static ProxyController getInstance(Context context, Phone[] phoneProxy,
            UiccController uiccController, CommandsInterface[] ci) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phoneProxy, uiccController, ci);
        }
        return sProxyController;
    }

    static public ProxyController getInstance() {
        return sProxyController;
    }

    private ProxyController(Context context, Phone[] phoneProxy, UiccController uiccController,
            CommandsInterface[] ci) {
        logd("Constructor - Enter");

        mContext = context;
        mProxyPhones = phoneProxy;
        mUiccController = uiccController;
        mCi = ci;

        mUiccPhoneBookController = new UiccPhoneBookController(mProxyPhones);
        mPhoneSubInfoController = new PhoneSubInfoController(mProxyPhones);
        mUiccSmsController = new UiccSmsController(mProxyPhones);
        mCardSubscriptionManager = CardSubscriptionManager.getInstance(context, uiccController, ci);
        mSubscriptionManager = SubscriptionManager.getInstance(context, uiccController, ci);

        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        ((PhoneProxy) mProxyPhones[sub]).updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        ((PhoneProxy) mProxyPhones[sub]).setInternalDataEnabled(true);
    }

    public void disableDataConnectivity(int sub,
            Message dataCleanedUpMsg) {
        ((PhoneProxy) mProxyPhones[sub]).setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public boolean enableDataConnectivityFlag(int sub) {
        return ((PhoneProxy) mProxyPhones[sub]).setInternalDataEnabledFlag(true);
    }

    public boolean disableDataConnectivityFlag(int sub) {
        return ((PhoneProxy) mProxyPhones[sub]).setInternalDataEnabledFlag(false);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        ((PhoneProxy) mProxyPhones[sub]).updateCurrentCarrierInProvider();
    }

    public void checkAndUpdatePhoneObject(Subscription userSub) {
        int subId = userSub.subId;
        if ((userSub.appType.equals("SIM")
                || userSub.appType.equals("USIM"))
                && (!mProxyPhones[subId].getPhoneName().equals("GSM"))) {
            logd("gets New GSM phone" );
            ((PhoneProxy) mProxyPhones[subId])
                .updatePhoneObject(ServiceState.RIL_RADIO_TECHNOLOGY_GSM);
        } else if ((userSub.appType.equals("RUIM")
                || userSub.appType.equals("CSIM")
                || userSub.appType.equals("GLOBAL"))
                && (!mProxyPhones[subId].getPhoneName().equals("CDMA"))) {
            logd("gets New CDMA phone" );
            ((PhoneProxy) mProxyPhones[subId])
                .updatePhoneObject(ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT);
        }
    }

    public void registerForAllDataDisconnected(int sub, Handler h, int what, Object obj) {
        ((PhoneProxy) mProxyPhones[sub]).registerForAllDataDisconnected(h, what, obj);
    }

    public void unregisterForAllDataDisconnected(int sub, Handler h) {
        ((PhoneProxy) mProxyPhones[sub]).unregisterForAllDataDisconnected(h);
    }

    public boolean isDataDisconnected(int sub) {
        Phone activePhone = ((PhoneProxy) mProxyPhones[sub]).getActivePhone();
        return ((PhoneBase) activePhone).mDcTracker.isDisconnected();
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }
}
