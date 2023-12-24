package com.yanamobility.computebox.UI;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.anastr.speedviewlib.AwesomeSpeedometer;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.yanamobility.computebox.R;
import com.yanamobility.computebox.Utils.Constants;
import com.yanamobility.computebox.Utils.TextUtil;
import com.yanamobility.usblibrary.CustomProber;
import com.yanamobility.usblibrary.VCIComm;
import com.yanamobility.usblibrary.SerialListener;
import com.yanamobility.usblibrary.SerialSocket;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity2 extends AppCompatActivity implements  SerialListener, OnMapReadyCallback {
    private int deviceId, portNum, baudRate;
    private BroadcastReceiver broadcastReceiver;
    private VCIComm vciComm;
    SerialListener serialListener;
    private Connected connected = Connected.False;


    private GoogleMap mMap;
    int vehicleSpeed, soc;
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }


    private enum Connected { False, Pending, True }
    private UsbSerialPort usbSerialPort;
    Context context;
    TextView  textViewVehicleSpeed;


    private String newline = TextUtil.newline_crlf;
    private boolean pendingNewline = false;
    public static String responseString;
    public static byte[] responseBytes;
    private static final String TAG = "MainActivity2";

    boolean dontStop = false;

    AwesomeSpeedometer speedView;
    ProgressBar sohProgressbar;
    ProgressBarDrawable bgProgress;
    TextView sohTextView, tvTime;
    ImageView imageViewRightIndicator, imageViewLeftIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui);
        context = MainActivity2.this;
        getSupportActionBar().hide();
        vehicleSpeed = 0;
        soc =0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.colorBlack));
        }
        init();
        sohTextView = findViewById(R.id.tv_soh);
        textViewVehicleSpeed = findViewById(R.id.tv_rpm);
        speedView =  findViewById(R.id.raySpeedometer);

        imageViewRightIndicator= findViewById(R.id.iv_right);
        imageViewLeftIndicator= findViewById(R.id.iv_left);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageViewRightIndicator.setImageResource(R.drawable.ic_right_arrow_grey);
                imageViewLeftIndicator.setImageResource(R.drawable.ic_right_arrow_grey);
                imageViewLeftIndicator.setRotation(180);
            }
        });

        tvTime = findViewById(R.id.tv_time);
        Handler h = new Handler();
        int delay = 1000; //milliseconds

        h.postDelayed(new Runnable(){
            public void run(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        String time = getPresentTime();
                        tvTime.setText(time);
                    }
                });
                h.postDelayed(this, delay);
            }
        }, delay);

        speedView.speedTo(0);


        sohProgressbar = findViewById(R.id.progress_bar_test);
        bgProgress = new ProgressBarDrawable(4);

        sohProgressbar.setProgressDrawable(bgProgress);
        sohProgressbar.setProgress(80);
        sohTextView.setText("80"+"%");

        dontStop = false;
       /* btnVehicleSpeed = findViewById(R.id.vehicleSpeed_btn);
        btnVehicleSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vciComm.VCI_GetLiveData();
                int vehicleSpeed = vciComm.getVehicleSpeed();
                Log.e(TAG,"VehicleSpeed"+vehicleSpeed);
            }
        });
*/

        //LiveData liveData = new LiveData();
        //liveData.start();


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dontStop = true;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dontStop = true;
    }

    class LiveData extends Thread{
        @Override
        public void run() {
            super.run();
            try {

                SystemClock.sleep(2000);

                while (!dontStop){
                    vciComm.VCI_GetLiveData();
                    SystemClock.sleep(100);
                    vehicleSpeed= vciComm.getVehicleSpeed();
                    soc = vciComm.getSOC();

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        connect();
    }
    @Override
    public void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /*
    *init, connect
    * */


    private void init(){
    //    Bundle arg = getIntent().getExtras();
    //    deviceId = arg.getInt("device");
    //    portNum = arg.getInt("port");
    //    baudRate = 921600;
//
    //    vciComm = new VCIComm(context,this);
    //    vciComm.attach(this);

        /*
        * Broadcast recveiver for listnening permission
        * */
    //broadcastReceiver = new BroadcastReceiver() {
    //    @Override
    //    public void onReceive(Context context, Intent intent) {
    //        if(Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
    //            Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
    //            connect(granted);
    //        }
    //    }
    //};



}


    private  void UpdateText(String message, boolean tasdf){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void connect() {
        connect(null);
    }
    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getApplication().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
           // status("connection failed: device not found");
            UpdateText("connection failed: device not found",false);
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            UpdateText("connection failed: no driver for device",false);
            return;
        }
        if(driver.getPorts().size() < portNum) {
            UpdateText("connection failed: not enough ports at device",false);
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getApplication(), 0, new Intent(Constants.INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
            {
                UpdateText("connection failed: permission denied",false);
            }
            else{
                UpdateText("connection failed: open failed",false);
            }
            return;
        }

        connected = Connected.Pending;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            SerialSocket socket = new SerialSocket(getApplicationContext(), usbConnection, usbSerialPort);
            vciComm.connect(socket);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }

    }

    private void UpdateUI(ArrayDeque<byte[]> datas) {
        try {
            float speedValue =0;
            SpannableStringBuilder spn = new SpannableStringBuilder();
            for (byte[] data : datas) {
                {
                    String msg = new String(data);
                    if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                        // don't show CR as ^M if directly before LF
                        msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);

                        // special handling if CR and LF come in separate fragments
                        if (pendingNewline && msg.charAt(0) == '\n') {
                            if(spn.length() >= 2) {
                                spn.delete(spn.length() - 2, spn.length());
                            } else {

                            }
                        }
                        pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                    }

                    spn.append(TextUtil.toCaretString(msg, newline.length() != 0));

                }
            }
            responseString = spn.toString();
            UpdateText(responseString,false);
            responseBytes = responseString.getBytes();
            vehicleSpeed = vciComm.getVehicleSpeed();
            soc = vciComm.getSOC();

            float finalSpeedValue = vehicleSpeed;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewVehicleSpeed.setText( vehicleSpeed+"");
                    speedView.speedTo(vehicleSpeed);
                    sohProgressbar.setProgress(soc);
                    sohTextView.setText(soc+"%");

                }
            });

            /*
             * Checking if the response belongs to Vehicle Live data and updated the textview
             * */
            if(responseBytes!=null && responseBytes.length>16 && responseBytes[5]==(byte) 0x34 && responseBytes[6]==(byte) 0x35)
            {

              /*  float finalSpeedValue = vehicleSpeed;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewVehicleSpeed.setText( finalSpeedValue+"");
                        speedView.speedTo(finalSpeedValue);

                    }
                });*/
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void disconnect() {
        connected = Connected.False;
        vciComm.disconnect();
        usbSerialPort = null;
    }


    @Override
    public void onSerialConnect() {
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        UpdateUI(datas);
    }

    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        UpdateUI(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        disconnect();
    }

    @Override
    public void Vci_RX_Callback(byte paramId, byte paramData) {
        System.out.println("ParamID: " + paramId);
        System.out.println("paramData: " + paramData);


        if(paramId ==(byte)0x2B ){
            if(paramData==(byte)0x1C)
            {
                imageViewRightIndicator.setImageResource(R.drawable.ic_right_arrow);
                imageViewRightIndicator.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink));
                imageViewLeftIndicator.setImageResource(R.drawable.ic_right_arrow_grey);
                imageViewLeftIndicator.setRotation(180);
            }
            else if(paramData==(byte)0x34)
            {
                imageViewRightIndicator.setImageResource(R.drawable.ic_right_arrow_grey);
                imageViewLeftIndicator.setImageResource(R.drawable.ic_right_arrow);
                imageViewLeftIndicator.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink));
                imageViewLeftIndicator.setRotation(180);
            }
        }
    }

    public class ProgressBarDrawable extends Drawable {

        private int parts = 10;

        private Paint paint = null;
        private int fillColor = Color.parseColor("#00adef");
        private int emptyColor = Color.parseColor("#942E2E2E");
        private int separatorColor = Color.parseColor("#000000");
        private RectF rectFill = null;
        private RectF rectEmpty = null;
        private List<RectF> separators = null;

        public ProgressBarDrawable(int parts)
        {
            this.parts = parts;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.separators = new ArrayList<RectF>();
        }

        @Override
        protected boolean onLevelChange(int level)
        {
            invalidateSelf();
            return true;
        }

        @Override
        public void draw(Canvas canvas)
        {
            // Calculate values
            Rect b = getBounds();
            float width = b.width();
            float height = b.height();

            int spaceFilled = (int)(getLevel() * width / 10000);
            this.rectFill = new RectF(0, 0, spaceFilled, height);
            this.rectEmpty = new RectF(spaceFilled, 0, width, height);

            int spaceBetween = (int)(width / 100);
            int widthPart = (int)(width / this.parts - (int)(0.9 * spaceBetween));
            int startX = widthPart;
            for (int i=0; i<this.parts - 1; i++)
            {
                this.separators.add( new RectF(startX, 0, startX + spaceBetween, height) );
                startX += spaceBetween + widthPart;
            }


            // Foreground
            this.paint.setColor(this.fillColor);
            canvas.drawRect(this.rectFill, this.paint);

            // Background
            this.paint.setColor(this.emptyColor);
            canvas.drawRect(this.rectEmpty, this.paint);

            // Separator
            this.paint.setColor(this.separatorColor);
            for (RectF separator : this.separators)
            {
                canvas.drawRect(separator, this.paint);
            }
        }

        @Override
        public void setAlpha(int alpha)
        {
        }

        public void changeColor(String colorString)
        {
            fillColor = Color.parseColor(colorString);
            this.paint.setColor(this.fillColor);

        }
        @Override
        public void setColorFilter(ColorFilter cf)
        {
        }

        @Override
        public int getOpacity()
        {
            return PixelFormat.TRANSLUCENT;
        }
    }
    public static String getPresentTime()
    {
        String Date = null;
        java.util.Date now = new Date();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm aa");
            String strDate= formatter.format(now);
            now = new SimpleDateFormat("HH:mm aa").parse(strDate);
            SimpleDateFormat simpleDateformat = new SimpleDateFormat("HH:mm aa"); // three digit abbreviation
            Date = simpleDateformat.format(now);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Date;
    }


}