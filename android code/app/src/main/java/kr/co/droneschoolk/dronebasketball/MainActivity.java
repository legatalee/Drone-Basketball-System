package kr.co.droneschoolk.dronebasketball;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.androidadvance.topsnackbar.TSnackbar;
import com.github.zagum.switchicon.SwitchIconView;

import info.hoang8f.widget.FButton;

public class MainActivity extends AppCompatActivity {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    public static Context mContext;
    public int impulse = 0;
    public int maximumImpulse = 1000;
    RoundCornerProgressBar impulseBar;
    // Layout Views
    private FButton mGripButton;
    private FButton mDropButton;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    private TextView impulseText;
    private TextView connectedDevice;
    private TextView impulseContent;
    private String readMessage;
    private View bluetoothButton;
    private SwitchIconView bluetoothToggle;
    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @SuppressLint("StringFormatInvalid")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    byte[] readBuf = (byte[]) msg.obj;
                    readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d("a", readMessage);
                    try {
                        impulse += Integer.parseInt(readMessage);
                    } catch (NumberFormatException e) {
                    }
                    if (impulse >= maximumImpulse) {
                        MainActivity.this.sendMessage("mdrop");
                        impulse = 0;
                        TSnackbar tSnackbar = TSnackbar.make(findViewById(android.R.id.content), "공이 떨어졌습니다!", TSnackbar.LENGTH_LONG);
                        TextView mainTextView = (TextView) (tSnackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                            mainTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        else
                            mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                        mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                        tSnackbar.show();
                    }
                    impulseBar.setProgress(impulse);
                    impulseText.setText(String.format(getResources().getString(R.string.impulse), impulse));
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    bluetoothToggle.setIconEnabled(true, true);
                    impulse = 0;
                    impulseBar.setProgress(impulse);
                    impulseText.setText(String.format(getResources().getString(R.string.impulse), impulse));
                    connectedDevice.setText(mConnectedDeviceName);
                    TSnackbar tSnackbar = TSnackbar.make(findViewById(android.R.id.content), mConnectedDeviceName + "에 연결되었습니다.", TSnackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (tSnackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        mainTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    else
                        mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    tSnackbar.show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private View buzzerButton;
    private SwitchIconView buzzerToggle;
    private View adjustButton;
    private SwitchIconView adjustToggle;
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    if (D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };

    @SuppressWarnings("Convert2Lambda")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        impulseContent = (TextView) findViewById(R.id.impulse_content);
        impulseContent.setText(String.format(getResources().getString(R.string.impulse_content), maximumImpulse));

        mContext = this;

        connectedDevice = (TextView) findViewById(R.id.connected_device);

        impulseBar = (RoundCornerProgressBar) findViewById(R.id.progress);
        impulseBar.setProgressColor(getResources().getColor(R.color.fbutton_color_alizarin));
        impulseBar.setProgressBackgroundColor(Color.parseColor("#808080"));
        impulseBar.setMax(maximumImpulse);
        impulseBar.setProgress(0);
        impulseBar.setRadius(25);

        buzzerButton = (View) findViewById(R.id.buzzer_button);
        buzzerToggle = (SwitchIconView) findViewById(R.id.buzzer_toggle);
        buzzerButton.setClickable(true);
        buzzerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    bluetoothToggle.setIconEnabled(false, true);
                    connectedDevice.setText("");

                    TSnackbar tSnackbar = TSnackbar.make(findViewById(android.R.id.content), R.string.not_connected, TSnackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (tSnackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        mainTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    else
                        mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    tSnackbar.show();
                } else {
                    sendMessage("buzz");
                }
            }
        });

        bluetoothButton = (View) findViewById(R.id.bluetooth_button);
        bluetoothToggle = (SwitchIconView) findViewById(R.id.bluetooth_toggle);
        bluetoothButton.setClickable(true);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    bluetoothToggle.setIconEnabled(false, true);
                    connectedDevice.setText("");
                }
                Intent serverIntent;
                serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        adjustButton = (View) findViewById(R.id.adjust_button);
        adjustToggle = (SwitchIconView) findViewById(R.id.adjust_toggle);
        adjustButton.setClickable(true);
        adjustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdjustDialog customDialog = new AdjustDialog(MainActivity.this);
                // 커스텀 다이얼로그를 호출한다.
                // 커스텀 다이얼로그의 결과를 출력할 TextView를 매개변수로 같이 넘겨준다.
                customDialog.callFunction(maximumImpulse);
                customDialog.setDialogListener(new AdjustDialog.DialogListener() {  // MyDialogListener 를 구현
                    @Override
                    public void onPositiveClicked(int progress) {
                        maximumImpulse = progress;
                        impulseContent.setText(String.format(getResources().getString(R.string.impulse_content), maximumImpulse));
                        impulseBar.setMax(maximumImpulse);
                    }
                });
            }
        });

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            TSnackbar tSnackbar = TSnackbar.make(findViewById(android.R.id.content), "블루투스를 지원하지 않는 장치입니다.", TSnackbar.LENGTH_LONG);
            TextView mainTextView = (TextView) (tSnackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                mainTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            else
                mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            tSnackbar.show();
            return;
        }

        impulseText = findViewById(R.id.impulse);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ((TextView) new AlertDialog.Builder(this)
                            .setTitle("런타임 권한 요청")
                            .setMessage(Html.fromHtml("<p>주변에 있는 블루투스 장치를 검색하려면 위치 기반 권한을 필요로 합니다.</p>" +
                                    "<p>자세한 내용은 <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">여기</a>에서 확인하실 수 있습니다.</p>"))
                            .setNeutralButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                                    }
                                }
                            })
                            .show()
                            .findViewById(android.R.id.message))
                            .setMovementMethod(LinkMovementMethod.getInstance());
                    // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    Intent serverIntent = null;
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    break;
            }
        }
    }

    public void reset() {
        impulse = 0;
        impulseBar.setProgress(impulse);
        impulseText.setText(String.format(getResources().getString(R.string.impulse), impulse));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult


        //이하 에뮬레이터 테스트 주석
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }


    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the send button with a listener that for click events
        mDropButton = (FButton) findViewById(R.id.button_drop);
        mDropButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                sendMessage("drop");
            }
        });

        mGripButton = (FButton) findViewById(R.id.button_grip);
        mGripButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                sendMessage("grip");
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            bluetoothToggle.setIconEnabled(false, true);
            connectedDevice.setText("");
            TSnackbar tSnackbar = TSnackbar.make(findViewById(android.R.id.content), R.string.not_connected, TSnackbar.LENGTH_LONG);
            TextView mainTextView = (TextView) (tSnackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                mainTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            else
                mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            mainTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            tSnackbar.show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    private final void setStatus(int resId) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

}
