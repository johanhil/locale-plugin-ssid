package org.johanhil.ssid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

/**
 * {@code Service} for monitoring the {@code REGISTERED_RECEIVER_ONLY} {@code Intent}s {@link Intent#ACTION_SCREEN_ON} and
 * {@link Intent#ACTION_SCREEN_OFF}.
 */
public final class BackgroundService extends Service
{

	/**
	 * REPRESENTATION INVARIANTS:
	 * <ol>
	 * <li>The {@link #REQUEST_REQUERY} {@code Intent} should not be modified after its static initialization completes</li>
	 * <li>{@link #isRunning} returns true only while the service is running</li>
	 * <li>{@link #mReceiver} is registered only while the service is running</li>
	 * <li>{@link #displayState} must be one of: -1 for unknown, 0 for off, or 1 for on</li>
	 * </ol>
	 */

	/**
	 * {@code Intent} to ask <i>Locale</i> to re-query our conditions. Cached here so that we only have to create this object
	 * once.
	 */
	private static final Intent REQUEST_REQUERY = new Intent(com.twofortyfouram.Intent.ACTION_REQUEST_QUERY);
	private static final String TAG = "locale-ssid-plugin";

	static
	{
		/*
		 * The Activity name must be present as an extra in this Intent, so that Locale will know who needs updating. This intent
		 * will be ignored unless the extra is present.
		 */
		REQUEST_REQUERY.putExtra(com.twofortyfouram.Intent.EXTRA_ACTIVITY, EditActivity.class.getName());
	}

	private static boolean isRunning = false;
	private BroadcastReceiver mReceiver;
	private static Set<String> SSIDs;
	private WifiManager wifi;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		SSIDs = new HashSet<String>();
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/*
		 * Listens for WifiManager.SCAN_RESULTS_AVAILABLE_ACTION intents and updates
		 * the SSIDs set.
		 * 
		 * Also listens to WIFI_STATE_CHANGED_ACTION to clear the SSID set on wifi disabling, 
		 * and NETWORK_STATE_CHANGED_ACTION to add the wifi that the user is connected to to the SSIDs.
		 */
		mReceiver = new BroadcastReceiver()
		{

			@SuppressWarnings("synthetic-access")
			@Override
			public void onReceive(final Context context, final Intent intent)
			{
				final String action = intent.getAction();

				Log.v(TAG, String.format("Received Intent action %s", action)); //$NON-NLS-1$ //$NON-NLS-2$

				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
				{
					SSIDs.clear();
					if (wifi != null)
					{
						List<ScanResult> results = wifi.getScanResults();
						
						if (results != null)
						{
							for (ScanResult result : results)
							{
								SSIDs.add(result.SSID);
							}
						}
					}
				}
				
				if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
				{
					Log.d(TAG, "WIFI State Changed");

					int new_state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);

					Log.d(TAG, "New Wifi state is " + new_state);

					if ((new_state == WifiManager.WIFI_STATE_DISABLED) ||
							(new_state == WifiManager.WIFI_STATE_DISABLING))
					{
						Log.d(TAG, "Wifi being turned off, clearing SSID list");
						SSIDs.clear();
					}					
				}
				
				if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
				{
					Log.d(TAG, "network state changed. possible connection.");
					
					NetworkInfo newNetwork = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					
					// TODO do we need to check if the state is "connecting"?
					if (newNetwork.getState() == NetworkInfo.State.CONNECTED)
					{
						String ssid = wifi.getConnectionInfo().getSSID();
						if (ssid != null)
						{
							Log.d(TAG, "connected to ssid \""+ssid+"\"");
							SSIDs.add(ssid);
						}
					}
				}

				/*
				 * Ask Locale to re-query our condition instances. Note: this plug-in does not keep track of what types of
				 * conditions have been set up. While executing this code, we have no idea whether there are even any Display
				 * conditions within Locale, or whether those conditions are checking for screen on/screen off. This is an
				 * intentional design decision to eliminate all sorts of complex synchronization problems.
				 */
				sendBroadcast(REQUEST_REQUERY);
			}
		};

		/*
		 * This a RECEIVER_REGISTERED_ONLY Intent
		 */
		final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		registerReceiver(mReceiver, filter);

		isRunning = true;
	}

	/**
	 * Determines whether the service is actually running or not.
	 *
	 * @return true if the service is running. False if it is not.
	 */
	public static boolean isRunning()
	{
		return isRunning;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(final Intent arg0)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		unregisterReceiver(mReceiver);
		isRunning = false;
	}

	public static Set<String> getSSIDs() {
		return SSIDs;
	}
}