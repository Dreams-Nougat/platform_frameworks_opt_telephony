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

package com.android.internal.telephony;

import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;

import java.util.ArrayList;

public class DummyCommandsInterface extends BaseCommands {

    private static String mUser;
    private static String mPassword;
    private static String mApnStr;

    public DummyCommandsInterface() {
        super(null);
        mUser = null;
        mPassword = null;
        mApnStr = null;
    }

    @Override
    public RadioState getRadioState() {
        return RadioState.RADIO_ON;
    }

    @Override
    public void registerForRadioStateChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForRadioStateChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForDataNetworkStateChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForDataNetworkStateChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForOn(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForOn(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForAvailable(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForAvailable(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForNotAvailable(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForNotAvailable(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForOffOrNotAvailable(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForOffOrNotAvailable(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForSIMReady(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForSIMReady(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForSIMLockedOrAbsent(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForSIMLockedOrAbsent(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForCallStateChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForCallStateChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForDataStateChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForDataStateChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForRadioTechnologyChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForRadioTechnologyChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForNVReady(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForNVReady(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForRUIMLockedOrAbsent(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForRUIMLockedOrAbsent(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        // TODO Auto-generated method stub
    }

    public void registerForRUIMReady(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    public void unregisterForRUIMReady(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForIccStatusChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForIccStatusChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnSmsOnSim(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnSmsOnSim(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnSmsStatus(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnSmsStatus(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnNITZTime(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnUSSD(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnUSSD(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnSignalStrengthUpdate(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnIccSmsFull(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnIccSmsFull(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnIccRefresh(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unsetOnIccRefresh(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnCallRing(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnCallRing(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnRestrictedStateChanged(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnRestrictedStateChanged(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnSuppServiceNotification(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForDisplayInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForCallWaitingInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForCallWaitingInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForSignalInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForSignalInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnUnsolOemHookRaw(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unSetOnUnsolOemHookRaw(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForNumberInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForNumberInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForLineControlInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForT53ClirInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setEmergencyCallbackMode(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForCdmaOtaProvision(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForCdmaOtaProvision(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForRingbackTone(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregisterForResendIncallMute(Handler h) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPin(String pin, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPin2(String pin2, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
            Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyNetworkDepersonalization(String netpin, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getCurrentCalls(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getPDPContextList(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getDataCallList(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void dial(String address, int clirMode, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        // TODO Auto-generated method stub
    }

    public void dial(String address, int clirMode, UUSInfo uusInfo, int a, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getIMSI(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getIMEI(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getIMEISV(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void hangupWaitingOrBackground(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void hangupForegroundResumeBackground(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void conference(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void acceptCall(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void rejectCall(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void explicitCallTransfer(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getLastCallFailCause(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getLastPdpFailCause(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getLastDataCallFailCause(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setMute(boolean enableMute, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getMute(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getSignalStrength(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getOperator(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendDtmf(char c, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void startDtmf(char c, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void stopDtmf(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendSMS(String smscPDU, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendSMSExpectMore(String smscPDU, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteSmsOnSim(int index, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteSmsOnRuim(int index, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeSmsToRuim(int status, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setRadioPower(boolean on, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void iccIO(int command, int fileid, String path, int p1, int p2, int p3, String data,
            String pin2, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryCLIP(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getCLIR(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCLIR(int clirMode, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number,
            int timeSeconds, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number,
            Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getNetworkSelectionMode(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAvailableNetworks(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getBasebandVersion(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryFacilityLock(String facility, String password, int serviceClass,
            Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendUSSD(String ussdString, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void cancelPendingUssd(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetRadio(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setBandMode(int bandMode, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryAvailableBandMode(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getPreferredNetworkType(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getNeighboringCids(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setLocationUpdates(boolean enable, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getSmscAddress(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendTerminalResponse(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendEnvelope(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleCallSetupRequestFromSim(boolean accept, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getGsmBroadcastConfig(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getDeviceIdentity(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getCDMASubscription(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendCDMAFeatureCode(String FeatureCode, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setPhoneType(int phoneType) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTTYMode(int ttyMode, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryTTYMode(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setupDataCall(String radioTechnology, String profile, String apn, String user,
            String password, String authType, String protocol, Message result) {
        // TODO Auto-generated method stub
        mUser = user;
        mPassword = password;
        mApnStr = apn;
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getCdmaBroadcastConfig(Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void exitEmergencyCallbackMode(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getIccCardStatus(Message result) {
        // TODO Auto-generated method stub
    }

    public void validateAndStoreAuthenticationKey(String akey, Message response) {
        // TODO Auto-generated method stub
    }

    public void iccExchangeAPDU(int cla, int command, int channel, int p1, int p2, int p3,
            String data, Message response) {
        // TODO Auto-generated method stub
    }

    public void iccOpenChannel(String aid, Message response) {
        // TODO Auto-generated method stub
    }

    public void iccCloseChannel(int channel, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void requestIsimAuthentication(String nonce, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getCdmaSubscriptionSource(Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockState, String password,
        int serviceClass, String appId, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void queryFacilityLockForApp(String facility, String password, int serviceClass,
        String appId, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getDataRegistrationState (Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getVoiceRegistrationState (Message response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPin2ForApp(String pin2, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    public void getQosStatus (int qosId, Message result){}

    public void setupQosReq (int callId, ArrayList<String> qosFlows, Message result){}

    public void releaseQos (int qosId, Message result){}

    public void modifyQos (int qosId, ArrayList<String> qosFlows, Message result){}

    public void suspendQos (int qosId, Message result){}

    public void resumeQos (int qosId, Message result){}

    public void setSubscriptionMode (int subscriptionMode, Message result){}

    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
            Message result){}

    public void getDataCallProfile(int appType, Message result){}

    public void iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message response) {
    }

    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message response) {
    }

    public void setDataSubscription (Message result) {
    }

    public void sendImsGsmSms (String smscPDU, String pdu, int retry, int messageRef,
            Message response) {
    }

    public void acceptCall(Message result, int callType) {
    }

    public void getIMSIForApp(String aid, Message result) {
    }

    public void supplyDepersonalization(String netpin, int type, Message result) {
    }

    public void getImsRegistrationState(Message result) {
    }

    public void getVoiceRadioTechnology(Message result) {
    }

    public void getAtr(int slot, Message message) {
    }
}
