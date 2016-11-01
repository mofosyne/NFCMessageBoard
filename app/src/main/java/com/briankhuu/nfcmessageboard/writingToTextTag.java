/*
*
* Basically this activity is focused on one thing and one thing only.
* Do well with writing to an NFC tag.
*
* If we want to read well, then make it another activity.
*
* TODO: GIVE THIS A SHOT. IT HAS GOOD INFO
* http://www.jessechen.net/blog/how-to-nfc-on-the-android-platform/
*
*
* */



package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static java.sql.Types.NULL;

public class WritingToTextTag extends AppCompatActivity {
    static String arrPackageName = "com.briankhuu.nfcmessageboard";
    static boolean write_mode = false;
    private static final String LOGGER_TAG = WritingToTextTag.class.getSimpleName();


    // Activity context
    Context ctx;


    // Tag reference
    Tag tag;
    private NfcAdapter mNfcAdapter; // Sets up an empty object of type NfcAdapter

    // We have a plain text mode for historical reason.
    public static final String MIME_TEXT_PLAIN = "text/plain";
    //public static final String TAG = "NfcTest"; // Don't think this is required


    // Information that we want to write to the tag
    public enum Message_mode {
        SIMPLE_TXT_MODE,        // Legacy support for txt only NFC bbs tags (could try doing something like "sms speak compression"
        STRUCTURED_TXT_MODE     // This is envisioned to be for tags that stores messages in a packed binary method (e.g. think messagepack) to make it easier to tag metadata to it
    }

