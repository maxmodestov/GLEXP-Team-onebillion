package org.onebillion.onecourse.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import org.onebillion.onecourse.mainui.MainActivity;
import org.onebillion.onecourse.mainui.generic.OC_Generic;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.regex.Pattern;

/**
 * Created by pedroloureiro on 30/08/16.
 */
public class OBConnectionManager
{
    public static OBConnectionManager sharedManager;
    //
    private BroadcastReceiver airplaneChangedReceiver;
    private BroadcastReceiver scanResultsReceiver;
    private int targetNetworkID;
    private double timeStampForConnectionAttempt;
    private WifiManager.WifiLock wifiLock;
    private Runnable timeoutRunnable;
    private ReentrantLock completionLock;


    public OBConnectionManager ()
    {
        sharedManager = this;
        timeStampForConnectionAttempt = -1;
    }

    public String wifiSSID ()
    {
        return MainActivity.mainActivity.configStringForKey(MainActivity.CONFIG_WIFI_SSID);
    }

    public String wifiPassword ()
    {
        return MainActivity.mainActivity.configStringForKey(MainActivity.CONFIG_WIFI_PASSWORD);
    }

    public void startupConnection (final OBUtils.RunLambda block)
    {
        MainActivity.log("OBConnectionManager.checkForConnection");
        ConnectivityManager connManager = (ConnectivityManager) MainActivity.mainActivity.getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        Network networks[] = connManager.getAllNetworks();
        boolean wifiFoundAndConnected = false;
        for (Network network : networks)
        {
            NetworkInfo info = connManager.getNetworkInfo(network);
            if (info.getType() == ConnectivityManager.TYPE_WIFI && info.isConnected())
            {
                wifiFoundAndConnected = true;
            }
        }
        if (!wifiFoundAndConnected)
        {
            OBExpansionManager.sharedManager.connectToWifiDialog();
            //
            OBUtils.runOnOtherThread(new OBUtils.RunLambda()
            {
                @Override
                public void run () throws Exception
                {
                    MainActivity.log("Wifi not connected. Attempting to activate and connect");
                    // attempt to connect to wifi
                    connectToNetwork_scanForWifi(wifiSSID(), wifiPassword(), block);
                }
            });
        }
        else
        {
            MainActivity.log("OBConnectionManager.wifi is already setup, continuing.");
            if (block != null)
            {
                try
                {
                    MainActivity.log("OBConnectionManager.startupConnection. running completion block");
                    OBUtils.runOnOtherThreadDelayed(1.0f, block);
                }
                catch (Exception e)
                {
                    MainActivity.log("OBConnectionManager.startupConnection.exception caught while running completion block");
//                    e.printStackTrace();
                }
            }
        }
    }

    public void disconnectWifi ()
    {
        MainActivity.log("OBConnectionManager.disconnectWifi");
        WifiManager wifiManager = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(MainActivity.WIFI_SERVICE);
        MainActivity.log("OBConnectionManager.disconnectWifi. Disconnected network");
        wifiManager.disconnect();
        MainActivity.log("OBConnectionManager.disconnectWifi. Disabling wifi");
        wifiManager.setWifiEnabled(false);
    }


    public Boolean keepWifiOn ()
    {
        String keepWifiOn = MainActivity.mainActivity.configStringForKey(MainActivity.CONFIG_KEEP_WIFI_ON);
        if (keepWifiOn != null && keepWifiOn.equals("false"))
        {
            return false;
        }
        return true;
    }


