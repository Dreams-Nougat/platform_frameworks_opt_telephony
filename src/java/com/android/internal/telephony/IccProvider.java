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
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

import java.util.List;
import com.android.internal.telephony.PhoneConstants;

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

    private static final int ADN = 1;
    private static final int FDN = 2;
    private static final int SDN = 3;
    private static final int ADN1 = 101;
    private static final int FDN1 = 102;
    private static final int SDN1 = 103;
    private static final int ADN2 = 201;
    private static final int FDN2 = 202;
    private static final int SDN2 = 203;
    private static final int ADN3 = 301;
    private static final int FDN3 = 302;
    private static final int SDN3 = 303;
    private static final int ADN4 = 401;
    private static final int FDN4 = 402;
    private static final int SDN4 = 403;

    private static final String STR_TAG = "tag";
    private static final String STR_NUMBER = "number";
    private static final String STR_EMAILS = "emails";
    private static final String STR_PIN2 = "pin2";

    private static final UriMatcher URL_MATCHER =
                            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URL_MATCHER.addURI("icc", "adn", ADN);
        URL_MATCHER.addURI("icc", "fdn", FDN);
        URL_MATCHER.addURI("icc", "sdn", SDN);
        URL_MATCHER.addURI("icc", "adn1", ADN1);
        URL_MATCHER.addURI("icc", "fdn1", FDN1);
        URL_MATCHER.addURI("icc", "sdn1", SDN1);
        URL_MATCHER.addURI("icc", "adn2", ADN2);
        URL_MATCHER.addURI("icc", "fdn2", FDN2);
        URL_MATCHER.addURI("icc", "sdn2", SDN2);
        URL_MATCHER.addURI("icc", "adn3", ADN3);
        URL_MATCHER.addURI("icc", "fdn3", FDN3);
        URL_MATCHER.addURI("icc", "sdn3", SDN3);
        URL_MATCHER.addURI("icc", "adn4", ADN4);
        URL_MATCHER.addURI("icc", "fdn4", FDN4);
        URL_MATCHER.addURI("icc", "sdn4", SDN4);
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        int efType;
        int simId;
        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        Cursor results = loadFromEf(efType, simId);
        return results;
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ADN:
            case ADN1:
            case ADN2:
            case ADN3:
            case ADN4:
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
            case SDN:
            case SDN1:
            case SDN2:
            case SDN3:
            case SDN4:
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
        int simId;

        if (DBG) log("insert");

        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        switch (match) {
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                pin2 = initialValues.getAsString("pin2");
                break;

            default:
                break;
        }

        String tag = initialValues.getAsString("tag");
        String number = initialValues.getAsString("number");
        // TODO(): Read email instead of sending null.
        boolean success = addIccRecordToEf(efType, tag, number, null, pin2, simId);

        if (!success) {
            return null;
        }

        StringBuilder buf = new StringBuilder("content://icc/");
        switch (match) {
            case ADN:
                buf.append("adn/");
                break;
            case ADN1:
                buf.append("adn1/");
                break;
            case ADN2:
                buf.append("adn2/");
                break;
            case ADN3:
                buf.append("adn3/");
                break;
            case ADN4:
                buf.append("adn4/");
                break;
            case FDN:
                buf.append("fdn/");
                break;
            case FDN1:
                buf.append("fdn1/");
                break;
            case FDN2:
                buf.append("fdn2/");
                break;
            case FDN3:
                buf.append("fdn3/");
                break;
            case FDN4:
                buf.append("fdn4/");
                break;
        }

        // TODO: we need to find out the rowId for the newly added record
        buf.append(0);

        resultUri = Uri.parse(buf.toString());

        /*
        // notify interested parties that an insertion happened
        getContext().getContentResolver().notifyInsert(
                resultUri, rowID, null);
        */

        return resultUri;
    }

    private String normalizeValue(String inVal) {
        int len = inVal.length();
        String retVal = inVal;

        if (inVal.charAt(0) == '\'' && inVal.charAt(len-1) == '\'') {
            retVal = inVal.substring(1, len-1);
        }

        return retVal;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int efType;
        int simId;

        if (DBG) log("delete");

        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);

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

            String[] pair = param.split("=", 2);

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

        if (TextUtils.isEmpty(number)) {
            return 0;
        }

        if (efType == IccConstants.EF_FDN && TextUtils.isEmpty(pin2)) {
            return 0;
        }

        boolean success = deleteIccRecordFromEf(efType, tag, number, emails, pin2, simId);
        if (!success) {
            return 0;
        }

        return 1;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int efType;
        String pin2 = null;
        int simId;

        if (DBG) log("update");

        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        switch (match) {
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                pin2 = values.getAsString("pin2");
                break;

            default:
                break;
        }

        String tag = values.getAsString("tag");
        String number = values.getAsString("number");
        String[] emails = null;
        String newTag = values.getAsString("newTag");
        String newNumber = values.getAsString("newNumber");
        String[] newEmails = null;
        // TODO(): Update for email.
        boolean success = updateIccRecordInEf(efType, tag, number,
                newTag, newNumber, pin2, simId);

        if (!success) {
            return 0;
        }

        return 1;
    }

    private MatrixCursor loadFromEf(int efType, int simId) {
        if (DBG) log("loadFromEf: efType=" + efType);

        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEf(efType);
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
    addIccRecordToEf(int efType, String name, String number, String[] emails, String pin2, int simId) {
        if (DBG) log("addIccRecordToEf: efType=" + efType + ", name=" + name +
                ", number=" + number + ", emails=" + emails + ", simId=" + simId);

        boolean success = false;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType, "", "",
                        name, number, pin2);
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
            String newName, String newNumber, String pin2, int simId) {
        if (DBG) log("updateIccRecordInEf: efType=" + efType +
                ", oldname=" + oldName + ", oldnumber=" + oldNumber +
                ", newname=" + newName + ", newnumber=" + newNumber);
        boolean success = false;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType,
                        oldName, oldNumber, newName, newNumber, pin2);
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
            String pin2, int simId) {
        if (DBG) log("deleteIccRecordFromEf: efType=" + efType +
                ", name=" + name + ", number=" + number + ", emails=" + emails + ", pin2=" + pin2);

        boolean success = false;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType,
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

    private IIccPhoneBook getIccPhbService(int simId) {

        IIccPhoneBook iccIpb;
        if (simId == PhoneConstants.SIM_ID_1) {
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
        } else if (simId == PhoneConstants.SIM_ID_2){
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook2"));
        } else if (simId == PhoneConstants.SIM_ID_3){
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook3"));
        } else {
            iccIpb = IIccPhoneBook.Stub.asInterface(
            ServiceManager.getService("simphonebook4"));
        }

        return iccIpb;
    }

    private int getRequestType(int match) {
        switch (match) {
            case ADN:
            case ADN1:
            case ADN2:
            case ADN3:
            case ADN4:
                return IccConstants.EF_ADN;
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                return IccConstants.EF_FDN;
            case SDN:
            case SDN1:
            case SDN2:
            case SDN3:
            case SDN4:
                return IccConstants.EF_SDN;
            default:
                throw new IllegalArgumentException("Unknown URL " + match);
        }
    }

    private int getRequestSim(int match) {
        switch (match) {
            case ADN:
            case FDN:
            case SDN:
                return TelephonyManager.getDefault().getDefaultSim();
            case ADN1:
            case FDN1:
            case SDN1:
                return PhoneConstants.SIM_ID_1;
            case ADN2:
            case FDN2:
            case SDN2:
                return PhoneConstants.SIM_ID_2;
            case ADN3:
            case FDN3:
            case SDN3:
                return PhoneConstants.SIM_ID_3;
            case ADN4:
            case FDN4:
            case SDN4:
                return PhoneConstants.SIM_ID_4;
            default:
                throw new IllegalArgumentException("Unknown URL " + match);
        }
    }
}
