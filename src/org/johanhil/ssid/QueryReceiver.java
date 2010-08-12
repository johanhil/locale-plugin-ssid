package org.johanhil.ssid;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This is the "query" {@code BroadcastReceiver} for a <i>Locale</i> plug-in condition.
 */
public final class QueryReceiver extends BroadcastReceiver
{
	public static final String TAG = "SSIDQueryReciever";

	/**
	 * @param context {@inheritDoc}.
	 * @param intent the incoming {@code Intent}. This should always contain the store-and-forward {@code Bundle} that was saved
	 *            by {@link EditActivity} and later broadcast by <i>Locale</i>.
	 */
	@Override
	public void onReceive(final Context context, final Intent intent)
	{
		/*
		 * Always be sure to be strict on your input parameters! A malicious third-party app could always send your plug-in an
		 * empty or otherwise malformed Intent. And since Locale applies settings in the background, you don't want your plug-in
		 * to crash.
		 */
		
		if (!com.twofortyfouram.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction()))
		{
			Log.w(TAG, "Received unexpected Intent action"); //$NON-NLS-1$//$NON-NLS-2$
			return;
		}

		final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE);
		if (bundle == null)
		{
			Log.e(TAG, "Received null BUNDLE"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (!bundle.containsKey(Constants.BUNDLE_EXTRA_SSID))
		{
			Log.e(TAG, "Missing SSID param in Bundle");
			return;
		}

		if (!BackgroundService.isRunning())
		{
			final Intent serviceIntent = new Intent(context, BackgroundService.class);
			serviceIntent.putExtras(intent);
			context.startService(serviceIntent);
		}

		// get all the ssids and see if we have a match
		final Set<String> SSIDs = BackgroundService.getSSIDs();
		String soughtSSID = bundle.getString(Constants.BUNDLE_EXTRA_SSID);
		
		if (SSIDs == null || ! SSIDs.contains(soughtSSID))
		{
			setResultCode(com.twofortyfouram.Intent.RESULT_CONDITION_UNSATISFIED);
		}
		else // SSIDs != null and SSIDs contains soughtSSID
		{
			setResultCode(com.twofortyfouram.Intent.RESULT_CONDITION_SATISFIED);
		}
	}
}