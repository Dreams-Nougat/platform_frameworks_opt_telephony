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

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.telephony.Rlog;

import java.util.List;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;


/**
 * {@hide}
 */
public class IccProvider extends ContentProvider {
    private static final String TAG = "IccProvider";
    private static final boolean DBG = false;


    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] {
        "name",
        "number",
        "emails",
        "_id"
    };

    protected static final int ADN_SUB1 = 1;
    protected static final int ADN_SUB2 = 2;
    protected static final int ADN_SUB3 = 3;
    protected static final int FDN_SUB1 = 4;
    protected static final int FDN_SUB2 = 5;
    protected static final int FDN_SUB3 = 6;
    protected static final int SDN      = 7;
    protected static final int ADN_ALL  = 8;

    protected static final String STR_TAG = "tag";
    protected static final String STR_NUMBER = "number";
    protected static final String STR_EMAILS = "emails";
    protected static final String STR_PIN2 = "pin2";

    private static final UriMatcher URL_MATCHER =
                            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URL_MATCHER.addURI("iccmsim", "adn", ADN_SUB1);
        URL_MATCHER.addURI("iccmsim", "adn_sub2", ADN_SUB2);
        URL_MATCHER.addURI("iccmsim", "adn_sub3", ADN_SUB3);
        URL_MATCHER.addURI("iccmsim", "adn_all", ADN_ALL);
        URL_MATCHER.addURI("iccmsim", "fdn", FDN_SUB1);
        URL_MATCHER.addURI("iccmsim", "fdn_sub2", FDN_SUB2);
        URL_MATCHER.addURI("iccmsim", "fdn_sub3", FDN_SUB3);
        URL_MATCHER.addURI("iccmsim", "sdn", SDN);
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        switch (URL_MATCHER.match(url)) {
            case ADN_SUB1:
                return loadFromEf(IccConstants.EF_ADN, PhoneConstants.SUB1);

            case ADN_SUB2:
                return loadFromEf(IccConstants.EF_ADN, PhoneConstants.SUB2);

            case ADN_SUB3:
                return loadFromEf(IccConstants.EF_ADN, PhoneConstants.SUB3);

            case ADN_ALL:
                return loadAllSimContacts(IccConstants.EF_ADN);

            case FDN_SUB1:
                return loadFromEf(IccConstants.EF_FDN, PhoneConstants.SUB1);

            case FDN_SUB2:
                return loadFromEf(IccConstants.EF_FDN, PhoneConstants.SUB2);

            case FDN_SUB3:
                return loadFromEf(IccConstants.EF_FDN, PhoneConstants.SUB3);

            case SDN:
                return loadFromEf(IccConstants.EF_SDN,
                    TelephonyManager.getDefault().getDefaultSubscription());

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private Cursor loadAllSimContacts(int efType) {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        Cursor [] result = new Cursor[phoneCount];
        for (int i = 0; i < phoneCount; i++) {
            if (TelephonyManager.getDefault().hasIccCard(i)) {
                result[i] = loadFromEf(efType, i);
                Rlog.i(TAG,"ADN Records loaded for Subscription ::" + i);
            } else {
                result[i] = null;
                Rlog.e(TAG,"ICC card is not present for subscription ::" + i);
            }
        }
        return new MergeCursor(result);
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ADN_SUB1:
            case ADN_SUB2:
            case ADN_SUB3:
            case FDN_SUB1:
            case FDN_SUB2:
            case FDN_SUB3:
            case SDN:
            case ADN_ALL:
                return "vnd.android.cursor.dir/sim-contact";

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        Uri resultUri;
        int efType;
        String pin2 = null;
        int phoneId = 0;

        if (DBG) log("insert");

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN_SUB1:
                phoneId = PhoneConstants.SUB1;
                efType = IccConstants.EF_ADN;
                break;

            case ADN_SUB2:
                phoneId = PhoneConstants.SUB2;
                efType = IccConstants.EF_ADN;
                break;

            case ADN_SUB3:
                phoneId = PhoneConstants.SUB3;
                efType = IccConstants.EF_ADN;
                break;

            case FDN_SUB1:
            case FDN_SUB2:
            case FDN_SUB3:
                efType = IccConstants.EF_FDN;
                pin2 = initialValues.getAsString("pin2");
                phoneId = initialValues.getAsInteger(PhoneConstants.SUBSCRIPTION_KEY);
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        String tag = initialValues.getAsString("tag");
        String number = initialValues.getAsString("number");
        long[] subId = SubscriptionController.getInstance().getSubId(phoneId);
        // TODO(): Read email instead of sending null.
        boolean success = addIccRecordToEf(efType, tag, number, null, pin2, subId[0]);

        if (!success) {
            return null;
        }

        StringBuilder buf = new StringBuilder("content://icc/");
        switch (match) {
            case ADN_SUB1:
                buf.append("adn/");
                break;

            case ADN_SUB2:
                buf.append("adn_sub2/");
                break;

            case ADN_SUB3:
                buf.append("adn_sub3/");
                break;

            case FDN_SUB1:
                buf.append("fdn/");
                break;

            case FDN_SUB2:
                buf.append("fdn_sub2/");
                break;

            case FDN_SUB3:
                buf.append("fdn_sub3/");
                break;
        }

        // TODO: we need to find out the rowId for the newly added record
        buf.append(0);

        resultUri = Uri.parse(buf.toString());

        getContext().getContentResolver().notifyChange(url, null);
        /*
        // notify interested parties that an insertion happened
        getContext().getContentResolver().notifyInsert(
                resultUri, rowID, null);
        */

        return resultUri;
    }

    private String normalizeValue(String inVal) {
        int len = inVal.length();
        // If name is empty in contact return null to avoid crash.
        if (len == 0) {
            if (DBG) log("len of input String is 0");
            return inVal;
        }
        String retVal = inVal;

        if (inVal.charAt(0) == '\'' && inVal.charAt(len-1) == '\'') {
            retVal = inVal.substring(1, len-1);
        }

        return retVal;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int efType;
        int phoneId = 0;

        if (DBG) log("delete");

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN_SUB1:
                phoneId = PhoneConstants.SUB1;
                efType = IccConstants.EF_ADN;
                break;

            case ADN_SUB2:
                phoneId = PhoneConstants.SUB2;
                efType = IccConstants.EF_ADN;
                break;

            case ADN_SUB3:
                phoneId = PhoneConstants.SUB3;
                efType = IccConstants.EF_ADN;
                break;

            case FDN_SUB1:
                phoneId = PhoneConstants.SUB1;
                efType = IccConstants.EF_FDN;
                break;

            case FDN_SUB2:
                phoneId = PhoneConstants.SUB2;
                efType = IccConstants.EF_FDN;
                break;

            case FDN_SUB3:
                phoneId = PhoneConstants.SUB3;
                efType = IccConstants.EF_FDN;
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        // parse where clause
        String tag = null;
        String number = null;
        String[] emails = null;
        String pin2 = null;

        String[] tokens = where.split("AND");
        int n = tokens.length;

        while (--n >= 0) {
            String param = tokens[n];
            if (DBG) log("parsing '" + param + "'");

            String[] pair = param.split("=");

            if (pair.length != 2) {
                Rlog.e(TAG, "resolve: bad whereClause parameter: " + param);
                continue;
            }
            String key = pair[0].trim();
            String val = pair[1].trim();

            if (STR_TAG.equals(key)) {
                tag = normalizeValue(val);
            } else if (STR_NUMBER.equals(key)) {
                number = normalizeValue(val);
            } else if (STR_EMAILS.equals(key)) {
                //TODO(): Email is null.
                emails = null;
            } else if (STR_PIN2.equals(key)) {
                pin2 = normalizeValue(val);
            }
        }

        if (((efType == FDN_SUB1) || efType == FDN_SUB2 || efType == FDN_SUB3) &&
            TextUtils.isEmpty(pin2)) {
            return 0;
        }

        if (efType == IccConstants.EF_FDN && TextUtils.isEmpty(pin2)) {
            return 0;
        }

        long[] subId = SubscriptionController.getInstance().getSubId(phoneId);
        boolean success = deleteIccRecordFromEf(efType, tag, number, emails, pin2, subId[0]);
        if (!success) {
            return 0;
        }

        getContext().getContentResolver().notifyChange(url, null);
        return 1;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int efType;
        String pin2 = null;
        int phoneId = 0;

        if (DBG) log("update");

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN_SUB1:
                phoneId = PhoneConstants.SUB1;
                efType = IccConstants.EF_ADN;
                break;
            case ADN_SUB2:
                phoneId = PhoneConstants.SUB2;
                efType = IccConstants.EF_ADN;
                break;
            case ADN_SUB3:
                phoneId = PhoneConstants.SUB3;
                efType = IccConstants.EF_ADN;
                break;

            case FDN_SUB1:
            case FDN_SUB2:
            case FDN_SUB3:

                efType = IccConstants.EF_FDN;
                pin2 = values.getAsString("pin2");
                phoneId = values.getAsInteger(PhoneConstants.SUBSCRIPTION_KEY);
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        String tag = values.getAsString("tag");
        String number = values.getAsString("number");
        String[] emails = null;
        String newTag = values.getAsString("newTag");
        String newNumber = values.getAsString("newNumber");
        String[] newEmails = null;
        long[] subId = SubscriptionController.getInstance().getSubId(phoneId);
        // TODO(): Update for email.
        boolean success = updateIccRecordInEf(efType, tag, number,
                newTag, newNumber, pin2, subId[0]);

        if (!success) {
            return 0;
        }

        getContext().getContentResolver().notifyChange(url, null);
        return 1;
    }

    private MatrixCursor loadFromEf(int efType, long subId) {
        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEfUsingSub(subId, efType);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }

        if (adnRecords != null) {
            // Load the results
            final int N = adnRecords.size();
            final MatrixCursor cursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, N);
            if (DBG) log("adnRecords.size=" + N);
            for (int i = 0; i < N ; i++) {
                loadRecord(adnRecords.get(i), cursor, i);
            }
            return cursor;
        } else {
            // No results to load
            Rlog.w(TAG, "Cannot load ADN records");
            return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
        }
    }

    private boolean
    addIccRecordToEf(int efType, String name, String number, String[] emails,
            String pin2, long subId) {
        if (DBG) log("addIccRecordToEf: efType=" + efType + ", name=" + name +
                ", number=" + number + ", emails=" + emails);

        boolean success = false;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.

        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearchUsingSub(subId, efType,
                        "", "", name, number, pin2);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("addIccRecordToEf: " + success);
        return success;
    }

    private boolean
    updateIccRecordInEf(int efType, String oldName, String oldNumber,
            String newName, String newNumber, String pin2, long subId) {
        if (DBG) log("updateIccRecordInEf: efType=" + efType +
                ", oldname=" + oldName + ", oldnumber=" + oldNumber +
                ", newname=" + newName + ", newnumber=" + newNumber +
                ", subscription=" + subId);
        boolean success = false;

        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearchUsingSub(subId, efType, oldName,
                        oldNumber, newName, newNumber, pin2);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("updateIccRecordInEf: " + success);
        return success;
    }


    private boolean deleteIccRecordFromEf(int efType, String name, String number, String[] emails,
            String pin2, long subId) {
        if (DBG) log("deleteIccRecordFromEf: efType=" + efType +
                ", name=" + name + ", number=" + number + ", emails=" + emails +
                ", pin2=" + pin2 + ", subscription=" + subId);

        boolean success = false;

        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearchUsingSub(subId, efType,
                          name, number, "", "", pin2);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("deleteIccRecordFromEf: " + success);
        return success;
    }

    /**
     * Loads an AdnRecord into a MatrixCursor. Must be called with mLock held.
     *
     * @param record the ADN record to load from
     * @param cursor the cursor to receive the results
     */
    private void loadRecord(AdnRecord record, MatrixCursor cursor, int id) {
        if (!record.isEmpty()) {
            Object[] contact = new Object[4];
            String alphaTag = record.getAlphaTag();
            String number = record.getNumber();

            if (DBG) log("loadRecord: " + alphaTag + ", " + number + ",");
            contact[0] = alphaTag;
            contact[1] = number;

            String[] emails = record.getEmails();
            if (emails != null) {
                StringBuilder emailString = new StringBuilder();
                for (String email: emails) {
                    if (DBG) log("Adding email:" + email);
                    emailString.append(email);
                    emailString.append(",");
                }
                contact[2] = emailString.toString();
            }
            contact[3] = id;
            cursor.addRow(contact);
        }
    }

    private void log(String msg) {
        Rlog.d(TAG, "[IccProvider] " + msg);
    }

}
