package com.ztestapp.clientapplication;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    Uri cpUri;
    ContentProviderClient cpClient;
    private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private String AUTHORITY_FILE = "content://com.zebra.securestoragemanager.securecontentprovider/file";
    static final String TAG = "client";

    private String COLUMN_ORIG_APP_PACKAGE = "orig_app_package";
    private String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private String COLUMN_TARGET_APP_SIGNATURE = "target_app_signature";
    private String COLUMN_DATA_TYPE = "data_type";
    private String COLUMN_DATA_NAME = "data_name";
    private String COLUMN_DATA_VALUE = "data_value";
    private String COLUMN_DATA_INPUT_FORM = "data_input_form";
    private String COLUMN_DATA_OUTPUT_FORM = "data_output_form";
    private String COLUMN_DATA_AUTO_DELETE = "auto_delete_required";
    private String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private String COLUMN_MULTI_INSTANCE_REQUIRED = "multi_instance_required";
    private String COLUMN_DATA_INPUT_ENCRYPTED_KEY = "data_input_encrypted_key";

    //SSM version
    private String SSM_DB_VERSION = "ssm_db_version";

    private static SecretKey mSecretKey = null;
    private static PublicKey mPublicKey = null;

    private static String packageName = "";
    ContentObserver mObserver;
    LocalContentObserver myContentObserver;
    LocalDataSetObserver myDataSetObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        packageName = getPackageName();

        cpUri = Uri.parse(AUTHORITY);
        cpClient = getContentResolver().acquireContentProviderClient(cpUri);

        myContentObserver = new LocalContentObserver(null);
        myDataSetObserver = new LocalDataSetObserver();
    }



    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(cpUri, true, myContentObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(myContentObserver);
    }

    public void onClickInsert(View view) {
        try {

            ContentValues values = new ContentValues();

            //for multiple appps
            /*values.put(COLUMN_TARGET_APP_PACKAGE,
                    "{\"pkgs_sigs\":" +
                            "[" +
                            "{\"pkg\":\"com.ztestapp.clientapplication\"," + "\"sig\":\"MIIEqDCCA5CgAwIBAgIJALOZgIbQVs/6MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMjQwNTBaFw0zNTA5MDEyMjQwNTBaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAJx4BZKsDV04HN6qZezIpgBuNkgMbXIHsSARvlCGOqvitV0Amt9xRtbyICKAx81Ne9smJDuKgGwms0sTdSOkkmgiSQTcAUk+fArPGgXIdPabA3tgMJ2QdNJCgOFrrSqHNDYZUer3KkgtCbIEsYdeEqyYwap3PWgAuer95W1Yvtjo2hb5o2AJnDeoNKbf7be2tEoEngeiafzPLFSW8s821k35CjuNjzSjuqtM9TNxqydxmzulh1StDFP8FOHbRdUeI0+76TybpO35zlQmE1DsU1YHv2mi/0qgfbX36iANCabBtJ4hQC+J7RGQiTqrWpGA8VLoL4WkV1PPX8GQccXuyCcCAQOjgfwwgfkwHQYDVR0OBBYEFE/koLPdnLop9x1yh8Tnw48ghsKZMIHJBgNVHSMEgcEwgb6AFE/koLPdnLop9x1yh8Tnw48ghsKZoYGapIGXMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbYIJALOZgIbQVs/6MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADggEBAFclUbjZOh9z3g9tRp+G2tZwFAApPIigzXzXeLc9r8wZf6t25iEuVsHHYc/EL9cz3lLFCuCIFM78CjtaGkNGBU2Cnx2CtCsgSL+ItdFJKe+F9g7dEtctVWV+IuPoXQTIMdYT0Zk4u4mCJH+jISVroS0dao+S6h2xw3Mxe6DAN/DRr/ZFrvIkl5+6bnoUvAJccbmBOM7z3fwFlhfPJIRc97QNY4L3J17XOElatuWTG5QhdlxJG3L7aOCA29tYwgKdNHyLMozkPvaosVUz7fvpib1qSN1LIC7alMarjdW4OZID2q4u1EYjLk/pvZYTlMYwDlE448/Shebk5INTjLixs1c=\"}," +
                            //"{\"pkg\":\"com.ztestapp.clientapplication2\",\"sig\":\"24727AB4E6C4A0BE91ACE1FE342F0D6872B27B3C\"}," +
                            "{\"pkg\":\"com.example.f1\",\"sig\":\"MIIEqDCCA5CgAwIBAgIJALOZgIbQVs/6MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMjQwNTBaFw0zNTA5MDEyMjQwNTBaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAJx4BZKsDV04HN6qZezIpgBuNkgMbXIHsSARvlCGOqvitV0Amt9xRtbyICKAx81Ne9smJDuKgGwms0sTdSOkkmgiSQTcAUk+fArPGgXIdPabA3tgMJ2QdNJCgOFrrSqHNDYZUer3KkgtCbIEsYdeEqyYwap3PWgAuer95W1Yvtjo2hb5o2AJnDeoNKbf7be2tEoEngeiafzPLFSW8s821k35CjuNjzSjuqtM9TNxqydxmzulh1StDFP8FOHbRdUeI0+76TybpO35zlQmE1DsU1YHv2mi/0qgfbX36iANCabBtJ4hQC+J7RGQiTqrWpGA8VLoL4WkV1PPX8GQccXuyCcCAQOjgfwwgfkwHQYDVR0OBBYEFE/koLPdnLop9x1yh8Tnw48ghsKZMIHJBgNVHSMEgcEwgb6AFE/koLPdnLop9x1yh8Tnw48ghsKZoYGapIGXMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbYIJALOZgIbQVs/6MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADggEBAFclUbjZOh9z3g9tRp+G2tZwFAApPIigzXzXeLc9r8wZf6t25iEuVsHHYc/EL9cz3lLFCuCIFM78CjtaGkNGBU2Cnx2CtCsgSL+ItdFJKe+F9g7dEtctVWV+IuPoXQTIMdYT0Zk4u4mCJH+jISVroS0dao+S6h2xw3Mxe6DAN/DRr/ZFrvIkl5+6bnoUvAJccbmBOM7z3fwFlhfPJIRc97QNY4L3J17XOElatuWTG5QhdlxJG3L7aOCA29tYwgKdNHyLMozkPvaosVUz7fvpib1qSN1LIC7alMarjdW4OZID2q4u1EYjLk/pvZYTlMYwDlE448/Shebk5INTjLixs1c=\"}" +
                            "]" +
                            "}");*/
            //for single app
            values.put(COLUMN_TARGET_APP_PACKAGE,
                    "{\"pkgs_sigs\": [{\"pkg\":\"com.ztestapp.clientapplication\",\"sig\":\"MIIEqDCCA5CgAwIBAgIJALOZgIbQVs/6MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMjQwNTBaFw0zNTA5MDEyMjQwNTBaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAJx4BZKsDV04HN6qZezIpgBuNkgMbXIHsSARvlCGOqvitV0Amt9xRtbyICKAx81Ne9smJDuKgGwms0sTdSOkkmgiSQTcAUk+fArPGgXIdPabA3tgMJ2QdNJCgOFrrSqHNDYZUer3KkgtCbIEsYdeEqyYwap3PWgAuer95W1Yvtjo2hb5o2AJnDeoNKbf7be2tEoEngeiafzPLFSW8s821k35CjuNjzSjuqtM9TNxqydxmzulh1StDFP8FOHbRdUeI0+76TybpO35zlQmE1DsU1YHv2mi/0qgfbX36iANCabBtJ4hQC+J7RGQiTqrWpGA8VLoL4WkV1PPX8GQccXuyCcCAQOjgfwwgfkwHQYDVR0OBBYEFE/koLPdnLop9x1yh8Tnw48ghsKZMIHJBgNVHSMEgcEwgb6AFE/koLPdnLop9x1yh8Tnw48ghsKZoYGapIGXMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbYIJALOZgIbQVs/6MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADggEBAFclUbjZOh9z3g9tRp+G2tZwFAApPIigzXzXeLc9r8wZf6t25iEuVsHHYc/EL9cz3lLFCuCIFM78CjtaGkNGBU2Cnx2CtCsgSL+ItdFJKe+F9g7dEtctVWV+IuPoXQTIMdYT0Zk4u4mCJH+jISVroS0dao+S6h2xw3Mxe6DAN/DRr/ZFrvIkl5+6bnoUvAJccbmBOM7z3fwFlhfPJIRc97QNY4L3J17XOElatuWTG5QhdlxJG3L7aOCA29tYwgKdNHyLMozkPvaosVUz7fvpib1qSN1LIC7alMarjdW4OZID2q4u1EYjLk/pvZYTlMYwDlE448/Shebk5INTjLixs1c=\"}]}");


            values.put(COLUMN_DATA_NAME, "account-name-updated");
            String input = "\"{\\\"nameupdated\\\":\\\"goo\\\",\\\"age\\\":32,\\\"class\\\":\\\"spa\\\"}\"";
            values.put(COLUMN_DATA_INPUT_FORM, "1"); //plaintext =1, encrypted= 2
            values.put(COLUMN_DATA_OUTPUT_FORM, "1"); //plaintext=1

            values.put(COLUMN_DATA_VALUE, input);
            values.put(COLUMN_DATA_PERSIST_REQUIRED, "false"); //original true
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "false"); //original true
            Uri createdRow = getContentResolver().insert(cpUri, values);
            Toast.makeText(getBaseContext(), "New Record Inserted", Toast.LENGTH_LONG).show();
            Log.d(TAG, "cxnt48@Created row: " + createdRow.toString());
        } catch (Exception e) {
            Log.d(TAG, "cxnt48@Insert - error: " + e.getMessage());
            Toast.makeText(getBaseContext(), "Insert error "+ e.getMessage(), Toast.LENGTH_LONG).show();

        }

    }

    public void onClickQuery(View view) {

        TextView resultView = (TextView) findViewById(R.id.res);
        Uri cpUriQuery = Uri.parse(AUTHORITY + "/[com.ztestapp.clientapplication]");

        String orderBy = "data_name";
        String selection = "target_app_package = '" + packageName + "'" /*+ " AND " + "data_persist_required = '" + "false" + "'" +
                " AND " + "multi_instance_required = '" + "false" + "'"*/;
        Cursor cursor = null;
        try {
            Log.d("Client - Query", "Query start time = " + System.currentTimeMillis());
            cursor = getContentResolver().query(cpUriQuery, null, /*selection*/null, null, null);
            Log.d("Client - Query", "Query end time = " + System.currentTimeMillis());
            if (cursor != null) {
                cursor.registerDataSetObserver(myDataSetObserver);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
            if (cursor != null) {
                cursor.unregisterDataSetObserver(myDataSetObserver);
            }
        }

        try {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                while (!cursor.isAfterLast()) {
                    Log.d("Client - Query", "Started constructing string for view =  " + System.currentTimeMillis());
                    strBuild.append("\n" + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_TARGET_APP_PACKAGE)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_NAME)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_VALUE)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_INPUT_FORM)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_OUTPUT_FORM)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_AUTO_DELETE)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_DATA_PERSIST_REQUIRED)) + " - " +
                            cursor.getString(cursor.getColumnIndex(COLUMN_MULTI_INSTANCE_REQUIRED)));

                    String nnn = cursor.getString(cursor.getColumnIndex(COLUMN_TARGET_APP_PACKAGE));
                    //String uri = cursor.getString (cursor.getColumnIndex("secure_file_uri"));
                    //openFileFromURI(uri);
                    Log.d(TAG, "vvv: " + nnn);

                    cursor.moveToNext();
                }
                Log.d(TAG, "Query data: " + strBuild);
                Log.d("Client - Query", "Set test to view =  " + System.currentTimeMillis());
                resultView.setText(strBuild);
            } else {
                resultView.setText("No Records Found");
            }
        } catch (Exception e) {
            Log.d(TAG, "Query data error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public void onClickUpdate(View view) {

        try{
            ContentValues values = new ContentValues();

            values.put(COLUMN_TARGET_APP_PACKAGE, "{\"pkgs_sigs\":[{\"pkg\":\"com.ztestapp.clientapplication\",\"sig\":\"MIIEqDCCA5CgAwIBAgIJALOZgIbQVs/6MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMjQwNTBaFw0zNTA5MDEyMjQwNTBaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAJx4BZKsDV04HN6qZezIpgBuNkgMbXIHsSARvlCGOqvitV0Amt9xRtbyICKAx81Ne9smJDuKgGwms0sTdSOkkmgiSQTcAUk+fArPGgXIdPabA3tgMJ2QdNJCgOFrrSqHNDYZUer3KkgtCbIEsYdeEqyYwap3PWgAuer95W1Yvtjo2hb5o2AJnDeoNKbf7be2tEoEngeiafzPLFSW8s821k35CjuNjzSjuqtM9TNxqydxmzulh1StDFP8FOHbRdUeI0+76TybpO35zlQmE1DsU1YHv2mi/0qgfbX36iANCabBtJ4hQC+J7RGQiTqrWpGA8VLoL4WkV1PPX8GQccXuyCcCAQOjgfwwgfkwHQYDVR0OBBYEFE/koLPdnLop9x1yh8Tnw48ghsKZMIHJBgNVHSMEgcEwgb6AFE/koLPdnLop9x1yh8Tnw48ghsKZoYGapIGXMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbYIJALOZgIbQVs/6MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADggEBAFclUbjZOh9z3g9tRp+G2tZwFAApPIigzXzXeLc9r8wZf6t25iEuVsHHYc/EL9cz3lLFCuCIFM78CjtaGkNGBU2Cnx2CtCsgSL+ItdFJKe+F9g7dEtctVWV+IuPoXQTIMdYT0Zk4u4mCJH+jISVroS0dao+S6h2xw3Mxe6DAN/DRr/ZFrvIkl5+6bnoUvAJccbmBOM7z3fwFlhfPJIRc97QNY4L3J17XOElatuWTG5QhdlxJG3L7aOCA29tYwgKdNHyLMozkPvaosVUz7fvpib1qSN1LIC7alMarjdW4OZID2q4u1EYjLk/pvZYTlMYwDlE448/Shebk5INTjLixs1c=\"}]}");
            //values.put(COLUMN_TARGET_APP_PACKAGE, "{\"pkgs_sigs\":[{\"pkg\":\"com.ztestapp.clientapplication\",\"sig\":\"24727AB4E6C4A0BE91ACE1FE342F0D6872B27B3C\"},{\"pkg\":\"com.ztestapp.clientapplication2\",\"sig\":\"24727AB4E6C4A0BE91ACE1FE342F0D6872B27B3C\"},{\"pkg\":\"com.ztestapp.abc\",\"sig\":\"25677AB4E6C4A0BE91ACE1FE342F0D6872B27B3C\"}]}");
            values.put(COLUMN_DATA_NAME, "account-name");
            String input = "mypassword-new";
            values.put(COLUMN_DATA_VALUE, input);
            values.put(COLUMN_DATA_PERSIST_REQUIRED,"false");
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED,"true");
            int rowNumbers = getContentResolver().update(cpUri, values, null , null);
            Log.d(TAG, "Records updated: " + String.valueOf(rowNumbers));
            Toast.makeText(getBaseContext(), "Records Updated: ", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Log.d(TAG, "Update - error: " + e.getMessage());
        }

    }

    public void onClickDelete(View view) {

        try{
            Uri cpUriDelete = Uri.parse(AUTHORITY + "/[com.ztestapp.clientapplication]");

            String whereClause = "target_app_package = '" + "com.ztestapp.clientapplication" + "'" +"AND "+ "data_persist_required = '" + "false" + "'" +
                    "AND "+"multi_instance_required = '"+ "true" + "'";
            getContentResolver().delete(cpUriDelete, whereClause , null);
            Toast.makeText(getBaseContext(), "Records Deleted", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Log.d(TAG, "Delete - error: " + e.getMessage());
        }

    }



    public void onClickInsertSignatureFromFile(View view) {
        try{
            ContentValues values = new ContentValues();

            values.put(COLUMN_TARGET_APP_PACKAGE, getTargetAppPackageContent("com.ztestapp.clientapplication"));
            values.put(COLUMN_DATA_NAME, "account-name");
            String input = "\"{\\\"name\\\":\\\"john\\\",\\\"age\\\":22,\\\"class\\\":\\\"mca\\\"}\""; ////"1111-2222-3333";
            //values.put(COLUMN_DATA_VALUE, input.getBytes(Charset.forName("UTF-8")));
            values.put(COLUMN_DATA_VALUE,input);

            values.put(COLUMN_DATA_INPUT_FORM, "1"); //plaintext =1
            values.put(COLUMN_DATA_OUTPUT_FORM, "1"); //plaintext=1
            values.put(COLUMN_DATA_PERSIST_REQUIRED, "false");
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "true");

            Uri createdRow = getContentResolver().insert(cpUri, values);

            Toast.makeText(getBaseContext(), "New Record Inserted", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Created row: " + createdRow.toString());
        }catch(Exception e){
            Log.d(TAG, "Insert - error: " + e.getMessage());
        }
    }

    public void onClickUpdateSignatureFromFile(View view) {

        try{
            ContentValues values = new ContentValues();

            values.put(COLUMN_TARGET_APP_PACKAGE, getTargetAppPackageContent("com.ztestapp.clientapplication"));
            values.put(COLUMN_DATA_NAME, "account-name");
            String input = "mypassword-new";
            //values.put(COLUMN_DATA_VALUE, input.getBytes(Charset.forName("UTF-8")));
            values.put(COLUMN_DATA_VALUE, input);
            values.put(COLUMN_DATA_PERSIST_REQUIRED, "false");
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "true");

            int rowNumbers = getContentResolver().update(cpUri, values, null , null);
            Log.d(TAG, "Records updated: " + String.valueOf(rowNumbers));
            Toast.makeText(getBaseContext(), "Records Updated: ", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Log.d(TAG, "Update - error: " + e.getMessage());
        }

    }

    /*
     * Get string as JSON array of target package name and base64 signature.
     * Param - target package name as string
     * @return - String as JSON array of target package name and base64 signature
     * */
    private String getTargetAppPackageContent(String targetPackageName) {

        String targetAppPackageContent = "{\"pkgs_sigs\": " +
                "[" +
                "{" +
                "\"pkg\":" +
                "\"" +
                targetPackageName +
                "\"" +
                "," +
                "\"sig\":" +
                "\"" +
                readSignatureFromFile(targetPackageName) +
                "\"" +
                "}]}";
        return targetAppPackageContent;
    }

    /*
    * Read signature file for particular target package stored in assets.
    * Param - target package name as string
    * @return - Base64 signature string stored in file
    * */
    private String readSignatureFromFile(String targetPackageName) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getBaseContext().getAssets().open(targetPackageName + ".txt")));
            String line = reader.readLine();
            while (line != null) {
                sb.append(line); // process line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            Log.d(TAG,"IOException:");
            e.printStackTrace();
        }
        return sb.toString();
    }
}

class LocalContentObserver extends ContentObserver {
    public LocalContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
        Log.d(MainActivity.TAG, "### received self change notification from uri: ");
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(MainActivity.TAG, "### received notification from uri: " + uri.toString());
    }
}

class LocalDataSetObserver extends DataSetObserver {
    public LocalDataSetObserver() {

    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        Log.d(MainActivity.TAG,"onInvalidate");
    }

    @Override
    public void onChanged() {
        super.onChanged();
        Log.d(MainActivity.TAG,"onChanged");
    }
}