    public String getCurrentWifiSSID()
    {
        WifiManager wifiManager = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(MainActivity.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo ();
        String ssid = info.getSSID();
        if (info.getSupplicantState() != SupplicantState.COMPLETED)
        {
            MainActivity.log("OBConnectionManager:getCurrentWifiSSID. not connected to current wifi. returning null");
            return null;
        }
        if (ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"')
        {
            return ssid.substring(1, ssid.length() - 1);
        }
        else
        {
            return ssid;
        }
    }


    public boolean isScanningDisabled ()
    {
        WifiManager wifiManager = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(MainActivity.WIFI_SERVICE);
        if (wifiManager.isScanAlwaysAvailable()) return false;
        //
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) return false;
        //
        return true;
    }


    public void disconnectWifiIfAllowed ()
    {
        if (!keepWifiOn())
        {
            WifiManager wifiManager = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(MainActivity.WIFI_SERVICE);
            wifiManager.setWifiEnabled(false);
        }
    }

    public boolean setBluetooth (boolean enable)
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null)
        {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (enable && !isEnabled)
            {
                return bluetoothAdapter.enable();
            }
            else if (!enable && isEnabled)
            {
                return bluetoothAdapter.disable();
            }
            return true;
        }
        return false;
    }

    /*
    public void connectToNetwork (final String ssid, final String password, final Boolean disconnectAfterBlock, final OBUtils.RunLambda block)
    {
        final WifiManager wifiManager = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //
        try
        {
            MainActivity.log("OBConnectionManager:connectToNetwork.disable airplane mode");
            Settings.Global.putInt(MainActivity.mainActivity.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            MainActivity.mainActivity.sendBroadcast(intent);
        }
        catch (Exception e)
        {
            MainActivity.log("OBConnectionManager:connectToNetwork:Exception Caught while trying to disable airplane mode");
//            e.printStackTrace();
        }
        //
        wifiManager.setWifiEnabled(true);
        //
        OBSystemsManager.unregisterReceiver(scanResultsReceiver);
        scanResultsReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive (Context context, Intent intent)
            {
                WifiInfo newInfo = wifiManager.getConnectionInfo();
                MainActivity.log("OBConnectionManager.connectToNetwork.scanResultsReceiver.NETWORK_STATE_CHANGED_ACTION.info --> " + newInfo.toString());
                //
                if (newInfo.getSupplicantState() == SupplicantState.COMPLETED)
                {
                    MainActivity.log("OBConnectionManager.connectToNetwork. Wifi is now connected. Go to connectToNetWork_complete");
                    OBSystemsManager.sharedManager.getMainHandler().removeCallbacks(timeoutRunnable);
                    connectToNetWork_complete(true, new OBUtils.RunLambda()
                    {
                        @Override
                        public void run () throws Exception
                        {
                            if (getCompletionLock().tryLock())
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork. WITH the completion lock. running block");
                                block.run();
                                if (disconnectAfterBlock)
                                {
                                    connectToNetwork_disconnect();
                                }
                                getCompletionLock().unlock();
                            }
                            else
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork. WITHOUT the completion lock. exiting.");
                            }
                        }
                    });
                }
            }
        };
        //
        WifiConfiguration wc = new WifiConfiguration();
        //
        wc.allowedAuthAlgorithms.clear();
        wc.allowedGroupCiphers.clear();
        wc.allowedKeyManagement.clear();
        wc.allowedPairwiseCiphers.clear();
        wc.allowedProtocols.clear();
        wc.SSID = quoteNonHex(ssid);
        wc.hiddenSSID = false;
        //
        if (password != null)
        {
            wc.preSharedKey = quoteNonHex(password, 64);
            wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        }
        else
        {
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        wc.status = WifiConfiguration.Status.ENABLED;
        //
        int id = wifiManager.updateNetwork(wc);
        if (id == -1)
        {
            MainActivity.log("OBConnectionManager:connectToNetwork: error while updating wifi network");
            id = wifiManager.addNetwork(wc);
            if (id == -1)
            {
                MainActivity.log("OBConnectionManager:connectToNetwork: error while adding wifi network");
            }
        }
        //
        if (id != -1)
        {
            MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            //
            wifiManager.saveConfiguration();
            //
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list)
            {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\""))
                {
                    MainActivity.log("OBConnectionManager:connectToNetwork:found SSID " + i.SSID + ". Connecting");
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }
        }
        else
        {
            try
            {
                if (getCompletionLock().tryLock())
                {
                    MainActivity.log("OBConnectionManager.connectToNetwork. WITH the completion lock. running block");
                    block.run();
                    if (disconnectAfterBlock)
                    {
                        connectToNetwork_disconnect();
                    }
                    getCompletionLock().unlock();
                }
            }
            catch (Exception e)
            {
                MainActivity.log("OBConnectionManager:connectToNetwork:exception caught while calling completion block after failing to connect to wifi");
                e.printStackTrace();
            }
        }
//        wifiManager.disconnect();
//        wifiManager.enableNetwork(id, true);
//        wifiManager.reconnect();
    }
    */


    public void connectToNetwork_unused (final String ssid, final String password, final OBUtils.RunLambda block)
    {
        if (timeStampForConnectionAttempt != -1)
        {
            MainActivity.log("OBConnectionManager.connectToNetwork an attempt is already in progress. Aborting");
            return;
        }
        //
        timeStampForConnectionAttempt = OC_Generic.currentTime();
        MainActivity.log("OBConnectionManager.connectToNetwork [" + ssid + "] [" + password + "]");
        if (ssid == null)
        {
            MainActivity.log("OBConnectionManager.connectToNetwork SSID is null. Aborting and not running completion block");
            connectToNetWork_complete(false, block);
            return;
        }
        //
        MainActivity.log("OBConnectionManager.connectToNetwork. disabling captive_portal_detection_enabled");
        Settings.Global.putInt(MainActivity.mainActivity.getContentResolver(), "captive_portal_detection_enabled", 0);
        try
        {
            MainActivity.log("OBConnectionManager.connectToNetwork. captive_portal_detection_enabled value " + Settings.Global.getInt(MainActivity.mainActivity.getContentResolver(), "captive_portal_detection_enabled"));
        }
        catch (Exception e)
        {
            MainActivity.log("OBConnectionManager.connectToNetwork. captive_portal_detection_enabled value NOT FOUND");
        }
        //
        OBSystemsManager.unregisterReceiver(airplaneChangedReceiver);
        airplaneChangedReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive (Context context, Intent intent)
            {
                MainActivity.log("OBConnectionManager.connectToNetwork.airplaneChangedReceiver " + intent.getAction());
                //
                if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED))
                {
                    MainActivity.log("OBConnectionManager.connectToNetwork.airplaneChangedReceiver it's related to AIRPLANE_MODE");
                    //
                    boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                    //
                    MainActivity.mainActivity.unregisterReceiver(airplaneChangedReceiver);
                    if (isAirplaneModeOn)
                    {
                        MainActivity.log("OBConnectionManager.connectToNetwork.airplaneChangedReceiver.AIRPLANE_MODE is now ON. Unlocking everything.");
                        connectToNetWork_complete(false, block);
                    }
                    else
                    {
                        MainActivity.log("OBConnectionManager.connectToNetwork.airplaneChangedReceiver.AIRPLANE_MODE is now OFF");
                        connectToNetwork_enableWifi(ssid, password, block);
                    }
                }
            }
        };
        //
        MainActivity.mainActivity.registerReceiver(airplaneChangedReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        //
        MainActivity.log("OBConnectionManager.connectToNetwork.disable airplane mode");
        Settings.Global.putInt(MainActivity.mainActivity.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", false);
        MainActivity.mainActivity.sendBroadcast(intent);
    }


    public void connectToNetwork_enableWifi (final String ssid, final String password, final OBUtils.RunLambda block)
    {
        final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wfMgr.createWifiLock("LockTag");
        wifiLock.acquire();
        //
        if (!wfMgr.isWifiEnabled())
        {
            OBSystemsManager.unregisterReceiver(scanResultsReceiver);
            scanResultsReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive (Context context, Intent intent)
                {
                    MainActivity.log("OBConnectionManager.connectToNetwork_enableWifi.scanResultsReceiver.WIFI_STATE_CHANGED_ACTION");
                    //
                    if (wfMgr.isWifiEnabled())
                    {
                        MainActivity.log("OBConnectionManager.connectToNetwork_enableWifi.scanResulsReceiver. Wifi is now enabled. Attempting to connect again");
                        //
                        OBSystemsManager.unregisterReceiver(scanResultsReceiver);
                        connectToNetwork_enableWifi(ssid, password, block);
                    }
                }
            };
            MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            wfMgr.setWifiEnabled(true);
        }
        else
        {
            connectToNetwork_connectToWifi(ssid, password, block);
        }
    }


    public void connectToNetWork_complete (boolean success, final OBUtils.RunLambda block)
    {
        final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //
        if (success)
        {
            if (block != null)
            {
                try
                {
                    MainActivity.log("OBConnectionManager.connectToNetWork_complete. running completion block");
                    OBUtils.runOnOtherThreadDelayed(1.0f, block);
                }
                catch (Exception e)
                {
                    MainActivity.log("OBConnectionManager.connectToNetWork_complete.exception caught while running completion block");
//                    e.printStackTrace();
                }
            }
            else
            {
                MainActivity.log("OBConnectionManager.connectToNetWork_complete.block is empty. nothing to do");
                //
                connectToNetwork_disconnect();
            }
        }
        if (wifiLock != null)
        {
            wifiLock.release();
        }
        //
        timeStampForConnectionAttempt = -1;
    }


    public void connectToNetwork_disconnect ()
    {
        final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //
        MainActivity.log("OBConnectionManager.connectToNetWork_complete.block complete");
        //
        MainActivity.log("OBConnectionManager.connectToNetWork_complete.disable wifi");
        wfMgr.setWifiEnabled(false);
        //
        try
        {
            MainActivity.log("OBConnectionManager.connectToNetWork_complete.enable airplane mode");
            Settings.Global.putInt(MainActivity.mainActivity.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", true);
            MainActivity.mainActivity.sendBroadcast(intent);
        }
        catch (Exception e)
        {
            MainActivity.log("Exception caught while trying to set the airplane mode");
//            e.printStackTrace();
        }
    }


    public void connectToNetwork_connectToWifi (final String ssid, final String password, final OBUtils.RunLambda block)
    {
        final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //
        String connectionSSID = wfMgr.getConnectionInfo().getSSID();
        if (connectionSSID.startsWith("\"") && connectionSSID.endsWith("\""))
        {
            connectionSSID = connectionSSID.substring(1, connectionSSID.length() - 1);
        }
        //
        SupplicantState connectionState = wfMgr.getConnectionInfo().getSupplicantState();
        if (connectionSSID.equals(ssid) && connectionState == SupplicantState.COMPLETED)
        {
            MainActivity.log("OBConnectionManager.connectToNetwork_connectToWifi. already connected to the network");
            connectToNetWork_complete(true, block);
        }
        else
        {
            MainActivity.log("OBConnectionManager.connectToNetwork_connectToWifi. Wifi not connected. Go to scan for wifi");
            connectToNetwork_scanForWifi(ssid, password, block);
        }
    }


    void connectToNetwork_scanForWifi (final String ssid, final String password, final OBUtils.RunLambda block)
    {
        final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //
        OBSystemsManager.unregisterReceiver(scanResultsReceiver);
        scanResultsReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive (Context context, Intent intent)
            {
                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.scanResultsReceiver.onReceive");
                //
                final WifiConfiguration wfc = new WifiConfiguration();
                wfc.SSID = "\"".concat(ssid).concat("\"");
                wfc.status = WifiConfiguration.Status.DISABLED;
                wfc.priority = 40;
                //
                List<ScanResult> networkList = wfMgr.getScanResults();
                //
                if (networkList != null)
                {
                    for (ScanResult network : networkList)
                    {
                        MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.available network: " + network.toString());
                        //
                        if (ssid.equals(network.SSID))
                        {
                            String capabilities = network.capabilities;
                            MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi." + network.SSID + " capabilities : " + capabilities);
                            //
                            if (capabilities.contains("WPA"))
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.WPA");
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                                wfc.preSharedKey = "\"".concat(password).concat("\"");
                            }
                            else if (capabilities.contains("WEP"))
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.WEP");
                                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                                wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                                wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                                wfc.wepKeys[0] = "\"".concat(password).concat("\"");
                                wfc.wepTxKeyIndex = 0;
                            }
                            else
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.Other");
                                wfc.BSSID = network.BSSID;
                              /*  wfc.allowedAuthAlgorithms.clear();
                                wfc.allowedGroupCiphers.clear();
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                                wfc.allowedKeyManagement.clear();*/
                                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                               /* wfc.allowedPairwiseCiphers.clear();
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                wfc.allowedProtocols.clear();
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                                wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);*/
                            }
                        }
                    }
                }
                //
                final int networkId = wfMgr.addNetwork(wfc);
                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.networkID " + networkId);
                //
                if (networkId != -1)
                {
                    OBSystemsManager.unregisterReceiver(scanResultsReceiver);
                    scanResultsReceiver = new BroadcastReceiver()
                    {
                        @Override
                        public void onReceive (Context context, Intent intent)
                        {
                            WifiInfo newInfo = wfMgr.getConnectionInfo();
                            MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.scanResultsReceiver.NETWORK_STATE_CHANGED_ACTION.info --> " + newInfo.toString());
                            //
                            if (newInfo.getSupplicantState() == SupplicantState.COMPLETED)
                            {
                                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi. Wifi is now connected. Go to connectToWifi");
                                OBSystemsManager.sharedManager.getMainHandler().removeCallbacks(timeoutRunnable);
                                connectToNetwork_connectToWifi(ssid, password, block);
                            }
//                            else if (newInfo.getSupplicantState() == SupplicantState.INACTIVE)
//                            {
//                                MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi. detected INACTIVE for target Wifi. Attempting to reconnect");
//                                wfMgr.disconnect();
//                                wfMgr.enableNetwork(networkId, true);
//                                wfMgr.reconnect();
////                                connectToNetwork_connectToWifi(ssid, password, block);
//                            }
                        }
                    };
                    timeoutRunnable = new Runnable()
                    {
                        @Override
                        public void run ()
                        {
                            MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.timeoutRunnable.timeout occured. Scanning for Wifi again.");
                            connectToNetwork_scanForWifi(ssid, password, block);
//                            MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.timeoutRunnable.timeout occured. cancelling attempting to backup database");
//                            connectToNetWork_complete(false, block);
                        }
                    };
                    //
                    boolean result = wfMgr.saveConfiguration();
                    if (!result)
                        MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.PROBLEM saving configuration");
                    //
                    MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                    MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.enabling network");
                    //
                    wfMgr.disconnect();
                    wfMgr.enableNetwork(networkId, true);
                    wfMgr.reconnect();
                    //
                    OBSystemsManager.sharedManager.getMainHandler().postDelayed(timeoutRunnable, 60000);
                }
                else
                {
                    MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi. PROBLEM while adding network to manager");
                    connectToNetWork_complete(false, block);
                }
            }
        };
        MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.registering receiver");
        MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //
        MainActivity.log("OBConnectionManager.connectToNetwork_scanForWifi.starting scan");
        wfMgr.setWifiEnabled(true);
        wfMgr.disconnect();
        wfMgr.startScan();
    }


    public Lock getCompletionLock ()
    {
        if (completionLock == null)
        {
            completionLock = new ReentrantLock();
        }
        return completionLock;
    }


