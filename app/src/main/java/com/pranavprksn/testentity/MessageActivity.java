package com.pranavprksn.testentity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MessageActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int SERVERPORT = 5000;

    public static final String SERVER_IP = "10.0.2.2";
    private ClientThread clientThread;
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage;
    static int connectionMessage = 0;
    static int hash;

    /*
    @Override
    protected void onStart() {
        // Storing data into SharedPreferences
        super.onStart();

        //If newly installed

        SharedPreferences sh = getSharedPreferences("KeyStores", Context.MODE_MULTI_PROCESS);

        boolean isInstalled = sh.getBoolean("isInstalled", false);

        if(!isInstalled){
//            try {
//                OnInstall();
//            } catch (InvalidKeyException e) {
//                e.printStackTrace();
//            }
        }else{
//            try {
//                buildSession();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            } catch (UntrustedIdentityException | InvalidKeyException e) {
//                e.printStackTrace();
//            }
        }

    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        setTitle("Client");
        clientTextColor = ContextCompat.getColor(this, R.color.blue_light);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(() -> msgList.addView(textView(message, color)));
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {

            msgList.removeAllViews();
            showMessage("Connecting to Server...", clientTextColor);

            try {
                Entity alice = new Entity(1, 314159, "alice");
                hash = alice.hashCode();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            // Session aliceToBobSession = new Session(alice.getStore(), bob.getPreKey(), bob.getAddress());

            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();

            //SENDING DATA
//            if (clientThread != null) {
//                //SENDING MESSAGES
//                clientThread.sendMessage("Connection first message");
//            }

            showMessage("Connected to Server...", clientTextColor);

            return;
        }

        if (view.getId() == R.id.send_data) {
            String clientMessage = edMessage.getText().toString().trim();
            showMessage(clientMessage, Color.WHITE);
            if (null != clientThread) {

                //SENDING MESSAGES

                if (connectionMessage++ == 0) {
                    //SENDING MESSAGES

                    clientThread.sendMessage("#"+ hash + " 1 314159 alice");
                }

                clientThread.sendMessage(clientMessage);
            }
        }
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);

                while (!Thread.currentThread().isInterrupted()) {

                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        showMessage(message, Color.RED);
                        break;
                    }
                    showMessage("Server: " + message, clientTextColor);
                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        void sendMessage(final String message) {
            new Thread(() -> {
                try {
                    if (null != socket) {
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),
                                true);
                        out.println(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
}