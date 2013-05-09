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

import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.DataCallState;

/**
 * Provides API for setting result from RIL, and verifying arguments.
 */
public interface RILSimulator {

    public enum CallRilAPIKind {
        CALLED_SEND_CDMA_SMS,
        CALLED_ACKNOWLEDGE_LAST_INCOMING_CDMA_SMS,
        CALLED_SEND_IMS_GSM_SMS,
        CALLED_ACKNOWLEDGE_LAST_INCOMING_GSM_SMS,
        CALLED_GET_IMS_REGISTRATION_STATE,
    }

    void registerForCallRilApi(Handler h, int what, Object obj);

    // *** Set result for RIL messages ***

    /** Sets result for RIL_REQUEST_VOICE_REGISTRATION_STATE */
    void setVoiceRegistrationState(String[] state, Throwable t);

    /** Sets result for RIL_REQUEST_DATA_REGISTRATION_STATE */
    void setDataRegistrationState(String[] state, Throwable t);

    /** Sets result for RIL_REQUEST_OPERATOR */
    void setOperator(String[] operator, Throwable t);

    /** Sets result for RIL_REQUEST_CDMA_SUBSCRIPTION */
    void setCDMASubscription(String[] state, Throwable t);

    /** Sets result for RIL_REQUEST_SETUP_DATA_CALL */
    void setSetupDataCall(DataCallState success, Throwable t);

    // *** For issuing RIL_UNSOL_RESPONSE_* ***

    /**  Simulates RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED */
    void changeRadioState(RadioState state);

    /** Simulates RIL_UNSOL_RESPONSE_DATA_NETWORK_STATE_CHANGED */
    void changeDataNetworkState();

    /** Simulates RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED */
    void changeVoiceNetworkState();

    /** Simulates RIL_UNSOl_CDMA_PRL_CHANGED */
    void changePrlVersion(Object ret);

    // *** Controls call state ***

    /** If a call is DIALING or ALERTING, progress it to the next state. */
    void progressConnectingCallState();

    /** Trigger an incomming call */
    void triggerRing(String number);

    /** Trigger call waiting */
    void triggerCallWaiting(String number);

   // *** Controls sms state ***

    /** Trigger an incoming CdmaSms */
    void triggerIncomingCdmaSms(Object ret);

    /** Trigger an incoming Cdma Sms StatusReport */
    void triggerIncomingCdmaSmsStatusReport(Object ret);

    /** Trigger an incoming Sms SubmitReport */
    void triggerSmsSubmitReport(int messageRef, String ackPdu, int errorCode, Throwable t);

    /** Trigger an incoming Sms StatusReport */
    void triggerIncomingImsSmsStatusReport(String response);

    /** Trigger an incoming ImsSms */
    void triggerIncomingImsSms(String response);

    /** Trigger notify IMS status */
    void triggerNotifyImsStatus(int imsRegisterd, int technologies);

    /** Trigger an incoming Gsm Broadcast SMS */
    void triggerIncomingGsmBroadcatSms(Object ret);

    // *** Verify that methods was called as expected. ***

    static final String DONOTCARE = "Don't care.";

    void verifySetupDataCall(String arg0, String arg1, String arg2, String arg3, String arg4,
            String arg5, String arg6);

    void verifySendBurstDtmf(String dtmfString, int on, int off);

    void verifyAcknowledgeSms(boolean success, int cause);

    // *** Get detail information of SMS which was sent. ***

    byte[] getSendCdmaSmsPdu();

    String getSendImsGsmSmsPdu();

    String getSendImsGsmSmsSmscPdu();

    int getSendImsGsmSmsRtryCount();

    int getSendImsGsmSmsMessageRef();

}
