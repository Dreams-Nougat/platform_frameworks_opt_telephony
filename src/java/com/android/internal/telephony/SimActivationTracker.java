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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.Rlog;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_ACTIVATED;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_DEACTIVATED;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_UNKNOWN;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_RESTRICTED;

public class SimActivationTracker {
    /**
     * SimActivationTracker serves as a central place to keep track of all knowledge of
     * voice & data activation state which is set by carrier custom/default apps.
     * Each phone object maintains a single activation tracker.
     */
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SAT";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    private Phone mPhone;
    /**
     * Voice Activation State
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_UNKNOWN
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_DEACTIVATED
     */
    private String mVoiceActivationState;
    /**
     * Data Activation State
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_UNKNOWN
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_DEACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_RESTRICTED
     */
    private String mDataActivationState;
    private LocalLog mVoiceActivationStateLog = new LocalLog(10);
    private LocalLog mDataActivationStateLog = new LocalLog(10);
    private BroadcastReceiver sReceiver;

    public SimActivationTracker(Phone phone) {
        mPhone = phone;
        mVoiceActivationState = SIM_ACTIVATION_STATE_UNKNOWN;
        mDataActivationState = SIM_ACTIVATION_STATE_UNKNOWN;

        sReceiver = new  BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (VDBG) log("action: " + action);
                if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)){
                    if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(
                            intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE))) {
                        if (DBG) {
                            log("onSimAbsent, reset activation state to UNKNOWN");
                        }
                        setVoiceActivationState(SIM_ACTIVATION_STATE_UNKNOWN);
                        setDataActivationState(SIM_ACTIVATION_STATE_UNKNOWN);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mPhone.getContext().registerReceiver(sReceiver, intentFilter);
    }

    public boolean setVoiceActivationState(String state) {
        if (!isValidActivationState(state) || SIM_ACTIVATION_STATE_RESTRICTED.equals(state)) {
            loge("invalid voice activation state: " + state);
            return false;
        }
        if (DBG) log("setVoiceActivationState=" + state);
        mVoiceActivationState = state;
        mVoiceActivationStateLog.log(state);
        mPhone.notifyVoiceActivationStateChanged(state);
        return true;
    }

    public boolean setDataActivationState(String state) {
        if (!isValidActivationState(state)) {
            loge("invalid data activation state: " + state);
            return false;
        }
        if (DBG) log("setDataActivationState=" + state);
        mDataActivationState = state;
        mDataActivationStateLog.log(state);
        mPhone.notifyDataActivationStateChanged(state);
        return true;
    }

    public String getVoiceActivationState() {
        return mVoiceActivationState;
    }

    public String getDataActivationState() {
        return mDataActivationState;
    }

    private static boolean isValidActivationState(String state) {
        switch (state) {
            case SIM_ACTIVATION_STATE_UNKNOWN:
            case SIM_ACTIVATION_STATE_ACTIVATED:
            case SIM_ACTIVATION_STATE_DEACTIVATED:
            case SIM_ACTIVATION_STATE_RESTRICTED:
                return true;
            default:
                return false;
        }
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

    public void dispose() {
        mPhone.getContext().unregisterReceiver(sReceiver);
    }
}