package com.drid.group_reasoning.network;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.drid.group_reasoning.data.contracts.PeerContract;
import com.drid.group_reasoning.engine.inference.InferenceEngine;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.network.model.Message;
import com.drid.group_reasoning.network.model.Peer;
import com.drid.group_reasoning.network.model.QueryHistoryMessage;
import com.drid.group_reasoning.network.model.QueryMessage;
import com.drid.group_reasoning.network.model.ResponseHistoryMessage;
import com.drid.group_reasoning.network.model.ResponseMessage;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.drid.group_reasoning.network.model.Peer.AVAILABLE;
import static com.drid.group_reasoning.network.model.Peer.CONNECTED;
import static com.drid.group_reasoning.network.model.Peer.CONNECTING;

public class NearbyService extends Service {

    private static final String TAG = NearbyService.class.getSimpleName();
    private String username;
    private static final String SERVICE_ID = "nearby_service";

    private IBinder binder = new NearbyBinder();

    private ConnectionsClient connectionsClient;

    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private final Map<String, Peer> discoveredPeers = new HashMap<>();
    private final Map<String, Peer> pendingConnections = new HashMap<>();
    private final Map<String, Peer> connectedPeers = new HashMap<>();

    Map<AtomicSentence, List<String>> queryHistory = new HashMap<>();

    List<AtomicSentence> tempFoundFacts =
            Collections.synchronizedList(new ArrayList<AtomicSentence>());


    private boolean isWaiting = false;

    public boolean isHistoryUpdated = false;

