package com.devstepbcn.wifi;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;
// import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import java.util.List;
import java.lang.Thread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;

import android.app.AlertDialog;

public class AndroidWifiModule extends ReactContextBaseJavaModule {

	//WifiManager Instance
	WifiManager wifi;

	//Constructor
	public AndroidWifiModule(ReactApplicationContext reactContext) {
		super(reactContext);
		wifi = (WifiManager)reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE); 
	}

	//Name for module register to use:
	@Override
	public String getName() {
		return "AndroidWifiModule";
	}

	//Method to load wifi list into string via Callback. Returns a stringified JSONArray
	@ReactMethod
	public void loadWifiList(Callback successCallback, Callback errorCallback) {
		try {
			List<ScanResult> results = wifi.getScanResults();
			JSONArray wifiArray = new JSONArray();

			for (ScanResult result : results) {
				JSONObject wifiObject = new JSONObject();
				if (!result.SSID.equals("")) {
					try {
						wifiObject.put("SSID", result.SSID);
						wifiObject.put("BSSID", result.BSSID);
						wifiObject.put("capabilities", result.capabilities);
						wifiObject.put("frequency", result.frequency);
						wifiObject.put("level", result.level);
						wifiObject.put("timestamp", result.timestamp);
						//Other fields not added
						//wifiObject.put("operatorFriendlyName", result.operatorFriendlyName);
						//wifiObject.put("venueName", result.venueName);
						//wifiObject.put("centerFreq0", result.centerFreq0);
						//wifiObject.put("centerFreq1", result.centerFreq1);
						//wifiObject.put("channelWidth", result.channelWidth);
					} catch (JSONException e) {
						errorCallback.invoke(e.getMessage());
					}
					wifiArray.put(wifiObject);
				}
			}
			successCallback.invoke(wifiArray.toString());
		} catch (IllegalViewOperationException e) {
			errorCallback.invoke(e.getMessage());
		}
	}

	//Method to check if wifi is enabled
	@ReactMethod
	public void isEnabled(Callback isEnabled) {
		isEnabled.invoke(wifi.isWifiEnabled());
	}

	//Method to connect/disconnect wifi service
	@ReactMethod
	public void setEnabled(Boolean enabled) {
		wifi.setWifiEnabled(enabled);
	}

	//Method to check if still connecting or connected
	@ReactMethod
	public boolean isConnecting() {
		ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		long waitingTime = 2000;
		long accumulatedTime = 0;
		long maxWaitingTime = 30000;

		do {
			// Still connecting
			try {
				Thread.sleep(waitingTime);
				accumulatedTime += waitingTime;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (mWifi.isConnectedOrConnecting() == true && mWifi.isConnected() == false
				&& accumulatedTime < maxWaitingTime);

		if (maxWaitingTime >= accumulatedTime) {
			return false;
		} else {
			return mWifi.isConnected();
		}
	}

	//Send the ssid and password of a Wifi network into this to connect to the network.
	//Example:  wifi.findAndConnect(ssid, password);
	//After 10 seconds, a post telling you whether you are connected will pop up.
	//Callback returns true if ssid is in the range
	@ReactMethod
	public void findAndConnect(String ssid, String password, Callback ssidFound) {
		List<ScanResult> results = wifi.getScanResults();
		boolean connected = false;
		for (ScanResult result : results) {
			String resultString = "" + result.SSID;
			if (ssid.equals(resultString)) {
				connected = connectTo(result, password, ssid);
			}
		}
		ssidFound.invoke(connected);
	}

	//Use this method to check if the device is currently connected to Wifi.
	@ReactMethod
	public void connectionStatus(Callback connectionStatusResult) {
		ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnected()) {
			connectionStatusResult.invoke(true);
		} else {
			connectionStatusResult.invoke(false);
		}
	}

	//Method to connect to WIFI Network
	public Boolean connectTo(ScanResult result, String password, String ssid) {
		//Make new configuration
		WifiConfiguration conf = new WifiConfiguration();

		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		// 	conf.SSID = ssid;
		// } else {
			conf.SSID = "\"" + ssid + "\"";
		// }

		String capabilities = result.capabilities;

		if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA/WPA2 PSK")) {

			// appropriate ciper is need to set according to security type used,
			// ifcase of not added it will not be able to connect
			conf.preSharedKey = "\"" + password + "\"";

			conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

			conf.status = WifiConfiguration.Status.ENABLED;

			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

			conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

		} else if (capabilities.contains("WEP")) {
			conf.wepKeys[0] = "\"" + password + "\"";
			conf.wepTxKeyIndex = 0;
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

		} else {
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}

		//Remove the existing configuration for this netwrok
		List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();

		int updateNetwork = -1;

		for (WifiConfiguration wifiConfig : mWifiConfigList) {
			if (wifiConfig.SSID.equals(conf.SSID)) {
				// conf.networkId = wifiConfig.networkId;
				conf = wifiConfig;
				updateNetwork = wifi.updateNetwork(conf);
			}
		}

		// If network not already in configured networks add new network
		if (updateNetwork == -1) {
			updateNetwork = wifi.addNetwork(conf);
			wifi.saveConfiguration();
		}
		;

		if (updateNetwork == -1) {
			return false;
		}

		boolean disconnect = wifi.disconnect();
		if (!disconnect) {
			return false;
		}
		;

		boolean enableNetwork = wifi.enableNetwork(updateNetwork, true);
		if (!enableNetwork) {
			return false;
		}
		;

		return true;
	}

	//Disconnect current Wifi.
	@ReactMethod
	public void disconnect() {
		wifi.disconnect();
	}

	//This method will return current ssid
	@ReactMethod
	public void getSSID(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();

		// This value should be wrapped in double quotes, so we need to unwrap it.
		String ssid = info.getSSID();
		if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
			ssid = ssid.substring(1, ssid.length() - 1);
		}

		callback.invoke(ssid);
	}

	//This method will return the basic service set identifier (BSSID) of the current access point
	@ReactMethod
	public void getBSSID(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();

		String bssid = info.getBSSID();

		callback.invoke(bssid.toUpperCase());
	}

	//This method will return current wifi signal strength
	@ReactMethod
	public void getCurrentSignalStrength(final Callback callback) {
		int linkSpeed = wifi.getConnectionInfo().getRssi();
		callback.invoke(linkSpeed);
	}

	//This method will return current IP
	@ReactMethod
	public void getIP(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();
		String stringip = longToIP(info.getIpAddress());
		callback.invoke(stringip);
	}

	@ReactMethod
	public void bindToNetwork(String ssid, final Callback status) {
		boolean binded = false;

		try {
			// To check Android SDK version requirements met (minimum is Lollipop)
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				// Get system-level service by name; in this case, the connectivity service
				final ConnectivityManager manager = (ConnectivityManager) getReactApplicationContext()
						.getSystemService(Context.CONNECTIVITY_SERVICE);

				for (Network network : manager.getAllNetworks()) {
					NetworkInfo ni = manager.getNetworkInfo(network);
					String extraInfo = "";
					if (ni.getExtraInfo() != null) {
						extraInfo = ni.getExtraInfo();
					}

					// System.out.println("EXTRA INFO: " + extraInfo);

					if (extraInfo != null) {
						if (extraInfo.contains(ssid)) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
								binded = manager.bindProcessToNetwork(network);
							} else {
								//This method was deprecated in API level 23
								binded = manager.setProcessDefaultNetwork(network);
							}
							break;
						}
					}
				}
			}
		} catch (Exception ex) {
			System.out.println("Error binding: " + ex);
		}

		status.invoke(binded);
	}

	@ReactMethod
	public void findConnectAndBind(String ssid, String password, int waitingTimeInt, int maxTimeoutInt,
			final Callback callback) {
		// Scan for specified network
		List<ScanResult> results = wifi.getScanResults();
		boolean connected = false;
		boolean binded = false;
		long waitingTime = waitingTimeInt;
		long maxTimeout = maxTimeoutInt;

		// Iterate through all networks found
		for (ScanResult result : results) {
			String resultString = "" + result.SSID;

			// Connect to specified network if found
			if (ssid.equals(resultString)) {
				connected = connectTo(result, password, ssid);
			}
		}

		long timeoutCounter = 0l;

		do {
			try {
				// To check Android SDK version requirements met (minimum is Lollipop)
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && connected) {
					// Get system-level service by name; in this case, the connectivity service
					final ConnectivityManager manager = (ConnectivityManager) getReactApplicationContext()
							.getSystemService(Context.CONNECTIVITY_SERVICE);

					for (Network network : manager.getAllNetworks()) {
						NetworkInfo ni = manager.getNetworkInfo(network);
						String extraInfo = "";
						if (ni.getExtraInfo() != null) {
							extraInfo = ni.getExtraInfo();
						}

						// System.out.println("EXTRA INFO: " + extraInfo);

						if (extraInfo != null) {
							if (extraInfo.contains(ssid)) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
									binded = manager.bindProcessToNetwork(network);
								} else {
									//This method was deprecated in API level 23
									binded = manager.setProcessDefaultNetwork(network);
								}
								break;
							}
						}
					}
				}

				Thread.sleep(waitingTime);
				timeoutCounter += waitingTime;
			} catch (Exception ex) {

			}
		} while (binded == false && timeoutCounter <= maxTimeout);

		callback.invoke(binded);

		// NetworkRequest.Builder builder;
		// builder = new NetworkRequest.Builder();

		// // Set the transport type do WIFI
		// builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

		// // Request a network to satisfy a set of NetworkCapabilities
		// manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
		// 	// Called when the framework connects and has declared a new network ready for use
		// 	@Override
		// 	public void onAvailable(Network network) {
		// 		// For Android Marshmallow 6.0 and later
		// 		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		// 			manager.bindProcessToNetwork(network);
		// 		} else {
		// 			//This method was deprecated in API level 23
		// 			ConnectivityManager.setProcessDefaultNetwork(network);
		// 		}

		// 		// Return value connected and binded
		// 		try {
		// 			//do a callback or something else to alert your code that it's ok to send the message through socket now
		// 			callback.invoke(true);
		// 		} catch (Exception e) {
		// 			e.printStackTrace();
		// 			callback.invoke(false);
		// 		}
		// 		manager.unregisterNetworkCallback(this);
		// 	}
		// });
	}

	@ReactMethod
	public void hasWallGarden(final Callback callback) {
		ConnectivityManager connectivityManager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		Network[] activeNetworks = connectivityManager.getAllNetworks();
		boolean invokeReturn = false;
		for(Network network:activeNetworks){
				Log.d("WIFIUNIFI-network", network.toString());

				if(connectivityManager.getNetworkInfo(network).getType() == ConnectivityManager.TYPE_WIFI){
						NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

						Log.d("WIFIUNIFI-networkcapabilities", networkCapabilities.toString());
						if(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)){
							//DO SOMETHING
							invokeReturn = true;
						}
						break;
				}
		}
		callback.invoke(invokeReturn);
	}

	@ReactMethod
	public void connectToWifi(String networkSSID, String password, final Callback statusCallback) {
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = "\"" + networkSSID + "\"";
		wc.preSharedKey = "\"" + password + "\"";
		wifi.addNetwork(wc);

		List<WifiConfiguration> list = wifi.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
			if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
				wifi.disconnect();
				wifi.enableNetwork(i.networkId, true);
				wifi.reconnect();
				statusCallback.invoke(true);
				break;
			}
		}
	}

	//This method will remove the wifi network as per the passed SSID from the device list
	@ReactMethod
	public void isRemoveWifiNetwork(String ssid, final Callback callback) {
		List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();
		for (WifiConfiguration wifiConfig : mWifiConfigList) {
			String comparableSSID = ('"' + ssid + '"'); //Add quotes because wifiConfig.SSID has them
			if (wifiConfig.SSID.equals(comparableSSID)) {
				wifi.removeNetwork(wifiConfig.networkId);
				wifi.saveConfiguration();
				callback.invoke(true);
				return;
			}
		}
		callback.invoke(false);
	}

	// This method is similar to `loadWifiList` but it forcefully starts the wifi scanning on android and in the callback fetches the list
	@ReactMethod
	public void reScanAndLoadWifiList(Callback successCallback, Callback errorCallback) {
		WifiReceiver receiverWifi = new WifiReceiver(wifi, successCallback, errorCallback);
		getReactApplicationContext().getCurrentActivity().registerReceiver(receiverWifi,
				new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wifi.startScan();
	}

	public static String longToIP(int longIp) {
		StringBuffer sb = new StringBuffer("");
		String[] strip = new String[4];
		strip[3] = String.valueOf((longIp >>> 24));
		strip[2] = String.valueOf((longIp & 0x00FFFFFF) >>> 16);
		strip[1] = String.valueOf((longIp & 0x0000FFFF) >>> 8);
		strip[0] = String.valueOf((longIp & 0x000000FF));
		sb.append(strip[0]);
		sb.append(".");
		sb.append(strip[1]);
		sb.append(".");
		sb.append(strip[2]);
		sb.append(".");
		sb.append(strip[3]);
		return sb.toString();
	}

	class WifiReceiver extends BroadcastReceiver {

		private Callback successCallback;
		private Callback errorCallback;
		private WifiManager wifi;

		public WifiReceiver(final WifiManager wifi, Callback successCallback, Callback errorCallback) {
			super();
			this.successCallback = successCallback;
			this.errorCallback = errorCallback;
			this.wifi = wifi;
		}

		// This method call when number of wifi connections changed
		public void onReceive(Context c, Intent intent) {
			// LocalBroadcastManager.getInstance(c).unregisterReceiver(this);
			c.unregisterReceiver(this);
			// getReactApplicationContext().getCurrentActivity().registerReceiver
			try {
				List<ScanResult> results = this.wifi.getScanResults();
				JSONArray wifiArray = new JSONArray();

				for (ScanResult result : results) {
					JSONObject wifiObject = new JSONObject();
					if (!result.SSID.equals("")) {
						try {
							wifiObject.put("SSID", result.SSID);
							wifiObject.put("BSSID", result.BSSID);
							wifiObject.put("capabilities", result.capabilities);
							wifiObject.put("frequency", result.frequency);
							wifiObject.put("level", result.level);
							wifiObject.put("timestamp", result.timestamp);
						} catch (JSONException e) {
							this.errorCallback.invoke(e.getMessage());
							return;
						}
						wifiArray.put(wifiObject);
					}
				}
				this.successCallback.invoke(wifiArray.toString());
				return;
			} catch (IllegalViewOperationException e) {
				this.errorCallback.invoke(e.getMessage());
				return;
			}
		}
	}
}
