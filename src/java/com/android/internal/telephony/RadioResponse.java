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

import android.hardware.radio.V1_0.IRadioResponse;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioResponseType;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.AppStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.*;
import java.util.ArrayList;
import static com.android.internal.telephony.RILConstants.*;

public class RadioResponse extends IRadioResponse.Stub {
    RIL mRil;

    public RadioResponse(RIL ril) {
        mRil = ril;
    }

    /**
     * This is a helper function to be called at the beginning of all response callbacks.
     * It takes care of acks, wakelocks, and finds and returns RILRequest corresponding to the
     * response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    private RILRequest processResponse(RadioResponseInfo responseInfo) {
        int serial = responseInfo.serial;
        int error = responseInfo.error;
        int type = responseInfo.type;

        RILRequest rr;

        if (type == RadioResponseType.SOLICITED_ACK) {
            synchronized (mRil.mRequestList) {
                rr = mRil.mRequestList.get(serial);
            }
            if (rr == null) {
                Rlog.w(RIL.RILJ_LOG_TAG, "processResponse: Unexpected solicited ack response! " +
                        "sn: " + serial);
            } else {
                mRil.decrementWakeLock(rr);
                if (RIL.RILJ_LOGD) {
                    mRil.riljLog(rr.serialString() + " Ack < " + RIL.requestToString(rr.mRequest));
                }
            }
        } else {
            rr = mRil.findAndRemoveRequestFromList(serial);

            if (rr == null) {
                Rlog.w(RIL.RILJ_LOG_TAG, "processResponse: Unexpected response! sn: " + serial +
                        " error: " + error);
                return null;
            }

            if (type == RadioResponseType.SOLICITED_ACK_EXP) {
                Message msg;
                // todo: use IRadio.sendAck() instead when it's available
                RILRequest response = RILRequest.obtain(RIL_RESPONSE_ACKNOWLEDGEMENT, null);
                msg = mRil.mSender.obtainMessage(RIL.EVENT_SEND_ACK, response);
                mRil.acquireWakeLock(rr, RIL.FOR_ACK_WAKELOCK);
                msg.sendToTarget();
                if (RIL.RILJ_LOGD) {
                    mRil.riljLog("Response received for " + rr.serialString() + " " +
                            RIL.requestToString(rr.mRequest) + " Sending ack to ril.cpp");
                }
            } else {
                // ack sent for SOLICITED_ACK_EXP above; nothing to do for SOLICITED
            }
        }

        return rr;
    }

    /**
     * This is a helper function to be called at the end of all response callbacks.
     * It takes care of logging, decrementing wakelock if needed, and releases the request from
     * memory pool.
     * @param rr RILRequest for which response callback was called
     * @param responseInfo RadioResponseInfo received in the callback
     * @param ret object to be returned to request sender
     */
    private void processResponseDone(RILRequest rr, RadioResponseInfo responseInfo, Object ret) {
        mRil.mEventLog.writeOnRilSolicitedResponse(rr.mSerial, responseInfo.error, rr.mRequest,
                ret);
        if (rr != null) {
            if (responseInfo.type == RadioResponseType.SOLICITED) {
                mRil.decrementWakeLock(rr);
            }
            rr.release();
        }
    }