    private EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        @NonNull final String endpointId,
                        @NonNull final DiscoveredEndpointInfo info) {


                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (getServiceId().equals(info.getServiceId())) {

                                if (!discoveredPeers.containsKey(endpointId)) {
                                    final Peer peer =
                                            new Peer(endpointId, info.getEndpointName(), AVAILABLE);

                                    discoveredPeers.put(endpointId, peer);


                                    if (!connectedPeers.containsKey(endpointId)) {
                                        insertPeer(peer);
                                    }
                                }

                            }
                        }
                    }).start();

                }

                @Override
                public void onEndpointLost(@NonNull String s) {
                    Peer peer = discoveredPeers.remove(s);
                    deletePeer(peer);
                }
            };

    private ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(
                        @NonNull final String peerId, @NonNull final ConnectionInfo info) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "onConnectionInitiated: connecting to "
                                    + info.getEndpointName());


                            final Peer peer = new Peer(peerId, info.getEndpointName(), CONNECTING);

                            pendingConnections.put(peerId, peer);

                            if (info.isIncomingConnection()) {


                                Intent intent = new Intent();
                                intent.setAction("incoming_connection");
                                intent.putExtra("incoming_peer", peer);
                                sendBroadcast(intent);

                                Log.i(TAG, "Incoming connection from "
                                        + info.getEndpointName());

                            } else {
                                acceptConnection(peer.getPeerId());
                            }
                        }
                    }).start();

                }


                @Override
                public void onConnectionResult(
                        @NonNull final String peerId, @NonNull final ConnectionResolution resolution) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!resolution.getStatus().isSuccess()) {
                                String errorMsg = String.format(
                                        "Connection failed. Received status %s.",
                                        resolution.getStatus());
                                Log.w(TAG, errorMsg);
                                Peer peer = pendingConnections.remove(peerId);

                                deletePeer(peer);

                            } else {
                                Peer connectedPeer = pendingConnections.remove(peerId);
                                connectedPeer.setStatus(CONNECTED);
                                connectedPeers.put(peerId, connectedPeer);
                                updatePeer(connectedPeer);

                                String msg = String.format("Connected to %s", connectedPeer.getName());
                                Log.i(TAG, msg);
                            }
                        }
                    }).start();
                }

                @Override
                public void onDisconnected(@NonNull String peerId) {
                    Peer peer = connectedPeers.remove(peerId);
                    deletePeer(peer);
                }
            };

    private PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String senderId, @NonNull final Payload payload) {
            Log.i(TAG, "onPayloadReceived: Payload received thread " + Thread.currentThread());
            NearbyService.this.onPayloadReceived(senderId, payload);
        }

        @Override
        public void onPayloadTransferUpdate(
                @NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        connectionsClient = Nearby.getConnectionsClient(this);

        username = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext()).getString("username", "");
        startAdvertising();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        disconnectFromAllPeers();
        stopSelf();
    }

    private void startAdvertising() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String localName = getName();
                connectionsClient.startAdvertising(
                        localName,
                        getServiceId(),
                        connectionLifecycleCallback,
                        new AdvertisingOptions.Builder()
                                .setStrategy(getStrategy())
                                .build())
                        .addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unusedResult) {
                                        String msg = "Now advertising endpoint " + localName;
                                        Log.d(TAG, msg);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        String msg = "startAdvertising() failed.";
                                        Log.w(TAG, msg, e);
                                    }
                                });

            }
        }).start();

    }

    public void startDiscovering() {

        Toast.makeText(
                getApplicationContext(),
                "Discovery started",
                Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                discoveredPeers.clear();
                deleteAvailablePeers();
                connectionsClient.startDiscovery(
                        getServiceId(),
                        endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder()
                                .setStrategy(getStrategy())
                                .build())
                        .addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        String msg = "startDiscovery success";
                                        Log.i(TAG, msg);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        String msg = "startDiscovery failed: " + e;
                                        Log.e(TAG, msg, e);
                                        stopDiscovery();
                                    }
                                });
            }
        }).start();

    }

    private void stopDiscovery() {
        connectionsClient.stopDiscovery();
    }

    public void acceptConnection(final String peerId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectionsClient
                        .acceptConnection(peerId, payloadCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                String logMsg = "acceptConnection() success.";
                                Log.w(TAG, logMsg);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        String errorMsg = "acceptConnection() failed.";
                                        Log.w(TAG, errorMsg, e);
                                    }
                                });
            }
        }).start();
    }

    public void rejectConnection(final String peerId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectionsClient.rejectConnection(peerId)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                String logMsg = "rejectConnection() success.";
                                Log.w(TAG, logMsg);
                            }
                        });
            }
        }).start();

    }

    public void connectToPeer(final Peer peer) {
        String message = String.format("Attempting to connect to %s", peer.getName());
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopDiscovery();
                connectionsClient.requestConnection(
                        getName(),
                        peer.getPeerId(),
                        connectionLifecycleCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i(TAG, "requestConnection success");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "requestConnection failed " + e);
                            }
                        });
            }
        }).start();

    }

    public void disconnectFromPeer(Peer peer) {
        connectionsClient.disconnectFromEndpoint(peer.getPeerId());
        connectedPeers.remove(peer.getPeerId());
    }

    public void disconnectFromAllPeers() {
        for (String peerId : connectedPeers.keySet()) {
            connectionsClient.disconnectFromEndpoint(peerId);
        }
        discoveredPeers.clear();
        connectedPeers.clear();

        deleteAllPeers();

    }

    private void insertPeer(Peer peer) {
        ContentValues values = new ContentValues();
        values.put(PeerContract.PeerEntry.COLUMN_PEER_ID, peer.getPeerId());
        values.put(PeerContract.PeerEntry.COLUMN_PEER_NAME, peer.getName());
        values.put(PeerContract.PeerEntry.COLUMN_PEER_STATUS, peer.getStatus());

        getContentResolver().insert(PeerContract.PeerEntry.PEER_URI, values);
    }

    private void updatePeer(Peer peer) {
        ContentValues values = new ContentValues();
        values.put(PeerContract.PeerEntry.COLUMN_PEER_STATUS, peer.getStatus());

        Uri peerUri =
                PeerContract.PeerEntry.PEER_URI.buildUpon().appendPath(PeerContract.PATH_PEER_ID).build();


        getContentResolver().update(
                peerUri,
                values,
                PeerContract.PeerEntry.COLUMN_PEER_ID + "=?",
                new String[]{peer.getPeerId()});
    }

    private void deletePeer(Peer peer) {
        getContentResolver().delete(
                PeerContract.PeerEntry.PEER_URI,
                PeerContract.PeerEntry.COLUMN_PEER_ID + "=?",
                new String[]{peer.getPeerId()});
    }

    private void deleteAllPeers() {
        getContentResolver().delete(
                PeerContract.PeerEntry.PEER_URI,
                null,
                null);
    }

    private void deleteAvailablePeers() {
        getContentResolver().delete(
                PeerContract.PeerEntry.PEER_URI,
                PeerContract.PeerEntry.COLUMN_PEER_STATUS + "=?",
                new String[]{Peer.AVAILABLE});
    }

    public void askPeers(final List<AtomicSentence> missingFacts) {

        Log.i(TAG, "askPeers: query history" + queryHistory);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (connectedPeers.isEmpty()) {
                    Log.i(TAG, "askPeers: No connected peers");
                    Intent intent = new Intent("no_connected_peers");
                    sendBroadcast(intent);
                } else {

                    List<String> recipientPeers = new ArrayList<>(connectedPeers.keySet());

                    Message message = new QueryMessage(username, missingFacts);
                    Payload payload = null;

                    try {
                        byte[] bytes = toByteArray(message);
                        payload = Payload.fromBytes(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.i(TAG, "askPeers: peers to ask " + recipientPeers);
                    connectionsClient.sendPayload(new ArrayList<>(recipientPeers), payload)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i(TAG, "onSuccess: payload transferred");
                                    Toast.makeText(getApplicationContext(), "payload transferred", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "onFailure: payload not transferred");
                                    Toast.makeText(getApplicationContext(), "payload not transferred", Toast.LENGTH_SHORT).show();
                                }
                            });


                    for (AtomicSentence fact : missingFacts) {
                        queryHistory.put(fact, recipientPeers);
                    }
                    Log.i(TAG, "askPeers: query history" + queryHistory);

                    waitForResponses(missingFacts);
                }

            }
        });

        t.start();


    }

    private void askPeers(
            final String sender,
            final String senderId,
            final List<AtomicSentence> requestedFacts,
            final List<AtomicSentence> missingFacts,
            final List<String> peersToAsk) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message query = new QueryMessage(username, missingFacts);

                Payload payload = null;

                try {
                    byte[] bytes = toByteArray(query);
                    payload = Payload.fromBytes(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                Log.i(TAG, "askPeers': peers to ask" + peersToAsk);
                if (peersToAsk.isEmpty()) {
                    Log.i(TAG, "No peers to ask " + missingFacts);
                    Log.i(TAG, "found facts so far " + tempFoundFacts);
                    Intent intent = new Intent("log_message");
                    intent.putExtra("message","No peers to ask ");
                    sendBroadcast(intent);
                    sendResponse(sender, senderId, tempFoundFacts);
                } else {
                    Log.i(TAG, "Asking " + peersToAsk);
                    connectionsClient.sendPayload(peersToAsk, payload)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i(TAG, "onSuccess: payload transferred");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "onFailure: payload not transferred");
                                }
                            });

                    Intent intent = new Intent("log_message");
                    intent.putExtra("message","Asking peers");
                    sendBroadcast(intent);
                    waitForResponses(sender,senderId, requestedFacts);
                }
            }
        }).start();

    }

    private void searchFacts(
            String sender, final String senderId, final List<AtomicSentence> requestedFacts) {

        Log.i(TAG, "searchFacts: Searching for missing facts " + requestedFacts);


        for (AtomicSentence fact : tempFoundFacts) {
            if (requestedFacts.contains(fact)) {
                requestedFacts.remove(fact);
            }
        }

        if (requestedFacts.isEmpty()) {
            Log.i(TAG, "searchFacts: All requested facts are already found ");
        } else {
            InferenceEngine engine = new InferenceEngine(getApplicationContext());
            engine.initKnowledgeBase();

            engine.solve(requestedFacts);

            List<AtomicSentence> resolvedQueries = engine.getResolvedQueries();
            List<AtomicSentence> unresolvedQueries = engine.getUnresolvedQueries();
            Set<AtomicSentence> notInferred = engine.getMissingFacts();
            StringBuilder builder = new StringBuilder();
            if (unresolvedQueries.isEmpty()) {
                Log.i(TAG, "searchFacts: All requested facts found: " + resolvedQueries);
                for (AtomicSentence fact : resolvedQueries) {
                    if (!requestedFacts.contains(fact)) {
                        resolvedQueries.remove(fact);
                    }
                }
                builder.append("Resolved queries:\n");

                for(AtomicSentence sentence : resolvedQueries){
                    builder.append("- " + sentence.getProposition() + "\n");
                }
                String log_message = builder.toString();
                Intent intent = new Intent("log_message");
                intent.putExtra("message", log_message);
                sendBroadcast(intent);

                sendResponse(sender,senderId, resolvedQueries);
            } else {
                Log.i(TAG, "searchFacts: Resolved queries : " + resolvedQueries);
                Log.i(TAG, "searchFacts: Unresolved queries: " + unresolvedQueries);
                Log.i(TAG, "searchFacts: Missing facts: " + notInferred);

                builder.append("Resolved queries:\n");

                for(AtomicSentence sentence : resolvedQueries){
                    builder.append("- " + sentence.getProposition() + "\n");
                }
                builder.append("Unresolved queries:\n");

                for(AtomicSentence sentence : unresolvedQueries){
                    builder.append("- " + sentence.getProposition() + "\n");
                }
                builder.append("Missing facts:\n");

                for(AtomicSentence sentence : notInferred){
                    builder.append("- " + sentence.getProposition() + "\n");
                }
                String log_message = builder.toString();

                Intent intent = new Intent("log_message");
                intent.putExtra("message", log_message);
                sendBroadcast(intent);

                List<AtomicSentence> factsToAsk = new ArrayList<>();

                for (AtomicSentence requested : requestedFacts) {
                    for (AtomicSentence resolved : resolvedQueries) {
                        if (resolved.getProposition().equals(requested.getProposition())) {
                            tempFoundFacts.add(resolved);
                        }
                    }
                }


                for (AtomicSentence query : unresolvedQueries) {
                    if (requestedFacts.contains(query)) {
                        factsToAsk.add(query);
                    }
                }

                requestQueryHistory(sender,senderId, requestedFacts, factsToAsk);
            }
        }
    }

    private void waitForResponses(final List<AtomicSentence> missingFacts) {
        isWaiting = true;
        Log.i(TAG, "waitForResponses: waiting for 2 seconds");
        Runnable waitRunnable = new Runnable() {
            @Override
            public void run() {
                while (isWaiting) {
                    if (missingFacts.size() == tempFoundFacts.size()) {
                        Log.i(TAG, "waitForResponses: All requested facts found");
                        isWaiting = false;
                        addNewFactsToKb();
                    }
                }
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(waitRunnable);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                Log.i(TAG, "waitForResponses: i can't wait any longer");
                Log.i(TAG, "waitForResponses: stopping " + tempFoundFacts);
                Intent intent = new Intent();
                intent.setAction("new_facts");
                intent.putParcelableArrayListExtra(
                        "facts",
                        new ArrayList<>(tempFoundFacts));
                sendBroadcast(intent);

            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void waitForResponses(
            final String sender,
            final String senderId,
            final List<AtomicSentence> missingFacts) {
        isWaiting = true;
        Log.i(TAG, "waitForResponses: waiting for 2 seconds");
        Runnable waitRunnable = new Runnable() {
            @Override
            public void run() {
                while (isWaiting) {
                    if (missingFacts.size() == tempFoundFacts.size()) {
                        Log.i(TAG, "waitForResponses: All requested facts found");
                        isWaiting = false;

                        sendResponse(sender, senderId, tempFoundFacts);
                    }
                }
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(waitRunnable);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                Log.i(TAG, "waitForResponses: i can't wait any longer");
                sendResponse(sender, senderId, tempFoundFacts);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void onPayloadReceived(final String senderId, final Payload payload) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "onPayloadReceived: New Message Received");

                Message message = null;
                try {
                    message = toObject(payload.asBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if (message instanceof QueryMessage) {
                    String sender = ((QueryMessage) message).getSender();
                    List<AtomicSentence> requestedFacts = ((QueryMessage) message).getRequestedFacts();
                    Log.i(TAG, "Query = {Sender:" + sender
                            + ", requesting facts: " + requestedFacts + "}");


                    StringBuilder builder = new StringBuilder();
                    builder.append(sender).append(" is asking for these facts:");
                    builder.append("\n");
                    for (AtomicSentence s : requestedFacts) {
                        builder.append("- " + s.getProposition() + "\n");
                    }

                    Intent log_message_intent = new Intent("log_message");
                    String queryMessage = builder.toString();
                    log_message_intent.putExtra("message", queryMessage);
                    sendBroadcast(log_message_intent);


                    Intent notification_intent = new Intent("new_query");
                    String notificationMessage = sender + " is asking for facts";
                    notification_intent.putExtra("notification_message", notificationMessage);
                    sendBroadcast(notification_intent);


                    searchFacts(sender,senderId, requestedFacts);
                } else if (message instanceof QueryHistoryMessage) {
                    Log.i(TAG, ((QueryHistoryMessage) message).getSender() + " is requesting history");
                    replyQueryHistory(senderId);
                } else if (message instanceof ResponseMessage) {

                    List<AtomicSentence> receivedFacts = ((ResponseMessage) message).getFoundFacts();

                    Log.i(TAG, "Response = {Sender:"
                            + ((ResponseMessage) message).getSender()
                            + " Received facts: "
                            + ((ResponseMessage) message).getFoundFacts() + "}");

                    tempFoundFacts.addAll(receivedFacts);
                    Log.i(TAG, "onPayloadReceived: tempFoundFacts " + tempFoundFacts);


                } else if (message instanceof ResponseHistoryMessage) {
                    setHistoryUpdated(true);
                    Log.i(TAG, ((ResponseHistoryMessage) message).getSender() + " has sent  history");
                    queryHistory.putAll(((ResponseHistoryMessage) message).getQueryHistory());
                }

            }
        });

        thread.start();


    }

    private void waitForHistory(
            final String sender,
            final String senderId,
            final List<AtomicSentence> requestedFacts,
            final List<AtomicSentence> factsToAsk) {
        isWaiting = true;
        Log.i(TAG, "waitForHistory: waiting for history for 2 seconds");
        Runnable waitRunnable = new Runnable() {
            @Override
            public void run() {
                while (isWaiting) {
                    if (isHistoryUpdated) {
                        Log.i(TAG, "waitForHistory: query history is updated");
                        isWaiting = false;
                        List<String> peersToAsk = new ArrayList<>(connectedPeers.keySet());

                        peersToAsk.remove(senderId);

                        for (Map.Entry<AtomicSentence, List<String>> entry : queryHistory.entrySet()) {
                            AtomicSentence factAsked = entry.getKey();
                            List<String> peersAsked = entry.getValue();

                            if (factsToAsk.contains(factAsked)) {
                                for (String peerId : peersToAsk) {
                                    if (peersAsked.contains(peerId)) {
                                        peersToAsk.remove(peerId);
                                    }
                                }
                            }

                        }
                        askPeers(sender,senderId, requestedFacts, factsToAsk, peersToAsk);
                    }
                }
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(waitRunnable);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                Log.i(TAG, "waitForHistory: i can't wait any longer for history");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void sendResponse(String sender, String senderId, final List<AtomicSentence> facts) {

        Log.i(TAG, "sendResponse: sending " + facts + " to " + senderId);

        StringBuilder builder = new StringBuilder();

        builder.append("Sending to " + sender + " these facts\n");

        for(AtomicSentence sentence : facts){
            builder.append("- " + sentence.getProposition() + "\n");
        }

        String log_message = builder.toString();

        Intent intent = new Intent("log_message");
        intent.putExtra("message",log_message);

        sendBroadcast(intent);

        Message message = new ResponseMessage(username, facts);
        Payload payload = null;

        try {
            byte[] bytes = toByteArray(message);
            payload = Payload.fromBytes(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }


        connectionsClient.sendPayload(senderId, payload)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "onSuccess: payload transferred");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "onFailure: payload not transferred");
            }
        });

        tempFoundFacts.removeAll(facts);
    }

    private void requestQueryHistory(
            final String sender,
            final String senderId,
            List<AtomicSentence> requestedFacts,
            List<AtomicSentence> factsToAsk) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "requestQueryHistory: requesting query history from " + senderId);

                Message message = new QueryHistoryMessage(username);

                Payload payload = null;

                try {
                    byte[] bytes = toByteArray(message);
                    payload = Payload.fromBytes(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                connectionsClient.sendPayload(senderId, payload)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i(TAG, "onSuccess: payload transferred");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "onFailure: payload not transferred");
                    }
                });
            }

        }).start();

        waitForHistory(sender,senderId, requestedFacts, factsToAsk);
    }

    private void replyQueryHistory(final String senderId) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "replyQueryHistory : sending query history to " + senderId);

                Message message = new ResponseHistoryMessage(username, queryHistory);

                Payload payload = null;

                try {
                    byte[] bytes = toByteArray(message);
                    payload = Payload.fromBytes(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                connectionsClient.sendPayload(senderId, payload)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i(TAG, "onSuccess: payload transferred");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "onFailure: payload not transferred");
                    }
                });


            }
        }).start();
    }

    private void addNewFactsToKb() {
        Log.i(TAG, "addNewFactsToKb: temp facts " + tempFoundFacts);
        Intent intent = new Intent();
        intent.setAction("new_facts");
        intent.putParcelableArrayListExtra(
                "facts",
                new ArrayList<>(tempFoundFacts));
        sendBroadcast(intent);
        tempFoundFacts.clear();
    }

    public byte[] toByteArray(Message message) throws IOException {
        byte[] bytes;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    public Message toObject(byte[] bytes) throws IOException, ClassNotFoundException {
        Message obj;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = (Message) ois.readObject();

        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }

    public String getName() {
        return username;
    }

    public static String getServiceId() {
        return SERVICE_ID;
    }

    public static Strategy getStrategy() {
        return STRATEGY;
    }


    public void setHistoryUpdated(boolean historyUpdated) {
        isHistoryUpdated = historyUpdated;
    }

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }
}
