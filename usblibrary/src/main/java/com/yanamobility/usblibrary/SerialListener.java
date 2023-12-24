package com.yanamobility.usblibrary;

import java.util.ArrayDeque;

public interface SerialListener {
    void onSerialConnect      ();
    void onSerialConnectError (Exception e);
    void onSerialRead         (byte[] data);                // socket -> service
    void onSerialRead         (ArrayDeque<byte[]> datas);   // service -> UI thread
    void onSerialIoError      (Exception e);
    void Vci_RX_Callback      (byte paramId, byte paramData);

}
