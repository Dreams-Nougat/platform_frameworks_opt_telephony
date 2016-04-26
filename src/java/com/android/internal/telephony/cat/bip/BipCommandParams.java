/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2016. All rights reserved.
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

package com.android.internal.telephony.cat;

/**
 * Container class for proactive command parameters.
 *
 * {@hide}
 */
class BipCommandParams {

}

/**
 * Class for Open Channel Parameters.
 *
 * {@hide}
 */
class OpenChannelParams extends CommandParams {
    public BearerDesc bearerDesc = null;
    public int bufferSize = 0;
    public OtherAddress localAddress = null;
    public TransportProtocol transportProtocol = null;
    public OtherAddress dataDestinationAddress = null;
    public TextMessage mTextMsg = null;

    public GprsParams gprsParams = null;

    OpenChannelParams(CommandDetails cmdDet,
            BearerDesc bearerDesc, int size, OtherAddress localAddress,
            TransportProtocol transportProtocol, OtherAddress address,
            String apn, String login, String pwd, TextMessage textMsg) {
        super(cmdDet);
        this.bearerDesc = bearerDesc;
        this.bufferSize = size;
        this.localAddress = localAddress;
        this.transportProtocol = transportProtocol;
        this.dataDestinationAddress = address;
        this.mTextMsg = textMsg;
        this.gprsParams = new GprsParams(apn, login, pwd);
    }

    /**
     * Class for Gprs Parameters.
     */
    public class GprsParams {
        public String accessPointName = null;
        public String userLogin = null;
        public String userPwd = null;

        GprsParams(String apn, String login, String pwd) {
            this.accessPointName = apn;
            this.userLogin = login;
            this.userPwd = pwd;
        }
    }
}

/**
 * Class for Close Channel Parameters.
 *
 * {@hide}
 */
class CloseChannelParams extends CommandParams {
    TextMessage mTextMsg = new TextMessage();
    int mCloseCid = 0;
    boolean mBackToTcpListen = false;

    CloseChannelParams(CommandDetails cmdDet, int cid, TextMessage textMsg,
            boolean backToTcpListen) {
        super(cmdDet);
        this.mTextMsg = textMsg;
        mCloseCid = cid;
        mBackToTcpListen = backToTcpListen;
    }
}

/**
 * Class for Receive Data Parameters.
 *
 * {@hide}
 */
class ReceiveDataParams extends CommandParams {
    int mChannelDataLength = 0;
    TextMessage mTextMsg = new TextMessage();
    int mReceiveDataCid = 0;

    ReceiveDataParams(CommandDetails cmdDet, int length, int cid, TextMessage textMsg) {
        super(cmdDet);
        this.mChannelDataLength = length;
        this.mTextMsg = textMsg;
        this.mReceiveDataCid = cid;
    }
}

/**
 * Class for Send Data Parameters.
 *
 * {@hide}
 */
class SendDataParams extends CommandParams {
    byte[] mChannelData = null;
    TextMessage mTextMsg = new TextMessage();
    int mSendDataCid = 0;
    int mSendMode = 0;

    SendDataParams(CommandDetails cmdDet, byte[] data, int cid, TextMessage textMsg, int sendMode) {
        super(cmdDet);
        this.mChannelData = data;
        this.mTextMsg = textMsg;
        mSendDataCid = cid;
        mSendMode =  sendMode;
    }
}

/**
 * Class for Get Channel Status Parameters.
 *
 * {@hide}
 */
class GetChannelStatusParams extends CommandParams {
    TextMessage mTextMsg = new TextMessage();

    GetChannelStatusParams(CommandDetails cmdDet, TextMessage textMsg) {
        super(cmdDet);
        this.mTextMsg = textMsg;
    }
}
