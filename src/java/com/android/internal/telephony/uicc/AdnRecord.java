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

package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.telephony.Rlog;

import com.android.internal.telephony.GsmAlphabet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import com.android.internal.util.HexDump;

/**
 *
 * Used to load or store ADNs (Abbreviated Dialing Numbers).
 *
 * {@hide}
 *
 */
public class AdnRecord implements Parcelable {
    static final String LOG_TAG = "AdnRecord";
    static final boolean DBG = false;

    public class Anr {
        String aasTag = "";
        String number = "";
        int aasExt = 0x00;//0x00 means no aas tag
        int efid;

        public Anr (byte[] record) {
                this(0, record);
        }

        public Anr (int efid, byte[] record) {
                this.efid = efid;
                parseRecord(record);
        }

        public Anr(int efid, String anrNumber) {
            this.efid = efid;
            number = anrNumber;
        }

        private void parseRecord(byte[] record) {
             try {
                 aasExt = 0xff & record[0];
                 final int footerOffset = 1;
                 final int numberLength = 0xff & record[footerOffset];

                 if (numberLength > MAX_NUMBER_SIZE_BYTES) {
                     // Invalid number length
                     number = "";
                     return;
                 }

                 // Please note 51.011 10.5.1:
                 //
                 // "If the Dialling Number/SSC String does not contain
                 // a dialling number, e.g. a control string deactivating
                 // a service, the TON/NPI byte shall be set to 'FF' by
                 // the ME (see note 2)."

                number = PhoneNumberUtils.calledPartyBCDToString(
                            record, footerOffset + 1, numberLength);
                if (DBG) Rlog.d("AdnRecord", "number = " + number + ":" + Integer.toHexString(efid));
                if (DBG) Rlog.d("AdnRecord", "numberhex = " + HexDump.dumpHexString(record));

            } catch (RuntimeException ex) {
                Rlog.e(LOG_TAG, "Error parsing AnrRecord", ex);
                number = "";
            }
        }
    }
    //<end>


    //***** Instance Variables

    String mAlphaTag = null;
    String mNumber = null;
    String[] mEmails;
    int mExtRecord = 0xff;
    int mEfid;                   // or 0 if none
    int mRecordNumber;           // or 0 if none
    String[] groups;
    byte mSfi = (byte) 0xFF;

    HashMap<Integer, Anr> anrs;

    //***** Constants

    // In an ADN record, everything but the alpha identifier
    // is in a footer that's 14 bytes
    static final int FOOTER_SIZE_BYTES = 14;

    // Maximum size of the un-extended number field
    static final int MAX_NUMBER_SIZE_BYTES = 11;

    static final int EXT_RECORD_LENGTH_BYTES = 13;
    static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    static final int EXT_RECORD_TYPE_MASK = 3;
    static final int MAX_EXT_CALLED_PARTY_LENGTH = 0xa;

    // ADN offset
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_TON_AND_NPI = 1;
    static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_DIALING_NUMBER_END = 11;
    static final int ADN_CAPABILITY_ID = 12;
    static final int ADN_EXTENSION_ID = 13;

    //***** Static Methods

    public static final Parcelable.Creator<AdnRecord> CREATOR
            = new Parcelable.Creator<AdnRecord>() {
        @Override
        public AdnRecord createFromParcel(Parcel source) {
            int efid;
            int recordNumber;
            String alphaTag;
            String number;
            String[] emails;
            String[] groups;

            efid = source.readInt();
            recordNumber = source.readInt();
            alphaTag = source.readString();
            number = source.readString();
            emails = source.readStringArray();
            groups = source.readStringArray();

            return new AdnRecord(efid, recordNumber, alphaTag, number, emails, groups);
        }

        @Override
        public AdnRecord[] newArray(int size) {
            return new AdnRecord[size];
        }
    };


    //***** Constructor
    public AdnRecord (byte[] record) {
        this(0, 0, record);
    }

