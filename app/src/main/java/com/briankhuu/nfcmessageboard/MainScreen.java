package com.briankhuu.nfcmessageboard;

/*

    My Objective Here is to learn how to read and write to an NFC tag.

 */
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;

import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/*
    Imports Used for ForeGround Dispatch
 */
import android.content.Intent;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;

/*
    Exceptions Handling
* */
import android.nfc.FormatException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/*
    Imports Used for handling intent
 */
import android.nfc.Tag;
import java.util.Arrays;
import java.util.TimeZone;

/*
*   Imports Used for reading ndef tags
* */
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;


public class MainScreen extends ActionBarActivity {

    Context ctx;

    // Yea, I know. Copy pastas. I'm new to it. Cut me some slack.
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcTest";

    /*
    *  Staging Area For The Message To Append
    * */
    public static boolean armed_nfc_write = false;
    public static TextView entry_msg;
    public static TextView entry_name;

    /*
    *  For tag creation purpose
    * */
    public static boolean armed_write_to_empty_tag = false;


    /*
        Technical Display
     */
    public static TextView infoDisp;
    public static TextView tagInfoDisp;
    public int tag_size = 0;

    // Tag reference
    Tag tag;

    // What objects is needed?
    private NfcAdapter mNfcAdapter; // Sets up an empty object of type NfcAdapter

    // For text view that will display what we read
    private TextView mTextView;

    // Needed for nick saving
    SharedPreferences sharedPref;

    //timestamp setting
    public static CheckBox CheckBox_enable_timestamp;

