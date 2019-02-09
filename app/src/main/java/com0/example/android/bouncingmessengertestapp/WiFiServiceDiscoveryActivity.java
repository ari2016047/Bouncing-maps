package com0.example.android.bouncingmessengertestapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.wifidirect.discovery.R;
import com.google.android.gms.maps.model.LatLng;

import com0.example.android.bouncingmessengertestapp.WiFiChatFragment.MessageTarget;
import com0.example.android.bouncingmessengertestapp.WiFiDirectServicesList.DeviceClickListener;
import com0.example.android.bouncingmessengertestapp.WiFiDirectServicesList.WiFiDevicesAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import static android.app.PendingIntent.getActivity;

/**
 * This activity registers a local service and
 * perform discovery over Wi-Fi p2p network. It also hosts a couple of fragments
 * to manage chat operations. When the app is launched, the device publishes a
 * chat service and also tries to discover services published by other peers. On
 * selecting a peer published service, the app initiates a Wi-Fi P2P (Direct)
 * connection with the peer. On successful connection with a peer advertising
 * the same service, the app opens up sockets to initiate a chat.
 * {@code WiFiChatFragment} is then added to the the main activity which manages
 * the interface and messaging needs for a chat session.
 */
public class WiFiServiceDiscoveryActivity extends Activity implements
        DeviceClickListener,Handler.Callback, MessageTarget,
        ConnectionInfoListener, WifiP2pManager.GroupInfoListener{

    public static final String TAG = "New Internet";
    //public static Button b1;
    //public static Button b2;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_Bouncing_Messenger";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static String mem_info = "";
    public static final String usercode = "#@@!";
    public static String uname = new String();
    public static String PORT_FLAG = "OFF";

    private static final String GO = "Group Owner";
    private final String disconnectMessage = "&^#973^&^##(&#(455DISCONNECTED";
    private ArrayList userList = new ArrayList();
    private Intent starterIntent ;
    private WifiManager wifiManager;
    private WifiP2pManager manager;
    static final int SERVER_PORT = 4545;
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private Handler handler = new Handler(this);
    private WiFiChatFragment chatFragment;
    private WiFiDirectServicesList servicesList;
    private WiFiDirectServicesList servicesList2;
    private static TextView statusTxtView;
    private Intent intentGroup;
    private boolean isWifiP2pEnabled = false;
    private int memberCount = 1;
    private GroupChatManager groupChatManager;
    private Button disconnect;
    private int distance;
    private Route locRou = null;

    //private ChatManager chatManager;

    final Handler hand = new Handler();

    public Handler getHandler() {
        return handler;
    }
    public void setHandler(Handler handler) {
        this.handler = handler;
    }


    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        SharedPreferences preferences = getSharedPreferences("MYPREFS", MODE_PRIVATE);
        uname = preferences.getString("username", "");
        //starterIntent = getIntent();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!(wifiManager.isWifiEnabled())){
            createalert();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        groupChatManager = new GroupChatManager();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        statusTxtView = (TextView) findViewById(R.id.status_text);
        appendStatus("***** Welcome "+uname+" *****");
        //userList.add(uname);
        disconnect = (Button)findViewById(R.id.disconnect);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();
        servicesList = new WiFiDirectServicesList();
        //servicesList2 = new WiFiDirectServicesList();
        getFragmentManager().beginTransaction()
                .add(R.id.container_root, servicesList, "services").commit();
        OnClickButtonListener();
        //getFragmentManager().beginTransaction().add(R.id.container_root2, servicesList2, "services2").commit();
        //chatFragmentTag = new WiFiChatFragment();
        //getFragmentManager().beginTransaction().add(R.id.container_root2,chatFragmentTag,"chats").commit();
        //OnClickButtonListener();
        //OnClickButtonListener1();
    }

    public void OnClickButtonListener(){
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectService();
            }
        });
    }

    /*public void OnClickButtonListener()
     {
       b1 = (Button)findViewById(R.id.button2);
       b1.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Intent intent = new Intent("com0.example.android.bouncingmessengertestapp.add_contacts");
               startActivity(intent);
           }
       });
     }*/

    /*public void OnClickButtonListener1()
    {
        b2 = (Button)findViewById(R.id.button3);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("com0.example.android.bouncingmessengertestapp.Contacts");
                startActivity(intent);
            }
        });
    }*/

    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }
    @Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Disconnected on Stop");
                }
            });
        }
        super.onStop();
    }
    /*@Override
    public void onBackPressed(){
        Intent i = new Intent(WiFiServiceDiscoveryActivity.this, WiFiServiceDiscoveryActivity.class);
        startActivity(i);
        ((Activity) WiFiServiceDiscoveryActivity.this).overridePendingTransition(0,0);

    }*/
    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE+":"+uname, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }
            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });
        discoverService();
    }
    private void discoverService() {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        manager.setDnsSdResponseListeners(channel,
                new DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {
                        // A service has been discovered. Is this our app?
                        String[] ser_ins = instanceName.split(":");
                        if (ser_ins[0].equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // update the UI and add the item the discovered
                            // device.
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            //WiFiDirectServicesList fragment2 = (WiFiDirectServicesList) getFragmentManager().findFragmentByTag("services2");
                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                if (adapter.isEmpty()) {
                                    Log.d(TAG,"empty "+service.device.deviceName);
                                    adapter.add(service);
                                    adapter.notifyDataSetChanged();
                                }
                                else {
                                    for (int bazoka = 0; bazoka < adapter.getCount(); bazoka++) {
                                        Log.d(TAG,"loop "+adapter.getItem(bazoka).device.deviceName
                                                +" " + service.device.deviceName);
                                        if (!adapter.getItem(bazoka).device.deviceName.
                                                equals(service.device.deviceName)) {
                                            Log.d(TAG,"service added "+service.device.deviceName);
                                            adapter.add(service);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                                    Log.d(TAG, "onBonjourServiceAvailable "
                                            + instanceName);
                            }
                        }
                    }
                }, new DnsSdTxtRecordListener() {
                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });
        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {
                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }
                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }
            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");
            }
        });
    }
    @Override
    public void connectP2p(final WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {
                        @Override
                        public void onSuccess() {
                        }
                        @Override
                        public void onFailure(int arg0) {
                        }
                    });
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connected to service");
                appendStatus("Connected to service");
                //intentGroup = new Intent(WiFiServiceDiscoveryActivity.this,GroupActivity.class);
                //intentGroup.putExtra("member_detail", (Serializable) service);
            }
            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG, "failed connecting to service");
                appendStatus("Failed connecting to service");
            }
        });
    }
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "inside handleMessage"  + readMessage);
                if(readMessage.startsWith(disconnectMessage)){
                    Log.d(TAG, "DISCONNECT MESSAGE FOUND");
                    if(readMessage.contains(usercode)){
                        readMessage = readMessage.replace(disconnectMessage+usercode,"");
                        Log.d(TAG, "GOT THE USERNAME"+readMessage);
                    }
                    if(mem_info.equals(GO)){
                        userList.remove(readMessage);
                        //userList.remove(uname);
                        Log.d(TAG, "USERNAME REMOVED"+userList.toString());
                        if(memberCount<=2){
                            Log.d(TAG,"disconnecting as GO");
                            disconnectService();
                        }
                        else{
                            groupChatManager.setUserList(userList);
                            (chatFragment).forEveryone((usercode+userList.toString()).getBytes());
                            printUserList(userList.toString());
                        }
                    }
                    else{
                        Log.d(TAG, "disconnecting as peer");
                        disconnectService();
                    }
                    break;
                }
                if(readMessage.startsWith(usercode) && mem_info.equals(GO))
                {
                    Log.d(TAG, " username is received to group owner now printing the list" + readMessage);
                    readMessage = stripcodeg(readMessage);
                    userList.add(readMessage);
                    groupChatManager.setUserList(userList);
                    (chatFragment).forEveryone((usercode+userList.toString()).getBytes());
                    printUserList(userList.toString()+memberCount);
                    appendStatus("you are connected as Group Owner");
                    break;
                }
                else if(readMessage.contains(usercode)){
                    Log.d(TAG, "LIST OF MEMBERS FOUND RECEIVED TO CLIENT " + readMessage);
                    readMessage = stripcode(readMessage);
                    userList.clear();
                    userList.add(readMessage);
                    printUserList(userList.toString());
                    appendStatus("you are connected as Client");
                    break;
                }
                String user[] = readMessage.split(":");
                if (user[0].equals(uname)){
                    //Log.d(TAG, "redundant information will be printed" + readMessage);
                    //chatFragment.pushMessage(readMessage);
                    int nevertobeuserinthiscode = 0;
                }

                else {
                    String[] var = readMessage.split("\n");
                    if(var[0].contains("Location Request") && mem_info.equals(GO)){
                        var[1] = var[1].substring(5);
                        var[2] = var[2].substring(3);
                        Log.d(TAG,"this is new msg"+var[1]+var[2]);
                        locRou = Directions(var[1],var[2]);
                    }
                    Log.d(TAG, "NONE OF THE IF OR ELSE IF STATEMENT EXECUTED SO NORMAL MESSAGE"+readMessage);
                    (chatFragment).pushMessage(readMessage);
                }
                if(mem_info.equals(GO) && !readMessage.startsWith(usercode)
                        && !readMessage.startsWith(disconnectMessage)){
                Log.d(TAG, "Message will be forwarded by group owner to all members" + readMessage);
                if(locRou!=null) {
                    List<Segment> arr = locRou.getSegments();
                    for (Segment seg : arr) {
                        Log.d("print", seg.getInstruction());
                        Log.d("print", String.valueOf(seg.getLength()));
                        String dir = "";
                        dir = seg.getInstruction() + String.valueOf(seg.getLength()) + "\n";
                        chatFragment.forEveryone(dir.getBytes());
                    }
                    locRou = null;
                }
                chatFragment.forEveryone(readMessage.getBytes());
            }
                break;
            case MY_HANDLE:
                Object obj = msg.obj;
                if(mem_info.equals(GO))
                    (chatFragment).setGroupChatManager((GroupChatManager) obj);
                else{
                    (chatFragment).setChatManager((ChatManager) obj);
                }
        }
        return true;
    }
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            mem_info = GO;
            try{
            handler = new GroupOwnerSocketHandler(
                    ((MessageTarget) this).getHandler());
            handler.start();}
            catch(Exception ex){
                Log.d(TAG,"failed to create server thread");
            }
                serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
                manager.addServiceRequest(channel, serviceRequest,
                        new ActionListener() {
                            @Override
                            public void onSuccess() {
                                appendStatus( "No of members in the group = " + String.valueOf(memberCount));
                            }
                            @Override
                            public void onFailure(int arg0) {
                                appendStatus("Failed adding service discovery request");
                            }
                        });
                if(PORT_FLAG.equals("OFF")) {
                    Log.d(TAG,"chat fragment started");
                    PORT_FLAG="ON";
                    chatFragment = new WiFiChatFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container_root2, chatFragment).commit();
                    disconnect.setVisibility(View.VISIBLE);
                    userList.add(uname);
                }
        } else {
            Log.d(TAG, "Connected as peer");
            mem_info = "Client";
            handler = new ClientSocketHandler(
                    ((MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
            chatFragment = new WiFiChatFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_root2, chatFragment).commit();
            disconnect.setVisibility(View.VISIBLE);

        }
                //statusTxtView.setVisibility(View.GONE);
        //getFragmentManager().beginTransaction().replace(R.id.container_root2, chatFragment).commit();
        //chatFragment = (WiFiChatFragment) getFragmentManager().findFragmentByTag("chats");
        //statusTxtView.setVisibility(View.GONE);
        //if(intentGroup!=null)
            //startActivity(intentGroup);
        }
        /*else{
            Log.d(TAG, "Connected as peer");
            mem_info = "Group Owner";
        }*/
    public static void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }
    public static void clearappendstatus(){
        statusTxtView.setText("***** Welcome "+uname+" *****");
    }
    public void printUserList(String s){
        if(s!=null){
            clearappendstatus();
            appendStatus(s);
        }
    }
    /*public void when_disconnected(){
        discoverService();
    }*/
    public void createalert(){
            AlertDialog.Builder wifialert = new AlertDialog.Builder(WiFiServiceDiscoveryActivity.this);
            wifialert.setMessage("The app won't work without the wifi service").setCancelable(false)
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            wifiManager.setWifiEnabled(true);
                        }
                    })
                    .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            AlertDialog alert = wifialert.create();
            alert.setTitle("permission");
            alert.show();
    }
    public String stripcode(String s){
        char[] ch = s.toCharArray();
        String temp="";
        for(int i = 1;i<ch.length-1;i++){
            if(!(ch[i]=='#' || ch[i]=='@' || ch[i]=='!' || ch[i]=='['))
                temp = temp + ch[i];
        }
        return temp;
    }
    public String stripcodeg(String s){
        char[] ch = s.toCharArray();
        String temp="";
        for(int i = 1;i<ch.length;i++){
            if(!(ch[i]=='#' || ch[i]=='@' || ch[i]=='!' || ch[i]=='['))
                temp = temp + ch[i];
        }
        return temp;
    }
    public void disconnectService(){
        if(manager!=null && channel!=null){
            if(mem_info.equals(GO)){
                chatFragment.forEveryone(disconnectMessage.getBytes());
                Log.d(TAG,"sending disconnect message");
            }
            else{
                chatFragment.forGO((disconnectMessage+usercode+uname).getBytes());
                Log.d(TAG,"sending disconnect message client");
            }
            manager.removeGroup(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    if(chatFragment!=null && mem_info.equals(GO)){
                        if (serviceRequest != null)
                            manager.removeServiceRequest(channel, serviceRequest,
                                    new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "on button click disconnect the service request " +
                                                    "of GO removed successfully");
                                        }
                                        @Override
                                        public void onFailure(int arg0) {
                                            Log.d(TAG,"on disconnect failed to remove service request of GO");
                                        }
                                    });
                        mem_info = "";
                        getFragmentManager().beginTransaction().remove(chatFragment).commit();
                        Fragment frag = getFragmentManager().findFragmentByTag("services");
                        if (frag != null) {
                            getFragmentManager().beginTransaction().remove(frag).commit();
                            servicesList.listAdapter.clear();
                            servicesList = new WiFiDirectServicesList();
                            getFragmentManager().beginTransaction()
                                    .add(R.id.container_root, servicesList, "services").commit();
                        }
                        clearappendstatus();
                        Toast.makeText(WiFiServiceDiscoveryActivity.this, "DISCONNECTED SUCCESSFULLY",
                                Toast.LENGTH_SHORT).show();
                        startRegistrationAndDiscovery();
                    }
                    else if(chatFragment!=null){
                        if (serviceRequest != null)
                            manager.removeServiceRequest(channel, serviceRequest,
                                    new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "on disconnect the service request " +
                                                    "of client removed successfully");
                                        }
                                        @Override
                                        public void onFailure(int arg0) {
                                            Log.d(TAG, "on disconnect the service request " +
                                                    "of client removed failed");
                                        }
                                    });
                        mem_info = "";
                        userList.clear();
                        getFragmentManager().beginTransaction().remove(chatFragment).commit();
                        Fragment frag = getFragmentManager().findFragmentByTag("services");
                        if (frag != null) {
                            getFragmentManager().beginTransaction().remove(frag).commit();
                            servicesList.listAdapter.clear();
                            servicesList = new WiFiDirectServicesList();
                            getFragmentManager().beginTransaction()
                                    .add(R.id.container_root, servicesList, "services").commit();
                        }
                        clearappendstatus();
                        Toast.makeText(WiFiServiceDiscoveryActivity.this, "DISCONNECTED SUCCESSFULLY",
                                Toast.LENGTH_SHORT).show();
                        startRegistrationAndDiscovery();
                        PORT_FLAG="OFF";
                    }
                }

                @Override
                public void onFailure(int reason) {
                    appendStatus("Disconnect failed. Reason :" + reason);
                }
            });
            disconnect.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        if(wifiP2pGroup.isGroupOwner()){
            try{
                Collection<WifiP2pDevice> deviceLists = wifiP2pGroup.getClientList();
                int count = 0;
                for(WifiP2pDevice peer : deviceLists){
                    Log.d(TAG,peer.deviceName);
                    count++;
                }
                memberCount = count + 1;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public Route Directions(String a, String b) {
        try {
            String url = "https://maps.googleapis.com/maps/api/directions/json?origin="+a+"&destination="+b+"&key=AIzaSyAw52XLZKxTNsTh-K2rRnBjF_7zW7Gvz_4";
            //String ip = "74.125.45.100";
            //String key = "9d64fcfdfacc213csfsfc7ddsf4ef911dfe97b55e4fdsf696be3532bf8302876c09ebad06b";
            //String url = "http://api.ipinfodb.com/v3/ip-city/?key=" + key + "&ip=" + ip + "&format=json";

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String response1 = response.toString();

            final Route route = new Route();
            final Segment segment = new Segment();

            try {
                final JSONObject json = new JSONObject(response1);
                //Get the route object
                final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
                //Get the leg, only one leg as we don't support waypoints
                final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
                //Get the steps for this leg
                final JSONArray steps = leg.getJSONArray("steps");
                //Number of steps for use in for loop
                final int numSteps = steps.length();
                //Set the name of this route using the start & end addresses
                route.setName(leg.getString("start_address") + " to " + leg.getString("end_address"));
                //Get google's copyright notice (tos requirement)
                route.setCopyright(jsonRoute.getString("copyrights"));
                //Get distance and time estimation
                route.setDurationText(leg.getJSONObject("duration").getString("text"));
                route.setDistanceText(leg.getJSONObject("distance").getString("text"));
                route.setEndAddressText(leg.getString("end_address"));
                //Get the total length of the route.
                route.setLength(leg.getJSONObject("distance").getInt("value"));
                //Get any warnings provided (tos requirement)
                if (!jsonRoute.getJSONArray("warnings").isNull(0)) {
                    route.setWarning(jsonRoute.getJSONArray("warnings").getString(0));
                }
                /* Loop through the steps, creating a segment for each one and
                 * decoding any polylines found as we go to add to the route object's
                 * map array. Using an explicit for loop because it is faster!
                 */
                for (int i = 0; i < numSteps; i++) {
                    //Get the individual step
                    final JSONObject step = steps.getJSONObject(i);
                    //Get the start position for this step and set it on the segment
                    final JSONObject start = step.getJSONObject("start_location");
                    final LatLng position = new LatLng(start.getDouble("lat"),
                            start.getDouble("lng"));
                    segment.setPoint(position);
                    //Set the length of this segment in metres
                    final int length = step.getJSONObject("distance").getInt("value");
                    distance += length;
                    segment.setLength(length);
                    segment.setDistance(distance / 1000);
                    //Strip html from google directions and set as turn instruction
                    segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
                    //Retrieve & decode this segment's polyline and add it to the route.
                    route.addPoints(decodePolyLine(step.getJSONObject("polyline").getString("points")));
                    //Push a copy of the segment to the route
                    route.addSegment(segment.copy());
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            /*List<Segment> arr = route.getSegments();
            for (Segment seg:arr) {
                Log.d("print",seg.getInstruction());
                Log.d("print",String.valueOf(seg.getLength()));
            }*/
            return route;
            //Gson g = new Gson();
            //Player p = g.fromJson(response1,Player.class);

            //Log.d("Printing", route.toString());

        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
    }
}