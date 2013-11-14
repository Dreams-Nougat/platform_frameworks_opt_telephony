/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

/*****************************************************************************
* Copyright 2007 - 2009 Broadcom Corporation.  All rights reserved.
*
* This program is the proprietary software of Broadcom Corporation and/or
* its licensors, and may only be used, duplicated, modified or distributed
* pursuant to the terms and conditions of a separate, written license
* agreement executed between you and Broadcom (an "Authorized License").
*
* Except as set forth in an Authorized License, Broadcom grants no license
* (express or implied), right to use, or waiver of any kind with respect to
* the Software, and Broadcom expressly reserves all rights in and to the
* Software and all intellectual property rights therein.  IF YOU HAVE NO
* AUTHORIZED LICENSE, THEN YOU HAVE NO RIGHT TO USE THIS SOFTWARE IN ANY
* WAY, AND SHOULD IMMEDIATELY NOTIFY BROADCOM AND DISCONTINUE ALL USE OF
* THE SOFTWARE.
*
* Except as expressly set forth in the Authorized License,
* 1. This program, including its structure, sequence and organization,
*    constitutes the valuable trade secrets of Broadcom, and you shall use
*    all reasonable efforts to protect the confidentiality thereof, and to
*    use this information only in connection with your use of Broadcom
*    integrated circuit products.
*
* 2. TO THE MAXIMUM EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED "AS IS"
*    AND WITH ALL FAULTS AND BROADCOM MAKES NO PROMISES, REPRESENTATIONS OR
*    WARRANTIES, EITHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, WITH
*    RESPECT TO THE SOFTWARE.  BROADCOM SPECIFICALLY DISCLAIMS ANY AND ALL
*    IMPLIED WARRANTIES OF TITLE, MERCHANTABILITY, NONINFRINGEMENT, FITNESS
*    FOR A PARTICULAR PURPOSE, LACK OF VIRUSES, ACCURACY OR COMPLETENESS,
*    QUIET ENJOYMENT, QUIET POSSESSION OR CORRESPONDENCE TO DESCRIPTION. YOU
*    ASSUME THE ENTIRE RISK ARISING OUT OF USE OR PERFORMANCE OF THE SOFTWARE.
*
* 3. TO THE MAXIMUM EXTENT PERMITTED BY LAW, IN NO EVENT SHALL BROADCOM OR ITS
*    LICENSORS BE LIABLE FOR (i) CONSEQUENTIAL, INCIDENTAL, SPECIAL, INDIRECT,
*    OR EXEMPLARY DAMAGES WHATSOEVER ARISING OUT OF OR IN ANY WAY RELATING TO
*    YOUR USE OF OR INABILITY TO USE THE SOFTWARE EVEN IF BROADCOM HAS BEEN
*    ADVISED OF THE POSSIBILITY OF SUCH DAMAGES; OR (ii) ANY AMOUNT IN EXCESS
*    OF THE AMOUNT ACTUALLY PAID FOR THE SOFTWARE ITSELF OR U.S. $1, WHICHEVER
*    IS GREATER. THESE LIMITATIONS SHALL APPLY NOTWITHSTANDING ANY FAILURE OF
*    ESSENTIAL PURPOSE OF ANY LIMITED REMEDY.
*/

package com.android.internal.telephony;

import android.util.Log;
import java.util.ArrayList;
import static com.android.internal.telephony.gsm.SmsCbConstants.*;
import com.android.internal.telephony.RILConstants.SimCardID;

/**
 * Manage enabled/disabled message identifiers in cell broadcast.
 * 1. Normal cell broadcast
 * Message id from 0 to 999;
 * 2. CMAS and ETWS
 * Message id of ETWS: 0x1100 ~ 0x1104 (4352~4356)
 * Message id of CMAS: 0x1112 ~ 0x112F (4370~4399)
 * Whole range of Message ids defined as emergencey: 0x1100 ~ 0x18FF (4352~6399)
 *
 */

