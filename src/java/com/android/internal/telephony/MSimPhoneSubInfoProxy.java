/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.telephony.Rlog;
import java.lang.NullPointerException;
import java.lang.ArrayIndexOutOfBoundsException;

import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneSubInfoProxy;

public class MSimPhoneSubInfoProxy extends IPhoneSubInfo.Stub {
    private static final String TAG = "MSimPhoneSubInfoProxy";
    private Phone[] mPhone;

    public MSimPhoneSubInfoProxy(Phone[] phone) {
        mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
    }


    public String getDeviceId() {
        return getDeviceIdOnSubscription(getDefaultSubscription());
    }

    public String getDeviceIdOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getDeviceId();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null" +
                      " for Subscription:"+subscription);
            return null;
        }
    }

    public String getDeviceSvn() {
        return getDeviceSvnOnSubscription(getDefaultSubscription());
    }

    public String getDeviceSvnOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getDeviceSvn();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null" +
                      " for Subscription:"+subscription);
            return null;
        }
    }

    public String getSubscriberId() {
        return getSubscriberIdOnSubscription(getDefaultSubscription());
    }

    public String getSubscriberIdOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getSubscriberId();
        } else {
            Rlog.e(TAG,"getSubscriberId phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    public String getIccSerialNumber() {
        return getIccSerialNumberOnSubscription(getDefaultSubscription());
    }

    public String getIccSerialNumberOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getIccSerialNumber();
        } else {
            Rlog.e(TAG,"getIccSerialNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1Number() {
        return getLine1NumberOnSubscription(getDefaultSubscription());
    }

    public String getLine1NumberOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getLine1Number();
        } else {
            Rlog.e(TAG,"getLine1Number phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1AlphaTag() {
        return getLine1AlphaTagOnSubscription(getDefaultSubscription());
    }

    public String getLine1AlphaTagOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getLine1AlphaTag();
        } else {
            Rlog.e(TAG,"getLine1AlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getMsisdn() {
        return getMsisdnOnSubscription(getDefaultSubscription());
    }

    public String getMsisdnOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getMsisdn();
        } else {
            Rlog.e(TAG,"getMsisdn phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailNumber() {
        return getVoiceMailNumberOnSubscription(getDefaultSubscription());
    }

    public String getVoiceMailNumberOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getVoiceMailNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberOnSubscription(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getCompleteVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getCompleteVoiceMailNumber phoneSubInfoProxy" +
                      " is null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTagOnSubscription(getDefaultSubscription());
    }

    public String getVoiceMailAlphaTagOnSubscription(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getVoiceMailAlphaTag();
        } else {
            Rlog.e(TAG,"getVoiceMailAlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    /**
     * get Phone sub info proxy object based on subscription.
     **/
    private PhoneSubInfoProxy getPhoneSubInfoProxy(int subscription) {
        try {
            return ((MSimPhoneProxy)mPhone[subscription]).getPhoneSubInfoProxy();
        } catch (NullPointerException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace();
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace();
            return null;
        }
    }

    private int getDefaultSubscription() {
        return MSimPhoneFactory.getDefaultSubscription();
    }


    public String getIsimImpi() {
        PhoneSubInfoProxy mPhoneSubInfo = getPhoneSubInfoProxy(getDefaultSubscription());
        return mPhoneSubInfo.getIsimImpi();
    }

    public String getIsimDomain() {
        PhoneSubInfoProxy mPhoneSubInfo = getPhoneSubInfoProxy(getDefaultSubscription());
        return mPhoneSubInfo.getIsimDomain();
    }

    public String[] getIsimImpu() {
        PhoneSubInfoProxy mPhoneSubInfo = getPhoneSubInfoProxy(getDefaultSubscription());
        return mPhoneSubInfo.getIsimImpu();
    }

     public String getGroupIdLevel1() {
        PhoneSubInfoProxy mPhoneSubInfo = getPhoneSubInfoProxy(getDefaultSubscription());
        return mPhoneSubInfo.getGroupIdLevel1();
     }

}