    public AdnRecord (int efid, int recordNumber, byte[] record) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        parseRecord(record);
    }

    public AdnRecord (String alphaTag, String number) {
        this(0, 0, alphaTag, number);
    }

    public AdnRecord(String alphaTag, String number, String[] emails, String[] groups) {
        this(0, 0, alphaTag, number, emails, groups);
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails,
        String[] groups) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.groups = groups;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = null;
        this.groups = null;
    }

    //***** Instance Methods

    public String getAlphaTag() {
        return mAlphaTag;
    }

    public String getNumber() {
        return mNumber;
    }

    public String[] getAnrs() {
        if (anrs == null ||anrs.size() == 0) return null;
        String[] anrStrings = new String[anrs.size()];
        Iterator it = anrs.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();
            anrStrings[i] = ((Anr)value).number;
            i++;
        }
        return anrStrings;
    }

    public int getNumberOfAnrs() {
        if (null == anrs)
            return 0;
        return anrs.size();
    }

    public int getRecordNumber() {
        return mRecordNumber;
    }

    public void setRecordNumber(int recordNumber) {
        this.mRecordNumber = recordNumber;
    }

    public int getEfid() {
        return mEfid;
    }

    public void setEfid(int new_efid) {
        this.mEfid = new_efid;
    }

    public void setAnr(int efid, byte[] anr) {
        if (this.anrs == null) this.anrs = new HashMap<Integer, Anr>();
        this.anrs.put(efid, new Anr(efid, anr));
    }

    public void setAnr(int efid, String anrNumber) {
        if (this.anrs == null)
            this.anrs = new HashMap<Integer, Anr>();
        this.anrs.put(efid, new Anr(efid, anrNumber));
    }

    byte[] buildAnrString(int anrIndex, int recordSize) {
        String anrNumberString = null;

        try {
            String[] strAnrs = getAnrs();
            if ((null != strAnrs) && (strAnrs.length > anrIndex)
                    && (!TextUtils.isEmpty(strAnrs[anrIndex]))) {
                anrNumberString = strAnrs[anrIndex];
            }
        } catch (RuntimeException ex) {
            Rlog.e("AdnRecord", "buildAnrString(): RuntimeException: message = " + ex.getMessage()
                    + ", ex = " + ex.toString());
        }

        byte[] retDataRecord = new byte[recordSize];
        Arrays.fill(retDataRecord, (byte) 0xFF);

        if (TextUtils.isEmpty(anrNumberString))
            return retDataRecord;

        byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(anrNumberString);
        System.arraycopy(bcdNumber, 0, retDataRecord, 2, bcdNumber.length);

        if (bcdNumber.length > 11) {
            Rlog.e("AdnRecord", "anr string length overflow");
        }

        retDataRecord[0] = (byte) 0x01; // aas;
        retDataRecord[1] = (byte) (bcdNumber.length); // length
        retDataRecord[13] = (byte) 0xFF; // capacility id
        retDataRecord[14] = (byte) 0xFF; // extension Record Id

        return retDataRecord;
    }
    //<end>

    public String[] getEmails() {
        return mEmails;
    }

    public int getNumberOfEmails() {
        if (null == mEmails)
            return 0;
        return mEmails.length;
    }

    public void setEmails(String[] emails) {
        this.mEmails = emails;
    }

    public void setSFI(int sfi) {
        this.mSfi = (byte) sfi;
    }

    public byte[] buildEmailString(int recordSize) {
        byte[] retDataRecord = null;

        retDataRecord = new byte[recordSize];
        Arrays.fill(retDataRecord, (byte) 0xFF);

        if ((null == mEmails) || (0 == mEmails.length) || (TextUtils.isEmpty(mEmails[0]))) {
            return retDataRecord;
        }

        try {
            final byte[] emailAddress = GsmAlphabet.stringToGsm8BitPacked(mEmails[0]);
            System.arraycopy(emailAddress, 0, retDataRecord, 0, emailAddress.length);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "buildEmailString(): Exception: message = " + e.getMessage() + ", e = "
                    + e.toString());
        }

        if (DBG) Rlog.d("AdnRecord", "buildEmailString(): mSfi = " + mSfi + ", this.mRecordNumber = " + this.mRecordNumber);

        retDataRecord[recordSize - 2] = mSfi;
        retDataRecord[recordSize - 1] = (byte) (this.mRecordNumber);

        return retDataRecord;
    }

    public String[] getGroups() {
        return groups;
    }

    public int getNumberOfGroups() {
        if (null == groups)
            return 0;
        return groups.length;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public byte[] buildGroupString(int recordSize) {
        byte[] retDataRecord = null;

        retDataRecord = new byte[recordSize];
        Arrays.fill(retDataRecord, (byte) 0xFF);

        if ((null == groups) || (0 == groups.length) || (TextUtils.isEmpty(groups[0]))) {
            return retDataRecord;
        }

        try {
            final byte[] groupsName = GsmAlphabet.stringToGsm8BitPacked(groups[0]);
            System.arraycopy(groupsName, 0, retDataRecord, 0, groupsName.length);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "buildGroupString(): Exception: message = " + e.getMessage() + ", e = "
                    + e.toString());
        }

        return retDataRecord;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AdnRecord: ");
        sb.append(mRecordNumber);
        sb.append(" '");
        sb.append(mAlphaTag);
        sb.append("' '");
        sb.append(mNumber);
        sb.append("'");
        if (null != mEmails) {
            sb.append(" '");
            for (int i=0;i<mEmails.length;i++) {
                if (i!=0)
                    sb.append(", ");
                sb.append(mEmails[i]);
            }
            sb.append("'");
        }

        String[] anrs = getAnrs();
        if (null != anrs) {
            sb.append(" '");
            for (int i=0;i<anrs.length;i++) {
                if (i!=0)
                    sb.append(", ");
                sb.append(anrs[i]);
            }
            sb.append("'");
        }

        if (null != groups) {
            sb.append(" '");
            for (int i = 0; i < groups.length; i++) {
                if (i != 0)
                    sb.append(", ");
                sb.append(groups[i]);
            }
            sb.append("'");
        }
        return  sb.toString();
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(mAlphaTag) && TextUtils.isEmpty(mNumber) && (mEmails == null)
                && (this.getAnrs() == null) && (groups == null);
    }

    public boolean hasExtendedRecord() {
        return mExtRecord != 0 && mExtRecord != 0xff;
    }

    /** Helper function for {@link #isEqual}. */
    private static boolean stringCompareNullEqualsEmpty(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }
        return (s1.equals(s2));
    }


    public boolean isEqual(AdnRecord adn) {
        if (!isNameAndNumberEqual(adn)) {
             return false;
        }
         return (Arrays.equals(mEmails, adn.mEmails) &&
                Arrays.equals(this.getAnrs(), adn.getAnrs()) &&
                Arrays.equals(groups, adn.groups));
    }

    public boolean isNameAndNumberEqual(AdnRecord adn) {
        return (stringCompareNullEqualsEmpty(mAlphaTag, adn.mAlphaTag) &&
                stringCompareNullEqualsEmpty(mNumber, adn.mNumber));
    }

    public boolean isGASEqual(AdnRecord gas) {
        if (gas == null)
        {
            Rlog.e("AdnRecord", "gas==null ");
            return false;
        }

        return (stringCompareNullEqualsEmpty(groups[0], gas.groups[0]));
    }
    //***** Parcelable Implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mEfid);
        dest.writeInt(mRecordNumber);
        dest.writeString(mAlphaTag);
        dest.writeString(mNumber);
        dest.writeStringArray(mEmails);
        dest.writeStringArray(groups);
    }

    /**
     * Build adn hex byte array based on record size
     * The format of byte array is defined in 51.011 10.5.1
     *
     * @param recordSize is the size X of EF record
     * @return hex byte[recordSize] to be written to EF record
     *          return null for wrong format of dialing number or tag
     */
    public byte[] buildAdnString(int recordSize) {
        byte[] bcdNumber;
        byte[] byteTag;
        byte[] adnString;
        int footerOffset = recordSize - FOOTER_SIZE_BYTES;

        // create an empty record
        adnString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            adnString[i] = (byte) 0xFF;
        }

        if (TextUtils.isEmpty(mNumber)) {
            Rlog.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            return adnString;   // return the empty record (for delete)
        } else if (mNumber.length()
                > (ADN_DIALING_NUMBER_END - ADN_DIALING_NUMBER_START + 1) * 2) {
            Rlog.w(LOG_TAG,
                    "[buildAdnString] Max length of dialing number is 20");
            return null;
        } else if (mAlphaTag != null && mAlphaTag.length() > footerOffset) {
            Rlog.w(LOG_TAG,
                    "[buildAdnString] Max length of tag is " + footerOffset);
            return null;
        } else {
            bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(mNumber);

            System.arraycopy(bcdNumber, 0, adnString,
                    footerOffset + ADN_TON_AND_NPI, bcdNumber.length);

            adnString[footerOffset + ADN_BCD_NUMBER_LENGTH]
                    = (byte) (bcdNumber.length);
            adnString[footerOffset + ADN_CAPABILITY_ID]
                    = (byte) 0xFF; // Capability Id
            adnString[footerOffset + ADN_EXTENSION_ID]
                    = (byte) 0xFF; // Extension Record Id

            if (!TextUtils.isEmpty(mAlphaTag)) {
                //for fix unicode encoding error issue
                if (isUnicodeCoding(mAlphaTag)) {
                    try{
                        int outByteIndex;
                        byte[] tempByte;
                        byteTag = new byte[FOOTER_SIZE_BYTES];
                        tempByte = mAlphaTag.getBytes("UTF-16BE");
                        byteTag[0] = (byte)0x80;
                        System.arraycopy(tempByte, 0, byteTag, 1, tempByte.length);
                        outByteIndex = tempByte.length + 1;
                        // pad with 0xff's
                        while(outByteIndex < FOOTER_SIZE_BYTES) {
                            byteTag[outByteIndex++] = (byte)0xff;
                        }
                    }catch (UnsupportedEncodingException e) {
                        byteTag = new byte[0];
                    }
                }else{
                    byteTag = GsmAlphabet.stringToGsm8BitPacked(mAlphaTag);
                }
                System.arraycopy(byteTag, 0, adnString, 0, byteTag.length);
            }

            return adnString;
        }
    }

    /**
     * See TS 51.011 10.5.10
     */
    public void
    appendExtRecord (byte[] extRecord) {
        try {
            if (extRecord.length != EXT_RECORD_LENGTH_BYTES) {
                return;
            }

            if ((extRecord[0] & EXT_RECORD_TYPE_MASK)
                    != EXT_RECORD_TYPE_ADDITIONAL_DATA) {
                return;
            }

            if ((0xff & extRecord[1]) > MAX_EXT_CALLED_PARTY_LENGTH) {
                // invalid or empty record
                return;
            }

            mNumber += PhoneNumberUtils.calledPartyBCDFragmentToString(
                                        extRecord, 2, 0xff & extRecord[1]);

            // We don't support ext record chaining.

        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord ext record", ex);
        }
    }

    //***** Private Methods

    /**
     * alphaTag and number are set to null on invalid format
     */
    private void
    parseRecord(byte[] record) {
        try {
            mAlphaTag = IccUtils.adnStringFieldToString(
                            record, 0, record.length - FOOTER_SIZE_BYTES);

            int footerOffset = record.length - FOOTER_SIZE_BYTES;

            int numberLength = 0xff & record[footerOffset];

            if (numberLength > MAX_NUMBER_SIZE_BYTES) {
                // Invalid number length
                mNumber = "";
                return;
            }

            // Please note 51.011 10.5.1:
            //
            // "If the Dialling Number/SSC String does not contain
            // a dialling number, e.g. a control string deactivating
            // a service, the TON/NPI byte shall be set to 'FF' by
            // the ME (see note 2)."

            mNumber = PhoneNumberUtils.calledPartyBCDToString(
                            record, footerOffset + 1, numberLength);


            mExtRecord = 0xff & record[record.length - 1];

            mEmails = null;
            groups = null;

        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord", ex);
            mNumber = "";
            mAlphaTag = "";
            mEmails = null;
            groups = null;
        }
    }

    /**
      * @param name is adn name
      * @return true if name include at least one unicode and need encode as unicode
      */
     public static boolean isUnicodeCoding(String name) {
        boolean isUcs2Coding = false;
        for (int i =0; i<name.length(); i++){
            final int d = name.codePointAt(i);
            if(d >= 0xff || d < 0){
                isUcs2Coding = true;
                break;
            }
        }

        return isUcs2Coding;
    }
}