    public Message_mode message_mode;
    public String message_str;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writing_to_text_tag);

        /* Activity Context For Toast to work easier!*/
        ctx = this;

        /* Setup NFC Adapter
        * */
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


        /* Read and process incoming android inten
        * */
        // Select mode
        String message_tag_type_str = getIntent().getStringExtra("tag_type");

        if (message_tag_type_str == null)
        {   // No intent was detected. Provide default content (good for testing)
            message_mode = Message_mode.SIMPLE_TXT_MODE;
            message_str = "This is an example text content to be included into this tag";
        }
        else
        {   // Intent Is Present
            if ( message_tag_type_str.equals("txt") )
            {
                message_mode = Message_mode.SIMPLE_TXT_MODE;
            }
            if ( message_tag_type_str.equals("struct-text") )
            {
                message_mode = Message_mode.STRUCTURED_TXT_MODE;
            }
            // fill in the intent with message that the user want to write to the tag
            message_str = getIntent().getStringExtra("tag_content");
        }



    }



    /****************************************************************************************************************************************************************
    ForeGround Dispatch
    */

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        // Guard
        if (intent == null)
        {
            Log.e( LOGGER_TAG,  "onNewIntent:"
                        +(intent==null?"null intent,":"")
                        );
            return;
        }

        handleIntent(intent);
    }

    // Aka: enable tag write mode
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter)
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

        write_mode = true;

        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        /*
            Setting up the container filter (It's the trigger)
         */
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
        /*
            Fill the filter with the same settings you had in your manifest
         */
        // Notice that this is the same filter as in our manifest.
        // ::bk:: Ah I see thanks. So just gotta make sure it matches.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        /*
            Put filter to the foreground dispatch.
         */
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
        adapter.disableForegroundDispatch(activity);
    }

    /*
    *  RESUME AND PAUSE SECTION
    * */
    @Override
    protected void onResume()
    { // App resuming from background        /* It's important, that the activity is in the foreground (resumed). Otherwise an IllegalStateException is thrown. */
        super.onResume();
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() { // App sent to background (when viewing other apps etc...) /* Call this before onPause, otherwise an IllegalArgumentException is thrown as well. */
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    /*
    *  Reset forground dispatch for tag creation purpose
    * */

    private void resetForegroundDispatch(){
        stopForegroundDispatch(this, mNfcAdapter);
        setupForegroundDispatch(this, mNfcAdapter);
    }

    /****************************************************************************************************************************************************************
        INTENT HANDLING
     */

    // Used by handleIntent()
    // By maybewecouldstealavan from http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }



    // TODO: Some way to auto verify and rewrite if tag verification fails
    private void handleIntent(Intent intent)
    {
        /*
        *  Create new tag mode:
        * */
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Toast.makeText(ctx, "Writing tag", Toast.LENGTH_LONG ).show();
        try
        {
            if(tag==null)
            {  // We expect that a tag has been detected
                Toast.makeText(ctx, ctx.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
                Log.e( LOGGER_TAG, ctx.getString(R.string.error_detected) );
            }
            else
            {
                // Write to tag
                write(message_str,tag);
                // Let user know it's all gravy
                Toast.makeText(ctx, ctx.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
            }
        }
        catch (IOException e)
        {
            Toast.makeText(ctx, "Cannot Write To Tag. (type:IO)", Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        }
        catch (FormatException e)
        {
            Toast.makeText(ctx, "Cannot Write To Tag. (type:Format)" , Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        }
        // commented away, because I think foreground dispatch on activation, actually pauses the activity. So this is not really needed.
        // Note: I think activity is paused on these situation: change scree, dialog, and foreground dispatch event.
        //resetForegroundDispatch();

        return;
    }


    /****************************************************************************************************************************************************************
        CREATE AND WRITE RECORDS
        --> createRecord() , truncateWhenUTF8() , write()
     */

    // Used in write()
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        /*
            Note: might want to use "NdefRecord createTextRecord (String languageCode, String text)" instead from NdefRecord.createTextRecord()
         */
        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        byte[] payload;
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    // http://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en
    public static String truncateWhenUTF8(String s, int maxBytes) {
        int b = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // ranges from http://en.wikipedia.org/wiki/UTF-8
            int skip = 0;
            int more;
            if (c <= 0x007f) {
                more = 1;
            }
            else if (c <= 0x07FF) {
                more = 2;
            }
            else if (c <= 0xd7ff) {
                more = 3;
            }
            else if (c <= 0xDFFF) {
                // surrogate area, consume next char as well
                more = 4;
                skip = 1;
            } else {
                more = 3;
            }

            if (b + more > maxBytes) {
                return s.substring(0, i);
            }
            b += more;
            i += skip;
        }
        return s;
    }


    private void write(String text, Tag tag) throws IOException, FormatException {
        int tag_size=0;

        if ((tag == null))
        {// Requires tag
            Log.e( LOGGER_TAG, "setupForegroundDispatch:"
                    +(tag==null ? "tag adapter," : "" )
            );
            return;
        }

        {// Calc tag size?
            // get NDEF tag details
            Ndef ndefTag = Ndef.get(tag);
            tag_size = ndefTag.getMaxSize();         // tag size
            boolean writable = ndefTag.isWritable(); // is tag writable?
            String type = ndefTag.getType();         // tag type
        }

        {// text -->[ trim text to fit in tag ]--> text
            /*
                http://stackoverflow.com/questions/11427997/android-app-to-add-mutiple-record-in-nfc-tag
            */
            // We want to include a reference to the app, for those who don't have one.
            // This way, their phones will open this app when a tag encoded with this app is used.
            final int AAR_RECORD_BYTE_LENGTH = 50; // I guess i suck at byte counting. well at least this should still work. This approach does lead to wasted space however.
            //infoMsg = "\n\n---\n To post here. Use the "NFC Messageboard" app: https://play.google.com/store/search?q=NFC%20Message%20Board ";

            // Trim to size (for now this is just a dumb trimmer...) (Later on, you want to remove whole post first
            // Seem that header and other things takes 14 chars. For safety. Lets just remove 20.
            // 0 (via absolute value) < valid entry size < Max Tag size
            final int NDEF_RECORD_HEADER_SIZE = 6;
            final int NDEF_STRING_PAYLOAD_HEADER_SIZE = 4;
            int maxTagByteLength = Math.abs(tag_size - NDEF_RECORD_HEADER_SIZE - NDEF_STRING_PAYLOAD_HEADER_SIZE - AAR_RECORD_BYTE_LENGTH);
            if (text.length() >= maxTagByteLength) { // Write like normal if content to write will fit without modification
                // Else work out what to remove. For now, just do a dumb trimming. // Unicode characters may take more than 1 byte.
                text = truncateWhenUTF8(text, maxTagByteLength);
            }
        }

        {// Write tag
            //NdefRecord[] records = { createRecord(text), aarNdefRecord };
            NdefMessage message = new NdefMessage(new NdefRecord[]{
                    createRecord(text)
                    , NdefRecord.createApplicationRecord(arrPackageName)
            });
            Ndef ndef = Ndef.get(tag);
            ndef.connect();
            ndef.writeNdefMessage(message);
            ndef.close();
        }
    }
}

