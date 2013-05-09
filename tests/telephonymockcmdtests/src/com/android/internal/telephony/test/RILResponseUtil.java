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

import com.android.internal.telephony.DataCallState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus;

/**
 * Utility class providing RIL response data.
 */
public class RILResponseUtil {

    /**
     * Results of RIL_REQUEST_VOICE_REGISTRATION_STATE (for CDMA). <br>
     * [0] registrationState <br>
     * [3] radioTechnology <br>
     * [4] baseStationId <br>
     * [5] baseStationLatitude <br>
     * [6] baseStationLongitude <br>
     * [7] cssIndicator <br>
     * [8] systemId <br>
     * [9] networkId <br>
     * [10] Roaming indicator <br>
     * [11] Indicates if current system is in PRL <br>
     * [12] Is default roaming indicator from PRL <br>
     * [13] Denial reason if registrationState = 3 <br>
     * [14] Primary code of current cell
     */
    public static final class RegistrationCDMA {
        // Results field number of RIL_REQUEST_VOICE_REGISTRATION_STATE
        private static final int RESULTS_FIELD_NUM = 15;
        private static final String[] sNormal = new String[RESULTS_FIELD_NUM];
        static {
            sNormal[0] = "1";
            sNormal[3] = "6";
            sNormal[4] = "1";
            sNormal[5] = "514084";
            sNormal[6] = "2012407";
            sNormal[7] = "0";
            sNormal[8] = "1";
            sNormal[9] = "65535";
            sNormal[10] = "1";
            sNormal[11] = "1";
            sNormal[12] = "0";
            sNormal[13] = "0";
            sNormal[14] = "0";
        }

        /** Registered in HOME */
        public static String[] REGISTERED_IN_HOME() {
            return sNormal.clone();
        }

        /** Registered in ROAMING */
        public static String[] REGISTERED_IN_ROAMING() {
            String[] ret = sNormal.clone();
            ret[0] = "5";
            return ret;
        }

        /** Unregistered */
        public static String[] UNREGISTERED() {
            String[] ret = new String[RESULTS_FIELD_NUM];
            ret[0] = "0";
            return ret;
        }
    }

    /**
     * Results of RIL_REQUEST_DATA_REGISTRATION_STATE. <br>
     * [0] registrationState <br>
     * [3] radioTechnology <br>
     * [4] Denial reason if registrationState = 3 <br>
     */
    public static class DataRegistration {

        private static final String[] sOutOfService = {"0", null, null, "0", "0", null};
        private static final String[] sRegDenied = {"3", null, null, "0", "0", null};
        private static final String[] sInService1xRtt = {"1", null, null, "6", "0", null};
        private static final String[] sInServiceEvdo0 = {"1", null, null, "7", "0", null};
        private static final String[] sInServiceEvdoA = {"1", null, null, "8", "0", null};
        private static final String[] sInServiceEvdoB = {"1", null, null, "12", "0", null};

        /** Out of service */
        public static String[] OUT_OF_SERVICE() {
            return sOutOfService.clone();
        }

        /** Registration denied */
        public static String[] REG_DENIED() {
            return sRegDenied.clone();
        }

        /** In service of 1xRTT */
        public static String[] IN_SERVICE_1xRTT() {
            return sInService1xRtt.clone();
        }

        /** In service of Evdo_0 */
        public static String[] IN_SERVICE_Evdo_0() {
            return sInServiceEvdo0.clone();
        }

        /** In service of Evdo_A */
        public static String[] IN_SERVICE_Evdo_A() {
            return sInServiceEvdoA.clone();
        }

        /** In service of Evdo_B */
        public static String[] IN_SERVICE_Evdo_B() {
            return sInServiceEvdoB.clone();
        }
    }

    /**
     * Results of RIL_REQUEST_SETUP_DATA_CALL <br>
     */
    public static final class SetupDataCall {

