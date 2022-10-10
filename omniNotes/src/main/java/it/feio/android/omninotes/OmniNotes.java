/*
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.omninotes;

import static com.snowplowanalytics.snowplow.internal.utils.Util.addToMap;
import static it.feio.android.omninotes.utils.Constants.PACKAGE;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_LANG;

import android.content.Context;
import android.content.res.Configuration;
import android.os.StrictMode;
import androidx.multidex.MultiDexApplication;
import com.pixplicity.easyprefs.library.Prefs;
import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.util.Basis;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import it.feio.android.omninotes.helpers.LanguageHelper;
import it.feio.android.omninotes.helpers.notifications.NotificationsHelper;
import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraToast;
import org.acra.sender.HttpSender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@AcraCore(buildConfigClass = BuildConfig.class)
@AcraHttpSender(uri = BuildConfig.CRASH_REPORTING_URL,
    httpMethod = HttpSender.Method.POST)
@AcraToast(resText = R.string.crash_toast)
public class OmniNotes extends MultiDexApplication {

  private static Context mContext;

  public static boolean isDebugBuild() {
    return BuildConfig.BUILD_TYPE.equals("debug");
  }

  public static Context getAppContext() {
    return OmniNotes.mContext;
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    ACRA.init(this);
    ACRA.getErrorReporter().putCustomData("TRACEPOT_DEVELOP_MODE", isDebugBuild() ? "1" : "0");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mContext = getApplicationContext();
    initSharedPreferences();
    enableStrictMode();
    new NotificationsHelper(this).initNotificationChannels();
    TrackerConfiguration trackerConfiguration = new TrackerConfiguration("salesAndroidApp")
            .sessionContext(true)
            .platformContext(true)
            .applicationContext(true)
            .screenContext(true)
            .lifecycleAutotracking(true)
            .screenViewAutotracking(false)
            .exceptionAutotracking(true)
            .installAutotracking(true);
    EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
            .requestCallback(getRequestCallback())
            .threadPoolSize(20)
            .emitRange(500)
            .byteLimitPost(52000);
    NetworkConfiguration networkConfiguration = new NetworkConfiguration("https://aaebce57-0848-493b-a528-143ec143e4c7.app.try-snowplow.com", HttpMethod.POST);
    SessionConfiguration sessionConfiguration = new SessionConfiguration(
            new TimeMeasure(30, TimeUnit.SECONDS),
            new TimeMeasure(30, TimeUnit.SECONDS)
    );
    GdprConfiguration gdprConfiguration = new GdprConfiguration(
            Basis.CONSENT,
            "someId",
            "0.1.0",
            "this is a demo document description"
    );
    GlobalContextsConfiguration gcConfiguration = new GlobalContextsConfiguration(null);
    Map<String, Object> pairs = new HashMap<>();
    addToMap(Parameters.APP_VERSION, "0.3.0", pairs);
    addToMap(Parameters.APP_BUILD, "3", pairs);
    gcConfiguration.add("ruleSetExampleTag", new GlobalContext(Collections.singletonList(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION, pairs))));

    TrackerController tracker = Snowplow.createTracker(mContext,
            "appTracker",
            networkConfiguration,
            trackerConfiguration,
            emitterConfiguration,
            sessionConfiguration,
            gdprConfiguration,
            gcConfiguration
    );
    Event event = new Structured("Category_example", "Action_example");
    tracker.track(event);

  }

  private RequestCallback getRequestCallback() {
    return new RequestCallback() {
      @Override
      public void onSuccess(int successCount) {
        System.out.println("Emitter Send Success:\n " +
                "- Events sent: " + successCount + "\n");
        System.out.println(successCount);
      }
      @Override
      public void onFailure(int successCount, int failureCount) {
        System.out.println("Emitter Send Failure:\n " +
                "- Events sent: " + successCount + "\n " +
                "- Events failed: " + failureCount + "\n");
        System.out.println(successCount);
      }
    };
  }

  private void initSharedPreferences() {
    new Prefs.Builder()
        .setContext(this)
        .setMode(MODE_PRIVATE)
        .setPrefsName(PACKAGE)
        .setUseDefaultSharedPreference(true)
        .build();
  }

  private void enableStrictMode() {
    if (isDebugBuild()) {
      StrictMode.enableDefaults();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    String language = Prefs.getString(PREF_LANG, "");
    LanguageHelper.updateLanguage(this, language);
  }

}
