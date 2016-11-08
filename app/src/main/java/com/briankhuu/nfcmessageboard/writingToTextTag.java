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
import android.nfc.tech.NdefFormatable;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ThreadLocalRandom;

public class WritingToTextTag extends AppCompatActivity {
    static String arrPackageName = "com.briankhuu.nfcmessageboard";
    private static final String LOGGER_TAG = WritingToTextTag.class.getSimpleName();

    // Activity context
    Context ctx;

    // Tag reference
    Tag tag;
    private NfcAdapter mNfcAdapter; // Sets up an empty object of type NfcAdapter

    // Haptic Feedback
    Vibrator vibrator;

    // Status Display
    public static TextView textView_infoDisp;

    // Information that we want to write to the tag
    public enum MessageWriteStatus_Enum {
        INITIALISE,                         //
        SUCCESS,                            // Successfully written into tag
        FAILED,                             // Could probbly just try again.
        FAILED_BECAUSE_CONTENT_MISMATCH,    // Could probbly just try again.
        FAILED_BECAUSE_IO_EXCEPTION,
        FAILED_BECAUSE_FORMAT_EXCEPTION,
        FAILED_BECAUSE_TAG_LOST,
        FAILED_BECAUSE_NULL_NDEF,
        FAILED_BECAUSE_INSUFFICIENT_SPACE,
        FAILED_BECAUSE_WRITE_PROTECTED         // Tag is write protected so just report and quit...
    }

    // Information that we want to write to the tag
    public enum MessageMode_Enum {
        SIMPLE_TXT_MODE,        // Legacy support for txt only NFC bbs tags (could try doing something like "sms speak compression"
        STRUCTURED_TXT_MODE     // This is envisioned to be for tags that stores messages in a packed binary method (e.g. think messagepack) to make it easier to tag metadata to it
    }


    // Deals with tag instances
    public class TagContent {
        MessageWriteStatus_Enum successfulWrite_status = MessageWriteStatus_Enum.INITIALISE;
        MessageMode_Enum message_mode = MessageMode_Enum.SIMPLE_TXT_MODE;
        String message_str = "";
    }

    TagContent tagContent = new TagContent();

    /***********************************************************************************************
     * Activity Lifecycle
     * https://developer.android.com/reference/android/app/Activity.html
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writing_to_text_tag);

        /* Activity Context For Toast to work easier!
        * */

        // Record the activity context pointer
        ctx = this;


        //setup vibrate
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        // TextView
        textView_infoDisp = (TextView) findViewById(R.id.textView_infoDisp);

        /* Setup NFC Adapter
        * */

