package com.yanamobility.usblibrary;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.yanamobility.usblibrary.Utils.DataConversion;
import com.yanamobility.usblibrary.Utils.GlobalVariables;
import com.yanamobility.usblibrary.Utils.TX;
import com.yanamobility.usblibrary.Utils.Tbus;
import com.yanamobility.usblibrary.Utils.TextUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class VCIComm implements SerialListener  {

    private SerialSocket serialSocket;
    private SerialListener listener;
    private boolean connected;

    private enum QueueType {Connect, ConnectError, Read, IoError, VCIRX}

    private static class QueueItem {
        QueueType type;
        ArrayDeque<byte[]> datas;

        byte paramId;
        byte paramData;

        Exception e;

        QueueItem(QueueType type) { this.type=type; if(type==QueueType.Read) init(); }
        QueueItem(QueueType type, Exception e) { this.type=type; this.e=e; }
        QueueItem(QueueType type, ArrayDeque<byte[]> datas) { this.type=type; this.datas=datas; }
        QueueItem(QueueType type, byte paramId,byte paramData) { this.type=type; this.paramId=paramId;this.paramData=paramData; }

        void init() { datas = new ArrayDeque<>(); }
        void add(byte[] data) { datas.add(data); }
    }
    private final ArrayDeque<QueueItem> queue1, queue2;
    private final QueueItem lastRead;
    private final Handler mainLooper;
    Context mContext;


    public VCIComm(Context context, SerialListener serialListener) {
        mContext = context;
        listener = serialListener;
        mainLooper = new Handler(Looper.getMainLooper());
        queue1 = new ArrayDeque<>();
        queue2 = new ArrayDeque<>();
        lastRead = new QueueItem(QueueType.Read);

    }

    /**
     * Api
     */
    public void connect(SerialSocket socket) throws IOException {
        socket.connect(this);
        this.serialSocket = socket;
        connected = true;
    }

    public void disconnect() {
        connected = false; // ignore data,errors while disconnecting
        if(serialSocket != null) {
            serialSocket.disconnect();
            serialSocket = null;
        }
    }

    public void write(byte[] data) throws IOException {
        if(!connected)
            throw new IOException("not connected");
        serialSocket.write(data);
    }

    public void attach(SerialListener listener) {
        AcceptedThread acceptedThread = new AcceptedThread(listener);
        acceptedThread.start();
    }


    private class AcceptedThread extends Thread{

        SerialListener listener;
        public AcceptedThread(SerialListener listener){
            this.listener = listener;
        }
        @Override
        public void run() {
            super.run();

            synchronized (this) {
                this.listener = listener;
            }
            for(QueueItem item : queue1) {
                switch(item.type) {
                    case Connect:       listener.onSerialConnect      (); break;
                    case ConnectError:  listener.onSerialConnectError (item.e); break;
                    case Read:          listener.onSerialRead         (item.datas); break;
                    case IoError:       listener.onSerialIoError      (item.e); break;
                    case VCIRX:       listener.Vci_RX_Callback(item.paramId,item.paramData); break;

                }
            }
            for(QueueItem item : queue2) {
                switch(item.type) {
                    case Connect:       listener.onSerialConnect      (); break;
                    case ConnectError:  listener.onSerialConnectError (item.e); break;
                    case Read:          listener.onSerialRead         (item.datas); break;
                    case IoError:       listener.onSerialIoError      (item.e); break;
                    case VCIRX:       listener.Vci_RX_Callback(item.paramId,item.paramData); break;

                }
            }
            queue1.clear();
            queue2.clear();
        }
    }

    public void detach() {
        if(connected)
        // items already in event queue (posted before detach() to mainLooper) will end up in queue1
        // items occurring later, will be moved directly to queue2
        // detach() and mainLooper.post run in the main thread, so all items are caught
        listener = null;
    }

    @Override
    public void onSerialConnect() {
        if(connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialConnect();
                        } else {
                            queue1.add(new QueueItem(QueueType.Connect));
                        }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.Connect));
                }
            }
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        if(connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialConnectError(e);
                        } else {
                            queue1.add(new QueueItem(QueueType.ConnectError, e));
                            disconnect();
                        }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.ConnectError, e));
                    disconnect();
                }
            }
        }
    }
    private boolean pendingNewline = false;
    SpannableStringBuilder spn = new SpannableStringBuilder();

    byte[] finalResponse =null;
    @Override
    public void onSerialRead(byte[] data) {

        if(connected) {

            StringBuilder readMessage = new StringBuilder();

            String read = new String(data);

            for(int i=0; i<read.length(); i++) {
                if(read.charAt(i)==':') {
                    readMessage = new StringBuilder();
                    readMessage.append(read.charAt(i));
                }
                else if (read.charAt(i)=='\n') {
                    readMessage.append(read.charAt(i));
                    synchronized (this) {
                        if (listener != null) {
                            boolean first;
                            synchronized (lastRead) {
                                first = lastRead.datas.isEmpty(); // (1)
                                lastRead.add(data); // (3)
                            }
                            if(first) {

                                StringBuilder finalReadMessage = readMessage;
                                mainLooper.post(() -> {
                                    ArrayDeque<byte[]> datas;
                                    synchronized (lastRead) {
                                        datas = lastRead.datas;
                                        String tempReadMessage = finalReadMessage.toString();
                                        finalResponse = parseFinalResposne2(tempReadMessage.getBytes());
                                        lastRead.init(); // (2)
                                    }
                                    if (listener != null) {
                                        listener.onSerialRead(datas);
                                    } else {
                                        queue1.add(new QueueItem(QueueType.Read, datas));
                                        queue1.add(new QueueItem(QueueType.VCIRX, datas));
                                    }
                                });
                                readMessage = new StringBuilder();
                                read = "";
                            }
                        } else {
                            if(queue2.isEmpty() || queue2.getLast().type != QueueType.Read)
                                queue2.add(new QueueItem(QueueType.Read));
                            queue2.getLast().add(data);
                        }
                    }
                }
                else{
                    readMessage.append(read.charAt(i));
                }
            }


        }
    }

    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        throw new UnsupportedOperationException();
    }

    private byte[] parseFinalResposne(ArrayDeque<byte[]> datas){
        byte[] response=null;
        spn = new SpannableStringBuilder();
        for (byte[] tempData : datas) {
            String msg = new String(tempData);
            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);

                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    if(spn.length() >= 2) {
                        spn.delete(spn.length() - 2, spn.length());
                    }
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
             spn.append(msg);
        }
        System.out.println("Final parsed response"+spn.toString());
        response = spn.toString().getBytes();

        if(response!=null && response.length>=10 && response[5]==(byte) 0x38 && response[6]==(byte) 0x31){
            /*
            * VCI notification RX response data
            * */
            byte[] didArray = new byte[2];
            didArray[0] =  response[7];
            didArray[1] =  response[8];


            response = Tbus.parseResponse(response);

            byte did = DataConversion._PanToByte(didArray);
            byte payload = DataConversion._PanToByte(response);
            listener.Vci_RX_Callback( did,payload);
        }

        if(response!=null && response.length>16 && response[5]==(byte) 0x34 && response[6]==(byte) 0x35){
            /*
            * Vehicel Live Data response
            * */
             byte[] vehicleLiveDataArr = Tbus.parseResponse(response);
                if(vehicleLiveDataArr!=null && vehicleLiveDataArr.length>16) {
                    parseData(vehicleLiveDataArr);
                }
                else{
                    System.out.println("unable to parse vehicle live data response");
                }
        }
        return response;
    }


    private byte[] parseFinalResposne2(byte[] data){
        byte[] response=null;

        response = data;

        if(response!=null && response.length>=10 && response[5]==(byte) 0x38 && response[6]==(byte) 0x31){
            /*
             * VCI notification RX response data
             * */
            byte[] didArray = new byte[2];
            didArray[0] =  response[7];
            didArray[1] =  response[8];


            response = Tbus.parseResponse(response);

            byte did = DataConversion._PanToByte(didArray);
            byte payload = DataConversion._PanToByte(response);
            listener.Vci_RX_Callback( did,payload);
        }

        if(response!=null && response.length>16 && response[5]==(byte) 0x34 && response[6]==(byte) 0x35){
            /*
             * Vehicel Live Data response
             * */
            byte[] vehicleLiveDataArr = Tbus.parseResponse(response);
            if(vehicleLiveDataArr!=null && vehicleLiveDataArr.length>16) {
                parseData(vehicleLiveDataArr);
            }
            else{
                System.out.println("unable to parse vehicle live data response");
            }
        }
        return response;
    }

    @Override
    public void onSerialIoError(Exception e) {
        if(connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialIoError(e);
                        } else {
                            queue1.add(new QueueItem(QueueType.IoError, e));
                            disconnect();
                        }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.IoError, e));
                    disconnect();
                }
            }
        }
    }

    @Override
    public void Vci_RX_Callback(byte paramId, byte paramData) {

    }

    private String newline = TextUtil.newline_crlf;

    private void send(String str) {
        if(!connected) {
            System.out.println("not connected");
            return;
        }
        try {
            String msg;
            byte[] data;
            msg = str;
            data = (str + newline).getBytes();
            write(data);
        } catch (SerialTimeoutException e) {

            System.out.println( "write timeout: "+ e.getMessage());
            e.printStackTrace();

        } catch (Exception e) {
            onSerialIoError(e);

        }
    }

    public void VCI_WriteSignal(String tx, int counter){
        try {
            byte byteValue = (byte) counter;
            byte[] res = DataConversion._WordToPAN(byteValue);
            int did = TX.valueOf(tx).ordinal();
            byte[] command = Tbus.formCommand((byte) 0x44, (byte) did, res, (short)res.length);
            send(new String(command));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void VCI_GetLiveData(){
        try {
            byte[] command = Tbus.formCommand((byte) 0x45, (byte) 0x00, null, (short)0);
            send(new String(command));

        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    void parseData(byte[] vehicleLiveDataArr){
        try {
            byte[] data = new byte[2];
          /*  data[0] = vehicleLiveDataArr[0];
            data[1] = vehicleLiveDataArr[1];
            GlobalVariables.MAIA_DISPLAY = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[2];
            data[1] = vehicleLiveDataArr[3];
            GlobalVariables.PB4 = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[4];
            data[1] = vehicleLiveDataArr[5];
            GlobalVariables.PB1 = DataConversion._PanToByte(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[6];
            data[1] = vehicleLiveDataArr[7];
            GlobalVariables.PB2 = DataConversion._PanToByte(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[8];
            data[1] = vehicleLiveDataArr[9];
            GlobalVariables.PB3 = DataConversion._PanToByte(data);


            data = new byte[4];
            data[0] = vehicleLiveDataArr[10];
            data[1] = vehicleLiveDataArr[11];
            data[2] = vehicleLiveDataArr[12];
            data[3] = vehicleLiveDataArr[13];

            GlobalVariables.ActivityID_Feedback = DataConversion._PanToHWord(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[14];
            data[1] = vehicleLiveDataArr[15];
            GlobalVariables.AIDUS = DataConversion._PanToByte(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[16];
            data[1] = vehicleLiveDataArr[17];
            GlobalVariables.DirectionOfSpeech = DataConversion._PanToByte(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[18];
            data[1] = vehicleLiveDataArr[19];
            GlobalVariables.Music = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[20];
            data[1] = vehicleLiveDataArr[21];
            GlobalVariables.MsgReceived = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[22];
            data[1] = vehicleLiveDataArr[23];
            GlobalVariables.LookLeftRight = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[24];
            data[1] = vehicleLiveDataArr[25];
            GlobalVariables.ClickSelfie = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[26];
            data[1] = vehicleLiveDataArr[27];
            GlobalVariables.Call = DataConversion._PanToByte(data);*/

            data = new byte[8];
            data[0] = vehicleLiveDataArr[0];
            data[1] = vehicleLiveDataArr[1];
            data[2] = vehicleLiveDataArr[2];
            data[3] = vehicleLiveDataArr[3];
            data[4] = vehicleLiveDataArr[4];
            data[5] = vehicleLiveDataArr[5];
            data[6] = vehicleLiveDataArr[6];
            data[7] = vehicleLiveDataArr[7];
            GlobalVariables.VehicleSpeedESC = DataConversion._PanToWord(data);


            data = new byte[4];
            data[0] = vehicleLiveDataArr[8];
            data[1] = vehicleLiveDataArr[9];
            data[2] = vehicleLiveDataArr[10];
            data[3] = vehicleLiveDataArr[11];
            GlobalVariables.batterySOC = DataConversion._PanToHWord(data);


            data = new byte[2];
            data[0] = vehicleLiveDataArr[12];
            data[1] = vehicleLiveDataArr[13];
            GlobalVariables.RkeStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[14];
            data[1] = vehicleLiveDataArr[15];
            GlobalVariables.IgnStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[16];
            data[1] = vehicleLiveDataArr[17];
            GlobalVariables.DoorStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[18];
            data[1] = vehicleLiveDataArr[19];
            GlobalVariables.AcSwitchStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[20];
            data[1] = vehicleLiveDataArr[21];
            GlobalVariables.EngineSpeed = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[22];
            data[1] = vehicleLiveDataArr[23];
            GlobalVariables.EngineStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[24];
            data[1] = vehicleLiveDataArr[25];
            GlobalVariables.CompressorAcStatus = DataConversion._PanToByte(data);

            data = new byte[2];
            data[0] = vehicleLiveDataArr[26];
            data[1] = vehicleLiveDataArr[27];
            GlobalVariables.EngineTemparature = DataConversion._PanToByte(data);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Something went wrong"+e.getMessage());
        }
    }
    public int getVehicleSpeed() {
        return GlobalVariables.VehicleSpeedESC;
    }
    public int getSOC() {
        return GlobalVariables.batterySOC/80;
    }


}
