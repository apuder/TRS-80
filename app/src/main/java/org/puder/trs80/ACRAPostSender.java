package org.puder.trs80;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ACRAPostSender implements ReportSender {

    private final static String BASE_URL      = "http://trs80.sourceforge.net/acra.php?email=mobile@puder.org";
    private final static String SHARED_SECRET = "mobl_secret";
    private Map<String, String> custom_data   = null;


    public ACRAPostSender(HashMap<String, String> custom_data) {
        this.custom_data = custom_data;
    }

    @Override
    public void send(Context context, CrashReportData report) throws ReportSenderException {
        Map<String, String> parameters = new LinkedHashMap<>();

        if (custom_data != null) {
            for (Map.Entry<String, String> entry : custom_data.entrySet()) {
                parameters.put(entry.getKey(), entry.getValue());
            }
        }

        parameters.put("DATE", new Date().toString());
        parameters.put("REPORT_ID", report.get(ReportField.REPORT_ID));
        parameters.put("APP_VERSION_CODE", report.get(ReportField.APP_VERSION_CODE));
        parameters.put("APP_VERSION_NAME", report.get(ReportField.APP_VERSION_NAME));
        parameters.put("PACKAGE_NAME", report.get(ReportField.PACKAGE_NAME));
        parameters.put("FILE_PATH", report.get(ReportField.FILE_PATH));
        parameters.put("PHONE_MODEL", report.get(ReportField.PHONE_MODEL));
        parameters.put("ANDROID_VERSION", report.get(ReportField.ANDROID_VERSION));
        parameters.put("BUILD", report.get(ReportField.BUILD));
        parameters.put("BRAND", report.get(ReportField.BRAND));
        parameters.put("PRODUCT", report.get(ReportField.PRODUCT));
        parameters.put("TOTAL_MEM_SIZE", report.get(ReportField.TOTAL_MEM_SIZE));
        parameters.put("AVAILABLE_MEM_SIZE", report.get(ReportField.AVAILABLE_MEM_SIZE));
        parameters.put("CUSTOM_DATA", report.get(ReportField.CUSTOM_DATA));
        parameters.put("STACK_TRACE", report.get(ReportField.STACK_TRACE));
        parameters.put("INITIAL_CONFIGURATION", report.get(ReportField.INITIAL_CONFIGURATION));
        parameters.put("CRASH_CONFIGURATION", report.get(ReportField.CRASH_CONFIGURATION));
        parameters.put("DISPLAY", report.get(ReportField.DISPLAY));
        parameters.put("USER_COMMENT", report.get(ReportField.USER_COMMENT));
        parameters.put("USER_APP_START_DATE", report.get(ReportField.USER_APP_START_DATE));
        parameters.put("USER_CRASH_DATE", report.get(ReportField.USER_CRASH_DATE));
        parameters.put("DUMPSYS_MEMINFO", report.get(ReportField.DUMPSYS_MEMINFO));
        parameters.put("DROPBOX", report.get(ReportField.DROPBOX));
        parameters.put("LOGCAT", report.get(ReportField.LOGCAT));
        parameters.put("EVENTSLOG", report.get(ReportField.EVENTSLOG));
        parameters.put("RADIOLOG", report.get(ReportField.RADIOLOG));
        parameters.put("IS_SILENT", report.get(ReportField.IS_SILENT));
        parameters.put("DEVICE_ID", report.get(ReportField.DEVICE_ID));
        parameters.put("INSTALLATION_ID", report.get(ReportField.INSTALLATION_ID));
        parameters.put("USER_EMAIL", report.get(ReportField.USER_EMAIL));
        parameters.put("DEVICE_FEATURES", report.get(ReportField.DEVICE_FEATURES));
        parameters.put("ENVIRONMENT", report.get(ReportField.ENVIRONMENT));
        parameters.put("SETTINGS_SYSTEM", report.get(ReportField.SETTINGS_SYSTEM));
        parameters.put("SETTINGS_SECURE", report.get(ReportField.SETTINGS_SECURE));
        parameters.put("SHARED_PREFERENCES", report.get(ReportField.SHARED_PREFERENCES));
        parameters.put("APPLICATION_LOG", report.get(ReportField.APPLICATION_LOG));
        parameters.put("MEDIA_CODEC_LIST", report.get(ReportField.MEDIA_CODEC_LIST));
        parameters.put("THREAD_DETAILS", report.get(ReportField.THREAD_DETAILS));

        try {
            URL url = getUrl();
            Log.e("xenim", url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }

            String query = builder.build().getEncodedQuery();
            connection.setFixedLengthStreamingMode(query.length());

            OutputStream out = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

            writer.write(query);
            writer.flush();
            writer.close();

            out.close();

            connection.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private URL getUrl() throws MalformedURLException {
        String token = getToken();
        String key = getKey(token);
        return new URL(String.format("%s&token=%s&key=%s&", BASE_URL, token, key));
    }

    private String getKey(String token) {
        return md5(String.format("%s+%s", SHARED_SECRET, token));
    }

    private String getToken() {
        return md5(UUID.randomUUID().toString());
    }

    public String md5(String s) {
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        m.update(s.getBytes(), 0, s.length());
        String hash = new BigInteger(1, m.digest()).toString(16);
        return hash;
    }
}