public class BrcmCbManager {
    private static final String TAG = "BrcmCbManager";

    private static final int CB_ERROR = -1;
    private static final int NORMAL_CB_START = 0;
    private static final int NORMAL_CB_END = 999;

    private static boolean sIsCmasWholeRangeOn[] = {false, false};
    private static boolean sIsCbWholeRangeOn[] = {false, false};
    private static boolean sIsCmasOn[] = {false, false};
    private static ArrayList<Integer> sCmasEnabledList_0 = new ArrayList<Integer>();
    private static ArrayList<Integer> sCmasEnabledList_1 = new ArrayList<Integer>();

    private static ArrayList<Integer> sNormalEnabledList_0 = new ArrayList<Integer>();
    private static ArrayList<Integer> sNormalEnabledList_1 = new ArrayList<Integer>();

    public BrcmCbManager() {
    }

    public int addNormalMsgId(int msgId, SimCardID simId) {
        Log.d(TAG, "adding " + msgId + " for sim " + simId.toInt());

        ArrayList<Integer> normalEnabledList = getNormalEnabledList(simId);

        if (msgId < NORMAL_CB_START || msgId > NORMAL_CB_END) {
            Log.d(TAG, "msgId out of normal range, no add");
            return CB_ERROR;
        }

        if (!normalEnabledList.contains(msgId)) {
            normalEnabledList.add(Integer.valueOf(msgId));
        } else {
            Log.d(TAG, "msgId " + msgId + " is already contained in the list");
        }

        dumpCmasEnabledList(simId);

        return normalEnabledList.size();
    }

    public int addNormalMsgId(int startId, int endId, SimCardID simId) {
        Log.d(TAG, "adding from " + startId + " to " + endId + " for sim " + simId.toInt());

        ArrayList<Integer> normalEnabledList = getNormalEnabledList(simId);

        if (startId < NORMAL_CB_START || endId > NORMAL_CB_END) {
            Log.d(TAG, "msgId out of normal range, no add");
            return CB_ERROR;
        }

        if (startId == NORMAL_CB_START && endId == NORMAL_CB_END) {
            Log.d(TAG, "Normal CB all channel on");
            sIsCbWholeRangeOn[simId.toInt()] = true;
            return 0;
        }

        for (int i = startId; i <= endId; i++) {
            if (!normalEnabledList.contains(i)) {
                normalEnabledList.add(Integer.valueOf(i));
            }
            else {
                Log.d(TAG, "msgId " + i + " is already contained in the list");
            }
        }

        dumpNormalEnabledList(simId);

        return normalEnabledList.size();
    }

    /**
     * Remove a message identifier from the normal enabled list.
     */
    public int removeNormalMsgId(int msgId, SimCardID simId) {
        Log.d(TAG, "removing " + msgId + " for sim " + simId.toInt());

        ArrayList<Integer> normalEnabledList = getNormalEnabledList(simId);

        if (normalEnabledList.remove(Integer.valueOf(msgId))) {
            return normalEnabledList.size();
        }
        else {
            Log.d(TAG, "msgId not in normalEnabledList");
            return CB_ERROR;
        }
    }

    /**
     * Remove a range of message identifiers from the normal enabled list.
     */
    public int removeNormalMsgId(int startId, int endId, SimCardID simId) {
        Log.d(TAG, "removing from " + startId + " to " + endId + " for sim " + simId.toInt());

        if (startId == NORMAL_CB_START && endId == NORMAL_CB_END) {
            Log.d(TAG, "Normal CB all channel off");
            sIsCbWholeRangeOn[simId.toInt()] = false;
            return 0;
        }

        ArrayList<Integer> normalEnabledList = getNormalEnabledList(simId);

        boolean removeSuccess = false;

        for (int i = startId; i <= endId; i++) {
            if (normalEnabledList.remove(Integer.valueOf(i))) {
                removeSuccess = true;
            }
            else {
                Log.d(TAG, "msgId " + i + " not in normalEnabledList");
            }
        }

        dumpNormalEnabledList(simId);

        if (removeSuccess)
            return normalEnabledList.size();
        else
            return CB_ERROR;
    }

