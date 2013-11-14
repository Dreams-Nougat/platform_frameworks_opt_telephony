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

import java.util.ArrayList;
import java.util.List;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.RILConstants.SimCardID;


/**
 * {@hide}
 */
public class IccProvider extends ContentProvider {
    private static final String TAG = "IccProvider";
    private static final boolean DBG = true;
    private static final boolean mUseIdx = true;

    public static final String ICC_TAG = "name";
    public static final String ICC_NUMBER = "number";
    public static final String ICC_EMAILS = "emails";
    public static final String ICC_GROUPS = "groups";
    public static final String ICC_ANRS = "anrs";
    public static final String ICC_ANRST = "anrst";
    public static final String ICC_INDEX = "_id"; //for using index to operate such as update,delete,add(-1)

    public static final String ICC_TAG_NEW = "newname";
    public static final String ICC_NUMBER_NEW = "newnumber";
    public static final String ICC_EMAILS_NEW = "newemails";
    public static final String ICC_GROUPS_NEW = "newgroups";
    public static final String ICC_ANRS_NEW = "newanrs";
    public static final String ICC_ANRST_NEW = "newanrst";

    public static final String ICC_ADN_TOTAL = "total_adn";
    public static final String ICC_ADN_USED = "used_adn";
    public static final String ICC_ADN_LEN = "alpha_len_adn";
    public static final String ICC_ADN_DIGIT_LEN = "digit_len_adn";
    public static final String ICC_EMAIL_TOTAL = "total_email";
    public static final String ICC_EMAIL_USED = "used_email";
    public static final String ICC_EMAIL_LEN = "alpha_len_email";
    public static final String ICC_ANR_TOTAL = "total_anr";
    public static final String ICC_ANR_USED = "used_anr";
    public static final String ICC_ANR_LEN = "digit_len_anr";
    public static final String ICC_GROUP_TOTAL = "total_group";
    public static final String ICC_GROUP_USED = "used_group";
    public static final String ICC_GROUP_LEN = "alpha_len_group";
    private static final String ICC_PIN2 = "pin2";

    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] {
        ICC_TAG, ICC_NUMBER, ICC_EMAILS, ICC_ANRS, ICC_GROUPS, ICC_INDEX
        /*
         * "name", "number", "emails", "groups", "anrs", "_id"
         */
    };

    /* this is for "icc/adn/group" */
    private static final String[] ADDRESS_BOOK_GROUP_COLUMN_NAMES = new String[] {
        ICC_GROUPS, ICC_INDEX
        /*
         * "name", "_id"
         */
    };

    /* this is for "icc/adn/info" */
    private static final String[] ADDRESS_BOOK_INFO_COLUMN_NAMES = new String[] {
        ICC_ADN_TOTAL, ICC_ADN_USED, ICC_ADN_LEN,ICC_ADN_DIGIT_LEN,
        ICC_EMAIL_TOTAL, ICC_EMAIL_USED, ICC_EMAIL_LEN,
        ICC_ANR_TOTAL, ICC_ANR_USED, ICC_ANR_LEN,
        ICC_GROUP_TOTAL, ICC_GROUP_USED, ICC_GROUP_LEN
        /*
         * "total_adn", "used_adn", "alpha_len_adn", "digit_len_adn",
         * "total_email","used_email","alpha_len_email",
         * "total_anr", "used_anr", "digit_len_anr",
         * "total_group", "used_group", "alpha_len_group",
         */
    };
    private int[] m_Size=new int[]{0,0,0};

    /*
      *1. The query result of 2G SIM through CAPI PBK API will always return the value when boot
      *2. Need to manage 2G Caps after boot because 2G SIM update/delete doesn't go through CAPI phonebook interface .
    */
       /*"total_adn", "used_adn", "alpha_len_adn", "digit_len_adn"*/
       private int[][] m_2GCaps=new int[][]{{0,0,0,0},{0,0,0,0}};
       private int[][] m_2GBootCaps=new int[][]{{0,0,0,0},{0,0,0,0}}; /*keep the boot values, and can use this info to check if hot swap or other error cases*/

    /*
     * CAPI interface need using String lists as a parameter for group related
     * operation,but IccProvider use index as parameter in single string to know
     * about specific ADN want to add/update/remove specific group. So GasNames
     * maintain GAS name list in absolute order correspond to the order of SIM.
     * It is mainly for convert index to string ,or string to index
     */
    private List<String> GasNames = new ArrayList<String>();

    private static final int ADN = 1;
    private static final int FDN = 2;
    private static final int SDN = 3;
    private static final int GAS = 4;
    private static final int ADNINFO = 5;
    /* Information Type numbers */
    private static final int INFO_NUM = IccConstants.INFO_NUM;

    private static final String STR_TAG = "tag";
    private static final String STR_NUMBER = "number";
    private static final String STR_EMAILS = "emails";
    private static final String STR_PIN2 = "pin2";

    private static final String STR_SIM2 = "icc2";

    private static final UriMatcher URL_MATCHER =
                            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URL_MATCHER.addURI("icc", "adn/info", ADNINFO);
        URL_MATCHER.addURI("icc", "adn/group", GAS);
        URL_MATCHER.addURI("icc", "adn", ADN);
        URL_MATCHER.addURI("icc", "fdn", FDN);
        URL_MATCHER.addURI("icc", "sdn", SDN);
        URL_MATCHER.addURI(STR_SIM2, "adn/info", ADNINFO);
        URL_MATCHER.addURI(STR_SIM2, "adn/group", GAS);
        URL_MATCHER.addURI(STR_SIM2, "adn", ADN);
        URL_MATCHER.addURI(STR_SIM2, "fdn", FDN);
        URL_MATCHER.addURI(STR_SIM2, "sdn", SDN);
    }
    private SimCardID mActivePhoneId = SimCardID.ID_ZERO;


    @Override
    public boolean onCreate() {
        GasNames.clear();
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        setSimCardId(url);

        switch (URL_MATCHER.match(url)) {
            case ADN:
                return loadFromEf(IccConstants.EF_ADN);

            case FDN:
                return loadFromEf(IccConstants.EF_FDN);

            case SDN:
                return loadFromEf(IccConstants.EF_SDN);

            case GAS:
                return loadFromEf(IccConstants.EF_GAS);

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ADN:
            case FDN:
            case SDN:
            case GAS:
            case ADNINFO:
                return "vnd.android.cursor.dir/sim-contact";

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private String[] convertGroupIndexToNameArr(String indexStr)
    {
        String[] groupArray;
        int index;

        if(TextUtils.isEmpty(indexStr)) {
            Rlog.e(TAG, "convertGroupIndexToNameArr:  indexStr is Empty");
            return null;
        }

        if (DBG) log("convertGroupIndexToNameArr:insert groups format:" + indexStr);

       index=0;
       groupArray = indexStr.split(",");
       String[] groups = null;
        if (0 < groupArray.length ) {
            groups = new String[groupArray.length];

            for (String group : groupArray) {
                try {
                    if (DBG) log("convertGroupIndexToNameArr:insert groups index: " + group + " Group Names: "+ GasNames.get(Integer.valueOf(group)));
                    groups[index] =  GasNames.get(Integer.valueOf(group));
                    } catch (NumberFormatException e) {
                            Rlog.e(TAG, "convertGroupIndexToNameArr: cannot convert invalid format to number :" + group);
                            groups[index]=null;
                    }
                index++;
            }
        }
        return groups;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        Uri resultUri;
        int efType;
        int gasIndex = -1;
        String pin2 = null;

        if (DBG) log("insert");

        final SimCardID activePhone = setSimCardId(url);

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                pin2 = initialValues.getAsString("pin2");
                break;

            case GAS:
                efType = IccConstants.EF_GAS;

                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        String tag = initialValues.getAsString(ICC_TAG);
        if (null == tag) {
            tag = "";
        }
        String number = initialValues.getAsString(ICC_NUMBER);
        if (null == number) {
            number = "";
        }

        String[] emails = null;
        String ary = initialValues.getAsString(ICC_EMAILS);
        if (ary != null) {
            emails = new String[]{ary};
        }

        String[] groups = null;
        ary = initialValues.getAsString(ICC_GROUPS);
        if (ary != null) {
            if (match != GAS)
            {
                groups = convertGroupIndexToNameArr(ary);
            } else {
                groups = new String[] {
                    ary
                };
            }
        }
        String[] anrs = null;
        ary = initialValues.getAsString(ICC_ANRS);
        if (ary != null) {
            anrs = new String[]{ary};
        }

        // TODO(): Read email instead of sending null.
        boolean success = addIccRecordToEf(efType, tag, number, emails, pin2, anrs, groups);

        if (!success) {
            return null;
        }

        StringBuilder buf;

        if (activePhone == SimCardID.ID_ONE) {
            buf = new StringBuilder("content://icc2/");
        } else {
            buf = new StringBuilder("content://icc/");
        }

        switch (match) {
            case ADN:
                buf.append("adn/");
                break;

            case FDN:
                buf.append("fdn/");
                break;

            case GAS:
                buf.append("adn/group/");
                for (gasIndex = 0; gasIndex< GasNames.size(); gasIndex++)
                {
                    /* find first empty entry to add */
                    if (GasNames.get(gasIndex).isEmpty())
                    {
                        GasNames.set(gasIndex, groups[0]);
                        if (DBG)
                            log("Adding group to GasNames.(" + gasIndex + ")=" + GasNames.get(gasIndex));
                        break;
                    }
                }
                break;
        }

        if (match != GAS)
        {
             // TODO: we need to find out the rowId for the newly added record
             buf.append(0);
             if (IsUSIM(efType) == false) {
                if (DBG) log("insert: 2G SIM");
                //Used number + 1
                m_2GCaps[getSimCardId(efType).toInt()][1]=m_2GCaps[getSimCardId(efType).toInt()][1]+1;
             }
        }else{
             buf.append(gasIndex);
        }

        resultUri = Uri.parse(buf.toString());

        /*
        // notify interested parties that an insertion happened
        getContext().getContentResolver().notifyInsert(
                resultUri, rowID, null);
        */

        return resultUri;
    }

    private String normalizeValue(String inVal) {
        if (null == inVal) {
            return "";
        }
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

        if (DBG) log("delete");
        setSimCardId(url);

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                break;

            case GAS:
                efType = IccConstants.EF_GAS;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        // parse where clause
        String tag = null;
        String number = null;
        String[] emails = null;
        String[] groups = null;
        String pin2 = null;
        String[] anrs = null;

        String[] tokens = where.split("AND");
        int n = tokens.length;
        int recordNumber = -1;

        while (--n >= 0) {
            String param = tokens[n];
            if (DBG) log("parsing '" + param + "'");

            String[] pair = param.split("=", 2);

            String key = pair[0].trim();
            String val = pair[1].trim();

            if (ICC_TAG.equals(key)) {
                tag = normalizeValue(val);
            } else if (ICC_NUMBER.equals(key)) {
                number = normalizeValue(val);
            } else if (ICC_EMAILS.equals(key)) {
                String ary = normalizeValue(val);
                if (!TextUtils.isEmpty(ary)) {
                    log("delete(): email = " + ary);
                    emails = new String[] {
                        ary
                    };
                }
            } else if (ICC_GROUPS.equals(key)) {
                String ary = normalizeValue(val);
                if (!TextUtils.isEmpty(ary)) {
                    if (match != GAS)
                    {
                        groups = convertGroupIndexToNameArr(ary);
                    } else {
                        groups = new String[] {
                            ary
                        };
                    }
                    log("delete(): group = " + ary);
                }
            } else if (ICC_ANRS.equals(key)) {
                String ary = normalizeValue(val);
                if (!TextUtils.isEmpty(ary)) {
                    log("delete(): anr = " + ary);
                    anrs = new String[]{ary};
                }
            } else if (ICC_PIN2.equals(key)) {
                pin2 = normalizeValue(val);
            } else if (ICC_INDEX.equals(key)) {
                String strIdx= normalizeValue(val);
                try {
                    recordNumber = Integer.parseInt(strIdx);
                } catch(Exception e) {
                    recordNumber = -1;
                }
            }
        }

        if (null == tag) {
            tag = "";
        }

        if (null == number) {
            number = "";
        }

        boolean success = false;
        if (mUseIdx&&recordNumber != -1) {
            if (DBG) log("delete by index " + recordNumber);
            success = deleteIccRecordFromEfByIdx(efType, pin2, recordNumber);
        } else {
            if (efType == FDN && TextUtils.isEmpty(pin2)) {
                return 0;
            }
            success = deleteIccRecordFromEf(efType, tag, number, emails, pin2, anrs, groups);
        }

        if (!success) {
            return 0;
        }

         if (IsUSIM(efType) == false) {
            if (DBG) log("insert: 2G SIM");
            // Used number - 1
            m_2GCaps[getSimCardId(efType).toInt()][1] = m_2GCaps[getSimCardId(efType).toInt()][1] - 1;
         }

        return 1;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int efType;
        String pin2 = null;

        if (DBG) log("update");
        setSimCardId(url);

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                pin2 = values.getAsString("pin2");
                break;
            case GAS:
                efType = IccConstants.EF_GAS;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        String tag = values.getAsString(ICC_TAG);
        if (null == tag) {
            tag = "";
        }
         String number = values.getAsString(ICC_NUMBER);
        if (null == number) {
            number = "";
        }
        String ary = values.getAsString(ICC_EMAILS);
        String[] emails = null;
        if (ary != null)
            emails = new String[]{ary};

        ary = values.getAsString(ICC_GROUPS);
        String[] groups = null;
        if (ary != null)
        {
            if (match != GAS)
            {
                groups = convertGroupIndexToNameArr(ary);
            } else {
                groups = new String[] {
                    ary
                };
            }
        }
        String[] anrs = null;
        ary = values.getAsString(ICC_ANRS);
        if (ary != null)
            anrs=new String[]{ary};

        String newTag = values.getAsString(ICC_TAG_NEW);
        if (null == newTag) {
            newTag = "";
        }
         String newNumber = values.getAsString(ICC_NUMBER_NEW);
        if (null == newNumber) {
            newNumber = "";
        }
        String[] newEmails = null;
        ary = values.getAsString(ICC_EMAILS_NEW);
        if (ary != null)
            newEmails=new String[]{ary};

        String[] newGroups = null;

        ary = values.getAsString(ICC_GROUPS_NEW);
        if (ary != null)
        {
            if (match != GAS)
            {
                newGroups = convertGroupIndexToNameArr(ary);
            } else {
                newGroups = new String[] {
                    ary
                };
            }
        }
        String[] newAnrs = null;
        ary = values.getAsString(ICC_ANRS_NEW);
        if (ary != null)
            newAnrs=new String[]{ary};

        Integer nm = values.getAsInteger(ICC_INDEX);
        int recordNumber = -1;
        if (nm != null) {
            recordNumber = nm;
        }

        // TODO(): Update for email.
        boolean success = false;
        if (mUseIdx&&recordNumber != -1) {
            success = updateIccRecordInEfByIdx(efType, newTag, newNumber, newEmails, newAnrs,
                    newGroups, pin2, recordNumber);
        } else {
            success = updateIccRecordInEf(efType, tag, number,
                    newTag, newNumber, pin2, emails, newEmails, anrs, newAnrs, groups, newGroups);
        }

        if (!success) {
            return 0;
        }

        if (match == GAS)
        {
            // update GasNames ArrayList value
            for (int i = 0; i < GasNames.size(); i++)
            {
                if (groups[0].equals(GasNames.get(i)))
                {
                    if (DBG)
                        log("Update group:From GasNames.get(" + i + ")" + GasNames.get(i));
                    GasNames.set(i, newGroups[0]);
                    if (DBG)
                        log("Update group:To GasNames.get(" + i + ")" + GasNames.get(i));
                    break;
                }
            }
        }
        return 1;
    }

    private MatrixCursor loadFromEf(int efType) {
        if (DBG) log("loadFromEf: efType=" + efType);

        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
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
            if ((efType == IccConstants.EF_ADN) || (efType == IccConstants.EF_GAS))
                m_Size[0]=N;
            if(efType==IccConstants.EF_FDN)
                m_Size[1]=N;
            if(efType==IccConstants.EF_SDN)
                m_Size[2]=N;

            MatrixCursor cursor;
            if (efType != IccConstants.EF_GAS)
            {
                cursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, N);
                if (DBG)
                    log("adnRecords.size=" + N);

                for (int i = 0; i < N; i++) {
                    loadAdnRecord(adnRecords.get(i), cursor, i);
                }
            } else {
                if (DBG)
                    log("group size=" + N);

                if (N == 0)
                {
                    return null;
                }

                cursor = new MatrixCursor(ADDRESS_BOOK_GROUP_COLUMN_NAMES, N);

                for (int i = 0; i < N; i++) {
                    loadGasRecord(adnRecords.get(i), cursor, i);
                }
            }
            return cursor;
        } else {
            // No results to load
            Rlog.w(TAG, "Cannot load ADN records");
            return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
        }
    }

    private boolean
        addIccRecordToEf(int efType, String name, String number, String[] emails, String pin2,
                         String[] anrs, String[] groups) {
        if (DBG) Rlog.d(TAG,"=>addIccRecordToEf(): efType=" + efType + ", name=" + name +
                    ", number=" + number + ", emails=" + emails + ",groups=" + groups);

        boolean success = false;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType, "", "",
                        name, number, pin2, null, emails, null, anrs, null, groups);
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
            String newName, String newNumber, String pin2,
            String[] oldEmails, String[] newEmails, String[] oldAnrs, String[] newAnrs,
            String[] oldGroups, String[] newGroups) {
        if (DBG) log("updateIccRecordInEf: efType=" + efType +
                    ", oldname=" + oldName + ", oldnumber=" + oldNumber + ", oldEmails="
                    + oldEmails + ", oldAnrs=" + oldAnrs + ", oldGroups=" + oldGroups +
                    ", newname=" + newName + ", newnumber=" + newNumber + ", newEmails="
                    + newEmails + ", newAnrs=" + newAnrs + ", newGroups=" + newGroups);
        boolean success = false;

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType,
                        oldName, oldNumber, newName, newNumber, pin2,
                        oldEmails, newEmails, oldAnrs, newAnrs, oldGroups, newGroups);
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
            String pin2, String[] anrs, String[] groups) {
        if (DBG) log("deleteIccRecordFromEf: efType=" + efType +
                    ", name=" + name + ", number=" + number + ", emails=" + emails + ", pin2="
                    + pin2 + ", groups=" + groups);

        boolean success = false;

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch(efType,
                        name, number, "", "", pin2, emails, null, anrs, null, groups, null);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("deleteIccRecordFromEf: " + success);
        return success;
    }

    private boolean
            addIccRecordToEfByIdx(int efType, String name, String number, String[] emails,
                    String pin2, String[] anrs, String[] groups) {
        if (DBG) log("addIccRecordToEf: efType=" + efType + ", name=" + name +
                    ", number=" + number + ", emails=" + emails + ", groups=" + groups);

        boolean success = false;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfByIndex(efType,
                        name, number, -1,pin2,
                        emails, anrs, groups);
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
    updateIccRecordInEfByIdx(int efType, String newName, String newNumber,
                    String[] newEmails, String[] newAnrs, String[] newGroups, String pin2, int index) {

        if (DBG) log("updateIccRecordInEfByIdx: efType=" + efType +
                    "newname=" + newName + ", newnumber=" + newNumber + ", newGroups=" + newGroups);

        boolean success = false;

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfByIndex(efType,
                        newName, newNumber, index,pin2,
                        newEmails, newAnrs, newGroups);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("updateIccRecordInEfByIdx: " + success);
        return success;
    }

    private boolean deleteIccRecordFromEfByIdx(int efType, String pin2, int index) {

        boolean success = false;

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfByIndex(efType,
                        "", "", index,pin2,
                        null, null, null);
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
    private void loadAdnRecord(AdnRecord record, MatrixCursor cursor, int id) {
        if (!record.isEmpty()) {
            Object[] contact = new Object[6];
            String alphaTag = record.getAlphaTag();
            String number = record.getNumber();

            if (DBG) log("loadRecord: " + alphaTag + ", " + number + ","+ Integer.toHexString(record.getEfid()) + " : " + (id+1));
            contact[0] = alphaTag;
            contact[1] = number;

            String[] emails = record.getEmails();
            String[] anrs = record.getAnrs();
            String[] groups = record.getGroups();
            if (emails != null) {
                StringBuilder emailString = new StringBuilder();
                for (String email: emails) {
                    if (DBG) log("Adding email:" + email);
                    emailString.append(email);
                    emailString.append(",");
                }
                contact[2] = emailString.toString();
            }

            //<3g pb>
            StringBuilder anrString = new StringBuilder();
            if (anrs != null) {
                for (String anr: anrs) {
                    anrString.append(anr);
                    anrString.append(",");
                }
                if (DBG) log("Adding anr:" + anrString);
                contact[3] = anrString.toString();
            } else {
                contact[3] = "";
            }
            //<end>

            if (groups != null) {
                StringBuilder groupString = new StringBuilder();
                for (String group : groups) {
                    if (DBG)
                        log("Adding group:" + group);

                    for (int i = 0; i < GasNames.size(); i++)
                    {
                        if (DBG)
                            log("Adding group:GasNames.get(" + i + ")" + GasNames.get(i));
                        if (group.equals(GasNames.get(i)))
                        {
                            groupString.append(Integer.toString(i));
                            groupString.append(",");
                            break;
                        }
                    }
                }
                contact[4] = groupString.toString();
                if (DBG)
                    log("Adding group index array:" + contact[4]);
            }
            contact[5] = Integer.toString(id + 1);
            cursor.addRow(contact);
        }
    }

    /**
     * Loads an GAS related AdnRecord into a MatrixCursor. Must be called with
     * mLock held.
     *
     * @param record the GAS record to load from
     * @param cursor the cursor to receive the results
     */
    private void loadGasRecord(AdnRecord record, MatrixCursor cursor, int id) {
        if (!record.isEmpty()) {
            Object[] contact = new Object[2];

            if (DBG)
                log("loadGasRecord: " + id + " group name" +   record.getGroups());

            String[] groups = record.getGroups();


            if (groups != null) {
                contact[0] = groups[0];
                GasNames.add(id, groups[0]);
            } else {
                log("Error: groups == null ");
                return;
            }

            if (groups[0].length() == 0)
            {
                return;
            }

            contact[1] = Integer.toString(id);
            cursor.addRow(contact);
        }
    }

    private SimCardID getSimCardId(int efType)
    {
        SimCardID activePhone = SimCardID.ID_ZERO;

        if (IccConstants.EF_FDN == efType || IccConstants.EF_ADN == efType ||
            IccConstants.EF_GAS == efType || IccConstants.EF_INFO == efType)
        {
            activePhone = mActivePhoneId;
        }
        else
        {
            log("!!!Warning -----Unknown efType = " + efType + "----------");
        }

        if (DBG) log("--------activePhone = " + activePhone + "----------");
        return activePhone;
    }

    private SimCardID setSimCardId(Uri url)
    {
        SimCardID activePhone;
        if (DBG) Rlog.d(TAG, "--------url.getAuthority()=" + url.getAuthority()+ "-------");
        if (STR_SIM2.equals(url.getAuthority()))
        {
            activePhone = SimCardID.ID_ONE;
        }
        else
        {
            activePhone = SimCardID.ID_ZERO;
        }

        mActivePhoneId = activePhone;
        return activePhone;
    }

    private boolean IsUSIM(int efType) {
        boolean isUSIM = false;

        try {
            IIccPhoneBook iccIpb;
            if (SimCardID.ID_ONE == getSimCardId(efType))
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook2"));
            }
            else
            {
                iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
            }

            if (iccIpb != null) {
                isUSIM = iccIpb.IsUSIM();
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("isUSIM: " + isUSIM);
        return isUSIM;
    }

    private void log(String msg) {
        Rlog.d(TAG, "[IccProvider] " + msg);
    }

}