        // Setting up NFC (You need to have NFC and you need to enable it to use.
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); // Grabs the reference for current NfcAdapter used by the system

        if (mNfcAdapter == null) {   // NFC is not supported
            Toast.makeText(this, "This device does not support NFC.", Toast.LENGTH_LONG).show();
            finish(); // Stop here, we definitely need NFC
            return;
        }

        if (!mNfcAdapter.isEnabled()) {   // NFC is Disabled
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        /* Read and process incoming android activity intent
        * */

        // Select mode
        String message_tag_type_str = getIntent().getStringExtra("tag_type");

        if (message_tag_type_str == null) {   // No intent was detected. Provide default content (good for testing)
            int random_number = ThreadLocalRandom.current().nextInt(0, 1000 + 1);

            // Load in default test content
            this.tagContent.message_mode = MessageMode_Enum.SIMPLE_TXT_MODE;
            this.tagContent.message_str = "This is an example text content to be included into this tag (" + Integer.toString(random_number) + ")";

        } else {   // Activity Intent Is Present

            // Load in content from incoming activity intent
            if (message_tag_type_str.equals("txt"))
                this.tagContent.message_mode = MessageMode_Enum.SIMPLE_TXT_MODE;
            if (message_tag_type_str.equals("struct-text"))
                this.tagContent.message_mode = MessageMode_Enum.STRUCTURED_TXT_MODE;

            // fill in the intent with message that the user want to write to the tag
            this.tagContent.message_str = getIntent().getStringExtra("tag_content");

        }


        /* Install Button Listeners
        * */

        // Cancel Button
        final Button button_write_tag = (Button) findViewById(R.id.button_cancel);
        button_write_tag.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        completed_and_now_returning(false); // Return write tag failed
                    }
                }
            );



    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    protected void onResume()
    {   // App resuming from background
        super.onResume();   // Call parent superclass. Otherwise an IllegalStateException is thrown.
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause()
    {   // App sent to background (when viewing other apps etc...)
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();    // Call parent superclass. otherwise an IllegalArgumentException is thrown as well.
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {   // Activity is closing (e.g. via  finish() ). May not reach here if app is killed to free up memory
        super.onDestroy();
    }

    /**********************************************************************************************/


    /***********************************************************************************************
     * Report Success or Failure to Write to parent app
     * e.g. http://stackoverflow.com/questions/22553672/android-startactivityforresult-setresult-for-a-view-class-and-an-activity-cla#22554156
     * e.g. https://developer.android.com/reference/android/app/Activity.html
     */
    protected void completed_and_now_returning(boolean write_successful)
    {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("extra stuff","lol");

        if (write_successful)
        {   // Successful Write
            setResult(Activity.RESULT_OK, resultIntent);
        }
        else
        {   // Failed Write
            setResult(Activity.RESULT_CANCELED, resultIntent);
        }

        // Kill this activity and return to original
        finish();
    }

    /*
        e.g.
        ## in calling activity:
            Intent intent = new Intent(v.getContext(), SecondActivity.class);
            startActivityForResult(
                         intent,
                         WRITE_TAG // = request code
                         );

         ## And later on:
             protected void onActivityResult(int requestCode, int resultCode, Intent data)
             {
                 if (requestCode == WRITE_TAG)
                 {
                     if (resultCode == Activity.RESULT_OK)
                     {
                         // Something to process the intent
                     }
                 }
            }
    */

    /***********************************************************************************************
     * ForeGround Dispatch
     * */

    @Override
    protected void onNewIntent(Intent intent)
    {   // This is called upon new incoming intent. In this context an NFC tag is being processed

        // Guard
        if (intent == null)
        {
            Log.e( LOGGER_TAG,  "onNewIntent:"
                        +(intent==null?"null intent,":"")
                        );
            return;
        }

        handle_NfcAdapter_Intent(intent);
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


    private void resetForegroundDispatch()
    {   // Foreground Dispatch Reset
        stopForegroundDispatch(this, mNfcAdapter);
        setupForegroundDispatch(this, mNfcAdapter);
    }

    /***********************************************************************************************
     *  INTENT HANDLING
     */

    // TODO: Some way to auto verify and rewrite if tag verification fails
    private void handle_NfcAdapter_Intent(Intent intent)
    {   // Handles any incoming NFC Adapter based intent

        //  Get NFC Tag Content (returns null if not present)
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(tag==null)
        {  // We expect that a tag has been detected
            Log.e( LOGGER_TAG, "handle_NfcAdapter_Intent:"
                    +"Intent Extra NfcAdapter.EXTRA_TAG Missing"
            );
            return;
        }

        Log.d( LOGGER_TAG, "Writing tag");

        writeMessageTag(this.tagContent,tag);

        return;
    }


    /***********************************************************************************************
     *  CREATE AND WRITE RECORDS
        --> createRecord() , truncateWhenUTF8() , write()
     */

    // Used in write()
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException
    {
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

        // Payload
        payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        // Return NDEF Record
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    // http://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en
    public static String truncateWhenUTF8(String s, int maxBytes)
    {
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


    private void writeMessageTag(TagContent tagContent_input, Tag tag) //throws IOException, FormatException
    {
        NdefRecord text_NdefRecord          = null;
        NdefRecord androidAAR_NdefRecord;
        int tag_size=0;

        if ((tag == null))
        {// Requires tag
            Log.e( LOGGER_TAG, "setupForegroundDispatch:"
                    +(tag==null ? "tag adapter," : "" )
            );
            textView_infoDisp.setText("tag missing?");
            return;
        }

        {// Tag
            // get NDEF tag details
            Ndef ndefTag = Ndef.get(tag);

            if (ndefTag == null)
            {   // Is not ndef formatted yet. Try to set this new tag up.
                Log.d(LOGGER_TAG, "New tag detected. Attempting to format tag.");

                NdefMessage emptyNdefMessage = new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null));

                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null)
                {
                    try
                    {
                        formatable.connect();
                        formatable.format(emptyNdefMessage);
                    }
                    catch (Exception e)
                    {
                        // let the user know the tag refused to connect
                        Log.e(LOGGER_TAG, "Tag Refuse to Connect for formatting");
                        textView_infoDisp.setText("Tag Refuse to Connect for formatting. Is this tag broken?");
                        Toast.makeText(ctx, "Tag Refuse to Connect for formatting", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        return;
                    }
                    finally
                    {
                        try
                        {
                            formatable.close();
                        }
                        catch (IOException e)
                        {
                            Log.e(LOGGER_TAG, "Cannot close tag while formatting");
                            textView_infoDisp.setText("Cannot close tag while formatting. Try tapping again.");
                            Toast.makeText(ctx, "Cannot close tag while formatting", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                            return;
                        }
                    }
                }
                else
                {
                    // let the user know the tag cannot be formatted
                    Log.e(LOGGER_TAG, "Cannot format NFC tag. Is not NdefFormatable");
                    textView_infoDisp.setText("Cannot format NFC Tag. Try preformatting this tag with some NDEF content. (NXP TagWriter could help here)");
                    Toast.makeText(ctx, "Cannot format NFC tag. Is not NdefFormatable", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Lets vibrate!
                long[] pattern = {0, 400, 400};
                vibrator.vibrate(pattern, -1);

                // Let user know that the tag has been formatted
                Log.d(LOGGER_TAG, "Tag formatted, please tap again");
                textView_infoDisp.setText("The tag was previously not formatted to the NDEF standard. Now it should be. Tap again to start writing to the tag.");
                Toast.makeText(ctx, "New tag was formatted. Please tap again.", Toast.LENGTH_SHORT).show();
                return;

            }

            // Get Tag Size
            tag_size = ndefTag.getMaxSize();
            Log.d( LOGGER_TAG, "tagsize:" + Integer.toString(tag_size) );

            // Check Tag Writability (That it is not read only)
            if (ndefTag.isWritable() != true)
            {   // Requires tag
                Log.e( LOGGER_TAG, "setupForegroundDispatch:"
                        +" Tag Is Not Writable "
                );
                Toast.makeText(ctx, "Tag was set to read only.", Toast.LENGTH_SHORT ).show();
                textView_infoDisp.setText("The tag reported that it was sent to read only. Maybe you got a ready only tag? Or something corrupted the tag.");
                return;
            }
        }

        {// Generate AAR Package Name
            androidAAR_NdefRecord = NdefRecord.createApplicationRecord(arrPackageName);
        }

        {// Generate text_NdefRecord (Could we use NdefRecord.createTextRecord() instead?)

            // Input Message String
            String text = tagContent_input.message_str;

            //  http://stackoverflow.com/questions/11427997/android-app-to-add-mutiple-record-in-nfc-tag
            final int AAR_RECORD_BYTE_LENGTH = 50;          // Estimated size of the AAR Record Byte, because I have no idea how to find the exact size.
            final int NDEF_RECORD_HEADER_SIZE = 6;          // Estimated size of NDEF Record Header
            final int NDEF_STRING_PAYLOAD_HEADER_SIZE = 4;  // Estimated size of NDEF string payload header size

            // Calc maximum safe text size
            int maxTagByteLength = Math.abs(tag_size - NDEF_RECORD_HEADER_SIZE - NDEF_STRING_PAYLOAD_HEADER_SIZE - AAR_RECORD_BYTE_LENGTH);
            if (text.length() >= maxTagByteLength) { // Write like normal if content to write will fit without modification
                // Else work out what to remove. For now, just do a dumb trimming. // Unicode characters may take more than 1 byte.
                text = truncateWhenUTF8(text, maxTagByteLength);
            }

            // Output Ndef Record
            try {
                text_NdefRecord = createRecord(text);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        {// Write tag
            MessageWriteStatus_Enum message_write_status = MessageWriteStatus_Enum.FAILED; // Generic Fail

            if ((text_NdefRecord == null) || (androidAAR_NdefRecord == null)) {
                return;
            }


            // NdefRecord[] records = { createRecord(text), aarNdefRecord };
            NdefMessage message = new NdefMessage(new NdefRecord[]{
                    text_NdefRecord
                    ,
                    androidAAR_NdefRecord
            });


            // Maximum Five Times Retries
            for (int i = 0; i < 5; i++) {
                message_write_status = writeNdefMessageToTag(message, tag, false);

                Log.d(LOGGER_TAG, "Writing To Tag (attempt: " + Integer.toString(i) + ") Status: " + message_write_status.toString());

                // Stop Loop as tag was written successfully
                if (message_write_status == MessageWriteStatus_Enum.SUCCESS)
                    break;  // Write Successful

                // Tag was lost, no use retrying. (Or you will end up with "java.lang.IllegalStateException: Close other technology first" exception)
                if (message_write_status == MessageWriteStatus_Enum.FAILED_BECAUSE_IO_EXCEPTION)
                    break;  // Tag Communication Was Lost
            }

            // Report Status
            tagContent_input.successfulWrite_status = message_write_status;

            switch (message_write_status) {
                case INITIALISE:
                    Toast.makeText(ctx, "FAILED: Did it not write?", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("Whoa that's amazing. Din't not expect you would see this :S");
                    break;
                case SUCCESS:   // Can close display now
                    //Toast.makeText(ctx, "Tag content is confirmed written successfully", Toast.LENGTH_SHORT ).show();
                    textView_infoDisp.setText("All good. Tag is now written. Returning");

                    // Lets vibrate!
                    long[] pattern = {0, 200, 200, 200, 200, 200, 200};
                    vibrator.vibrate(pattern, -1);

                    completed_and_now_returning(true);
                    break;
                case FAILED:    // Prompt user to tap?
                    Toast.makeText(ctx, "Tag writing failed for unknown reason. Tap again?", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("Not sure what happened. Try again.");
                    break;
                case FAILED_BECAUSE_CONTENT_MISMATCH: // Prompt User to tap again
                    Toast.makeText(ctx, "Tag content mismatch. Tap again", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("There was a possible write corruption. You should tap again to try and fix this.");
                    break;
                case FAILED_BECAUSE_IO_EXCEPTION:   // Exit failed
                    Toast.makeText(ctx, "Cannot Write To Tag. (type:IO). Try again?", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("There was an IO error. The tag could have been physically misaligned during write. Hold it securely to the phone on the next tap.");
                    break;
                case FAILED_BECAUSE_FORMAT_EXCEPTION: // Exit failed? (or make sure to write new NDEF?)
                    Toast.makeText(ctx, "Cannot Write To Tag. (type:Format). Not NDEF formatted?", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("The tag is not yet formatted. Please preformat your tag to use NDEF.");
                    break;
                case FAILED_BECAUSE_TAG_LOST:   // Can't trigger this yet
                    Toast.makeText(ctx, "Lost connection to tag. Tap again.", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("Connection was lost to the tag. Please tap again. Make sure to hold it more securely.");
                    break;
                case FAILED_BECAUSE_NULL_NDEF:
                    Toast.makeText(ctx, "Tag Write Failed: NULL NDEF", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("NULL NDEF error (not formatted yet?)");
                    break;
                case FAILED_BECAUSE_INSUFFICIENT_SPACE:
                    Toast.makeText(ctx, "Cannot Write To Tag. Message is too big", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("Message is too big for the tag");
                    break;
                case FAILED_BECAUSE_WRITE_PROTECTED:
                    Toast.makeText(ctx, "Cannot Write To Tag. Tag is Read Only", Toast.LENGTH_SHORT).show();
                    textView_infoDisp.setText("Tag is write protected. You should use a new one.");
                    break;
            }

        }

    }

    public MessageWriteStatus_Enum writeNdefMessageToTag(NdefMessage message, Tag tag, boolean writeProtect)
    {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect(); // ( Throws: IOException )

                if (!ndef.isWritable())
                {
                    return MessageWriteStatus_Enum.FAILED_BECAUSE_WRITE_PROTECTED; // Tag is read-only;
                }

                if (ndef.getMaxSize() < message.toByteArray().length)
                {
                    return MessageWriteStatus_Enum.FAILED_BECAUSE_INSUFFICIENT_SPACE; // "size error"
                }

                ndef.writeNdefMessage(message); // ( Throws: FormatException )

                if ( writeProtect ==  true ) // Uses Boolean instead of boolean for nullable boolean value
                {
                    Toast.makeText(ctx, "Setting Tag to Write Only", Toast.LENGTH_SHORT ).show();
                    ndef.makeReadOnly();
                }

                // Checking if tag is written correctly
                if(ndef.getNdefMessage().equals(message))   // (ndef throws null exception if tag is missing)
                {   // Read and check somehow?
                    return MessageWriteStatus_Enum.SUCCESS;
                }


                ndef.close(); // ( Throws: IOException if tag is missing)


                // Content Mismatch
                return MessageWriteStatus_Enum.FAILED_BECAUSE_CONTENT_MISMATCH;

            } else {
                return MessageWriteStatus_Enum.FAILED_BECAUSE_NULL_NDEF; // writeTag: ndef==null!
            }
        }
        catch (IOException e)
        {   // IO Error (In ndef.connect or ndef.close )
            e.printStackTrace();
            return MessageWriteStatus_Enum.FAILED_BECAUSE_IO_EXCEPTION;
        }
        catch (FormatException e)
        {   // Format Error (In ndef.writeNdefMessage)
            e.printStackTrace();
            return MessageWriteStatus_Enum.FAILED_BECAUSE_FORMAT_EXCEPTION;
        }
        catch (NullPointerException e)
        {   // Format Error (In ndef.writeNdefMessage)
            e.printStackTrace();
            return MessageWriteStatus_Enum.FAILED_BECAUSE_NULL_NDEF;
        }
    }


} /* END OF ACTIVITY CLASS */

