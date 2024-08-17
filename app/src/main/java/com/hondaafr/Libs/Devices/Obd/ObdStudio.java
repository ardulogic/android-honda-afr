package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ObdStudio extends Debuggable {
    private final Context context;
    private final ObdStudioListener listener;
    public static Double tps = 0D;
    public static Integer speed = 0;
    public static Integer rpm = 0;
    public static Integer map = 0;
    public static Integer intakeTemp = 0;

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;
    private Long lastReadingTimestamp = 0L;
    private boolean isBusy = false;
    private boolean ecuConnected = false;

    ArrayList<String> pendingCommands = new ArrayList<>();

    public ObdStudio(Context mContext, ObdStudioListener listener) {
        this.listener = listener;
        this.context = mContext;
    }

    public void startRequestingSensorReadings() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }

        final Runnable requestTask = this::requestSensorReadings;

        scheduler.scheduleAtFixedRate(requestTask, 0, 100, TimeUnit.MILLISECONDS);
        readingsOn = true;
    }

    public void stopRequestingSensorReadings() {

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        readingsOn = false;
        scheduler = null;
    }


    public void requestSensorReadings() {
        if (deviceIsNotBusy()) {
            if (!ecuConnected) {
                initBtConnection();
            } else {
                if (pendingCommands.isEmpty()) {
                    pendingCommands.add(ObdCommands.requestTps());
                    pendingCommands.add(ObdCommands.requestMap());
//                    pendingCommands.add(ObdCommands.requestSpeed());
//                    pendingCommands.add(ObdCommands.requestRpm());
                }


                String pendingCommand = pendingCommands.get(0);
                pendingCommands.remove(0);

                BluetoothService.send(context, pendingCommand, "obd");
                isBusy = true;
            }
        }
    }


    private boolean deviceIsNotBusy() {
        return !isBusy || timeSinceLastSensorReadings() > 10000;
    }

    public void initBtConnection() {
        isBusy = true;

        // Example usage of the send method
        ArrayList<String> lines = new ArrayList<>();
        lines.add(ObdCommands.resetObd());
        BluetoothService.send(context, lines, "obd");
    }

    public void requestTps() {

        BluetoothService.send(context, ObdCommands.requestTps(), "obd");
    }

    public void requestSpeed() {
        BluetoothService.send(context, ObdCommands.requestSpeed(), "obd");
    }

    public void requestRpm() {
        BluetoothService.send(context, ObdCommands.requestRpm(), "obd");
    }

    public void onDataReceived(String data) {
        d(data, 1);
        isBusy = false;

        if (ObdCommands.dataIsBusy(data)) {
            isBusy = true;
        }

        if (ObdCommands.dataIsTps(data)) {
            tps = ObdCommands.parseTpsData(data);
//            mTpsHistory.add(tps);
            listener.onObdTpsReceived(tps);
        }

        if (ObdCommands.dataIsSpeed(data)) {
            speed = ObdCommands.parseSpeedData(data);
            listener.onObdSpeedReceived(speed);
        }

        if (ObdCommands.dataIsRpm(data)) {
            rpm = ObdCommands.parseRpmData(data);
            listener.onObdRpmReceived(rpm);
        }

        if (ObdCommands.dataIsMap(data)) {
            map = ObdCommands.parseMapData(data);
            listener.onObdMapReceived(map);
        }

        if (ObdCommands.dataIsIntakeTemp(data)) {
            intakeTemp = ObdCommands.parseIntakeTempData(data);
            listener.onObdIntakeTempReceived(intakeTemp);
        }

        if (ObdCommands.dataInitComplete(data)) {
            BluetoothService.send(context, ObdCommands.requestPids(), "obd");
            isBusy = true;
        }

        if (ObdCommands.dataCantConnectToEcu(data)) {
            ecuConnected = false;
        }

        if (ObdCommands.dataEcuConnected(data)) {
            ecuConnected = true;
        }

        updateLastTrackingDataTimestamp();
    }

    public void start() {
        initBtConnection();

        if (!readingsOn) {
            startRequestingSensorReadings();
        } else {
            stopRequestingSensorReadings();
        }
    }


    public void updateLastTrackingDataTimestamp() {
        lastReadingTimestamp = System.currentTimeMillis();
    }

    public long timeSinceLastSensorReadings() {
        return System.currentTimeMillis() - lastReadingTimestamp;
    }


}