    public boolean getIsNormalMsgIdEmpty(SimCardID simId) {
        if (sIsCbWholeRangeOn[simId.toInt()]) {
            return false;
        }
        return getNormalEnabledList(simId).isEmpty();
    }

    public void setIsCmasOn(boolean on, SimCardID simId) {
        sIsCmasOn[simId.toInt()] = on;
    }

    public boolean getIsCmasOn(SimCardID simId) {
        return sIsCmasOn[simId.toInt()];
    }

    /**
     * Add a single message identifiers to the CMAS enabled list.
     * In 3GPP TS 23.041, msg ids after MESSAGE_ID_CMAS_LAST_IDENTIFIER(0x112F) are defined as
     * future extentions and shall not be sent by operators.
     * Since we don't want to waste memory for the whole 2000+ msg ids, these future extensions
     * are not maintained in sCmasEnabledList.
     */
    public int addCmasMsgId(int msgId, SimCardID simId) {
        Log.d(TAG, "adding " + msgId + " for sim " + simId.toInt());

        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        if (msgId < MESSAGE_ID_PWS_FIRST_IDENTIFIER || msgId > MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "msgId out of emergency range, no add");
            return CB_ERROR;
        }

        if (msgId > MESSAGE_ID_CMAS_LAST_IDENTIFIER) {
            Log.d(TAG, "not maintaining future extensions");
            return CB_ERROR;
        }

        if (!cmasEnabledList.contains(msgId)) {
            cmasEnabledList.add(getIndexToBeInserted(msgId, simId), Integer.valueOf(msgId));
        } else {
            Log.d(TAG, "msgId " + msgId + " is already contained in the list");
        }

        dumpCmasEnabledList(simId);