    // Vibrate
    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);              // Saves the session
        setContentView(R.layout.activity_main_screen);   // Start the main activity (The GUI display)

        ctx = this;

        //setup vibrate
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        // Setup the Write Tag Interface
        entry_msg = (TextView)findViewById(R.id.edit_msg);
        entry_name = (TextView)findViewById(R.id.edit_name);

        //timestamp box
        CheckBox_enable_timestamp = (CheckBox)findViewById(R.id.enable_timestamp);


        // Setup the TextView display ( ::bk:: This looks very much like calling DOM objects in javascript )
        mTextView = (TextView) findViewById(R.id.textView_maindisplay);
        tagInfoDisp = (TextView) findViewById(R.id.textView_taginfo);

        // Contains General Alerts e.g. word count exceeding wordcounts etc...
        infoDisp = (TextView) findViewById(R.id.textView_info);

        // Setting up NFC (You need to have NFC and you need to enable it to use.
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); // Grabs the reference for current NfcAdapter used by the system
        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish(); // Stop here, we definitely need NFC
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled. Please enable.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Grabs and handles intent that just arrived (When the app just opened)
        handleIntent(getIntent());        


        /*
            Setup the user's preferred nick and timestamp settings
         */
        //nick
        sharedPref = ctx.getSharedPreferences( getString(R.string.preference_file_key), ctx.MODE_PRIVATE);
        String defaultNick = getResources().getString(R.string.defaultnick);
        String nick = sharedPref.getString(getString(R.string.edit_nick), defaultNick);
        entry_name.setText(nick);
        //timestamp setting
        Boolean enable_timestamp_value = sharedPref.getBoolean(getString(R.string.enable_timestamp), true);
        CheckBox_enable_timestamp.setChecked(enable_timestamp_value);


        /*
            Character Counting
         */
        // Msg Character count
        final TextWatcher entry_msg_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                infoDisp.setText( "Message Character Count:"+String.valueOf(s.length())+" | "+String.valueOf(140-s.length())+"/140" );
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        entry_msg.addTextChangedListener(entry_msg_watcher);

        // Nick Count
        final TextWatcher entry_name_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                infoDisp.setText( "Nickname Character Count:"+String.valueOf(s.length())+" | "+String.valueOf(20-s.length())+"/20" );
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        entry_name.addTextChangedListener(entry_name_watcher);
    }



    /*
        sharedPref Setting
     */
    public void setNickPref(String nick){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.edit_nick), nick);
        editor.commit();
    }
    public void setTimeStampPref(){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.enable_timestamp), CheckBox_enable_timestamp.isChecked());
        editor.commit();
    }


    /*
        Writing Message To Tag
        Using tutorial:
        http://www.framentos.com/en/android-tutorial/2012/07/31/write-hello-world-into-a-nfc-tag-with-a/
    * */

    public void addMsgButton(View view){
        // I think for now, just assume that the tag read is the current content.
        // Which is made easier by the fact that this app already auto read the tag on touch.
        //Toast.makeText(ctx, ":D", Toast.LENGTH_LONG ).show();

        if (armed_write_to_empty_tag) {
            Toast.makeText(ctx, "Please disable 'New Tag Creation Mode', before posting normal messages.", Toast.LENGTH_LONG ).show();
        }else{
            infoDisp.setText("Please Tap To Write Your Message");
            armed_nfc_write = true;
        }
        //add_message();
    }


    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        /*
            Note: might want to use "NdefRecord createTextRecord (String languageCode, String text)" instead from NdefRecord.createTextRecord()

         */
        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        /*
        *   We don't want to overwrite the first line if it's a header
        * */
        /*
         boolean headerExist = false;
        String headerText = "";
        if (text.substring(0,1).contains("#")){
            int indexNewline = text.indexOf("\n");
            if (indexNewline>0) {
                headerText = text.substring(0, indexNewline);
                text = text.substring(indexNewline + 1, text.length() );
            }else{
                headerText = text;
                text = "";
            }
            headerExist = true;
        }
        */

        /*
         http://stackoverflow.com/questions/11427997/android-app-to-add-mutiple-record-in-nfc-tag
          */
        // We want to include a reference to the app, for those who don't have one.
        String arrPackageName = "com.briankhuu.nfcmessageboard";
        final int AAR_RECORD_BYTE_LENGTH = 50; // I guess i suck at byte counting. well at least this should still work. This approach does lead to wasted space however.
        //infoMsg = "\n\n---\n To post here. Use the "NFC Messageboard" app: https://play.google.com/store/search?q=NFC%20Message%20Board ";


        // Trim to size (for now this is just a dumb trimmer...) (Later on, you want to remove whole post first
        // Seem that header and other things takes 14 chars. For safety. Lets just remove 20.
        // 0 (via absolute value) < valid entry size < Max Tag size
        final int NDEF_RECORD_HEADER_SIZE = 6;
        final int NDEF_STRING_PAYLOAD_HEADER_SIZE = 4;
        int maxTagByteLength = Math.abs(tag_size - NDEF_RECORD_HEADER_SIZE - NDEF_STRING_PAYLOAD_HEADER_SIZE - AAR_RECORD_BYTE_LENGTH);
        if (text.length() >= maxTagByteLength ){ // Write like normal if content to write will fit without modification
            // Else work out what to remove. For now, just do a dumb trimming. // Unicode characters may take more than 1 byte.
            text = truncateWhenUTF8(text, maxTagByteLength);
        }

        // Write tag
        //NdefRecord[] records = { createRecord(text), aarNdefRecord };
        NdefMessage message = new NdefMessage(new NdefRecord[]{
                createRecord(text)
                ,NdefRecord.createApplicationRecord(arrPackageName)
        });
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private void add_message(){
        /*
            Want to at least save your nickname first
            Oh and to also save the state of the timestamp
        * */
        setNickPref(entry_name.getText().toString());
        setTimeStampPref();
        /*
            This is called after a read function and activate if write is armed.
         */
        try {
            if(tag==null){
                Toast.makeText(ctx, ctx.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
            }else{
                String new_entry = "Hello World! Yo";
                // Get current time for timestamping
                String dateStamp_entry;
                if (CheckBox_enable_timestamp.isChecked()) {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); //SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'") //ISO date standard
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String dateStamp = df.format(new Date());
                    dateStamp_entry = ", D: " + dateStamp;
                } else {
                    dateStamp_entry = "";
                }
                // Calling the labels again manually (Just in case it dissapears for some reason (This is a hack? Its to avoid " W/Editor﹕ GetLabel fail! Do framework orig behavior" which causes the field to be empty )
                entry_msg = (TextView)findViewById(R.id.edit_msg);
                entry_name = (TextView)findViewById(R.id.edit_name);
                // Get the text
                String message = entry_msg.getText().toString();
                String nick = entry_name.getText().toString();
                 // Construct text
                new_entry = message+"\n_{=: "+nick+dateStamp_entry+" }\n\n" + mTextView.getText().toString();
                // Write to tag
                write(new_entry,tag);
                // Clear the message field. Name field is left alone. And all is done.
                entry_msg.setText("");
                infoDisp.setText("Message Written. Thank You.");
                // Lets vibrate!
                long[] pattern = {0, 200, 200, 200, 200, 200, 200};
                vibrator.vibrate(pattern,-1);
                // Update the display with what was posted to make user experience more responsive
                mTextView.setText(new_entry);
                // Let user know it's all gravy
                Toast.makeText(ctx, ctx.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
                infoDisp.setText("SUCCESS! New Tag Created");
            }
        } catch (IOException e) {
            Toast.makeText(ctx, "D: Cannot Write To Tag. (Tip: Hold up to tag and press ADD MSG) (type:IO)", Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        } catch (FormatException e) {
            Toast.makeText(ctx, "D: Cannot Write To Tag. (Tip: Hold up to tag and press ADD MSG)(type:Format)" , Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        }

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

    /*
    *  RESUME AND PAUSE SECTION
    * */
    @Override
    protected void onResume() { // App resuming from background        /* It's important, that the activity is in the foreground (resumed). Otherwise an IllegalStateException is thrown. */
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


    /*
        INTENT HANDLING
     */

    private void handleIntent(Intent intent) {

        /*
        *  Create new tag mode:
        * */
        if (armed_write_to_empty_tag){
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(ctx, "Writing tag", Toast.LENGTH_LONG ).show();
            try {
                if(tag==null){
                    Toast.makeText(ctx, ctx.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
                }else{
                    entry_msg = (TextView)findViewById(R.id.edit_msg);
                    // Get the text
                    String message = "# "+entry_msg.getText().toString();
                    // Write to tag
                    write(message,tag);
                    // Clear the message field. Name field is left alone. And all is done.
                    entry_msg.setText("");
                    infoDisp.setText("New tag created! Thank You.");
                    // Lets vibrate!
                    long[] pattern = {0, 200, 200, 200, 200, 200, 200};
                    vibrator.vibrate(pattern,-1);
                    // Update the display with what was posted to make user experience more responsive
                    mTextView.setText(message);
                    // Let user know it's all gravy
                    Toast.makeText(ctx, ctx.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
                }
            } catch (IOException e) {
                Toast.makeText(ctx, "D: Cannot Write To Tag. (Tip: Hold up to tag and press ADD MSG) (type:IO)", Toast.LENGTH_LONG ).show();
                e.printStackTrace();
            } catch (FormatException e) {
                Toast.makeText(ctx, "D: Cannot Write To Tag. (Tip: Hold up to tag and press ADD MSG)(type:Format)" , Toast.LENGTH_LONG ).show();
                e.printStackTrace();
            }
            // Success Message:
            infoDisp.setText("New Message Board Tag Created - Now disabling new tag write mode. Tap again to confirm content");
            // Let's revert back to normal behaviour
            armed_write_to_empty_tag = false;
            // commented away, because I think foreground dispatch on activation, actually pauses the activity. So this is not really needed.
            // Note: I think activity is paused on these situation: change scree, dialog, and foreground dispatch event.
            //resetForegroundDispatch();
            return;
        }

        /*
        *   Else just read tag as usual.
        * */
        /*
            This detects the intent and calls an nfc reading task
            I modified this to keep the tag object exposed in this class.
            This is since we will want to write to the same tag later.
         */
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {
                //uses: import android.nfc.Tag;
                tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
            } else {
                //Log.d(TAG, "Wrong mime type: " + type);
            }

        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }

        }

        /*
            If detect tag, vibrate as well
         */
        if (tag !=null){
            long[] pattern = {0, 500, 100};
            vibrator.vibrate(pattern,-1);
        }


        /*
            So some useful tag info
            http://stackoverflow.com/questions/9971820/how-to-read-detected-nfc-tag-ndef-content-details-in-android
         */
        if (tag != null) {
            // get NDEF tag details
            Ndef ndefTag = Ndef.get(tag);
            tag_size = ndefTag.getMaxSize();         // tag size
            boolean writable = ndefTag.isWritable(); // is tag writable?
            String type = ndefTag.getType();         // tag type

            // get NDEF message details
            NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
            NdefRecord[] ndefRecords = ndefMesg.getRecords();
            int len = ndefRecords.length;
            String[] recTypes = new String[len];     // will contain the NDEF record types
            for (int i = 0; i < len; i++) {
                recTypes[i] = new String(ndefRecords[i].getType());
            }

            //display technical info
            tagInfoDisp.setText("tag size: " + tag_size + " | writeable?: " + Boolean.toString(writable) + " | tag type: " + type + " | recTypes: " + TextUtils.join(",", recTypes));

            //Alert user if tag is write protected
            if (!writable){
                infoDisp.setText(getString(R.string.infoDisp_locked));
                Toast.makeText(ctx, ctx.getString(R.string.locked_tag) , Toast.LENGTH_LONG ).show();
            } else {
                infoDisp.setText(getString(R.string.infoDisp_writable));
                Toast.makeText(ctx, ctx.getString(R.string.writable_tag) , Toast.LENGTH_LONG ).show();
            }
        }

    }




    /*
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
        handleIntent(intent);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // ::bk:: Not quite sure whats going on here...
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
        if (!armed_write_to_empty_tag) {
            filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            try {
                filters[0].addDataType(MIME_TEXT_PLAIN);
            } catch (MalformedMimeTypeException e) {
                throw new RuntimeException("Check your mime type.");
            }
        } else {
            filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        }
        /*
            Put filter to the foreground dispatch.
         */
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding BaseActivity requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    /*
        MENUS
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.creating_a_tag:
                // show the dialog window
                new AlertDialog.Builder(this)
                        .setTitle("Creating A Tag")
                        .setMessage(getString(R.string.tag_creation))
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Add your code for the button here.
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            case R.id.write_new_tag:
                infoDisp.setText("New tag writing mode - !!Armed!! - Any text in message field will be used as the header message. Please tap on empty NFC tag.");
                Toast.makeText(ctx, "Please Tap To Create New Message Board Tag", Toast.LENGTH_LONG ).show();
                armed_write_to_empty_tag = true;
                resetForegroundDispatch();
                return true;
            case R.id.cancel_write_new_tag:
                infoDisp.setText("New tag writing mode - Disarmed");
                Toast.makeText(ctx, "Disarmed", Toast.LENGTH_LONG ).show();
                armed_write_to_empty_tag = false;
                resetForegroundDispatch();
                return true;
            case R.id.about:
                int versionCode = BuildConfig.VERSION_CODE;
                String versionName = BuildConfig.VERSION_NAME;
                new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage(getString(R.string.about_app)+" | VerCode:"+versionCode+" | VerName: "+versionName)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Add your code for the button here.
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
        infoDisp.setText("Please Tap To Write Your Message");
        armed_nfc_write = true;
    * */

    /**
     *
     *
     *
     *  INNNER CLASSES
     *
     *
     */

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        //Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
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

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText(result);
            }

            /*
            *   Auto writes message when armed_nfc_write is activated
            * */
            if ( armed_nfc_write ){
                add_message();
                armed_nfc_write = false;
            }
        }
    }

}
