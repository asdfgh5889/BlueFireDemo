package co.raisense.bluetoothdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bluefire.api.BlueFire;
import com.bluefire.api.CANBusSpeeds;
import com.bluefire.api.ConnectionStates;
import com.bluefire.api.Const;
import com.bluefire.api.HardwareTypes;
import com.bluefire.api.J1939;
import com.bluefire.api.RecordIds;
import com.bluefire.api.RecordingModes;
import com.bluefire.api.RetrievalMethods;
import com.bluefire.api.SleepModes;
import com.bluefire.api.Truck;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import co.raisense.bluetoothdemo.callback.GetService;
import co.raisense.bluetoothdemo.network.RetrofitInstance;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
{
    // Adapter Layout
    private RelativeLayout layoutAdapter;

    private TextView textStatus;
    private TextView textHardware;
    private TextView textFirmware;
    private TextView textKeyState;
    private TextView textHeartbeat;

    private CheckBox checkUseBT21;
    private CheckBox checkUseBLE;
    private CheckBox checkUseJ1939;
    private CheckBox checkUseJ1708;
    private CheckBox checkUseOBD2;
    private Button buttonConnect;
    private Button btn_db;
    private Button send_server;

    private TextView dataView1;
    private TextView dataView2;
    private TextView dataView3;
    private TextView dataView4;
    private TextView dataView5;
    private TextView dataView6;
    private TextView dataView7;

    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private TextView textView5;
    private TextView textView6;
    private TextView textView7;

    private TextView textLatitude;
    private TextView textLongitude;

    //Retrofit
    GetService service;

    //Database
    private DBHelper dbHelper;
    private static final int MAX_AVAILABLE = 100;
    private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

    // App variables
    private boolean isConnecting;
    private boolean isReconnecting;
    private boolean isConnected;
    private boolean isConnectButton;

    private int groupNo = 0;

    private RetrievalMethods retrievalMethod;
    private int retrievalInterval;

    private ConnectAdapterThread connectThread;

    private boolean isKeyOn;

    private boolean secureDevice = false;
    private boolean secureAdapter = false;

    private ConnectionStates connectionState = ConnectionStates.NA;
    private String connectionMessage = "";

    private Queue<Message> EventsQueue = new LinkedList<Message>();
    private ReceiveEventsThreading ReceiveEventsThread;

    // BlueFire adapter and service
    private BlueFire blueFire;
    private Service demoService;

    private boolean isStartingService;

    // Android process pid for the activity or service that instantiates the API.
    private int appPid;
    private boolean killApp = false;

    // BlueFire App settings\

    private boolean ignoreSettings = false;

    private SharedPreferences settings;
    private SharedPreferences.Editor settingsSave;

    private HardwareTypes appHardwareType = HardwareTypes.HW_Unknown;

    private boolean appUseBLE = false;
    private boolean appUseBT21 = false;

    private int appLedBrightness = 100;
    private SleepModes appSleepMode = SleepModes.NoSleep;
    private boolean appPerformanceMode = false;
    private boolean appDisconnectedReboot = false;
    private int appDisconnectedRebootInterval = 60; // minutes
    public int appMinInterval;
    public boolean appSendAllPackets = false;

    private boolean appIgnoreJ1939 = false;
    private boolean appIgnoreJ1708 = false;
    private boolean appIgnoreOBD2 = false;

    private String appDeviceId = "";
    private String appAdapterId = "";
    private boolean appConnectToLastAdapter = false;

    private String appUserName = "";
    private String appPassword = "";
    private boolean appSecureDevice = false;
    private boolean appSecureAdapter = false;

    private int appDiscoveryTimeout;
    private int appMaxConnectAttempts;
    private int appMaxReconnectAttempts;
    private int appBluetoothRecycleAttempt;
    private int appBleDisconnectWaitTime;

    private boolean appOptimizeDataRetrieval = false;

    // ELD settings
    public boolean appELDStarted = false;
    public boolean appSecureELD = false;
    public boolean appRecordIFTA = false;
    public boolean appRecordStats = false;
    public boolean appAlignELD = false;
    public boolean appAlignIFTA = false;
    public boolean appAlignStats = false;
    public boolean appStreamingELD = false;
    public int appRecordingMode = RecordingModes.RecordNever.getValue();
    public String appDriverId = "";
    public String appELDAdapterId = "";
    public float appELDInterval = 60; // minutes;
    public float appIFTAInterval = 1; // minutes;
    public float appStatsInterval = 60; // minutes;
    public RecordingModes RecordingMode;

    private boolean isUploading;
    private int currentRecordNo = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Retrofit instance
        service = RetrofitInstance.getRetrofit().create(GetService.class);

        // Set to kill the app when the user exits.
        // Note, this is recommended as it will ensure that all API resources such as
        // Bluetooth (BLE GATT) are released.
        killApp = true;
        appPid = Process.myPid();

        logNotifications("API Demo started.");

        blueFire = new BlueFire(this, eventHandler);

        this.setTitle("Bluetooth Demo");

        // Set to use an insecure connection.
        // Note, there are other Android devices that require this other than just ICS (4.0.x).
        if (android.os.Build.VERSION.RELEASE.startsWith("4.0."))
            blueFire.SetUseInsecureConnection(true);
        else
            blueFire.SetUseInsecureConnection(false);

        // Establish settings persistent storage
        try
        {
            settings = this.getPreferences(Context.MODE_PRIVATE);
            settingsSave = settings.edit();

            // Get application settings
            getSettings();
        }
        catch (Exception ex)
        {
            ignoreSettings = true;
        }

        // Initialize adapter properties
        initializeAdapter();

        // Initialize settings that are not in the form for the
        // user to change.
        initializeSettings();

        // Initialize the app startup form
        initializeForm();

        // Setup to receive API events
        ReceiveEventsThread = new ReceiveEventsThreading();
        ReceiveEventsThread.start();
    }

    private void initializeSettings()
    {
        if (ignoreSettings)
            return;

        // Initialize settings that are not in the form for the
        // user to change.

        // Set the sleep mode
        appSleepMode = SleepModes.NoSleep;
        blueFire.SetSleepMode(appSleepMode);

        // Test setting connect attempts
        //appMaxConnectAttempts = 1; // default is blueFire.MaxConnectAttemptsDefault (5)
        //appMaxReconnectAttempts = 0; // default is blueFire.MaxReconnectAttemptsDefault (5)

        // Set the performance mode
        appPerformanceMode = false;
        blueFire.SetPerformanceModeOn(appPerformanceMode);

        // Set the disconnect reboot option
        appDisconnectedReboot = true;
        appDisconnectedRebootInterval = 60; // minutes
        blueFire.SetDisconnectedReboot(appDisconnectedReboot, appDisconnectedRebootInterval);

        // Set Optimize Data Retrieval
        appOptimizeDataRetrieval = true;

        // Save settings
        saveSettings();
    }

    private void getSettings()
    {
        if (ignoreSettings)
            return;

        // Get the application settings
        // Note, these should be retrieved from persistent storage.
        appUseBLE = settings.getBoolean("UseBLE", true);
        appUseBT21 = settings.getBoolean("UseBT21", false);
        appIgnoreJ1939 = settings.getBoolean("IgnoreJ1939", false);
        appIgnoreJ1708 = settings.getBoolean("IgnoreJ1708", true);
        appIgnoreOBD2 = settings.getBoolean("IgnoreOBD2", true);
        appPerformanceMode = settings.getBoolean("PerformanceMode", false);
        appDisconnectedReboot = settings.getBoolean("DisconnectedReboot", false);
        appDisconnectedRebootInterval = settings.getInt("DisconnectedRebootInterval", blueFire.DisconnectedRebootIntervalDefault);
        appSendAllPackets = settings.getBoolean("SendAllPackets", false);
        appSecureDevice = settings.getBoolean("SecureDevice", false);
        appSecureAdapter = settings.getBoolean("SecureAdapter", false);
        appConnectToLastAdapter = settings.getBoolean("ConnectToLastAdapter", false);
        appDeviceId = settings.getString("DeviceId", "");
        appAdapterId = settings.getString("AdapterId", "");
        appUserName = settings.getString("UserName", "");
        appPassword = settings.getString("Password", "");
        appLedBrightness = settings.getInt("LedBrightness", 100);
        appMinInterval = settings.getInt("MinInterval", blueFire.MinIntervalDefault);
        appDiscoveryTimeout = settings.getInt("DiscoveryTimeout", blueFire.DiscoveryTimeoutDefault);
        appMaxConnectAttempts = settings.getInt("MaxConnectAttempts", blueFire.MaxConnectAttemptsDefault);
        appMaxReconnectAttempts = settings.getInt("MaxReconnectAttempts", blueFire.MaxReconnectAttemptsDefault);
        appBluetoothRecycleAttempt = settings.getInt("BluetoothRecycleAttempt", blueFire.BluetoothRecycleAttemptDefault);
        appBleDisconnectWaitTime = settings.getInt("BleDisconnectWaitTime", blueFire.BleDisconnectWaitTimeDefault);
        appOptimizeDataRetrieval = settings.getBoolean("OptimizeDataRetrieval", true);

        // Get ELD settings
        appELDStarted = settings.getBoolean("ELDStarted", false);
        appSecureELD = settings.getBoolean("SecureELD", false);
        appRecordIFTA = settings.getBoolean("RecordIFTA", false);
        appRecordStats = settings.getBoolean("RecordStats", false);
        appAlignELD = settings.getBoolean("AlignELD", false);
        appAlignIFTA = settings.getBoolean("AlignIFTA", false);
        appAlignStats = settings.getBoolean("AlignStats", false);
        appStreamingELD = settings.getBoolean("StreamingELD", false);
        appRecordingMode = settings.getInt("RecordingMode", RecordingModes.RecordNever.getValue());
        appDriverId = settings.getString("DriverId", "");
        appELDAdapterId = settings.getString("ELDAdapterId", "");
        appELDInterval = settings.getFloat("ELDInterval", 60); // minutes;
        appIFTAInterval = settings.getFloat("IFTAInterval", 1); // minutes;
        appStatsInterval = settings.getFloat("StatsInterval", 60); // minutes;
    }

    private void saveSettings()
    {
        // Save the application settings.
        settingsSave.putBoolean("UseBLE", appUseBLE);
        settingsSave.putBoolean("UseBT21", appUseBT21);
        settingsSave.putBoolean("IgnoreJ1939", appIgnoreJ1939);
        settingsSave.putBoolean("IgnoreJ1708", appIgnoreJ1708);
        settingsSave.putBoolean("IgnoreOBD2", appIgnoreOBD2);
        settingsSave.putBoolean("PerformanceMode", appPerformanceMode);
        settingsSave.putBoolean("DisconnectedReboot", appDisconnectedReboot);
        settingsSave.putInt("DisconnectedRebootInterval", appDisconnectedRebootInterval);
        settingsSave.putBoolean("SendAllPackets", appSendAllPackets);
        settingsSave.putBoolean("SecureDevice", appSecureDevice);
        settingsSave.putBoolean("SecureAdapter", appSecureAdapter);
        settingsSave.putBoolean("ConnectToLastAdapter", appConnectToLastAdapter);
        settingsSave.putString("DeviceId", appDeviceId);
        settingsSave.putString("AdapterId", appAdapterId);
        settingsSave.putString("UserName", appUserName);
        settingsSave.putString("Password", appPassword);
        settingsSave.putInt("LedBrightness", appLedBrightness);
        settingsSave.putInt("MinInterval", appMinInterval);
        settingsSave.putInt("DiscoveryTimeout", appDiscoveryTimeout);
        settingsSave.putInt("MaxConnectAttempts", appMaxConnectAttempts);
        settingsSave.putInt("MaxReconnectAttempts", appMaxReconnectAttempts);
        settingsSave.putInt("BluetoothRecycleAttempt", appBluetoothRecycleAttempt);
        settingsSave.putInt("BleDisconnectWaitTime", appBleDisconnectWaitTime);
        settingsSave.putBoolean("OptimizeDataRetrieval", appOptimizeDataRetrieval);

        settingsSave.putBoolean("ELDStarted", appELDStarted);
        settingsSave.putBoolean("SecureELD", appSecureELD);
        settingsSave.putBoolean("RecordIFTA", appRecordIFTA);
        settingsSave.putBoolean("RecordStats", appRecordStats);
        settingsSave.putBoolean("AlignELD", appAlignELD);
        settingsSave.putBoolean("AlignIFTA", appAlignIFTA);
        settingsSave.putBoolean("AlignStats", appAlignStats);
        settingsSave.putBoolean("StreamingELD", appStreamingELD);
        settingsSave.putInt("RecordingMode", appRecordingMode);
        settingsSave.putString("DriverId", appDriverId);
        settingsSave.putString("ELDAdapterId", appELDAdapterId);
        settingsSave.putFloat("ELDInterval", appELDInterval); // minutes;
        settingsSave.putFloat("IFTAInterval", appIFTAInterval); // minutes;
        settingsSave.putFloat("StatsInterval", appStatsInterval); // minutes;

        settingsSave.commit();
    }

    private void resetSettings()
    {
        if (ignoreSettings)
            return;

        settingsSave.clear();
        settingsSave.commit();
    }

    private void initializeAdapter()
    {
        // Set Bluetooth adapter type
        blueFire.UseBLE = appUseBLE;
        blueFire.UseBT21 = appUseBT21;

        // Set to receive notifications from the adapter.
        // Note, this should only be used during testing.
        blueFire.SetNotificationsOn(false);

        // Set to ignore data bus settings
        blueFire.SetIgnoreJ1939(appIgnoreJ1939);
        blueFire.SetIgnoreJ1708(appIgnoreJ1708);
        blueFire.SetIgnoreOBD2(appIgnoreOBD2);

        // Set the minimum interval
        blueFire.SetMinInterval(appMinInterval);

        // Set the BLE Disconnect Wait Timeout.
        // Note, in order for BLE to release the connection to the adapter and allow reconnects
        // or subsequent connects, it must be completely closed. Unfortunately Android does not
        // have a way to detect this other than waiting a set amount of time after disconnecting
        // from the adapter. This wait time can vary with the Android version and the make and
        // model of the mobile device. The default is 2 seconds. If your app experiences numerous
        // unable to connect and BlueFire LE fails to show up under Bluetooth settings, try increasing
        // this value.
        blueFire.SetBleDisconnectWaitTime(appBleDisconnectWaitTime);

        // Set the Bluetooth discovery timeout.
        // Note, depending on the number of Bluetooth devices present on the mobile device,
        // discovery could take a long time.
        // Note, if this is set to a high value, the app needs to provide the user with the
        // capability of canceling the discovery.
        blueFire.SetDiscoveryTimeout(appDiscoveryTimeout);

        // Set number of Bluetooth connection attempts.
        // Note, if the mobile device does not connect, try setting this to a value that
        // allows for a consistent connection. If you're using multiple adapters and have
        // connection problems, un-pair all devices before connecting.
        // Note: Bluetooth Classic (UseBT21) uses Com sockets and they can block for a
        // considerably amount of time depending on the OEM device. It is therefore recommended
        // that you adjust the MaxConnectAttempts, MaxReconnectAttempts, and the DiscoveryTimeout
        // to compensate for this duration.
        blueFire.SetMaxConnectAttempts(appMaxConnectAttempts);
        blueFire.SetMaxReconnectAttempts(appMaxReconnectAttempts);
        blueFire.SetBluetoothRecycleAttempt(appBluetoothRecycleAttempt);

        // Set the device and adapter ids
        blueFire.SetDeviceId(appDeviceId);
        blueFire.SetAdapterId(appAdapterId);

        // Set the connect to last adapter setting
        blueFire.SetConnectToLastAdapter(appConnectToLastAdapter);

        // Set the adapter security parameters
        blueFire.SetSecurity(appSecureDevice, appSecureAdapter, appUserName, appPassword);

        // Set to optimize data retrieval
        blueFire.SetOptimizeDataRetrieval(appOptimizeDataRetrieval);

        // Set the send all packets option
        blueFire.SetSendAllPackets(appSendAllPackets);

        // Set streaming and recording mode
        blueFire.ELD.SetStreaming(appStreamingELD);
        blueFire.ELD.SetRecordingMode(RecordingModes.forValue(appRecordingMode));
    }

    private void initializeForm()
    {
        dbHelper = new DBHelper(this);
        // Adapter layout
        layoutAdapter = (RelativeLayout) findViewById(R.id.layoutAdapter);

        textStatus = findViewById(R.id.textStatus);
        textHardware = findViewById(R.id.textHardware);
        textFirmware = findViewById(R.id.textFirmware);
        textKeyState = findViewById(R.id.textKeyState);
        textHeartbeat = findViewById(R.id.textHeartbeat);

        checkUseBT21 = findViewById(R.id.checkUseBT21);
        checkUseBLE = findViewById(R.id.checkUseBLE);
        checkUseJ1939 = findViewById(R.id.checkUseJ1939);
        checkUseJ1708 = findViewById(R.id.checkUseJ1708);
        checkUseOBD2 = findViewById(R.id.checkUseOBD2);
        buttonConnect = findViewById(R.id.buttonConnect);
        btn_db = findViewById(R.id.btn_db);
        send_server = findViewById(R.id.send_data);

        btn_db.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InfoActivity.class));
            }
        });

        send_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(blueFire.IsConnected()){
//                    String data = dataView1.getText().toString()
//                            + " " + dataView2.getText().toString()
//                            + " " + dataView3.getText().toString()
//                            + " " + dataView4.getText().toString()
//                            + " " + dataView5.getText().toString()
//                            + " " + dataView6.getText().toString()
//                            + " " + dataView7.getText().toString()
//                            + " " + textLatitude.getText().toString() +
//                            " " + textLongitude.getText().toString();

                String data = "216516"
                        + " " + "654684"
                        + " " + "846"
                        + " " + "651313"
                        + " " + "56464"
                        + " " + "86468462"
                        + " " + "2130"
                        + " " + "212131";

                    Call<String> call = service.sendData(data);
                    call.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            String resp = response.body();
                            if(response.isSuccessful() && resp != null){
                                Toast.makeText(MainActivity.this, "Data are sent! Response: \n" + resp, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "There is problem with the internet!", Toast.LENGTH_SHORT).show();
                        }
                    });