        return cmasEnabledList.size();
    }

    /**
     * Add a range of message identifiers to the CMAS enabled list.
     * In 3GPP TS 23.041, msg ids after MESSAGE_ID_CMAS_LAST_IDENTIFIER(0x112F) are defined as
     * future extentions and shall not be sent by operators.
     * Since we don't want to waste memory for the whole 2000+ msg ids, these future extensions
     * are not maintained in sCmasEnabledList.
     */
    public int addCmasMsgId(int startId, int endId, SimCardID simId) {
        Log.d(TAG, "adding from " + startId + " to " + endId + " for sim " + simId.toInt());

        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        if (startId < MESSAGE_ID_PWS_FIRST_IDENTIFIER || endId > MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "msgId out of emergency range, no add");
            return CB_ERROR;
        }

        if (startId == MESSAGE_ID_PWS_FIRST_IDENTIFIER
                && endId == MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "whole range enable");
            sIsCmasWholeRangeOn[simId.toInt()] = true;
            //not maintaining future extensions
            endId = MESSAGE_ID_CMAS_LAST_IDENTIFIER;
        }

        for (int i = startId; i <= endId; i++) {
            if (!cmasEnabledList.contains(i)) {
                cmasEnabledList.add(getIndexToBeInserted(i, simId), Integer.valueOf(i));
                //sCmasEnabledList.add(i);
            }
            else {
                Log.d(TAG, "msgId " + i + " is already contained in the list");
            }
        }

        dumpCmasEnabledList(simId);

        return cmasEnabledList.size();
    }

    private int getIndexToBeInserted(int msgId, SimCardID simId) {
        int i;

        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        for (i = 0; i < cmasEnabledList.size(); i++) {
            int currentMsgId = cmasEnabledList.get(i);
            if (currentMsgId < msgId) {
                continue;
            } else {
                return i;
            }
        }
        return i;
    }

    private static ArrayList<Integer> getNormalEnabledList(SimCardID simId) {
        if(SimCardID.ID_ONE == simId) {
            return sNormalEnabledList_1;
        } else {
            return sNormalEnabledList_0;
        }
    }

    private static ArrayList<Integer> getCmasEnabledList(SimCardID simId) {
        if(SimCardID.ID_ONE == simId) {
            return sCmasEnabledList_1;
        } else {
            return sCmasEnabledList_0;
        }
    }

    /**
     * Remove a message identifier from the CMAS enabled list.
     */
    public int removeCmasMsgId(int msgId, SimCardID simId) {
        Log.d(TAG, "removing " + msgId + " for sim " + simId.toInt());

        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        if (msgId < MESSAGE_ID_PWS_FIRST_IDENTIFIER || msgId > MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "msgId out of emergency range, no remove");
            return CB_ERROR;
        }
        if (cmasEnabledList.remove(Integer.valueOf(msgId))) {
            return cmasEnabledList.size();
        }
        else {
            Log.d(TAG, "msgId not in sCmasEnabledList");
            return CB_ERROR;
        }
    }

    /**
     * Remove a range of message identifiers from the CMAS enabled list.
     */
    public int removeCmasMsgId(int startId, int endId, SimCardID simId) {
        Log.d(TAG, "removing from " + startId + " to " + endId + " for sim " + simId.toInt());

        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        boolean removeSuccess = false;

        if (startId == MESSAGE_ID_PWS_FIRST_IDENTIFIER
                && endId == MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "whole range disable");
            sIsCmasWholeRangeOn[simId.toInt()] = false;
            endId = MESSAGE_ID_CMAS_LAST_IDENTIFIER;
        }

        for (int i = startId; i <= endId; i++) {
            if (cmasEnabledList.remove(Integer.valueOf(i))) {
                removeSuccess = true;
            }
            else {
                Log.d(TAG, "msgId " + i + " not in sCmasEnabledList");
            }
        }

        dumpCmasEnabledList(simId);

        if (removeSuccess)
            return cmasEnabledList.size();
        else
            return CB_ERROR;
    }

    public boolean isCmasEnabledListEmpty(SimCardID simId) {
        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        return cmasEnabledList.isEmpty();
    }

    /**
     * Filter out emergency broadcasts that are not defined in the CMAS enabled list.
     */
    public static boolean allowEmergencyMessageDispatched(int msgId, SimCardID simId) {
        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        //always allow PRESENTIAL
        if (msgId == MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL) {
            return true;
        }

        //bypass the future extensions
        if (sIsCmasWholeRangeOn[simId.toInt()] && msgId > MESSAGE_ID_CMAS_LAST_IDENTIFIER
                && msgId <= MESSAGE_ID_PWS_LAST_IDENTIFIER) {
            Log.d(TAG, "Intended as PWS range in future version of 3GPP TS 23.041");
            return true;
        }

        if (cmasEnabledList.contains((Integer)msgId)) {
            return true;
        }
        return false;
    }

    public void dumpNormalEnabledList(SimCardID simId) {
        ArrayList<Integer> normalEnabledList = getNormalEnabledList(simId);

        String dumpStr = "sNormalEnabledList: ";
        for (int i = 0; i < normalEnabledList.size(); i++) {
            dumpStr += ("[" + i + "]" + ((Integer)normalEnabledList.get(i)).intValue() + ", ");
        }
        dumpStr += " for sim " + simId.toInt();
        Log.d(TAG, dumpStr);
    }

    public void dumpCmasEnabledList(SimCardID simId) {
        ArrayList<Integer> cmasEnabledList = getCmasEnabledList(simId);

        String dumpStr = "sCmasEnabledList: ";
        for (int i = 0; i < cmasEnabledList.size(); i++) {
            dumpStr += ("[" + i + "]" + ((Integer)cmasEnabledList.get(i)).intValue() + ", ");
        }
        dumpStr += " for sim " + simId.toInt();
        Log.d(TAG, dumpStr);
    }
}
