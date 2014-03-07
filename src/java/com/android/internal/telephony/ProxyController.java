/*
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.internal.telephony;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;

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

  //  private SubscriptionManager mSubscriptionManager;

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
       // mSubscriptionManager = SubscriptionManager.getInstance(context, uiccController, ci);

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