//                }else{
//                    Toast.makeText(MainActivity.this, "Please connect to an adapter!", Toast.LENGTH_SHORT).show();
//                }
            }
        });

        findViewById(R.id.clean_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.deleteAllData();
            }
        });

        dataView1 = findViewById(R.id.dataLabel1);
        dataView2 = findViewById(R.id.dataLabel2);
        dataView3 = findViewById(R.id.dataLabel3);
        dataView4 = findViewById(R.id.dataLabel4);
        dataView5 = findViewById(R.id.dataLabel5);
        dataView6 = findViewById(R.id.dataLabel6);
        dataView7 = findViewById(R.id.dataLabel7);

        textView1 = findViewById(R.id.dataText1);
        textView2 = findViewById(R.id.dataText2);
        textView3 = findViewById(R.id.dataText3);
        textView4 = findViewById(R.id.dataText4);
        textView5 = findViewById(R.id.dataText5);
        textView6 = findViewById(R.id.dataText6);
        textView7 = findViewById(R.id.dataText7);

        textLatitude = findViewById(R.id.locationLatText);
        textLongitude = findViewById(R.id.locationLongText);

        // Clear the form
        clearForm();

        showConnectButton();

        //buttonNextFault.setVisibility(View.INVISIBLE);
        //buttonResetFault.setVisibility(View.INVISIBLE);

        connectionState = ConnectionStates.NA;
        showStatus();

        buttonConnect.setFocusable(true);
        buttonConnect.setFocusableInTouchMode(true);
        buttonConnect.requestFocus();
    }

    private void clearForm()
    {
        // Disable adapter parameters
        enableAdapterParms();

        textHeartbeat.setText("0");

        // Show user settings
        checkUseBT21.setChecked(appUseBT21);
        checkUseBLE.setChecked(appUseBLE);
        checkUseJ1939.setChecked(!appIgnoreJ1939); // checkUseJ1939 is the opposite of ignoreJ1939
        checkUseJ1708.setChecked(!appIgnoreJ1708); // checkUseJ1708 is the opposite of ignoreJ1708
        checkUseOBD2.setChecked(!appIgnoreOBD2); // checkUseJ1708 is the opposite of ignoreJ1708
    }

    private void enableAdapterParms()
    {
        // Enable/Disable Adapter page parameters

        // Enable only if not connecting or connected
        boolean isNotConnecting = (!isConnecting && !isConnected);

        checkUseBLE.setEnabled(isNotConnecting);
        checkUseBT21.setEnabled(isNotConnecting);

        checkUseJ1939.setEnabled(isNotConnecting);
        checkUseJ1708.setEnabled(isNotConnecting);
        checkUseOBD2.setEnabled(isNotConnecting);
    }

    // Connect Button
    public void onConnectClick(View view)
    {
        try
        {
            if (isConnectButton)
            {
                clearForm();

                isConnecting = true;
                isConnected = false;
                isReconnecting = false;

                checkUseBT21.setEnabled(false);
                checkUseBLE.setEnabled(false);

                checkUseJ1939.setEnabled(false);
                checkUseJ1708.setEnabled(false);
                checkUseOBD2.setEnabled(false);

                showDisconnectButton();

                enableAdapterParms();

                if (appUseBT21)
                    startConnection();
                else
                    checkBluetoothPermissions();

                startELD();
            }
            else
            {
                Thread.sleep(500); // allow eld to stop before disconnecting
                disconnectAdapter();
                stopELD();
            }
        } catch (Exception ex)
        {
        }
    }

    // Start Service Button
    public void onStartServiceClick(View view)
    {
        isStartingService = true;

        enableAdapterParms();

//        buttonStartService.setEnabled(false);
//        buttonStopService.setEnabled(true);

        buttonConnect.setEnabled(false);

        if (appUseBT21)
            startConnection();
        else
            checkBluetoothPermissions();
    }

    // Stop Service Button
    public void onStopServiceClick(View view)
    {
        if (demoService == null)
            return;

        demoService.stopService();

        enableAdapterParms();

//        buttonStartService.setEnabled(true);
//        buttonStopService.setEnabled(false);

        buttonConnect.setEnabled(true);
    }

    private void checkBluetoothPermissions()
    {
        // BLE adapters require Android 6.0 and the user must accept location access permission
        // Check for Android 6 or higher
        if (blueFire.AndroidVersion()[0] < 6)
        {
            adapterDisconnected();

            Toast.makeText(this, "BLE Adapters require Android 6+.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check and request access permission for BLE
        // Note, ActivityCompat.shouldShowRequestPermissionRationale doesn't always work so we don't use it here.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        int MinSDKVersion = 0;
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MinSDKVersion = this.getApplicationInfo().minSdkVersion;
            }
        }
        catch (Error e){}

        // Check for user granting permission.
        // Note, iff request is cancelled, the result arrays are empty.
        // Note, only minSDKVersion 23 and above requires the user to grant location permission.

        if (MinSDKVersion < 23 || (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
        {
            // Ensure location services is turned on
            if (isLocationEnabled())
                startConnection();
            else
            {
                adapterDisconnected();

                Toast.makeText(this, "You need to turn on Location to use the BLE Adapter.", Toast.LENGTH_LONG).show();
            }
        }
        // User refused the permission request, do not allow connection.
        else
        {
            adapterDisconnected();

            Toast.makeText(this, "You need to allow Location Access to use the BLE Adapter.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isLocationEnabled()
    {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void startConnection()
    {
        if (isStartingService)
        {
            isStartingService = false;
            StartService();
        }
        else
        {
            // Connect to the adapter via a thread so that the blocking
            // connection will run in it's own thread and allow the app
            // to show status.
            connectThread = new ConnectAdapterThread();
            connectThread.start();
        }
    }

    // Start Service
    public void StartService()
    {
        if (demoService == null)
            demoService = new Service(this);

        demoService.startService();
    }

    private class ConnectAdapterThread extends Thread
    {
        public void run()
        {
            // Initialize adapter properties (in case they were changed)
            initializeAdapter();

            // Connect to the adapter.
            // Note, this is a blocking call and must run in it's own thread.
            blueFire.Connect();
        }
    }

    private void showConnectButton()
    {
        checkKeyState();

        isConnectButton = true;
        buttonConnect.setText("Connect");
        buttonConnect.setEnabled(true);

//        buttonStartService.setEnabled(true);
//        buttonStopService.setEnabled(false);
    }

    private void showDisconnectButton()
    {
        isConnectButton = false;
        buttonConnect.setText("Disconnect");
        buttonConnect.setEnabled(true);

        //buttonStartService.setEnabled(false);
    }

    private void disconnectAdapter()
    {
        try
        {
            enableAdapterParms();

            buttonConnect.setEnabled(false);

            //buttonStartService.setEnabled(false);
            //buttonStopService.setEnabled(false);

            // Wait for the adapter to disconnect so that the Connect button
            // is not displayed too prematurely.
            boolean WaitForDisconnect = isConnected; // just for code clarity
            blueFire.Disconnect(WaitForDisconnect);

        } catch (Exception e)
        {
        }
    }

    private void adapterDisconnected()
    {
        adapterNotConnected(false);

        String Message = "Adapter disconnected.";
        logNotifications(Message);
        Toast.makeText(this, Message, Toast.LENGTH_SHORT).show();
    }

    private void adapterNotConnected()
    {
        adapterNotConnected(true);
    }
    private void adapterNotConnected(boolean logMessage)
    {
        isConnected = false;
        isConnecting = false;
        isReconnecting = false;

        showConnectButton();

        enableAdapterParms();

        checkUseBT21.setEnabled(true);
        checkUseBLE.setEnabled(true);

        checkUseJ1939.setEnabled(true);
        checkUseJ1708.setEnabled(true);
        checkUseOBD2.setEnabled(true);

        buttonConnect.requestFocus();

        showStatus();

        if (logMessage)
        {
            String Message = "Adapter not connected.";
            logNotifications(Message);
            Toast.makeText(this, Message, Toast.LENGTH_SHORT).show();
        }
    }

    private void adapterReconnecting()
    {
        isConnected = false;
        isConnecting = true;
        isReconnecting = true;

        enableAdapterParms();

        showDisconnectButton();

        String Message = "Reconnecting to the Adapter.";
        logNotifications(Message);
        Toast.makeText(this, Message, Toast.LENGTH_SHORT).show();
    }

    private void adapterReconnected()
    {
        isReconnecting = false;

        logNotifications("Adapter is reconnected and re-retrieving data.");
    }

    private void adapterNotReconnected()
    {
        adapterNotConnected(false);

        String Message = "Adapter did not reconnect.";
        logNotifications(Message);
        Toast.makeText(this, Message, Toast.LENGTH_SHORT).show();
    }

    private void adapterNotAuthenticated()
    {
        logNotifications("Adapter is not authenticated.");

        adapterNotConnected();

        Toast.makeText(this, "You are not authorized to access this adapter. Check for the correct adapter, the 'Connect to Last Adapter' setting, or your 'User Name and Password'.", Toast.LENGTH_LONG).show();
    }

    private void adapterConnected()
    {
        isConnected = true;
        isConnecting = false;

        // Enable Use J1939/J1708
        checkUseJ1939.setEnabled(true);
        checkUseJ1708.setEnabled(true);

        // Enable adapter parameters
        enableAdapterParms();

        // Enable buttons
        showDisconnectButton();

        buttonConnect.requestFocus();

        // Connect to ELD
        blueFire.ELD.Connect();

        // Get adapter data
        getAdapterData();

        String Message = "Adapter is connected.";
        logNotifications(Message);
        Toast.makeText(this, Message, Toast.LENGTH_SHORT).show();
    }

    private void startingCAN()
    {
        // Get the CAN bus speed
        CANBusSpeeds CANBusSpeed = blueFire.CANBusSpeed();

        String Message;
        if (blueFire.IsOBD2())
            Message = "OBD2";
        else
            Message = "J1939";
        Message += " is starting, CAN bus speed is ";

        switch (CANBusSpeed)
        {
            case K250:
                Message += "250K.";
                break;
            case K500:
                Message += "500K.";
                break;
            default:
                Message += "unknown.";
                break;
        }
        logNotifications(Message, true);

        // Key is on so double check the key state
        checkKeyState();

        // Re-retrieve truck data
        reRetrieveTruckData();
    }

    private void j1708Restarting()
    {
        // Re-retrieve truck data
        reRetrieveTruckData();
    }

    // Start retrieving data after connecting to the adapter
    private void getAdapterData()
    {
        // Check for an incompatible version.
        if (!blueFire.IsCompatible())
        {
            logNotifications("Incompatible Adapter.");

            Toast.makeText(this, "The Adapter is not compatible with this API.", Toast.LENGTH_LONG).show();
            disconnectAdapter();
            return;
        }

        // Get the adapter hardware type
        appHardwareType = blueFire.HardwareType();

        // Get the device and adapter ids
        appDeviceId = blueFire.DeviceId();
        appAdapterId = blueFire.AdapterId();

        // Check for API setting the adapter data
        appUseBT21 = blueFire.UseBT21;
        appUseBLE = blueFire.UseBLE;

        appIgnoreJ1939 = blueFire.IgnoreJ1939();
        appIgnoreJ1708 = blueFire.IgnoreJ1708();
        appIgnoreOBD2 = blueFire.IgnoreOBD2();

        // Save any changed data from the API
        saveSettings();

        // Update UseBT21 and UseBLE checkboxes
        checkUseBT21.setChecked(appUseBT21);
        checkUseBLE.setChecked(appUseBLE);

        // Update J1939 and J1708 checkboxes
        checkUseJ1939.setChecked(!appIgnoreJ1939);
        checkUseJ1708.setChecked(!appIgnoreJ1708);
        checkUseOBD2.setChecked(!appIgnoreOBD2);

        // Show hardware and firmware versions
        textHardware.setText(blueFire.HardwareVersion());
        textFirmware.setText(blueFire.FirmwareVersion());

        // Check the key state (key on/off)
        checkKeyState();
    }

    private void reRetrieveTruckData()
    {
        getTruckData();
    }

    // BLE Checkbox
    public void onUseBLECheck(View view)
    {
        appUseBLE = checkUseBLE.isChecked();

        // Update api
        blueFire.UseBLE = appUseBLE;

        saveSettings();
    }

    // BT21 Checkbox
    public void onUseBT21Check(View view)
    {
        appUseBT21 = checkUseBT21.isChecked();

        // Update api
        blueFire.UseBT21 = appUseBT21;

        saveSettings();
    }


    // J1939 Checkbox
    public void onUseJ1939Check(View view)
    {
        // Set to ignore J1939 (opposite of checkUseJ1939)
        appIgnoreJ1939 = !checkUseJ1939.isChecked();

        if (!appIgnoreJ1939)
        {
            appIgnoreOBD2 = true;
            checkUseOBD2.setChecked(!appIgnoreOBD2); // opposite
        }
        else
        {
            appIgnoreJ1708 = false;
            checkUseJ1708.setChecked(!appIgnoreJ1708); // opposite
        }

        // Update api
        blueFire.SetIgnoreJ1939(appIgnoreJ1939);
        blueFire.SetIgnoreJ1708(appIgnoreJ1708);
        blueFire.SetIgnoreOBD2(appIgnoreOBD2);

        saveSettings();
    }

    // J1708 Checkbox
    public void onUseJ1708Check(View view)
    {
        // Set to ignore J708 (opposite of checkUseJ1708)
        appIgnoreJ1708 = !checkUseJ1708.isChecked();

        if (!appIgnoreJ1708)
        {
            appIgnoreOBD2 = true;
            checkUseOBD2.setChecked(!appIgnoreOBD2); // opposite
        }
        else
        {
            appIgnoreJ1939 = false;
            checkUseJ1939.setChecked(!appIgnoreJ1939); // opposite
        }

        // Update api
        blueFire.SetIgnoreJ1939(appIgnoreJ1939);
        blueFire.SetIgnoreJ1708(appIgnoreJ1708);
        blueFire.SetIgnoreOBD2(appIgnoreOBD2);

        saveSettings();
    }

    // OBD2 Checkbox
    public void onUseOBD2Check(View view)
    {
        // Set to ignore J708 (opposite of checkUseJ1708)
        appIgnoreOBD2 = !checkUseOBD2.isChecked();

        if (!appIgnoreOBD2)
        {
            appIgnoreJ1939 = true;
            appIgnoreJ1708 = true;
        }
        else
        {
            appIgnoreJ1939 = false;
            appIgnoreJ1708 = true;
        }
        checkUseJ1939.setChecked(!appIgnoreJ1939); // opposite
        checkUseJ1708.setChecked(!appIgnoreJ1708); // opposite

        // Update api
        blueFire.SetIgnoreJ1939(appIgnoreJ1939);
        blueFire.SetIgnoreJ1708(appIgnoreJ1708);
        blueFire.SetIgnoreOBD2(appIgnoreOBD2);

        saveSettings();
    }

    // Reset Settings Button
    public void onResetSettingsClick(View view)
    {
        resetSettings();

        getSettings();

        initializeAdapter();

        clearForm();

        buttonConnect.requestFocus();

        Toast.makeText(this, "App settings have been reset.", Toast.LENGTH_SHORT).show();
    }

    private void checkKeyState()
    {
        boolean keyIsOn = blueFire.IsKeyOn();

        if (isKeyOn != keyIsOn)
        {
            if (keyIsOn)
                textKeyState.setText("Key On");
            else
                textKeyState.setText("Key Off");

            // Double check key change by retrieving IsCANAvailable and IsJ1708Available.
            // Note, only do this on change of state, not constantly.
            blueFire.GetKeyState();

            isKeyOn = keyIsOn;
        }
    }

    private void showData()
    {
        // Check the key state (key on/off)
        checkKeyState();

        // Show truck data
        if (blueFire.IsTruckDataChanged())
            showTruckData();

        // Show ELD data
        if ( blueFire.ELD.IsDataRetrieved())
            showELDData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    available.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dbHelper.inserData(
                        Calendar.getInstance().getTime().toString(),
                        dataView1.getText().toString(),
                        dataView2.getText().toString(),
                        dataView3.getText().toString(),
                        dataView4.getText().toString(),
                        dataView5.getText().toString(),
                        dataView6.getText().toString(),
                        dataView7.getText().toString(),
                        textLatitude.getText().toString() +
                                " " + textLongitude.getText().toString()
                );
                available.release();
            }
        }).start();
    }

    private void showHeartbeat()
    {
        // Check the key state (key on/off)
        checkKeyState();

        textHeartbeat.setText(String.valueOf(blueFire.HeartbeatCount()));
    }

    private void clearAdapterData()
    {
        boolean isRetrievingVINID = false;
        blueFire.StopDataRetrieval();
    }

    private void getTruckData()
    {
        // Set the retrieval method and interval.
        // Note, this is here for demo-ing the different methods.
        retrievalMethod = RetrievalMethods.OnChange;
        retrievalInterval = blueFire.MinInterval(); // default, only required if RetrievalMethod is OnInterval

        switch (groupNo)
        {
            case 0: //RPM
                textView1.setText("RPM");
                textView2.setText("Speed");
                textView3.setText("Accel Pedal");
                textView4.setText("Pct Load");
                textView5.setText("Pct Torque");
                textView6.setText("Driver Torque");
                textView7.setText("Torque Mode");

                clearAdapterData();
                blueFire.GetEngineData1(retrievalMethod, retrievalInterval); // RPM, Percent Torque, Driver Torque, Torque Mode
                blueFire.GetEngineData2(retrievalMethod, retrievalInterval); // Percent Load, Accelerator Pedal Position
                blueFire.GetEngineData3(retrievalMethod, retrievalInterval); // Vehicle Speed, Max Set Speed, Brake Switch, Clutch Switch, Park Brake Switch, Cruise Control Settings and Switches
                break;

            case 3: //Fuel
                textView1.setText("Fuel Rate");
                textView2.setText("Fuel Used");
                textView3.setText("HiRes Fuel");
                textView4.setText("Idle Fuel Used");
                textView5.setText("Avg Fuel Econ");
                textView6.setText("Inst Fuel Econ");
                textView7.setText("Throttle Pos");
                clearAdapterData();
                blueFire.GetFuelData(retrievalMethod, retrievalInterval); // Fuel Levels, Fuel Used, Idle Fuel Used, Fuel Rate, Instant Fuel Economy, Avg Fuel Economy, Throttle Position
                break;

            case 6: //RPM
                textView1.setText("RPM");
                textView2.setText("Speed");
                textView3.setText("Distance");
                textView4.setText("Odometer");
                textView5.setText("Total Hours");
                textView6.setText("");
                textView7.setText("");
                clearAdapterData();
                blueFire.GetELDData(); // RPM, Speed, Distance/Odometer, Total Hours
                break;
        }
    }

    private void showTruckData()
    {
        switch (groupNo)
        {
            case 0:
                dataView1.setText(formatInt(Truck.RPM));
                dataView2.setText(formatFloat(Truck.Speed * Const.KphToMph,2));
                dataView3.setText(formatFloat(Truck.AccelPedal,2));
                dataView4.setText(formatInt(Truck.PctLoad));
                dataView5.setText(formatInt(Truck.PctTorque));
                dataView6.setText(formatInt(Truck.DrvPctTorque));
                dataView7.setText(String.valueOf(Truck.TorqueMode));

                break;

            case 3:
                dataView1.setText(formatFloat(Truck.FuelRate * Const.LphToGalPHr,2));
                dataView2.setText(formatFloat(Truck.FuelUsed * Const.LitersToGal,3));
                dataView3.setText(formatFloat(Truck.HiResFuelUsed * Const.LitersToGal,3));
                dataView4.setText(formatFloat(Truck.IdleFuelUsed * Const.LitersToGal,3));
                dataView5.setText(formatFloat(Truck.AvgFuelEcon * Const.KplToMpg,2));
                dataView6.setText(formatFloat(Truck.InstFuelEcon * Const.KplToMpg,2));
                dataView7.setText(formatFloat(Truck.ThrottlePos,2));

                break;

            case 6:
                dataView1.setText(formatInt(Truck.RPM));
                dataView2.setText(formatFloat(Truck.Speed * Const.KphToMph,2));
                dataView3.setText(formatFloat(Truck.Distance * Const.MetersToMiles,2)); // hi-res or converted lo-res
                dataView4.setText(formatFloat(Truck.Odometer * Const.MetersToMiles,2)); // hi-res or converted lo-res
                dataView5.setText(formatFloat(Truck.TotalHours,3));
                dataView6.setText("");
                dataView7.setText("");

                break;
        }
    }

    private void showStatus()
    {
        textStatus.setText(connectionState.toString());
    }

    // ELD Button
    public void onELDDataClick(View view)
    {
        if (!blueFire.ELD.IsCompatible())
        {
            Toast.makeText(this, "The Adapter is not compatible with ELD Recording.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!blueFire.IsConnected())
        {
            Toast.makeText(this, "The Adapter must be connected for ELD Recording.", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void showELDData()
    {
        if (blueFire.ELD.CurrentRecordNo() > 0 || blueFire.ELD.IsRecordingLocally())
            if (blueFire.ELD.RecordNo() > 0 && blueFire.ELD.RecordNo() != currentRecordNo)
            {
                // Only show the record once
                currentRecordNo = blueFire.ELD.RecordNo();

                // Show the ELD record
                showELDRecord(currentRecordNo);

                // Check for recording locally
                if (blueFire.ELD.IsRecordingLocally() && !isUploading)
                {
                    if (blueFire.ELD.IsStarted() || blueFire.ELD.LocalRecordNo() > 0)
                    {
                        blueFire.ELD.SetLocalRecordNo(blueFire.ELD.RecordNo());

                        // Write local ELD record
                        writeELDRecord();
                    }
                }
                else // recording or uploading from the adapter
                {
                    // Check for uploading records
                    if (isUploading)
                        uploadELD();
                }
            }
    }

    private void showELDRecord(int RecordNo)
    {
        RecordIds RecordId = RecordIds.forValue(blueFire.ELD.RecordId());

        // ELD
        if (RecordId == RecordIds.IFTA) {
            textLatitude.setText(formatDecimal(blueFire.ELD.Latitude(), 7));
            textLongitude.setText(formatDecimal(blueFire.ELD.Longitude(), 7));
        } else {
            textLatitude.setText(formatDecimal(blueFire.ELD.Latitude(), 7));
            textLongitude.setText(formatDecimal(blueFire.ELD.Longitude(), 7));
        }
    }

    private void uploadELD()
    {
        // Record the ELD record someplace
        writeELDRecord();

        // Check for more records
        if (currentRecordNo < blueFire.ELD.CurrentRecordNo())
            blueFire.ELD.GetRecord(currentRecordNo + 1);

            // No more records, done uploading
        else
        {
            // Stop the upload
            isUploading = false;

            blueFire.ELD.StopUpload();

            if (blueFire.ELD.IsRecordingLocally())
                Toast.makeText(this, "The ELD records were saved.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, "The ELD Upload is completed.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeELDRecord()
    {
        // Do something with the record data
        Log.d("Upload", String.valueOf(blueFire.ELD.RecordNo()) + "," + String.valueOf(blueFire.ELD.RecordId()));
    }

    // ELD Start Button
    public void onStartELDClick(View view)
    {
        if (blueFire.ELD.IsStarted())
            stopELD();
        else
            startELD();
    }

    private boolean editELDParms()
    {
        // Set ELD parameters
        blueFire.ELD.DriverId = appDriverId; // not persistent by adapter

        blueFire.ELD.ELDInterval = appELDInterval;
        blueFire.ELD.AlignELD = appAlignELD;

        blueFire.ELD.RecordIFTA = appRecordIFTA;
        blueFire.ELD.IFTAInterval = appIFTAInterval;
        blueFire.ELD.AlignIFTA = appAlignIFTA;

        blueFire.ELD.RecordStats = appRecordStats;
        blueFire.ELD.StatsInterval = appStatsInterval;
        blueFire.ELD.AlignStats = appAlignStats;

        return true;
    }

    private void startELD()
    {
        if (!blueFire.IsConnected())
        {
            Toast.makeText(this, "The adapter is not connected.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!blueFire.ELD.IsStreaming() && blueFire.ELD.RecordingMode() == RecordingModes.RecordNever)
        {
            Toast.makeText(this, "Please select either streaming or recording data.", Toast.LENGTH_LONG).show();
            return;
        }
        // if not started, edit ELD parameters
        if (!blueFire.ELD.IsStarted())
            if (!editELDParms())
                return;

        // Start recording
        if (!blueFire.ELD.IsStarted())
        {
            // Set the time in the adapter.
            // Note, must do this here so the custom record will have the correct date.
            // If no custom record is to be sent, StartRecording will also set the time.
            blueFire.SetTime();

            blueFire.ELD.StartRecording();
        }

        // And start streaming
        blueFire.ELD.StartStreaming();
    }

    private void stopELD()
    {
        if (!blueFire.IsConnected())
            return;

        // Stop streaming
        blueFire.ELD.StopStreaming();

        // Stop recording
        blueFire.ELD.StopRecording();
    }

    // *********************************** End ELD *******************************************

    private String formatInt(int data)
    {
        if (data < 0)
            return "NA";
        else
            return String.valueOf(data);
    }

    private String formatFloat(float data, int precision)
    {
        if (data < 0)
            return "NA";

        return formatDecimal(data, precision);
    }

    private String formatDecimal(double data, int precision)
    {
        BigDecimal bd = new BigDecimal(data);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return String.valueOf(bd.floatValue());
    }

    private void showMessage(String title, String message)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.show();
    }

    private void logNotifications(String notification)
    {
        logNotifications(notification, false);
    }

    private void logNotifications(String message, boolean showMessage)
    {
        if (message.equals(""))
            return;

        Log.d("BlueFire", message);
    }

    private void processEvent(Message msg)
    {
        try
        {
            connectionState = ConnectionStates.values()[msg.what];
            connectionMessage = (String)msg.obj;

            //android.util.Log.d("BlueFire", "Main ProcessEvent, State=" + connectionState);

            switch (connectionState)
            {
                case Initializing:
                case Initialized:
                case Discovering:
                case Disconnecting:
                    // Status only
                    break;

                case Connecting:
                    if (!blueFire.IsReconnecting())
                        logNotifications("Connection Attempt " + blueFire.ConnectAttempts(), true);
                    break;

                case Connected:
                    adapterConnected();
                    break;

                case NotAuthenticated:
                    adapterNotAuthenticated();
                    break;

                case Disconnected:
                    adapterDisconnected();
                    break;

                case Reconnecting:
                    logNotifications("Reconnect Attempt " + blueFire.ReconnectAttempts(), true);
                    adapterReconnecting();
                    break;

                case Reconnected:
                    adapterReconnected();
                    break;

                case NotReconnected:
                    adapterNotReconnected();
                    break;

                case CANStarting:
                    startingCAN();
                    break;

                case J1708Restarting:
                    j1708Restarting();
                    break;


                case NotConnected:
                    adapterNotConnected();
                    break;

                case DataChanged:
                    showData();
                    break;

                case Heartbeat:
                    showHeartbeat();
                    return; // do not show status

                case CANFilterFull:
                    logNotifications("The CAN Filter is Full", true);
                    showMessage("Adapter Data Retrieval", "The CAN Filter is Full. Some data will not be retrieved.");
                    break;

                case Notification:
                    logNotifications("API Notification - " + connectionMessage, true);
                    return; // do not show status

                case AdapterMessage:
                    logNotifications("Adapter Message - " + connectionMessage, true);
                    return; // do not show status

                case AdapterReboot:
                    if (appMaxReconnectAttempts == 0) // no reconnect attempt
                        adapterNotConnected();
                    logNotifications("Adapter Rebooting - " + connectionMessage, true);
                    return; // do not show status

                case DataError:
                    if (appMaxReconnectAttempts == 0) // no reconnect attempt
                        adapterNotConnected();
                    logNotifications("Adapter Data Error - " + connectionMessage, true);
                    return; // do not show status

                case DataTimeout:
                    if (appMaxReconnectAttempts == 0) // no reconnect attempt
                        adapterNotConnected();
                    logNotifications("Adapter Data Timeout - Lost connection with the Adapter", true);
                    return; // do not show status

                case BluetoothTimeout:
                    adapterNotConnected();
                    logNotifications("API Bluetooth Timeout - Unable to connect to Bluetooth." , true);
                    break;

                case AdapterTimeout:
                    adapterNotConnected();
                    logNotifications("Adapter Connection Timeout - Bluetooth unable to connect to the Adapter." , true);
                    break;

                case SystemError:
                    adapterNotConnected();
                    logNotifications("API System Error - " + connectionMessage , true);
                    showMessage("API System Error", "See the log for details.");
                    break;
            }

            showStatus();
        }
        catch (Exception e) {}
    }

    // BlueFire Event Handler Thread
    private class ReceiveEventsThreading extends Thread
    {
        public void run()
        {
            while (true)
            {
                if (!EventsQueue.isEmpty())
                {
                    final Message handleMessage = EventsQueue.poll();
                    if (handleMessage != null)
                    {
                        runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                processEvent(handleMessage);
                            }
                        });
                    }
                }
                threadSleep(1); // allow other threads to execute
            }
        }
    }

    // BlueFire Event Handler
    @SuppressLint("HandlerLeak")
    private Handler eventHandler = new Handler()
    {
        @Override
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg)
        {
            Message handleMessage = new Message();
            handleMessage.what = msg.what;
            handleMessage.obj = msg.obj;

            EventsQueue.add(handleMessage);
        }
    };

    private byte[] hexStringToBytes(String hexString)
    {
        byte[] hexBytes = new byte[8];

        byte[] StringBytes = new BigInteger(hexString,16).toByteArray();

        int index = 7;
        for (int i = StringBytes.length-1; i >= 0; i--)
        {
            hexBytes[index] = StringBytes[i];
            if (index == 0)
                break;
            index--;
        }

        return hexBytes;
    }

    private String bytesToHexString(byte[] hexBytes)
    {
        // Convert hex bytes to a hex string.
        // Note, this will pad < 8 bytes with hex 0.

        String hexString = ("0000000000000000" + new BigInteger(1, hexBytes).toString(16));

        int startIndex = hexString.length()-16;
        if (startIndex > 16)
            startIndex = 16;

        return hexString.substring(startIndex);
    }

    private float celciusToFarenheit(float temp)
    {
        if (temp < 0)
            return -1;
        else
            return (temp * 1.8F + 32F);
    }

    private float farenheitToCelcius(float temp)
    {
        if (temp < 0)
            return -1;
        else
            return ((temp -32) / 1.8F);
    }

    private void threadSleep(int Interval)
    {
        try
        {
            Thread.sleep(Interval);
        }
        catch(Exception ex) {}
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        saveSettings();

        blueFire.Dispose();
    }

}