//    public void connectToNetwork(final String ssid, final String password, final OBUtils.RunLambda block)
//    {
//        MainActivity.log("OBConnectionManager.connectToNetwork [" + ssid + "] [" + password + "]");
//        if (ssid == null)
//        {
//            MainActivity.log("OBConnectionManager.connectToNetwork SSID is null. Aborting and not running completion block");
//            return;
//        }
//        //
//        BroadcastReceiver receiver = new BroadcastReceiver()
//        {
//            @Override
//            public void onReceive (Context context, Intent intent)
//            {
//                if (intent.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED))
//                {
//                    final WifiManager wfMgr = (WifiManager) MainActivity.mainActivity.getSystemService(Context.WIFI_SERVICE);
//                    //
//                    if (!wfMgr.isWifiEnabled())
//                    {
//                        OBSystemsManager.unregisterReceiver(scanResultsReceiver);
//                        scanResultsReceiver = new BroadcastReceiver()
//                        {
//                            @Override
//                            public void onReceive (Context context, Intent intent)
//                            {
//                                MainActivity.log("OBConnectionManager.receiver.WIFI_STATE_CHANGED_ACTION");
//                                //
//                                if (wfMgr.isWifiEnabled())
//                                {
//                                    MainActivity.log("OBConnectionManager.connectToNetwork. Wifi is now enabled. Attempting to connect again");
//                                    //
//                                    OBSystemsManager.unregisterReceiver(scanResultsReceiver);
//                                    connectToNetwork(ssid, password, block);
//                                }
//                            }
//                        };
//                        MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
//                        wfMgr.setWifiEnabled(true);
//                    }
//                    else
//                    {
//                        WifiInfo newInfo = wfMgr.getConnectionInfo();
//                        if (newInfo.getSSID().equals(ssid) && newInfo.getSupplicantState() == SupplicantState.COMPLETED)
//                        {
//                            MainActivity.log("OBConnectionManager.connectToNetwork. already connected to the network");
//                            //
//                            if (block != null)
//                            {
//                                try
//                                {
//                                    MainActivity.log("OBConnectionManager.connectToNetwork. running completion block");
//                                    OBUtils.runOnOtherThreadDelayed(1.0f, block);
//                                    //
//                                    Settings.Global.putString(MainActivity.mainActivity.getContentResolver(), "airplane_mode_on", "1");
//                                }
//                                catch (Exception e)
//                                {
//                                    MainActivity.log("OBConnectionManager.connectToNetwork.exception caught while running completion block");
//                                    e.printStackTrace();
//                                }
//                            }
//                            else
//                            {
//                                MainActivity.log("OBConnectionManager.connectToNetwork.block is empty. nothing to do");
//                            }
//                        }
//                        else
//                        {
//                            OBSystemsManager.unregisterReceiver(scanResultsReceiver);
//                            scanResultsReceiver = new BroadcastReceiver()
//                            {
//                                @Override
//                                public void onReceive (Context context, Intent intent)
//                                {
//                                    MainActivity.log("OBConnectionManager.connectToNetwork.scanResultsReceiver.onReceive");
//                                    //
//                                    WifiConfiguration wfc = new WifiConfiguration();
//                                    wfc.SSID = "\"".concat(ssid).concat("\"");
//                                    wfc.status = WifiConfiguration.Status.DISABLED;
//                                    wfc.priority = 40;
//                                    //
//                                    List<ScanResult> networkList = wfMgr.getScanResults();
//                                    //
//                                    if (networkList != null)
//                                    {
//                                        for (ScanResult network : networkList)
//                                        {
////                            MainActivity.log("OBConnectionManager.connectToNetwork.available network: " + network.toString());
//                                            //
//                                            if (ssid.equals(network.SSID))
//                                            {
//                                                String capabilities = network.capabilities;
//                                                MainActivity.log(network.SSID + " capabilities : " + capabilities);
//                                                //
//                                                if (capabilities.contains("WPA"))
//                                                {
////                                    MainActivity.log("OBConnectionManager.connectToNetwork.WPA");
//                                                    wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                                                    wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                                                    wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//                                                    wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                                                    wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//                                                    wfc.preSharedKey = "\"".concat(password).concat("\"");
//                                                }
//                                                else if (capabilities.contains("WEP"))
//                                                {
////                                    MainActivity.log("OBConnectionManager.connectToNetwork.WEP");
//                                                    wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//                                                    wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                                                    wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                                                    wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//                                                    wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
//                                                    wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                                                    wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                                                    wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//                                                    wfc.wepKeys[0] = "\"".concat(password).concat("\"");
//                                                    wfc.wepTxKeyIndex = 0;
//                                                }
//                                                else
//                                                {
////                                    MainActivity.log("OBConnectionManager.connectToNetwork.Other");
//                                                    wfc.BSSID = network.BSSID;
//                                                    wfc.allowedAuthAlgorithms.clear();
//                                                    wfc.allowedGroupCiphers.clear();
//                                                    wfc.allowedKeyManagement.clear();
//                                                    wfc.allowedPairwiseCiphers.clear();
//                                                    wfc.allowedProtocols.clear();
//                                                }
//                                            }
//                                        }
//                                    }
//                                    //
//                                    int networkId = wfMgr.addNetwork(wfc);
////                    MainActivity.log("OBConnectionManager.connectToNetwork.networkID " + networkId);
//                                    //
//                                    if (networkId != -1)
//                                    {
//                                        OBSystemsManager.unregisterReceiver(scanResultsReceiver);
//                                        scanResultsReceiver = new BroadcastReceiver()
//                                        {
//                                            @Override
//                                            public void onReceive (Context context, Intent intent)
//                                            {
//                                                WifiInfo newInfo = wfMgr.getConnectionInfo();
//                                                MainActivity.log("OBConnectionManager.receiver.NETWORK_STATE_CHANGED_ACTION.info --> " + newInfo.toString());
//                                                //
//                                                if (newInfo.getSupplicantState() == SupplicantState.COMPLETED)
//                                                {
//                                                    MainActivity.log("Wifi is now connected!!");
//                                                    OBSystemsManager.unregisterReceiver(scanResultsReceiver);
//                                                    //
//                                                    if (block != null)
//                                                    {
//                                                        try
//                                                        {
//                                                            MainActivity.log("OBConnectionManager.connectToNetwork. running completion block");
//                                                            OBUtils.runOnOtherThreadDelayed(1.0f, block);
//                                                            //
//                                                            Settings.Global.putString(MainActivity.mainActivity.getContentResolver(), "airplane_mode_on", "1");
//                                                        }
//                                                        catch (Exception e)
//                                                        {
//                                                            MainActivity.log("OBConnectionManager.connectToNetwork.exception caught while running completion block");
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                                    else
//                                                    {
//                                                        MainActivity.log("OBConnectionManager.connectToNetwork.block is empty. nothing to do");
//                                                    }
//                                                }
//                                            }
//                                        };
//                                        //
//                                        MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
//                                        MainActivity.log("OBConnectionManager.connectToNetwork.enabling network");
//                                        wfMgr.enableNetwork(networkId, true);
//                                    }
//                                }
//                            };
//                            MainActivity.log("OBConnectionManager.connectToNetwork.registering receiver");
//                            MainActivity.mainActivity.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//                            MainActivity.log("OBConnectionManager.connectToNetwork.starting scan");
//                            wfMgr.disconnect();
//                            wfMgr.startScan();
//                        }
//                    }
//                }
//            }
//        };
//        MainActivity.mainActivity.registerReceiver(receiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
//        //
//        MainActivity.log("OBConnectionManager.connectToNetwork.disable airplane mode");
//        Settings.Global.putInt(MainActivity.mainActivity.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
//        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//        intent.putExtra("state", false);
//        MainActivity.mainActivity.sendBroadcast(intent);
//    }



    private static String convertToQuotedString (String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        // If already quoted, return as-is
        if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
        {
            return s;
        }
        return '\"' + s + '\"';
    }

    private static String quoteNonHex (String value, int... allowedLengths)
    {
        return isHexOfLength(value, allowedLengths) ? value : convertToQuotedString(value);
    }



    private static boolean isHexOfLength (CharSequence value, int... allowedLengths)
    {
        final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");
        //
        if (value == null || !HEX_DIGITS.matcher(value).matches())
        {
            return false;
        }
        if (allowedLengths.length == 0)
        {
            return true;
        }
        for (int length : allowedLengths)
        {
            if (value.length() == length)
            {
                return true;
            }
        }
        return false;
    }
}
