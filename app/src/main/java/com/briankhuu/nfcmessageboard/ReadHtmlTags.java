package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.Arrays;

public class ReadHtmlTags extends Activity {


    public enum CompressionMode_Enum
    {
        NONE,
        GZIP // Deflate Based https://en.wikipedia.org/wiki/Gzip
    }


    private static final String LOGGER_TAG = WritingToTextTag.class.getSimpleName();
    public static final String MIME_TEXT_HTML = "text/html";
    public static final String MIME_TEXT_HTML_GZIP = "text/html+gzip"; // http://superuser.com/questions/901962/what-is-the-correct-mime-type-for-a-tar-gz-file

    // Content of the html page to render
    String html_value = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"><title>Lorem Ipsum</title></head><body style=\"width:300px; color: #00000; \"><p><strong> About us</strong> </p><p><strong> Lorem Ipsum</strong> is simply dummy text .</p><p><strong> Lorem Ipsum</strong> is simply dummy text </p><p><strong> Lorem Ipsum</strong> is simply dummy text </p></body></html>";
    WebView myWebView;

    NfcAdapter mNfcAdapter;
    Tag tag;
    CompressionMode_Enum tag_compression_mode;

    Activity ctx;

    /*
        Technical Display
     */
    public static String tagID_string = "";
    public static TextView tagID_Disp;
    public static TextView tagInfoDisp;
    public int tag_size = 0;
    public String[] recTypes;
    public String tag_type;
    public boolean tag_writable;
    public int tag_html_payload_size;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_html_tags);

        ctx = this;

        // Technical Display
        tagID_Disp = (TextView) findViewById(R.id.textView_tagID);

        // Setting up NFC (You need to have NFC and you need to enable it to use.
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); // Grabs the reference for current NfcAdapter used by the system
        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC.", Toast.LENGTH_LONG).show();
            finish(); // Stop here, we definitely need NFC
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        syncWebView_data(html_value); //Placeholder
        // Grabs and handles intent that just arrived (When the app just opened)
        handleIntent(getIntent());

        WebView myWebView = (WebView) findViewById(R.id.htmldisp);
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.getSettings().setJavaScriptEnabled(true);
    }

    /**
    *   Deal with HTML loading
    */
    public void syncWebView_data(String htmlcontent) {
        WebView myWebView = (WebView) findViewById(R.id.htmldisp);
        WebSettings webSettings = myWebView.getSettings();
        // height support
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        myWebView.setLayoutParams(new RelativeLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels, (int) (height * getResources().getDisplayMetrics().density)));
        // zoom support
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        //myWebView.setInitialScale(1);
        myWebView.loadData(htmlcontent, "text/html; charset=UTF-8", null);
    }

    public void syncWebView_url(String datauriString) {
        WebSettings webSettings = myWebView.getSettings();
        // zoom support
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        //myWebView.setInitialScale(1);
        myWebView.loadUrl(datauriString);
    }

    /****
     *  Deal with NFC loading
     */

    // Used by handleIntent()
    // By maybewecouldstealavan from http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void handleIntent(Intent intent) {
        /*
        *   Else just read tag as usual.
        * */
        /*
            This detects the intent and calls an nfc reading task
            I modified this to keep the tag object exposed in this class.
            This is since we will want to write to the same tag later.
         */
        String action = intent.getAction();

        // We want to read only valid html tags
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {
            boolean mime_type_found = false;
            String type = intent.getType();

            if (MIME_TEXT_HTML.equals(type))
            {
                Toast.makeText(this, "Reading Html Tag", Toast.LENGTH_SHORT).show();
                tag_compression_mode = CompressionMode_Enum.NONE;
                mime_type_found = true;
            }
            else
            if (MIME_TEXT_HTML_GZIP.equals(type))
            {
                Toast.makeText(this, "Reading Gzipped Html Tag", Toast.LENGTH_SHORT).show();
                tag_compression_mode = CompressionMode_Enum.GZIP;
                mime_type_found = true;
            }


            // if plain/html
            if (mime_type_found) {
                tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
            } else {
                //Log.d(TAG, "Wrong mime type: " + type);
            }
        }
        else
        {
            Log.e(LOGGER_TAG, "Skipping Tag as it is not NDEF");
        }

        /*
            So some useful tag info
            http://stackoverflow.com/questions/9971820/how-to-read-detected-nfc-tag-ndef-content-details-in-android
         */
        if (tag != null) {
            //Toast.makeText(this, "Reading Tag Info", Toast.LENGTH_SHORT).show();
            // get NDEF tag details
            Ndef ndefTag = Ndef.get(tag);

            tag_writable = ndefTag.isWritable(); // is tag writable?

            // get NDEF message details
            NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
            NdefRecord[] ndefRecords = ndefMesg.getRecords();
            int len = ndefRecords.length;
            recTypes = new String[len];     // will contain the NDEF record types
            for (int i = 0; i < len; i++) {
                recTypes[i] = new String(ndefRecords[i].getType());
            }

            // Get Tag ID
            tagID_string = bytesToHex(tag.getId());
            tag_size = ndefTag.getMaxSize();     // tag size
            tag_type = ndefTag.getType();         // tag type
        }
    }




    /***********************************************************************************************
     * ForeGround Dispatch
     * */

    @Override
    protected void onNewIntent(Intent intent)
    {   // This is called upon new incoming intent. In this context an NFC tag is being processed
        handleIntent(intent);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter)
    {
        final Intent        intent;
        final PendingIntent pendingIntent;

        // Guard
        if ((activity == null)||(adapter == null))
        {
            Log.e( LOGGER_TAG, "setupForegroundDispatch:"
                    +(activity==null?"null activity,":"")
                    +(adapter==null?"null adapter,":"" )
            );
            return;
        }

        // Set up Intent
        intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        // Setting Up Intent Filter And Tech List
        IntentFilter[]  filters     = new IntentFilter[1];
        String[][]      techList    = new String[][]{};

        // We are looking for any tag
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        // Start the Dispatch
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);

    }

    /**
     * @param activity The corresponding BaseActivity requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter)
    {
        // Guard
        if ((activity == null)||(adapter == null))
        {
            Log.e( LOGGER_TAG, "setupForegroundDispatch:"
                    +(activity==null?"null activity,":"")
                    +(adapter==null?"null adapter,":"" )
            );
            return;
        }

        // Stop the Dispatch
        adapter.disableForegroundDispatch(activity);
    }


    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String>
    {

        @Override
        protected String doInBackground(Tag... params)
        {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null)
            {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records)
            {
                if (ndefRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA)
                {
                    try
                    {
                        return readText(ndefRecord);
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        Log.e(LOGGER_TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException
        {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();


            switch (tag_compression_mode) {
                case NONE:
                {
                    // For html byte array payload, we always assume that it is UTF-8 and we load the whole payload as a string.
                    tag_html_payload_size = payload.length;

                    // Get the Text
                    return new String(payload, 0, payload.length, "UTF-8");
                }
                case GZIP:
                {   // http://stackoverflow.com/questions/23606047/decompressing-to-a-byte-using-gzipinputstream

                    String uncompressed_string = "";
                    char[] buffer = new char[10240];
                    ByteArrayInputStream byte_array_input = new ByteArrayInputStream(payload);
                    Writer writer = new StringWriter();

                    try
                    {   // Decompressing Gzip
                        InputStream ungzippedResponse = new GZIPInputStream(byte_array_input);
                        InputStreamReader reader = new InputStreamReader(ungzippedResponse, "UTF-8");

                        for (int length ; (length = reader.read(buffer)) > 0 ; )
                        {
                            writer.write(buffer, 0, length);
                        }

                        uncompressed_string = writer.toString();

                    }
                    catch (IOException e)
                    {
                        Toast.makeText(ctx, "GZIP error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    return uncompressed_string;
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d(LOGGER_TAG, "Content" + result);
                syncWebView_data(result); //Placeholder

                tagID_Disp.setText("TAG-ID: 0x"+tagID_string+" | bytes-free: "+tag_size+"-"+ tag_html_payload_size + "="+ (tag_size-tag_html_payload_size) );
            }
        }
    } // End of NdefReaderTask
}