    /**
     * Helper function to send response msg
     * @param msg Response message to be sent
     * @param ret Return object to be included in the response message
     */
    private void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    /**
     * Response function for IRadio.getIccCardStatus()
     * @param responseInfo Response info struct containing response type, serial no. & error
     * @param cardStatus ICC card status as defined by CardStatus in types.hal
     */
    public void iccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        mRil.riljLog("iccCardStatusResponse: serial " + responseInfo.serial +
                " cardStatus.cardState " + cardStatus.cardState);
        RILRequest rr = processResponse(responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == 0) {
                mRil.riljLog("iccCardStatusResponse: rr.mResult != null");
                ret = responseIccCardStatus(cardStatus);
                sendMessageResponse(rr.mResult, ret);
            }
            processResponseDone(rr, responseInfo, ret);
        } else {
            mRil.riljLog("iccCardStatusResponse: rr == null");
        }
    }

    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
                                                       int var2) {}

    public void getCurrentCallsResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.Call> var2) {}

    public void dialResponse(RadioResponseInfo responseInfo) {}

    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String var2) {}

    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {}

    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {}

    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {}

    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {}

    public void conferenceResponse(RadioResponseInfo responseInfo) {}

    public void rejectCallResponse(RadioResponseInfo responseInfo) {}

    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo, int var2) {}

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo,
                                          android.hardware.radio.V1_0.SignalStrength var2) {}

    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                  VoiceRegStateResult var2) {}

    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                 DataRegStateResult var2) {}

    public void getOperatorResponse(RadioResponseInfo responseInfo,
                                    String var2,
                                    String var3,
                                    String var4) {}

    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {}

    public void sendDtmfResponse(RadioResponseInfo responseInfo) {}

    public void sendSmsResponse(RadioResponseInfo responseInfo,
                                SendSmsResult var2) {}

    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo,
                                          SendSmsResult var2) {}

    public void setupDataCallResponse(RadioResponseInfo responseInfo,
                                      SetupDataCallResult var2) {}

    public void iccIOForApp(RadioResponseInfo responseInfo,
                            android.hardware.radio.V1_0.IccIoResult var2) {}

    public void sendUssdResponse(RadioResponseInfo responseInfo) {}

    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {}

    public void getClirResponse(RadioResponseInfo responseInfo, int var2, int var3) {}

    public void setClirResponse(RadioResponseInfo responseInfo) {}

    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo,
                                             ArrayList<android.hardware.radio.V1_0.CallForwardInfo> var2) {}

    public void setCallForwardResponse(RadioResponseInfo responseInfo) {}

    public void getCallWaitingResponse(RadioResponseInfo responseInfo,
                                       boolean var2,
                                       int var3) {}

    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {}

    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {}

    public void acceptCallResponse(RadioResponseInfo responseInfo) {}

    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {}

    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int var2) {}

    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {}

    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean var2) {}

    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {}

    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {}

    public void getAvailableNetworksResponse(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.OperatorInfo> var2) {}

    public void startDtmfResponse(RadioResponseInfo responseInfo) {}

    public void stopDtmfResponse(RadioResponseInfo responseInfo) {}

    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String var2) {}

    public void separateConnectionResponse(RadioResponseInfo responseInfo) {}

    public void setMuteResponse(RadioResponseInfo responseInfo) {}

    public void getMuteResponse(RadioResponseInfo responseInfo, boolean var2) {}

    public void getClipResponse(RadioResponseInfo responseInfo, int var2) {}

    public void getDataCallListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<SetupDataCallResult> var2) {}

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo,
                                             ArrayList<Byte> var2) {}

    public void sendOemRilRequestStringsResponse(RadioResponseInfo responseInfo,
                                                 ArrayList<String> var2) {}

    public void sendScreenStateResponse(RadioResponseInfo responseInfo) {}

    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {}

    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int var2) {}

    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {}

    public void setBandModeResponse(RadioResponseInfo responseInfo) {}

    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo,
                                              ArrayList<Integer> var2) {}

    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String var2) {}

    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {}

    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {}

    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {}

    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {}

    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int var2) {}

    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo,
                                           NeighboringCell var2) {}

    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {}

    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {}

    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {}

    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int var2) {}

    public void setTTYModeResponse(RadioResponseInfo responseInfo) {}

    public void getTTYModeResponse(RadioResponseInfo responseInfo, int var2) {}

    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {}

    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo,
                                                 boolean var2) {}

    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {}

    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {}

    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult var2) {}

    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {}

    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                              GsmBroadcastSmsConfigInfo var2) {}

    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {}

    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {}

    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                               CdmaBroadcastSmsConfigInfo var2) {}

    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {}

    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {}

    public void getCDMASubscriptionResponse(RadioResponseInfo responseInfo,
                                            String var2,
                                            String var3,
                                            String var4,
                                            String var5,
                                            String var6) {}

    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int var2) {}

    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {}

    public void getDeviceIdentityResponse(RadioResponseInfo responseInfo,
                                          String var2,
                                          String var3,
                                          String var4,
                                          String var5) {}

    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {}

    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String var2) {}

    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {}

    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {}

    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int var2) {}

    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String var2) {}

    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {}

    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo,
                                               android.hardware.radio.V1_0.IccIoResult var2) {}

    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int var2) {}

    public void getCellInfoListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.CellInfo> var2) {}

    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {}

    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {}

    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                boolean var2,
                                                int var3) {}

    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult var2) {}

    public void iccTransmitApduBasicChannelResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult var2) {}

    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo,
                                              int var2,
                                              ArrayList<Byte> var3) {}

    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {}

    public void iccTransmitApduLogicalChannelResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult var2) {}

    public void nvReadItemResponse(RadioResponseInfo responseInfo, String var2) {}

    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {}

    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {}

    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {}

    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {}

    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {}

    public void getHardwareConfigResponse(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> var2) {}

    public void requestIccSimAuthenticationResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult var2) {}

    public void setDataProfileResponse(RadioResponseInfo responseInfo) {}

    public void requestShutdownResponse(RadioResponseInfo responseInfo) {}

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability var2) {}

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability var2) {}

    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo var2) {}

    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo var2) {}

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo var2) {}

    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo,
                                             ActivityStatsInfo var2) {}

    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int var2) {}

    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo,
                                           boolean var2,
                                           CarrierRestrictions var3) {}

    private Object
    responseIccCardStatus(CardStatus cardStatus) {
        IccCardApplicationStatus appStatus;

        IccCardStatus iccCardStatus = new IccCardStatus();
        iccCardStatus.setCardState(cardStatus.cardState);
        iccCardStatus.setUniversalPinState(cardStatus.universalPinState);
        iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus.gsmUmtsSubscriptionAppIndex;
        iccCardStatus.mCdmaSubscriptionAppIndex = cardStatus.cdmaSubscriptionAppIndex;
        iccCardStatus.mImsSubscriptionAppIndex = cardStatus.imsSubscriptionAppIndex;
        int numApplications = cardStatus.numApplications;

        // limit to maximum allowed applications
        if (numApplications > com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS) {
            numApplications = com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS;
        }
        iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
        for (int i = 0 ; i < numApplications ; i++) {
            AppStatus rilAppStatus = cardStatus.applications[i];
            appStatus = new IccCardApplicationStatus();
            appStatus.app_type       = appStatus.AppTypeFromRILInt(rilAppStatus.appType);
            appStatus.app_state      = appStatus.AppStateFromRILInt(rilAppStatus.appState);
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(
                    rilAppStatus.persoSubstate);
            appStatus.aid            = rilAppStatus.aidPtr;
            appStatus.app_label      = rilAppStatus.appLabelPtr;
            appStatus.pin1_replaced  = rilAppStatus.pin1Replaced;
            appStatus.pin1           = appStatus.PinStateFromRILInt(rilAppStatus.pin1);
            appStatus.pin2           = appStatus.PinStateFromRILInt(rilAppStatus.pin2);
            iccCardStatus.mApplications[i] = appStatus;
        }
        mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
        return iccCardStatus;
    }
}
