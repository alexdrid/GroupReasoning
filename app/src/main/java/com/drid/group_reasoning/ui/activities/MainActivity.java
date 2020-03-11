package com.drid.group_reasoning.ui.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.data.contracts.PeerContract;
import com.drid.group_reasoning.ui.fragments.KnowledgeFragment;
import com.drid.group_reasoning.ui.fragments.LogicSolverFragment;
import com.drid.group_reasoning.ui.fragments.PeerListFragment;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.network.NearbyService;
import com.drid.group_reasoning.network.model.Peer;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        PeerListFragment.OnFragmentInteractionListener,
        LogicSolverFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";


    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String CHANNEL_ID = "group_reasoning_notification_channel";

    public NearbyService service;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            NearbyService.NearbyBinder nearbyBinder = (NearbyService.NearbyBinder) iBinder;
            service = nearbyBinder.getService();
            Log.i(TAG, "onServiceConnected: Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            Log.i(TAG, "onServiceConnected: Service disconnected");
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals("incoming_connection")) {
                Peer peer = intent.getExtras().getParcelable("incoming_peer");
                assert peer != null;
                displayConnectToPeerDialog(peer);
            }else if (action.equals("new_query")){
                if(!(active instanceof LogicSolverFragment)){
                    addNotificationBadge();
                }
                createNotification(intent.getStringExtra("notification_message"));
            }

        }
    };

    private int notificationCounter = 0;

    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment peerListFragment = new PeerListFragment();
    Fragment knowledgeFragment = new KnowledgeFragment();
    Fragment logicSolverFragment = new LogicSolverFragment();
    Fragment active = peerListFragment;

    private BottomNavigationView bottomNavigationView;

    private BottomNavigationView.OnNavigationItemSelectedListener bottomNavigationListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.nav_nearby_devices:
                            fragmentManager.beginTransaction().hide(active)
                                    .show(peerListFragment)
                                    .commit();
                            active = peerListFragment;
                            break;
                        case R.id.nav_knowledge:
                            fragmentManager.beginTransaction().hide(active)
                                    .show(knowledgeFragment)
                                    .commit();
                            active = knowledgeFragment;
                            break;
                        case R.id.nav_logic_resolver:
                            if(notificationCounter > 0 ){
                                removeNotificationBadge();
                            }
                            fragmentManager.beginTransaction().hide(active)
                                    .show(logicSolverFragment)
                                    .commit();
                            active = logicSolverFragment;
                            break;
                    }

                    return true;
                }

            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("incoming_connection");
        intentFilter.addAction("new_query");

        registerReceiver(this.receiver, intentFilter);


        fragmentManager.beginTransaction().add(R.id.fragment_container, logicSolverFragment, "logic_solver").commit();
        fragmentManager.beginTransaction().add(R.id.fragment_container, knowledgeFragment, "knowledge").commit();
        fragmentManager.beginTransaction().add(R.id.fragment_container, peerListFragment, "peer_list").commit();
        fragmentManager.beginTransaction().hide(logicSolverFragment).commit();
        fragmentManager.beginTransaction().hide(knowledgeFragment).commit();

        Toolbar tb = findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavigationListener);

    }



    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                            this,
                            getRequiredPermissions(),
                            REQUEST_CODE_REQUIRED_PERMISSIONS);
                } else {
                    requestPermissions(
                            getRequiredPermissions(),
                            REQUEST_CODE_REQUIRED_PERMISSIONS);
                }
            }
        }
        createNotificationChannel();
        startService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        getContentResolver().delete(
                PeerContract.PeerEntry.PEER_URI,
                null,
                null);

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof PeerListFragment) {
            PeerListFragment peerListFragment = (PeerListFragment) fragment;
            peerListFragment.setOnFragmentCreatedListener(this);
        } else if (fragment instanceof LogicSolverFragment) {
            LogicSolverFragment logicSolverFragment = (LogicSolverFragment) fragment;
            logicSolverFragment.setOnFragmentCreatedListener(this);
        }
    }

    @Override
    public void discoverPeers() {
        this.service.startDiscovering();
    }

    @Override
    public void connectToPeer(Peer peer) {
        this.service.connectToPeer(peer);
    }

    @Override
    public void disconnectFromPeer(Peer selectedPeer) {
        this.service.disconnectFromPeer(selectedPeer);
    }

    @Override
    public void disconnectFromAllPeers() {
        this.service.disconnectFromAllPeers();
    }

    @Override
    public void askPeers(List<AtomicSentence> missingFacts) {
        this.service.askPeers(missingFacts);
    }


    public static String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    private boolean hasPermissions(Context context, String... requiredPermissions) {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, NearbyService.class);
        startService(serviceIntent);
        bindService();
    }

    private void bindService() {
        Intent serviceIntent = new Intent(this, NearbyService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void displayConnectToPeerDialog(final Peer peer) {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.AlertDialog))
                .setTitle("Do you want to connect to " + peer.getName() + "?")
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        service.acceptConnection(peer.getPeerId());
                        insertPeer(peer);
                    }
                }).setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        service.rejectConnection(peer.getPeerId());
                    }
                }).create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    private void insertPeer(Peer peer) {
        ContentValues values = new ContentValues();
        values.put(PeerContract.PeerEntry.COLUMN_PEER_ID, peer.getPeerId());
        values.put(PeerContract.PeerEntry.COLUMN_PEER_NAME, peer.getName());
        values.put(PeerContract.PeerEntry.COLUMN_PEER_STATUS, peer.getStatus());
        getContentResolver().insert(PeerContract.PeerEntry.PEER_URI, values);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void createNotification(String message){

        NotificationCompat.Builder builder
                = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle("New message arrived!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager
                = NotificationManagerCompat.from(getApplicationContext());

        notificationManager.notify(1, builder.build());
    }


    public void addNotificationBadge(){
        notificationCounter++;
        BottomNavigationMenuView menuView =
                (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        View view = menuView.getChildAt(2);

        BottomNavigationItemView menuItemView = (BottomNavigationItemView) view;

        View notification_badge = LayoutInflater.from(this).inflate(
                R.layout.layout_notification_badge,
                menuView,
                false);

        TextView badgeTextView = notification_badge.findViewById(R.id.badge_text_view);

        badgeTextView.setText(String.valueOf(notificationCounter));
        menuItemView.addView(notification_badge);
    }

    public void removeNotificationBadge(){
        BottomNavigationMenuView menuView =
                (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        View view = menuView.getChildAt(2);

        BottomNavigationItemView menuItemView = (BottomNavigationItemView) view;

        menuItemView.removeViewsInLayout(2, notificationCounter);
        notificationCounter = 0;
    }


}
