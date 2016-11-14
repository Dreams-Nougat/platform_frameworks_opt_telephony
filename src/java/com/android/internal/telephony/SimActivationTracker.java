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

import android.telephony.Rlog;
import android.telephony.SimActivationState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import static android.telephony.SimActivationState.STATE_UNKNOWN;

public class SimActivationTracker {
    /**
     * SimActivationTracker serves as a central place to keep track of all knowledge of
     * voice & data activation state which is set by carrier custom/default apps.
     * Each phone object maintains a single activation tracker.
     */
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SAT";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    private SimActivationState mAS;
    private GsmCdmaPhone mPhone;
    private SubscriptionManager mSubscriptionManager;
    private OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    private LocalLog mVoiceActivationStateLog = new LocalLog(10);
    private LocalLog mDataActivationStateLog = new LocalLog(10);

    public SimActivationTracker(GsmCdmaPhone phone) {
        mPhone = phone;
        mAS = new SimActivationState();
        mSubscriptionManager = SubscriptionManager.from(mPhone.getContext());
        mOnSubscriptionsChangedListener = new OnSubscriptionsChangedListener(){
            public final AtomicInteger mSubId =
                    new AtomicInteger(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            /**
             * Callback invoked when there is any change to any SubscriptionInfo. Typically
             * this method invokes {@link SubscriptionManager#getActiveSubscriptionInfoList}
             */
            @Override
            public void onSubscriptionsChanged() {
                if (VDBG) log("SubscriptionListener.onSubscriptionInfoChanged");
                int subId = mPhone.getSubId();
                if (mSubId.getAndSet(subId) != subId
                        && SubscriptionManager.isValidSubscriptionId(subId)) {
                    if (DBG) {
                        log("onSimInsertorSubIdChanged: " + subId
                                + "setActivationState to UNINT");
                    }
                    setVoiceActivationState(STATE_UNKNOWN);
                    setDataActivationState(STATE_UNKNOWN);
                    logDataActivationStateChange();
                    logVoiceActivationStateChange();
                }
            }
        };
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
    }

    private void logDataActivationStateChange() {
        mDataActivationStateLog.log(mAS.getDataActivationStateStr());
    }

    private void logVoiceActivationStateChange() {
        mVoiceActivationStateLog.log(mAS.getVoiceActivationStateStr());
    }

    public boolean setVoiceActivationState(int state) {
        if (!SimActivationState.isValidActivationState(state, false)) {
            loge("invalid voice activation state: " + state);
            return false;
        }
        if (mAS.setVoiceActivationState(state)) {
            logVoiceActivationStateChange();
            mPhone.notifySimActivationStateChanged(mAS);
        }
        return true;
    }

    public boolean setDataActivationState(int state) {
        if (!SimActivationState.isValidActivationState(state, true)) {
            loge("invalid data activation state: " + state);
            return false;
        }
        if (mAS.setDataActivationState(state)) {
            logDataActivationStateChange();
            mPhone.notifySimActivationStateChanged(mAS);
        }
        return true;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println(" mVoiceActivationState Log:");
        ipw.increaseIndent();
        mVoiceActivationStateLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        pw.println(" mDataActivationState Log:");
        ipw.increaseIndent();
        mDataActivationStateLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
    }

}