        private static final String[] IP_ADDRESS = {"10.141.231.9/30"};
        private static final String[] DNS_ADDRESS = {"106.187.2.41"};
        private static final String[] GATEWAY_ADDRESS = {"10.141.231.10"};

        public static DataCallState SUCCESS() {
            DataCallState dataCallState = new DataCallState();
            dataCallState.version = 6;
            // STATUS
            dataCallState.status = 0;//No Error(0)
            // RETRY TIME
            dataCallState.suggestedRetryTime = -1;//no retry(-1)
            // PDP CID
            dataCallState.cid = 0;
            // ACTIVE STATUS
            dataCallState.active = 2;//active/physical link up(2)
            // PDP type
            dataCallState.type = "IP";
            // network interface name
            dataCallState.ifname = "rmnet0";
            // IP addresses
            dataCallState.addresses = IP_ADDRESS;
            // DNS addresses
            dataCallState.dnses = DNS_ADDRESS;
            // GATEWAY addresses
            dataCallState.gateways = GATEWAY_ADDRESS;
            return dataCallState;
        }
    }

    /**
     * Result of RIL_REQUEST_OPERATOR. <br>
     * [0] is long alpha or null if unregistered <br>
     * [1] is short alpha or null if unregistered <br>
     * [2] is numeric or null if unregistered <br>
     */
    public static final class Operator {
        // Results field number of RIL_REQUEST_OPERATOR
        private static final int RESULTS_FIELD_NUM = 3;

        private static final String[] sNormal = new String[RESULTS_FIELD_NUM];
        static {
            sNormal[0] = "Dummy";
            sNormal[1] = "";
            sNormal[2] = "00101";
        }

        public static String[] Dummy() {
            return sNormal.clone();
        }
    }

    /**
     * Result of RIL_REQUEST_CDMA_SUBSCRIPTION
     */
    public static final class CDMASubscription {
        // Results field number of RIL_REQUEST_CDMA_SUBSCRIPTION
        private static final int RESULTS_FIELD_NUM = 5;

        private static final String[] sNormal = new String[RESULTS_FIELD_NUM];
        static {
            sNormal[0] = "01234567890"; // MDN
            sNormal[1] = "12304,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"; // SID
            sNormal[2] = "65535,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"; // NID
            sNormal[3] = "7891234456"; // MIN
            sNormal[4] = "1"; // PRL Version
        };

        /** Normal sim */
        public static String[] REAL_SIM() {
            return sNormal.clone();
        }
    }

    /**
     * Results of RIL_REQUEST_GET_SIM_STATUS <br>
     */
    public static final class GetIccCardStatus {

        public static IccCardStatus CDMA_SUCCESS() {
            IccCardStatus status = new IccCardStatus();
            IccCardApplicationStatus ca;

            status.setCardState(1);
            status.setUniversalPinState(0);
            status.mGsmUmtsSubscriptionAppIndex = 0;
            status.mCdmaSubscriptionAppIndex = 0;
            status.mImsSubscriptionAppIndex = 0;

            int numApplications = 1;
            status.mApplications = new IccCardApplicationStatus[numApplications];
            for (int i = 0 ; i < numApplications ; i++) {
                ca = new IccCardApplicationStatus();
                ca.app_type = AppType.APPTYPE_CSIM;
                status.mApplications[i] = ca;
            }
            return status;
        }

        public static IccCardStatus GSM_SUCCESS() {
            IccCardStatus status = new IccCardStatus();
            IccCardApplicationStatus ca;

            status.setCardState(1);
            status.setUniversalPinState(0);
            status.mGsmUmtsSubscriptionAppIndex = 0;
            status.mCdmaSubscriptionAppIndex = 0;
            status.mImsSubscriptionAppIndex = 0;

            int numApplications = 1;
            status.mApplications = new IccCardApplicationStatus[numApplications];
            for (int i = 0 ; i < numApplications ; i++) {
                ca = new IccCardApplicationStatus();
                ca.app_type = AppType.APPTYPE_USIM;
                status.mApplications[i] = ca;
            }
            return status;
        }
    }
}
