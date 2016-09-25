/*
 * Copyright (C) 2006 The Android Open Source Project
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

/**
 * Call fail causes from TS 24.008 .
 * These are mostly the cause codes we need to distinguish for the UI.
 * See 22.001 Annex F.4 for mapping of cause codes to local tones.
 *
 * CDMA call failure reasons are derived from the possible call failure scenarios described
 * in "CDMA IS2000 - Release A (C.S0005-A v6.0)" standard.
 *
 * {@hide}
 *
 */
public interface CallFailCause {
    // The disconnect cause is not valid (Not received a disconnect cause)
    public static final int NOT_VALID = -1;

    /**
     * No disconnect cause provided. Generally a local disconnect
     * or an incoming missed call
     */
    public static final int NO_DISCONNECT_CAUSE_AVAILABLE = 0;

    // Unassigned/Unobtainable number
    int UNOBTAINABLE_NUMBER = 1;

    // No route to destination
    public static final int NO_ROUTE_TO_DEST = 3;

    // Channel unacceptable
    public static final int CHANNEL_UNACCEPTABLE = 6;

    // Operator determined barring
    public static final int OPERATOR_DETERMINED_BARRING = 8;

    int NORMAL_CLEARING     = 16;
    // Busy Tone
    int USER_BUSY           = 17;

    // No user responding
    public static final int NO_USER_RESPONDING = 18;

    // User alerting, no answer
    public static final int USER_ALERTING_NO_ANSWER = 19;

    // Call rejected
    public static final int CALL_REJECTED = 21;

    // Number changed
    int NUMBER_CHANGED      = 22;

    // Pre-emption
    public static final int PRE_EMPTION = 25;

    // Non selected user clearing
    public static final int NON_SELECTED_USER_CLEARING = 26;

    // Destination out of order
    public static final int DESTINATION_OUT_OF_ORDER = 27;

    // Invalid number format (incomplete number)
    public static final int INVALID_NUMBER_FORMAT = 28;

    // Facility rejected
    public static final int FACILITY_REJECTED = 29;

    int STATUS_ENQUIRY      = 30;
    int NORMAL_UNSPECIFIED  = 31;

    // Congestion Tone
    int NO_CIRCUIT_AVAIL    = 34;

    // Network out of order
    public static final int NETWORK_OUT_OF_ORDER = 38;

    int TEMPORARY_FAILURE   = 41;
    int SWITCHING_CONGESTION    = 42;

    // Access information discarded
    public static final int ACCESS_INFORMATION_DISCARDED = 43;

    int CHANNEL_NOT_AVAIL   = 44;

    // Resources unavailable, unspecified
    public static final int RESOURCES_UNAVAILABLE_UNSPECIFIED = 47;

    int QOS_NOT_AVAIL       = 49;

    // Requested facility not subscribed
    public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED = 50;

    // Incoming calls barred within the CUG
    public static final int INCOMING_CALL_BARRED_WITHIN_CUG = 55;

    // Bearer capability not authorized
    public static final int BEARER_CAPABILITY_NOT_AUTHORISED = 57;

    int BEARER_NOT_AVAIL    = 58;

    // Service or option not available, unspecified
    public static final int SERVICE_OR_OPTION_NOT_AVAILABLE = 63;

    // Bearer service not implemented
    public static final int BEARER_SERVICE_NOT_IMPLEMENTED = 65;

    // others
    int ACM_LIMIT_EXCEEDED = 68;

    // Requested facility not implemented
    public static final int REQUESTED_FACILITY_NOT_IMPLEMENTED = 69;

    // Only restricted digital information bearer capability is available
    public static final int ONLY_RESTRICTED_DIGITAL_INFO_BC_AVAILABLE = 70;

    // Service or option not implemented, unspecified
    public static final int SERVICE_OR_OPTION_NOT_IMPLEMENTED = 79;

    // Invalid transaction identifier value
    public static final int INVALID_TRANSACTION_ID_VALUE = 81;

    // User not member of CUG
    public static final int USER_NOT_MEMBER_OF_CUG = 87;

    // Incompatible destination
    public static final int INCOMPATIBLE_DESTINATION = 88;

    // Invalid transit network selection
    public static final int INVALID_TRANSIT_NETWORK_SELECTION = 91;

    // Semantically incorrect message
    public static final int SEMANTICALLY_INCORRECT_MESSAGE = 95;

    // Invalid mandatory information
    public static final int INVALID_MANDATORY_INFORMATION = 96;

    // Message type non-existent or not implemented
    public static final int MESSAGE_TYPE_NON_EXISTENT = 97;

    // Message type not compatible with protocol state
    public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE = 98;

    // Information element non-existent or not implemented
    public static final int IE_NON_EXISTENT_OR_NOT_IMPLEMENTED = 99;

    // Conditional IE error
    public static final int CONDITIONAL_IE_ERROR = 100;

    // Message not compatible with protocol state
    public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 101;

    // Recovery on timer expiry
    public static final int RECOVERY_ON_TIMER_EXPIRY = 102;

    // Protocol error, unspecified
    public static final int PROTOCOL_ERROR_UNSPECIFIED = 111;

    // Interworking, unspecified
    public static final int INTERWORKING_UNSPECIFIED = 127;

    int CALL_BARRED        = 240;
    int FDN_BLOCKED        = 241;

    /** The given IMSI is not known at the VLR */
    /** TS 24.008 cause 4 */
    public static final int IMSI_UNKNOWN_IN_VLR = 242;

    /**
     * The network does not accept emergency call establishment using an IMEI or
     * not accept attach procedure for emergency services using an IMEI
     */
    public static final int IMEI_NOT_ACCEPTED = 243;

    // Stk Call Control
    int DIAL_MODIFIED_TO_USSD = 244;
    int DIAL_MODIFIED_TO_SS   = 245;
    int DIAL_MODIFIED_TO_DIAL = 246;

    int CDMA_LOCKED_UNTIL_POWER_CYCLE  = 1000;
    int CDMA_DROP                      = 1001;
    int CDMA_INTERCEPT                 = 1002;
    int CDMA_REORDER                   = 1003;
    int CDMA_SO_REJECT                 = 1004;
    int CDMA_RETRY_ORDER               = 1005;
    int CDMA_ACCESS_FAILURE            = 1006;
    int CDMA_PREEMPTED                 = 1007;

    // For non-emergency number dialed while in emergency callback mode.
    int CDMA_NOT_EMERGENCY             = 1008;

    // Access Blocked by CDMA Network.
    int CDMA_ACCESS_BLOCKED            = 1009;

    int ERROR_UNSPECIFIED = 0xffff;

}
