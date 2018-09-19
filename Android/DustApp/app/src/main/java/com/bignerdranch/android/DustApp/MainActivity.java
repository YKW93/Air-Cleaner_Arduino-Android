package com.bignerdranch.android.DustApp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import es.dmoral.toasty.Toasty;

public class MainActivity extends Activity {
    private static final String TAG = "bluetooth2";
    private ImageView imgSet;
    private LinearLayout layout;
    private TextView conditionTxt, statusTxt, explainTxt, temperaturTxt, humidityTxt;
    private SwitchCompat ledSwitch, speakSwitch;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private String str = "";
    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "98:D3:31:FC:54:9E";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TEST", "...onCreate - try connect...");

        setContentView(R.layout.activity_main);

        imgSet = (ImageView) findViewById(R.id.imageSet);
        layout = (LinearLayout) findViewById(R.id.layout);
        conditionTxt = (TextView) findViewById(R.id.contidionText);
        statusTxt = (TextView) findViewById(R.id.stausText);
        explainTxt = (TextView) findViewById(R.id.explainText);
        ledSwitch = (SwitchCompat) findViewById(R.id.ledSwitch);
        speakSwitch = (SwitchCompat) findViewById(R.id.speakSwitch);
        temperaturTxt = (TextView) findViewById(R.id.temperaturText);
//        humidityTxt = (TextView) findViewById(R.id.HumidityText);

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        ConnectingBluetooth();

        ledSwitch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toasty.success(getApplicationContext(), "LED ON", Toast.LENGTH_SHORT, true).show();
                    mConnectedThread.write("1"); // LED ON
                } else {
                    Toasty.error(getApplicationContext(), "LED OFF", Toast.LENGTH_SHORT, true).show();
                    mConnectedThread.write("2"); // LED OFF

                }
            }
        });

        speakSwitch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toasty.success(getApplicationContext(), "스피커 ON", Toast.LENGTH_SHORT, true).show();
                    mConnectedThread.write("3");

                } else {
                    Toasty.error(getApplicationContext(), "스피커 OFF", Toast.LENGTH_SHORT, true).show();
                    mConnectedThread.write("4");
                }
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("TEST", "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.


    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
    private void ConnectingBluetooth() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Toasty.success(getApplicationContext(), "블루투스 연결 성공!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        mConnectedThread.write("0"); //안드로이드 <-> 아두이노 블루투스 연결 성공했을 때,
        Log.d("TEST", "...스레드 실행 - try connect...");
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String strBuf = new String(buffer, 0, bytes);

                    Log.d("test", "strBuf:"+ strBuf);

                    for (int i = 0; i < bytes; i++) {
                        if (strBuf.charAt(i) == '#') { //먼지센서값
                            str = str.replace("#", "");
                            showMessage(str, "Dust");
                            str = "";
                            break;
                        }
                        else if (strBuf.charAt(i) == '!') { // 온도값
                            str = str.replace("!", "");
                            showMessage(str, "Temperature");
                            str = "";
                            break;
                        }
//                        else if (strBuf.charAt(i) == '%') { // 습도값
//                            str = str.replace("%", "");
//                            showMessage(str, "Humidity");
//                            Log.d("test1234", "str:"+str);
//                            str = "";
//                            break;
//                        }
                        else {
                            str += strBuf.charAt(i);
                            Log.d("test", "str:"+str);
                        }
                    }

                } catch (IOException e) {
                    break;
                }

//                try {
//                    // Read from the InputStream
//                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
//                    String strBuf = new String(buffer, 0, bytes);
//
//                    str += strBuf;
//                    Log.d("test", "str:"+ str);
//                    if(str.matches("#")){
////                        "[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*"
//                        str = str.replace("#", "");
//                        showMessage(str);
//                        str = "";
//                    }
//
//                } catch (IOException e) {
//                    break;
//                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
    // 메시지를 화면에 표시
    public void showMessage(String strMsg, String tmp) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        Log.d("DUST","showmessage:"+ strMsg);

        if (tmp == "Dust") {
            mHandler.sendMessage(msg);
        } else if (tmp == "Temperature") {
            mHandler2.sendMessage(msg);
        }
//        else if (tmp == "Humidity") {
//            mHandler3.sendMessage(msg);
//        }
        Log.d("tag1", strMsg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String)msg.obj;
                Log.d("test12", strMsg);
                Double dust = Double.parseDouble(strMsg);
                if (dust >= 0.01 && dust <= 30) {
                    String strColor = "#006aff";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.lovee);
                    conditionTxt.setText("먼지 농도 : " + dust + "㎍/m³");
                    statusTxt.setText("현재 상태 : 좋음");
                    explainTxt.setText("매우 좋은 상태 입니다~♥");
                }
                else if(dust > 30 && dust <= 80) {
                    String strColor = "#01b6c3";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.goodd);
                    conditionTxt.setText("먼지 농도 : " + dust + "㎍/m³");
                    statusTxt.setText("현재 상태 : 보통");
                    explainTxt.setText("그냥 무난한 상태에요~");
                }
                else if(dust > 80 && dust <= 150) {
                    String strColor = "#ff6600";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.sosoo);
                    conditionTxt.setText("먼지 농도 : " + dust + "㎍/m³");
                    statusTxt.setText("현재 상태 : 나쁨");
                    explainTxt.setText("공기가 탁하네요. 환기좀 시켜주세요~ ");
                }
                else if(dust > 150) {
                    String strColor = "#e60000";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.badd);
                    conditionTxt.setText("먼지 농도 : " + dust + "㎍/m³");
                    statusTxt.setText("현재 상태 : 매우 나쁨");
                    explainTxt.setText("위험합니다! 빨리 환기 시켜주세요!!");
                }
            }
        }
    };

    Handler mHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String)msg.obj;
                Double tem = Double.parseDouble(strMsg);
                if (tem == 0.0) {
                    temperaturTxt.setText("온도 : " + "분석중....");

                }
                temperaturTxt.setText("온도 : " + tem + "℃");
            }
        }
    };

//    Handler mHandler3 = new Handler() {
//        public void handleMessage(Message msg) {
//            if (msg.what == 0) {
//                String strMsg = (String)msg.obj;
//                Double hum = Double.parseDouble(strMsg);
//                humidityTxt.setText("습도:" + hum);
//            }
//        }
//    };

}
