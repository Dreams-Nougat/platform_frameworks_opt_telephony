/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.ServiceManager;
import com.android.internal.telephony.uicc.AdnRecord;
import android.telephony.TelephonyManager;

import java.util.List;


/**
 * SimPhoneBookInterfaceManager to provide an inter-process communication to
 * access ADN-like SIM records.
 */
public class IccPhoneBookInterfaceManagerProxy extends IIccPhoneBook.Stub {
    private IccPhoneBookInterfaceManager mIccPhoneBookInterfaceManager;

    public IccPhoneBookInterfaceManagerProxy(IccPhoneBookInterfaceManager
            iccPhoneBookInterfaceManager) {
        this(iccPhoneBookInterfaceManager, TelephonyManager.getDefault().getDefaultSim());
    }

    public IccPhoneBookInterfaceManagerProxy(IccPhoneBookInterfaceManager
            iccPhoneBookInterfaceManager, int simId) {
        mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;

        if (PhoneConstants.SIM_ID_1 == simId) {
            if (ServiceManager.getService("simphonebook") == null) {
                ServiceManager.addService("simphonebook", this);
            }
        } else if (PhoneConstants.SIM_ID_2 == simId){
            if (ServiceManager.getService("simphonebook2") == null) {
                ServiceManager.addService("simphonebook2", this);
            }
        } else if (PhoneConstants.SIM_ID_3 == simId){
            if (ServiceManager.getService("simphonebook3") == null) {
                ServiceManager.addService("simphonebook3", this);
            }
        } else {
            if (ServiceManager.getService("simphonebook4") == null) {
                ServiceManager.addService("simphonebook4", this);
            }
        }
    }

    public void setmIccPhoneBookInterfaceManager(
            IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager) {
        mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;
    }

    @Override
    public boolean
    updateAdnRecordsInEfBySearch (int efid,
            String oldTag, String oldPhoneNumber,
            String newTag, String newPhoneNumber,
            String pin2) {
        return mIccPhoneBookInterfaceManager.updateAdnRecordsInEfBySearch(
                efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    @Override
    public boolean
    updateAdnRecordsInEfByIndex(int efid, String newTag,
            String newPhoneNumber, int index, String pin2) {
        return mIccPhoneBookInterfaceManager.updateAdnRecordsInEfByIndex(efid,
                newTag, newPhoneNumber, index, pin2);
    }

    @Override
    public int[] getAdnRecordsSize(int efid) {
        return mIccPhoneBookInterfaceManager.getAdnRecordsSize(efid);
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEf(int efid) {
        return mIccPhoneBookInterfaceManager.getAdnRecordsInEf(efid);
    }
}
