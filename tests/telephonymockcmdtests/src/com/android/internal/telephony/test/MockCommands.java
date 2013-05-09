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

import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DataCallState;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.CallFailCause;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

class MockCommands extends BaseCommands implements RILSimulator {

    private static final String TAG = "MockCommands";

    private int mResutOrder;

    private RILResult mRegistrationStateResult;
    private RILResult mDataRegistrationStateResult;
    private RILResult mCDMASubscriptionResult;
    private RILResult mOperatorResult;
    private RILResult mSetupDataCallResult;
    private RILResult mSmsResponseResult;
    private byte[] mSendCdmaSmsArguments = null;
    private byte[] mSendImsGsmSmsArguments = null;
    private boolean mAcknowledgeSmsSuccess = false;
    private int mAcknowledgeSmsCause = 0;
    private int mImsRegisterd = 0; // IMS is NOT registered
    private int mTechnologies = 2; // 3GPP2 (CDMA, EVDO)
    private String mImsGsmSmsSmscPdu = null;
    private String mImsGsmSmsPdu = null;
    private int mRetry = 0;
    private int mMessageRef = 0;
    private String[] mSetupDataCallArguments = null;
    private String[] mSendBurstDtmfArguments = null;

    private SimulatedCdmaCallState mSimulatedCallState;
    private Message mResponse;
    RegistrantList mCallRilApiRegistrants =  new RegistrantList();
    // **** Constructor ****

    public MockCommands(Context context, int phoneType) {
        super(context);
        mPhoneType = phoneType;
        mResutOrder = 0;
        mSimulatedCallState = new SimulatedCdmaCallState();
    }

    // **** Implements CommandsInterface ****
    public void setSuppServiceNotifications(boolean enable, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPin(String pin, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPuk(String puk, String newPin, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPin2(String pin2, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        // TODO Auto-generated method stub
    }

    public void changeIccPin(String oldPin, String newPin, Message result) {
        // TODO Auto-generated method stub
    }

    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        // TODO Auto-generated method stub
    }

    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message r) {
        // TODO Auto-generated method stub
    }

    public void supplyNetworkDepersonalization(String netpin, Message result) {
        // TODO Auto-generated method stub
    }

    public void getCurrentCalls(Message result) {
        Log.d(TAG, "getCurrentCalls() is called.");
        List<DriverCall> ret = mSimulatedCallState.getDriverCalls();
        AsyncResult.forMessage(result, ret, null);
        result.sendToTarget();
    }

    public void getPDPContextList(Message result) {
        // TODO Auto-generated method stub
    }

    public void getDataCallList(Message result) {
        // TODO Auto-generated method stub
    }

    public void dial(String address, int clirMode, Message result) {
        Log.d(TAG, "dial() is called - address:" + address + ", clirMode:" + clirMode);
        AsyncResult.forMessage(result);
        mSimulatedCallState.onDial(address);

        result.getTarget().sendMessageDelayed(result, 100);
    }

    public void dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        // TODO Auto-generated method stub
    }

    public void dial(String address, int clirMode, UUSInfo uusInfo, int escv, Message result) {
        // TODO Auto-generated method stub
    }

    public void getIMSI(Message result) {
        Log.d(TAG, "getIMSI() is called");
        AsyncResult.forMessage(result, "440500123456789", null);
        result.sendToTarget();
    }

    public void getIMEI(Message result) {
        // TODO Auto-generated method stub
    }

    public void getIMEISV(Message result) {
        // TODO Auto-generated method stub
    }

    public void hangupConnection(int gsmIndex, Message result) {
        // TODO Auto-generated method stub
    }

    public void hangupWaitingOrBackground(Message result) {
        // TODO Auto-generated method stub
    }

    public void hangupForegroundResumeBackground(Message result) {
        mSimulatedCallState.releaseActiveAcceptHeldOrWaiting();
        result.getTarget().sendMessageDelayed(result, 100);
    }

    public void switchWaitingOrHoldingAndActive(Message result) {
        // TODO Auto-generated method stub
    }

