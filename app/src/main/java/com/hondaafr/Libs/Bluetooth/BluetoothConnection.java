package com.hondaafr.Libs.Bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.hondaafr.Libs.Helpers.Debuggable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Bluetooth connector is used as a helper for the bluetooth service
 * It handles the actual sending and receiving commands
 */
public class BluetoothConnection extends Debuggable {
    private static final String TAG = "BluetoothConnection";
    private final String serviceUuid;
    protected int D = 1;

    public static final String ACTION = "com.hondaafr.Libs.Bluetooth.Services.action.bt.connector";

    private int mState;

    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice connectedDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Context mContext;
    private BluetoothConnectionListener listener;
    private final String deviceName;
    public boolean isSending = false;



    public String id; // Id for simultaneous connections

    public BluetoothConnection(Context mContext, BluetoothDeviceData deviceData, BluetoothConnectionListener listener, String id, String uuid) {
        this.mContext = mContext;
        this.listener = listener;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(deviceData.getAddress());
        deviceName = (deviceData.getName() == null) ? deviceData.getAddress() : deviceData.getName();
        mState = BluetoothStates.STATE_BT_NONE;
        this.id = id;
        this.serviceUuid = uuid;
    }

    public BluetoothSocket getConnectedSocket() {
        if (mConnectedThread != null) {
            return mConnectedThread.getSocket();
        }

        return null;
    }

    public synchronized void connect() {
        d("Connecting to: " + connectedDevice, 1);

        stopConnectionThreads();

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();
    }

    public synchronized void stop() {
        d("Stopping", 1);

        stopConnectionThreads();

        setState(BluetoothStates.STATE_BT_NONE);
    }

    /**
     * Update bluetooth state
     * This will send an intent
     *
     * @param state
     */
    private synchronized void setState(int state) {
        if (Objects.equals(this.id, "spartan") && BluetoothStates.labelOfState(mState).equals("Idle.") && BluetoothStates.labelOfState(state).equals("Connecting...")) {
            Log.d("setState", "Connection stared.");
        }
        d("setState(" + this.id + ") " + BluetoothStates.labelOfState(mState) + " -> " + BluetoothStates.labelOfState(state), 1);
        mState = state;
        listener.onStateChanged(state, this.id);
    }

    public synchronized int getState() {
        return mState;
    }

    private void stopConnectionThreads() {
        if (mConnectedThread != null) {
            d("Stopping previous 'connected' thread", 2);
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mConnectThread != null) {
            d("Stopping previous 'connecting' thread", 2);
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    /**
     * Is called after the socket has been created
     *
     * @param socket
     */
    public synchronized void connected(BluetoothSocket socket) {
        d("Bluetooth Socked Created", 1);

        stopConnectionThreads();

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(BluetoothStates.STATE_BT_CONNECTED);
        listener.onNotification(BluetoothStates.NOTIFICATION_CONNECTED_TO_SSID, deviceName, id);
    }

    /**
     * Sends data to the socket
     *
     * @param data
     */
    public void write(byte[] data) {
        if (mConnectedThread != null) {
            if (data.length == 1) mConnectedThread.write(data[0]);
            else mConnectedThread.writeData(data);
        }
    }

    public void write(ArrayList<String> lines, boolean add_nl) {
        String s = TextUtils.join(add_nl ? "\n" : "", lines);
        d("Sending: " + s, 3);
        write(s.getBytes());
    }

    public void write(String line, boolean add_nl) {
        String s = line + (add_nl ? "\n" : "");
        d("Sending: " + s, 3);
        write(s.getBytes());
    }

    private void connectionFailed() {
        d("Connecion Failed.", 1);

        setState(BluetoothStates.STATE_BT_DISCONNECTED);
    }


    private void connectionLost() {
        d("Connecion Lost.", 1);

        setState(BluetoothStates.STATE_BT_DISCONNECTED);
    }

    public boolean isConnected() {
        if (mConnectedThread != null) {
            if (mConnectedThread.getSocket() != null) {
                return mConnectedThread.getSocket().isConnected();
            }
        }

        return false;
    }


    /**
     * Handles bluetooth connection to a certain device
     * on a separate thread
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            d("Creating 'ConnectThread'.", 1);
            mmDevice = device;

            if (serviceUuid != null) {
                mmSocket = BluetoothUtils.createRfcommSocketOnServiceUuid(mmDevice, serviceUuid);
            } else {
                mmSocket = BluetoothUtils.createRfcommSocket(mmDevice);
            }

            d("Socket created.", 1);
            d("Socket type:", mmSocket.getConnectionType());
        }

        @SuppressLint("MissingPermission")
        public void run() {
            d("Running 'ConnectThread'.", 1);
            btAdapter.cancelDiscovery();

            if (mmSocket == null) {
                d("Unable to connect to device, failed to create socket during connection.", 1);
                connectionFailed();
                return;
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                setState(BluetoothStates.STATE_BT_CONNECTING);
                mmSocket.connect();

            } catch (IOException e) {
                // read failed, socket might closed or timeout, read ret: -1
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    d("Unable to close socket during connection.", 1);
                }

                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnection.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }

        public void cancel() {
            d("Cancel of ConnectThread.", 2);

            if (mmSocket == null) {
                d("Socket was already closed.", 1);
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                d("Could not close() the socked", 1);
            }
        }
    }

    /**
     * Thread for reading/writing bluetooth stream
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BluetoothSocket getSocket() {
            return mmSocket;
        }

        public ConnectedThread(BluetoothSocket socket) {
            d( "Creating 'ConnectedThread'", 2);

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                d("Could not create temporary sockets!", 1);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * Read stream
         */
        public void run() {
            d( "Running 'ConnectedThread'", 3);
            byte[] buffer = new byte[512];
            int bytes;

            String messageBuffer = "";

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readedPart = new String(buffer, 0, bytes);
                    messageBuffer += readedPart;

                    boolean endedWithNewline = messageBuffer.contains("\n");

                    messageBuffer = messageBuffer.replace("\r\r", "\r");
                    boolean endedWithRR = messageBuffer.contains(("\r"));

                    if (endedWithNewline || endedWithRR) {
                        int newlineIndex = -1;
                        if (endedWithNewline)
                            newlineIndex = messageBuffer.indexOf("\n");
                        else if (endedWithRR) {
                            newlineIndex = messageBuffer.indexOf("\r");
                        }

                        String receivedLine = messageBuffer.substring(0, newlineIndex + 1);
                        messageBuffer = messageBuffer.substring(newlineIndex + 1);

                        listener.onDataReceived(receivedLine, id);;
                    }
                } catch (IOException e) {
                    d("Disconnected while trying to read stream.", 1);
                    connectionLost();
                    break;
                }
            }
        }


        /**
         * Sends multiple bytes
         * @param chunk
         */
        public void writeData(byte[] chunk) {

            try {
                mmOutStream.write(chunk);
                mmOutStream.flush();

                //if (D) Log.e(TAG, "Data chunk has been sent");
            } catch (IOException e) {
                d("Exception during write()", 1);
            }
        }

        /**
         * Sends one byte
         * @param command
         */
        public void write(byte command) {
            byte[] buffer = new byte[1];
            buffer[0] = command;

            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
            } catch (IOException e) {
                d("Exception during write()", 1);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
               d("Could not close() the socket", 1);
            }
        }
    }

}