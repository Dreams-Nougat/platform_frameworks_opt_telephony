/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final String TAG = "PhoneSubInfoController";
    private Phone[] mPhone;

    public PhoneSubInfoController(Phone[] phone) {
        mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
    }


    public String getDeviceId() {
        return getDeviceIdUsingSub(getDefaultSubscription());
    }

    public String getDeviceIdUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getDeviceId();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null" +
                      " for Subscription:"+subscription);
            return null;
        }
    }

    public String getDeviceSvn() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getDeviceSvn();
        } else {
            Rlog.e(TAG,"getDeviceSvn phoneSubInfoProxy is null");
            return null;
        }
    }

    public String getSubscriberId() {
        return getSubscriberIdUsingSub(getDefaultSubscription());
    }

    public String getSubscriberIdUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getSubscriberId();
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
        return getIccSerialNumberUsingSub(getDefaultSubscription());
    }

    public String getIccSerialNumberUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getIccSerialNumber();
        } else {
            Rlog.e(TAG,"getIccSerialNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1Number() {
        return getLine1NumberUsingSub(getDefaultSubscription());
    }

    public String getLine1NumberUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getLine1Number();
        } else {
            Rlog.e(TAG,"getLine1Number phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1AlphaTag() {
        return getLine1AlphaTagUsingSub(getDefaultSubscription());
    }

    public String getLine1AlphaTagUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getLine1AlphaTag();
        } else {
            Rlog.e(TAG,"getLine1AlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getMsisdn() {
        return getMsisdnUsingSub(getDefaultSubscription());
    }

    public String getMsisdnUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getMsisdn();
        } else {
            Rlog.e(TAG,"getMsisdn phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailNumber() {
        return getVoiceMailNumberUsingSub(getDefaultSubscription());
    }

    public String getVoiceMailNumberUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getVoiceMailNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberUsingSub(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getCompleteVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getCompleteVoiceMailNumber phoneSubInfoProxy" +
                      " is null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTagUsingSub(getDefaultSubscription());
    }

    public String getVoiceMailAlphaTagUsingSub(long subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getVoiceMailAlphaTag();
        } else {
            Rlog.e(TAG,"getVoiceMailAlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    /**
     * get Phone sub info proxy object based on subscription.
     **/
    private PhoneSubInfoProxy getPhoneSubInfoProxy(long subscription) {

        SubscriptionManager subMgr = SubscriptionManager.getInstance();
        long phoneId = subMgr.getSimId(subscription);

        try {
            return ((PhoneProxy)mPhone[(int)phoneId]).getPhoneSubInfoProxy();
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

    private long getDefaultSubscription() {
        return  PhoneFactory.getDefaultSubscription();
    }


    public String getIsimImpi() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimImpi();
    }

    public String getIsimDomain() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimDomain();
    }

    public String[] getIsimImpu() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimImpu();
    }

     public String getGroupIdLevel1() {
         return getGroupIdLevel1UsingSub(getDefaultSubscription());
     }

     public String getGroupIdLevel1UsingSub(long subscription) {
         PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
         if (phoneSubInfoProxy != null) {
             return phoneSubInfoProxy.getGroupIdLevel1();
         } else {
             Rlog.e(TAG,"getGroupIdLevel1 phoneSubInfoProxy is" +
                       " null for Subscription:"+subscription);
             return null;
         }
     }
}
