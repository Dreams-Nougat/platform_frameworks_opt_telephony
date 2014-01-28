/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.internal.telephony.dataconnection;

import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;
import com.android.internal.telephony.PhoneConstants;

import android.os.Message;
import android.util.Log;

public class DcSwitchAsyncChannel extends AsyncChannel {
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private static final String LOG_TAG = "PHONE";

    private int tagId = 0;
    private DcSwitchState mDcSwitchState;

    // ***** Event codes for driving the state machine
    private static final int BASE = Protocol.BASE_DATA_CONNECTION_TRACKER + 0x00002000;
    static final int REQ_CONNECT = BASE + 0;
    static final int RSP_CONNECT = BASE + 1;
    static final int REQ_DISCONNECT = BASE + 2;
    static final int RSP_DISCONNECT = BASE + 3;
    static final int REQ_IS_IDLE_STATE = BASE + 4;
    static final int RSP_IS_IDLE_STATE = BASE + 5;
    static final int REQ_IS_IDLE_OR_DEACTING_STATE = BASE + 6;
    static final int RSP_IS_IDLE_OR_DEACTING_STATE = BASE + 7;

    private static final int CMD_TO_STRING_COUNT = RSP_IS_IDLE_OR_DEACTING_STATE - BASE + 1;
    private static String[] sCmdToString = new String[CMD_TO_STRING_COUNT];
    static {
        sCmdToString[REQ_CONNECT - BASE] = "REQ_CONNECT";
        sCmdToString[RSP_CONNECT - BASE] = "RSP_CONNECT";
        sCmdToString[REQ_DISCONNECT - BASE] = "REQ_DISCONNECT";
        sCmdToString[RSP_DISCONNECT - BASE] = "RSP_DISCONNECT";
        sCmdToString[REQ_IS_IDLE_STATE - BASE] = "REQ_IS_IDLE_STATE";
        sCmdToString[RSP_IS_IDLE_STATE - BASE] = "RSP_IS_IDLE_STATE";
        sCmdToString[REQ_IS_IDLE_OR_DEACTING_STATE - BASE] = "REQ_IS_IDLE_OR_DEACTING_STATE";
        sCmdToString[RSP_IS_IDLE_OR_DEACTING_STATE - BASE] = "RSP_IS_IDLE_OR_DEACTING_STATE";		
    }

    protected static String cmdToString(int cmd) {
        cmd -= BASE;
        if ((cmd >= 0) && (cmd < sCmdToString.length)) {
            return sCmdToString[cmd];
        } else {
            return AsyncChannel.cmdToString(cmd + BASE);
        }
    }

    public DcSwitchAsyncChannel(DcSwitchState dcSwitchState, int id) {
        mDcSwitchState = dcSwitchState;
        tagId = id;
    }

    public void reqConnect(String type) {
        sendMessage(REQ_CONNECT, type);
        if (DBG) log("reqConnect");
    }

    public int rspConnect(Message response) {
        int retVal = response.arg1;
        if (DBG) log("rspConnect=" + retVal);
        return retVal;
    }

    public int connectSync(String type) {
        Message response = sendMessageSynchronously(REQ_CONNECT, type);
        if ((response != null) && (response.what == RSP_CONNECT)) {
            return rspConnect(response);
        } else {
            log("rspConnect error response=" + response);
            return PhoneConstants.APN_REQUEST_FAILED;
        }
    }

    public void reqDisconnect(String type) {
        sendMessage(REQ_DISCONNECT, type);
        if (DBG) log("reqDisconnect");
    }

    public int rspDisconnect(Message response) {
        int retVal = response.arg1;
        if (DBG) log("rspDisconnect=" + retVal);
        return retVal;
    }

    public int disconnectSync(String type) {
        Message response = sendMessageSynchronously(REQ_DISCONNECT, type);
        if ((response != null) && (response.what == RSP_DISCONNECT)) {
            return rspDisconnect(response);
        } else {
            log("rspDisconnect error response=" + response);
            return PhoneConstants.APN_REQUEST_FAILED;
        }
    }

    public void reqIsIdle() {
        sendMessage(REQ_IS_IDLE_STATE);
        if (DBG) log("reqIsIdle");
    }

    public boolean rspIsIdle(Message response) {
        boolean retVal = response.arg1 == 1;
        if (DBG) log("rspIsIdle=" + retVal);
        return retVal;
    }

    public boolean isIdleSync() {
        Message response = sendMessageSynchronously(REQ_IS_IDLE_STATE);
        if ((response != null) && (response.what == RSP_IS_IDLE_STATE)) {
            return rspIsIdle(response);
        } else {
            log("rspIsIndle error response=" + response);
            return false;
        }
    }

    public void reqIsIdleOrDeacting() {
        sendMessage(REQ_IS_IDLE_OR_DEACTING_STATE);
        if (DBG) log("reqIsIdleOrDeacting");
    }

    public boolean rspIsIdleOrDeacting(Message response) {
        boolean retVal = response.arg1 == 1;
        if (DBG) log("rspIsIdleOrDeacting=" + retVal);
        return retVal;
    }

    public boolean isIdleOrDeactingSync() {
        Message response = sendMessageSynchronously(REQ_IS_IDLE_OR_DEACTING_STATE);
        if ((response != null) && (response.what == RSP_IS_IDLE_OR_DEACTING_STATE)) {
            return rspIsIdleOrDeacting(response);
        } else {
            log("rspIsIndleOrDeacting error response=" + response);
            return false;
        }
    }	

    @Override
    public String toString() {
        return mDcSwitchState.getName();
    }

    private void log(String s) {
        Log.d(LOG_TAG, "[DcSwitchAsyncChannel-" + tagId + "]: " + s);
    }
}