    public void conference(Message result) {
        // TODO Auto-generated method stub
    }

    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        // TODO Auto-generated method stub
    }

    public void getPreferredVoicePrivacy(Message result) {
        // TODO Auto-generated method stub
    }

    public void separateConnection(int gsmIndex, Message result) {
        // TODO Auto-generated method stub
    }

    public void acceptCall(Message result) {
        mSimulatedCallState.onAnswer();
        result.getTarget().sendMessageDelayed(result, 100);
    }

    public void rejectCall(Message result) {
        // TODO Auto-generated method stub
    }

    public void explicitCallTransfer(Message result) {
        // TODO Auto-generated method stub
    }

    public void getLastCallFailCause(Message result) {
        Log.d(TAG, "getLastCallFailCause() is called.");
        int[] ret = new int[1];
        ret[0] = CallFailCause.NORMAL_CLEARING;
        AsyncResult.forMessage(result, ret, null);
        result.sendToTarget();
    }

    public void getLastPdpFailCause(Message result) {
        // TODO Auto-generated method stub
    }

    public void getLastDataCallFailCause(Message result) {
        // TODO Auto-generated method stub
    }

    public void setMute(boolean enableMute, Message response) {
        // TODO Auto-generated method stub
    }

    public void getMute(Message response) {
        // TODO Auto-generated method stub
    }

    public void getSignalStrength(Message response) {
        // TODO Auto-generated method stub
    }

    public void getOperator(Message response) {
        Log.d(TAG, "getOperator() is called." );
        AsyncResult.forMessage(response, mOperatorResult.result, mOperatorResult.exception);
        sendResult(mOperatorResult, response);
    }

    public void sendDtmf(char c, Message result) {
        // TODO Auto-generated method stub
    }

    public void startDtmf(char c, Message result) {
        // TODO Auto-generated method stub
    }

    public void stopDtmf(Message result) {
        // TODO Auto-generated method stub
    }

    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        Log.d(TAG, "sendBurstDtmf(" + dtmfString + ", " + on + ", " + off + ")");

        // Add codes for sending result if necessary.

        mSendBurstDtmfArguments = new String[3];
        mSendBurstDtmfArguments[0] = dtmfString;
        mSendBurstDtmfArguments[1] = Integer.valueOf(on).toString();
        mSendBurstDtmfArguments[2] = Integer.valueOf(off).toString();
    }

    public void sendSMS(String smscPDU, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendSMSExpectMore(String smscPDU, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendCdmaSms(byte[] pdu, Message response) {
        Log.d(TAG, "Called sendCdmaSms");
        mSendCdmaSmsArguments = pdu;
        mResponse = response;

        // debug
        for(int i=0 ; i < mSendCdmaSmsArguments.length; i++){
            Log.d(TAG, Integer.toHexString(mSendCdmaSmsArguments[i] & 0xff));
        }

        // notify TestCase that RIL API was called.
        notifyRegistrants(mCallRilApiRegistrants,
                new AsyncResult(null, CallRilAPIKind.CALLED_SEND_CDMA_SMS, null));
    }

    public void deleteSmsOnSim(int index, Message response) {
        // TODO Auto-generated method stub
    }

    public void deleteSmsOnRuim(int index, Message response) {
        // TODO Auto-generated method stub
    }

    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void writeSmsToRuim(int status, String pdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void setupDefaultPDP(String apn, String user, String password, Message response) {
        // TODO Auto-generated method stub
    }

    public void deactivateDefaultPDP(int cid, Message response) {
        // TODO Auto-generated method stub
    }

    public void setRadioPower(boolean on, Message response) {
        // TODO Auto-generated method stub
    }

    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message response) {
       Log.d(TAG, "acknowledgeLastIncomingGsmSms is called - success:"
                + success + ", cause:" + cause);

        mAcknowledgeSmsSuccess = success;
        mAcknowledgeSmsCause = cause;               // cause code according to X.S004-550E

        // notify TestCase that RIL API was called.
        notifyRegistrants(mCallRilApiRegistrants,
                new AsyncResult(null,
                        CallRilAPIKind.CALLED_ACKNOWLEDGE_LAST_INCOMING_GSM_SMS, null));

        if (null != response) {
            AsyncResult.forMessage(response, null, null);
            response.sendToTarget();
        }
    }

    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message response) {
        Log.d(TAG, "acknowledgeLastIncomingCdmaSms is called - success:"
                + success + ", cause:" + cause);

        mAcknowledgeSmsSuccess = success;           // RIL_CDMA_SMS_ErrorClass
        mAcknowledgeSmsCause = cause;               // cause code according to X.S004-550E

        // notify TestCase that RIL API was called.
        notifyRegistrants(mCallRilApiRegistrants,
                new AsyncResult(null,
                        CallRilAPIKind.CALLED_ACKNOWLEDGE_LAST_INCOMING_CDMA_SMS, null));

        if (null != response) {
            AsyncResult.forMessage(response, null, null);
            response.sendToTarget();
        }
    }

    public void iccIO(int command, int fileid, String path, int p1, int p2, int p3, String data,
            String pin2, Message response) {

        Log.d(TAG, "iccIO() -"
                + " command:0x" + Integer.toHexString(command)
                + " fileid:0x" + Integer.toHexString(fileid));

        switch (mPhoneType) {
            case RILConstants.CDMA_PHONE:
                if (command == 0xc0 && fileid == IccConstants.EF_ICCID) {
                    byte[] result = IccUtils.hexStringToBytes("0000000a2fe2040000000005020000");
                    response.obj = new AsyncResult(response.obj,
                            new IccIoResult(0x90, 0x0, result), null);
                    response.sendToTarget();

                } else if (command == 0xb0 && fileid == IccConstants.EF_ICCID) {
                    byte[] result = IccUtils.hexStringToBytes("981803000101003603f3");
                    response.obj = new AsyncResult(response.obj,
                            new IccIoResult(0x90, 0x0, result), null);
                    response.sendToTarget();
                }
                break;
            case RILConstants.GSM_PHONE:
                // TODO Implements SIM data loading.
                break;
            default:
                break;
        }
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

    public void queryCLIP(Message response) {
        // TODO Auto-generated method stub
    }

    public void getCLIR(Message response) {
        // TODO Auto-generated method stub
    }

    public void setCLIR(int clirMode, Message response) {
        // TODO Auto-generated method stub
    }

    public void queryCallWaiting(int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    public void setCallWaiting(boolean enable, int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    public void setCallForward(int action, int cfReason, int serviceClass, String number,
            int timeSeconds, Message response) {
        // TODO Auto-generated method stub
    }

    public void queryCallForwardStatus(int cfReason, int serviceClass, String number,
            Message response) {
        // TODO Auto-generated method stub
    }

    public void setNetworkSelectionModeAutomatic(Message response) {
        // TODO Auto-generated method stub
    }

    public void setNetworkSelectionModeManual(String operatorNumeric, Message response) {
        // TODO Auto-generated method stub
    }

    public void getNetworkSelectionMode(Message response) {
        Log.d(TAG, "getNetworkSelectionMode() is called.");
        int[] ret = new int[1];
        ret[0] = 0;
        AsyncResult.forMessage(response, ret, null);
        response.sendToTarget();
    }

    public void getAvailableNetworks(Message response) {
        // TODO Auto-generated method stub
    }

    public void getBasebandVersion(Message response) {
        Log.d(TAG, "getBasebandVersion() is called.");
        String ret = "1234A-ABCDEFGH-1234-00";
        AsyncResult.forMessage(response, ret, null);
        response.sendToTarget();
    }

    public void queryFacilityLock(String facility, String password, int serviceClass,
            Message response) {
        // TODO Auto-generated method stub
    }

    public void setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendUSSD(String ussdString, Message response) {
        // TODO Auto-generated method stub
    }

    public void cancelPendingUssd(Message response) {
        // TODO Auto-generated method stub
    }

    public void resetRadio(Message result) {
        // TODO Auto-generated method stub
    }

    public void setBandMode(int bandMode, Message response) {
        // TODO Auto-generated method stub
    }

    public void queryAvailableBandMode(Message response) {
        // TODO Auto-generated method stub
    }

    public void setPreferredNetworkType(int networkType, Message response) {
        // TODO Auto-generated method stub
    }

    public void getPreferredNetworkType(Message response) {
        // TODO Auto-generated method stub
    }

    public void getNeighboringCids(Message response) {
        // TODO Auto-generated method stub
    }

    public void setLocationUpdates(boolean enable, Message response) {
        // TODO Auto-generated method stub
    }

    public void getSmscAddress(Message result) {
        // TODO Auto-generated method stub
    }

    public void setSmscAddress(String address, Message result) {
        // TODO Auto-generated method stub
    }

    public void reportSmsMemoryStatus(boolean available, Message result) {
        // TODO Auto-generated method stub
    }

    public void reportStkServiceIsRunning(Message result) {
        // TODO Auto-generated method stub
    }

    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        // TODO Auto-generated method stub
    }

    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendTerminalResponse(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendEnvelope(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    public void handleCallSetupRequestFromSim(boolean accept, Message response) {
        // TODO Auto-generated method stub
    }

    public void setGsmBroadcastActivation(boolean activate, Message result) {
        // TODO Auto-generated method stub
    }

    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {
        // TODO Auto-generated method stub
    }

    public void getGsmBroadcastConfig(Message response) {
        // TODO Auto-generated method stub
    }

    public void getDeviceIdentity(Message response) {
        Log.d(TAG, "getDeviceIdentity() is called.");
        String[] ret = new String[4];
        ret[0] = "004401234567890";
        ret[1] = "00";
        ret[2] = "1234567A";
        ret[3] = "00440123456789";

        AsyncResult.forMessage(response, ret, null);
        response.sendToTarget();
    }

    public void getCDMASubscription(Message response) {
        Log.d(TAG, "getCDMASubscription() is called.");
        if (mCDMASubscriptionResult != null) {
            AsyncResult.forMessage(response,
                    mCDMASubscriptionResult.result,
                    mCDMASubscriptionResult.exception);
            response.sendToTarget();
        } else {
            String[] subs = RILResponseUtil.CDMASubscription.REAL_SIM();
            setCDMASubscription(subs, null);
            AsyncResult.forMessage(response,
                    mCDMASubscriptionResult.result,
                    mCDMASubscriptionResult.exception);
            response.sendToTarget();
        }
    }

    public void sendCDMAFeatureCode(String FeatureCode, Message response) {
        // TODO Auto-generated method stub
    }

    public void setPhoneType(int phoneType) {
        // TODO Auto-generated method stub
    }

    public void queryCdmaRoamingPreference(Message response) {
        // TODO Auto-generated method stub
    }

    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        // TODO Auto-generated method stub
    }

    public void setCdmaSubscription(int cdmaSubscriptionType, Message response) {
        // TODO Auto-generated method stub
    }

    public void setTTYMode(int ttyMode, Message response) {
        // TODO Auto-generated method stub
    }

    public void queryTTYMode(Message response) {
        // TODO Auto-generated method stub
    }

    public void setTransmitPower(int powerLevel, Message response) {
        // TODO Auto-generated method stub
    }

    public void requestIsimAuthentication(String nonce, Message response) {
        // TODO Auto-generated method stub
    }

    public void getCdmaSubscriptionSource(Message response) {
        Log.d(TAG, "getCdmaSubscriptionSource() is called.");
        int[] ret = new int[1];
        ret[0] = 0;
        AsyncResult.forMessage(response, ret, null);
        response.sendToTarget();
    }

    public void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response) {
        // TODO Auto-generated method stub
    }

    public void sendEnvelopeWithStatus(String contents, Message response) {
        // TODO Auto-generated method stub
    }

    public void setFacilityLockForApp(String facility, boolean lockState, String password,
        int serviceClass, String appId, Message response) {
        // TODO Auto-generated method stub
    }

    public void queryFacilityLockForApp(String facility, String password, int serviceClass,
        String appId, Message response) {
        Log.d(TAG, "queryFacilityLockForApp() is called");
        int[] ret = new int[1];
        ret[0] = 2;
        AsyncResult.forMessage(response, ret, null);
        response.sendToTarget();
    }

    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message response) {
        // TODO Auto-generated method stub
    }

    public void getDataRegistrationState (Message response) {
        Log.d(TAG, "getDataRegistrationState() is called");
        AsyncResult.forMessage(response,
                mDataRegistrationStateResult.result,
                mDataRegistrationStateResult.exception);
        sendResult(mDataRegistrationStateResult, response);
     }

    public void getVoiceRegistrationState (Message response) {
        Log.d(TAG, "getVoiceRegistrationState() is called." );
        AsyncResult.forMessage(response,
                mRegistrationStateResult.result,
                mRegistrationStateResult.exception);
        sendResult(mRegistrationStateResult, response);
    }

    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message result) {
        // TODO Auto-generated method stub
    }

    public void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message result) {
        // TODO Auto-generated method stub

    }

    public void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPin2ForApp(String pin2, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    public void supplyIccPinForApp(String pin, String aid, Message result) {
        // TODO Auto-generated method stub
    }

    public void setupDataCall(String radioTechnology, String profile, String apn, String user,
            String password, String authType, String protocol, Message result) {

        Log.d(TAG, "setupDataCall() is called - user:" + user + " password:" + password);

        AsyncResult.forMessage(result, mSetupDataCallResult.result, mSetupDataCallResult.exception);
        result.sendToTarget();

        mSetupDataCallArguments = new String[7];
        mSetupDataCallArguments[0] = radioTechnology;
        mSetupDataCallArguments[1] = profile;
        mSetupDataCallArguments[2] = apn;
        mSetupDataCallArguments[3] = user;
        mSetupDataCallArguments[4] = password;
        mSetupDataCallArguments[5] = authType;
        mSetupDataCallArguments[6] = protocol;
    }


    public void deactivateDataCall(int cid, int reason, Message result) {
        // TODO Auto-generated method stub
    }

    public void setCdmaBroadcastActivation(boolean activate, Message result) {
        // TODO Auto-generated method stub

    }

    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message result) {
        // TODO Auto-generated method stub
    }

    public void getCdmaBroadcastConfig(Message result) {
        // TODO Auto-generated method stub
    }

    public void getCdmaPrlVersion(Message result) {
        // TODO Auto-generated method stub
    }

    public void exitEmergencyCallbackMode(Message response) {
        // TODO Auto-generated method stub
    }

    public void getIccCardStatus(Message result) {
        Log.d(TAG, "getIccCardStatus() is called");
        IccCardApplicationStatus ca;
        IccCardStatus status = new IccCardStatus();
        status.setCardState(1);
        status.setUniversalPinState(0);
        status.mGsmUmtsSubscriptionAppIndex = 0;
        status.mCdmaSubscriptionAppIndex = 1;
        status.mImsSubscriptionAppIndex = 2;
        int numApplications = 3;
        status.mApplications = new IccCardApplicationStatus[numApplications];

        ca = new IccCardApplicationStatus();
        //IccCardApplicationStatus.AppType.APPTYPE_USIM
        ca.app_type       = AppType.APPTYPE_USIM;
        ca.app_state      = AppState.APPSTATE_READY;
        ca.perso_substate = PersoSubState.PERSOSUBSTATE_UNKNOWN;
        ca.aid            = "";
        ca.app_label      = "";
        ca.pin1_replaced  = 3;
        ca.pin1           = PinState.PINSTATE_DISABLED;
        ca.pin2           = PinState.PINSTATE_ENABLED_NOT_VERIFIED;
        status.mApplications[0] = ca;

        IccCardApplicationStatus ca1 = new IccCardApplicationStatus();
        //IccCardApplicationStatus.AppType.APPTYPE_CSIM
        ca1.app_type       = AppType.APPTYPE_CSIM;
        ca1.app_state      = AppState.APPSTATE_READY;
        ca1.perso_substate = PersoSubState.PERSOSUBSTATE_UNKNOWN;
        ca1.aid            = "";
        ca1.app_label      = "";
        ca1.pin1_replaced  = 3;
        ca1.pin1           = PinState.PINSTATE_DISABLED;
        ca1.pin2           = PinState.PINSTATE_ENABLED_NOT_VERIFIED;
        status.mApplications[1] = ca1;

        IccCardApplicationStatus ca2 = new IccCardApplicationStatus();
        //IccCardApplicationStatus.AppType.APPTYPE_ISIM
        ca2.app_type       = AppType.APPTYPE_ISIM;
        ca2.app_state      = AppState.APPSTATE_READY;
        ca2.perso_substate = PersoSubState.PERSOSUBSTATE_UNKNOWN;
        ca2.aid            = "";
        ca2.app_label      = "";
        ca2.pin1_replaced  = 3;
        ca2.pin1           = PinState.PINSTATE_DISABLED;
        ca2.pin2           = PinState.PINSTATE_ENABLED_NOT_VERIFIED;
        status.mApplications[2] = ca2;

        AsyncResult.forMessage(result, status, null);
        result.sendToTarget();
    }

    public void validateAndStoreAuthenticationKey(String akey, Message response) {
        // TODO Auto-generated method stub
    }

    // **** Implements RILSimulator ****

    public void setVoiceRegistrationState(String[] state, Throwable t) {
        mRegistrationStateResult = new RILResult(state, t, mResutOrder++);
    }

    public void setDataRegistrationState(String[] state, Throwable t) {
        mDataRegistrationStateResult = new RILResult(state, t, mResutOrder++);
    }

    public void setOperator(String[] operator, Throwable t) {
        mOperatorResult = new RILResult(operator, t);
    }


    public void setCDMASubscription(String[] state, Throwable t) {
        Log.d(TAG, "setCDMASubscription() is called");
        mCDMASubscriptionResult = new RILResult(state, t);
    }

    public void setSetupDataCall(DataCallState success, Throwable t) {
        mSetupDataCallResult = new RILResult(success, t);
    }


    public void verifySetupDataCall(String arg0, String arg1, String arg2, String arg3,
            String arg4, String arg5, String arg6) {

        Assert.assertNotNull("setupDataCall() was not called.", mSetupDataCallArguments);

        if (arg0 != DONOTCARE) Assert.assertEquals(arg0, mSetupDataCallArguments[0]);
        if (arg1 != DONOTCARE) Assert.assertEquals(arg1, mSetupDataCallArguments[1]);
        if (arg2 != DONOTCARE) Assert.assertEquals(arg2, mSetupDataCallArguments[2]);
        if (arg3 != DONOTCARE) Assert.assertEquals(arg3, mSetupDataCallArguments[3]);
        if (arg4 != DONOTCARE) Assert.assertEquals(arg4, mSetupDataCallArguments[4]);
        if (arg5 != DONOTCARE) Assert.assertEquals(arg5, mSetupDataCallArguments[5]);
        if (arg6 != DONOTCARE) Assert.assertEquals(arg6, mSetupDataCallArguments[6]);
    }

    public void verifySendBurstDtmf(String dtmfString, int on, int off) {

        Assert.assertNotNull("sendBurstDtmf() was not called.", mSendBurstDtmfArguments);

        if (dtmfString != DONOTCARE) Assert.assertEquals(dtmfString, mSendBurstDtmfArguments[0]);
        Assert.assertEquals(Integer.valueOf(on).toString(),  mSendBurstDtmfArguments[1]);
        Assert.assertEquals(Integer.valueOf(off).toString(), mSendBurstDtmfArguments[2]);
    }

    public void verifyAcknowledgeSms(boolean success, int cause) {
        Assert.assertEquals(mAcknowledgeSmsSuccess,success);
        Assert.assertEquals(mAcknowledgeSmsCause,cause);
    }

    public void changeRadioState(RadioState state) {
        setRadioState(state);
    }

    public void changeDataNetworkState() {
        notifyRegistrants(mDataNetworkStateRegistrants);
    }

    public void changeVoiceNetworkState() {
        notifyRegistrants(mVoiceNetworkStateRegistrants);
    }

    public void changePrlVersion(Object ret) {
        notifyRegistrants(mCdmaPrlChangedRegistrants, new AsyncResult(null, ret, null));
    }

    public void progressConnectingCallState() {
        mSimulatedCallState.progressConnectingCallState();
        notifyRegistrants(mCallStateRegistrants);
    }

    public void triggerRing(String number) {
        mSimulatedCallState.triggerRing(number);
        notifyRegistrants(mCallStateRegistrants);
    }

    public void triggerIncomingCdmaSms(Object ret) {
        notifyRegistrant(mCdmaSmsRegistrant,new AsyncResult(null, ret, null));
    }

    public void triggerIncomingCdmaSmsStatusReport(Object ret) {
        android.telephony.SmsMessage sms = (android.telephony.SmsMessage) ret;
        notifyRegistrant(mCdmaSmsRegistrant,new AsyncResult(null, sms, null));
    }

    public void triggerCallWaiting(String number) {
        CdmaCallWaitingNotification r = new CdmaCallWaitingNotification();
        r.number = number;
        notifyRegistrants(mCallWaitingInfoRegistrants, new AsyncResult(null, r, null));
    }

    public void triggerSmsSubmitReport(int messageRef, String ackPdu, int errorCode, Throwable t){
        SmsResponse  response = new SmsResponse(messageRef, ackPdu, errorCode);
        mSmsResponseResult = new RILResult(response, t, 10);
        if (null != mResponse) {
            AsyncResult.forMessage(mResponse, mSmsResponseResult.result,
                    mSmsResponseResult.exception);
            sendResult(mSmsResponseResult, mResponse);
        }
    }

    public void triggerIncomingImsSmsStatusReport(String response){
        if (mSmsStatusRegistrant != null) {
            notifyRegistrant(mSmsStatusRegistrant,
                    new AsyncResult(null, response, null));
        }
    }

    public void triggerIncomingImsSms(String response){
        String a[] = new String[2];
        a[1] = response;
        SmsMessage sms;
        sms = SmsMessage.newFromCMT(a);
        if (mGsmSmsRegistrant != null) {
            notifyRegistrant(mGsmSmsRegistrant,
                    new AsyncResult(null, sms, null));
        }
    }
    public void triggerIncomingGsmBroadcatSms(Object ret) {
        if (null != mGsmBroadcastSmsRegistrant) {
            notifyRegistrant(mGsmBroadcastSmsRegistrant,
                    new AsyncResult(null, ret, null));
        }
    }

    /**
     * Notify Ims Network state changed.
     *
     * @param imsRegisterd 1    :IMS is registered
     *                     not 1:IMS is NOT registered
     * @param technologies 1:3GPP (GSM, WCDMA, LTE)
     *                     2:3GPP2 (CDMA, EVDO)
     */
    public void triggerNotifyImsStatus(int imsRegisterd, int technologies){
        mImsRegisterd = imsRegisterd;
        mTechnologies = technologies;
//        notifyRegistrants(mImsNetworkStateChangedRegistrants);
    }

    public void registerForCallRilApi(Handler h, int what, Object obj) {
        mCallRilApiRegistrants.add(h, what, obj);
    }

    public byte[] getSendCdmaSmsPdu() {
        return mSendCdmaSmsArguments;
    }

    public String getSendImsGsmSmsPdu() {
        return mImsGsmSmsPdu;
    }

    public String getSendImsGsmSmsSmscPdu() {
        return mImsGsmSmsSmscPdu;
    }

    public int getSendImsGsmSmsRtryCount() {
        return mRetry;
    }

    public int getSendImsGsmSmsMessageRef() {
        return mMessageRef;
    }

    private void notifyRegistrants(RegistrantList r) {
        notifyRegistrants(r, null);
    }

    private void notifyRegistrants(final RegistrantList r, final AsyncResult result) {
        if (r != null) {
            // for safety, make another thread to notify
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                    if (result != null) {
                        r.notifyRegistrants(result);
                    } else {
                        r.notifyRegistrants();
                    }
                }
            }.start();
        }
    }

    private void notifyRegistrant(final Registrant r, final AsyncResult result) {
        if (r != null) {
            // for safety, make another thread to notify
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                    if (result != null) {
                        r.notifyRegistrant(result);
                    } else {
                        r.notifyRegistrant();
                    }
                }
            }.start();
        }
    }

    private synchronized void sendResult(RILResult rr, Message response) {
        if (rr.order == -1) {
            // don't care about the order
            response.sendToTarget();
        } else {
            // set delay according to its order
            response.getTarget().sendMessageDelayed(response, rr.order*100);
        }
    }

    private static class RILResult {

        Object result;
        Throwable exception;
        int order;

        RILResult(Object s, Throwable t) {
            this(s, t, -1);
        }

        RILResult(Object s, Throwable t, int o) {
            result = s;
            exception = t;
            order = o;
        }
    }

    public void getQosStatus (int qosId, Message result) {
    }

    public void setupQosReq (int callId, ArrayList<String> qosFlows, Message result) {
    }

    public void releaseQos (int qosId, Message result) {
    }

    public void modifyQos (int qosId, ArrayList<String> qosFlows, Message result) {
    }

    public void suspendQos (int qosId, Message result) {
    }

    public void resumeQos (int qosId, Message result) {
    }

    public void setSubscriptionMode (int subscriptionMode, Message result) {
    }

    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
            Message result) {
    }

    public void getDataCallProfile(int appType, Message result) {
    }

    public void iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message response) {
        Log.d(TAG, "iccIOForApp() -"
                + " command:0x" + Integer.toHexString(command)
                + " fileid:0x" + Integer.toHexString(fileid));

        if (command == 0xc0) {
            if (fileid == IccConstants.EF_ICCID) {
                byte[] result = IccUtils.hexStringToBytes("0000000a2fe2040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_AD) {
                byte[] result = IccUtils.hexStringToBytes("000000046fad040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_PL) {
                byte[] result = IccUtils.hexStringToBytes("0000000a2f05040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MSISDN) {
                byte[] result = IccUtils.hexStringToBytes("000000276f40040000000005020127");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SST) {
                byte[] result = IccUtils.hexStringToBytes("0000000c6f38040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_LI) {
                byte[] result = IccUtils.hexStringToBytes("000000026f3a040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_SPN) {
                byte[] result = IccUtils.hexStringToBytes("000000236f41040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_MDN) {
                byte[] result = IccUtils.hexStringToBytes("0000000b6f4404000000000502010b");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_IMSIM) {
                byte[] result = IccUtils.hexStringToBytes("0000000a6f22040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_CDMAHOME) {
                byte[] result = IccUtils.hexStringToBytes("000000646f28040000000005020105");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_IMPI) {
                byte[] result = IccUtils.hexStringToBytes("00");
                response.obj = new AsyncResult(response.obj, new IccIoResult(0x90, 0x0, result),
                        CommandException.fromRilErrno(RILConstants.REQUEST_NOT_SUPPORTED));
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_IMPU) {
                byte[] result = IccUtils.hexStringToBytes("00");
                response.obj = new AsyncResult(response.obj, new IccIoResult(0x90, 0x0, result),
                        CommandException.fromRilErrno(RILConstants.REQUEST_NOT_SUPPORTED));
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_DOMAIN) {
                byte[] result = IccUtils.hexStringToBytes("00");
                response.obj = new AsyncResult(response.obj, new IccIoResult(0x90, 0x0, result),
                        CommandException.fromRilErrno(RILConstants.REQUEST_NOT_SUPPORTED));
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MBI) {
                byte[] result = IccUtils.hexStringToBytes("000000046fc9040000000005020104");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MWIS) {
                byte[] result = IccUtils.hexStringToBytes("000000056fca040000000005020105");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x6A, 0x82, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CFIS) {
                byte[] result = IccUtils.hexStringToBytes("000000106fcb040000000005020110");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CFF_CPHS) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x6A, 0x82, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SPDI) {
                byte[] result = IccUtils.hexStringToBytes("000000406fcd040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_PNN) {
                byte[] result = IccUtils.hexStringToBytes("000000446fc5040000000005020122");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_OPL) {
                byte[] result = IccUtils.hexStringToBytes("000000a06fc6040000000005020108");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_INFO_CPHS) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x6A, 0x82, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSP_CPHS) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x6A, 0x82, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SPN) {
                byte[] result = IccUtils.hexStringToBytes("000000116f46040000000005020000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MAILBOX_CPHS) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x6A, 0x82, result), null);
                response.sendToTarget();
            } else {
                Log.d(TAG, "NOT SUPPORTED -"
                        + " command:0x" + Integer.toHexString(command)
                        + " fileid:0x" + Integer.toHexString(fileid));
            }
        } else if (command == 0xb0) {
            if (fileid == IccConstants.EF_ICCID) {
                byte[] result = IccUtils.hexStringToBytes("981803000101003603f3");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_EPRL) {
                byte[] result = IccUtils.hexStringToBytes("02290000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_AD) {
                byte[] result = IccUtils.hexStringToBytes("00000002");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_PL) {
                byte[] result = IccUtils.hexStringToBytes("6a61656effffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MSISDN) {
                byte[] result = IccUtils.hexStringToBytes(
                  "3039303337313130323235ffffffffffffffffffffffffffff06ff9170112052ffffffffffffff"
                  );
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SST) {
                byte[] result = IccUtils.hexStringToBytes("000a140c210e000000000004");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_LI) {
                byte[] result = IccUtils.hexStringToBytes("0504");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_SPN) {
                byte[] result = IccUtils.hexStringToBytes(
                  "0102014b444449ffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_IMSIM) {
                byte[] result = IccUtils.hexStringToBytes("0059007228962b805301");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SPN) {
                byte[] result = IccUtils.hexStringToBytes("0047454d504c5553ffffffffffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_SPDI) {
                byte[] result = IccUtils.hexStringToBytes(
                  "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                  "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else {
                Log.d(TAG, "NOT SUPPORTED -"
                        + " command:0x" + Integer.toHexString(command)
                        + " fileid:0x" + Integer.toHexString(fileid));
            }
        } else if (command == 0xb2) {
            if (fileid == IccConstants.EF_MSISDN) {
                byte[] result = IccUtils.hexStringToBytes(
                  "3039303337313130323235ffffffffffffffffffffffffffff06ff9170112052ffffffffffffff"
                  );
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_MDN) {
                byte[] result = IccUtils.hexStringToBytes("0b9a3a17a122f5ffff0000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CSIM_CDMAHOME) {
                byte[] result = IccUtils.hexStringToBytes("0000000000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MWIS) {
                byte[] result = IccUtils.hexStringToBytes("0000000000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_PNN) {
                byte[] result = IccUtils.hexStringToBytes(
                  "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_OPL) {
                byte[] result = null;
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x69, 0x82, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_MBI) {
                byte[] result = IccUtils.hexStringToBytes("00000000");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else if (fileid == IccConstants.EF_CFIS) {
                byte[] result = IccUtils.hexStringToBytes("ff00ffffffffffffffffffffffffffff");
                response.obj = new AsyncResult(response.obj,
                        new IccIoResult(0x90, 0x0, result), null);
                response.sendToTarget();
            } else {
                Log.d(TAG, "NOT SUPPORTED -"
                        + " command:0x" + Integer.toHexString(command)
                        + " fileid:0x" + Integer.toHexString(fileid));
            }
        } else {
            Log.d(TAG, "NOT SUPPORTED -"
                    + " command:0x" + Integer.toHexString(command)
                    + " fileid:0x" + Integer.toHexString(fileid));
        }
    }

    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message response) {
    }

    public void setDataSubscription (Message result) {
    }

    public void sendImsGsmSms (String smscPDU, String pdu, int retry, int messageRef,
            Message response) {

        Log.d(TAG, "sendImsGsmSms() is Called.");
        mImsGsmSmsSmscPdu = smscPDU;
        mImsGsmSmsPdu     = pdu;
        mRetry            = retry;
        mMessageRef       = messageRef;
        mResponse         = response;
        // notify TestCase that RIL API was called.
        notifyRegistrants(mCallRilApiRegistrants,
                new AsyncResult(null, CallRilAPIKind.CALLED_SEND_IMS_GSM_SMS, null));
    }

    public void acceptCall(Message result, int callType) {
    }

    public void getIMSIForApp(String aid, Message result) {
        Log.d(TAG, "getIMSIForApp() is called.");
        AsyncResult.forMessage(result, "440500123456789", null);
        result.sendToTarget();
    }

    public void supplyDepersonalization(String netpin, int type, Message result) {
    }

    public void getImsRegistrationState(Message result) {

        Log.d(TAG, "getImsRegistrationState() is called.");
        int response[];
        response = new int[2];
        response[0] = mImsRegisterd;
        response[1] = mTechnologies;

        // notify TestCase that RIL API was called.
        notifyRegistrants(mCallRilApiRegistrants,
                new AsyncResult(null,
                        CallRilAPIKind.CALLED_GET_IMS_REGISTRATION_STATE, null));
        AsyncResult.forMessage(result, response, null);
        result.sendToTarget();
    }

    public void getVoiceRadioTechnology(Message result) {
        Log.d(TAG, "getVoiceRadioTechnology() is called.");
        int[] ret = new int[1];
        ret[0] = 6;
        AsyncResult.forMessage(result, ret, null);
        result.sendToTarget();
    }

    public void getAtr(int slot, Message message) {
    }
}
