package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.app.PendingIntent;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class writingToTextTag extends AppCompatActivity {
    static boolean write_mode = false;


    // Tag reference
    Tag tag;
    private NfcAdapter mNfcAdapter; // Sets up an empty object of type NfcAdapter

    // We have a plain text mode for historical reason.
    public static final String MIME_TEXT_PLAIN = "text/plain";
    //public static final String TAG = "NfcTest"; // Don't think this is required


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writing_to_text_tag);
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

    // Aka: enable tag write mode
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
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
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
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
        *  Create new tag mode:
        * */
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Toast.makeText(ctx, "Writing tag", Toast.LENGTH_LONG ).show();
        try {
            if(tag==null){  // We expect that a tag has been detected
                Toast.makeText(ctx, ctx.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
            }else{
                String message = "";
                if (armed_write_to_restore_tag) { // Is this to restore a broken tag?
                    mTextView = (TextView) findViewById(R.id.textView_maindisplay);
                    // Get the original Tag content
                    message = mTextView.getText().toString();
                } else {
                    // Get the object for message field
                    entry_msg = (TextView) findViewById(R.id.edit_msg);
                    // Get the text
                    message = "# " + entry_msg.getText().toString() + "\n";
                    // Clear the message field. Name field is left alone. And all is done.
                    entry_msg.setText("");
                }
                // Write to tag
                write(message,tag);

                infoDisp.setText("New tag created.");
                // Lets vibrate!
                long[] pattern = {0, 200, 200, 200, 200, 200, 200};
                vibrator.vibrate(pattern,-1);
                // Update the display with what was posted to make user experience more responsive
                mTextView.setText(message);
                // Let user know it's all gravy
                Toast.makeText(ctx, ctx.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
            }
        } catch (IOException e) {
            Toast.makeText(ctx, "Cannot Write To Tag. (type:IO)", Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        } catch (FormatException e) {
            Toast.makeText(ctx, "Cannot Write To Tag. (type:Format)" , Toast.LENGTH_LONG ).show();
            e.printStackTrace();
        }
        // Success Message:
        infoDisp.setText("New Message Board Tag Created");
        // Let's revert back to normal behaviour
        armed_write_to_empty_tag = false;
        armed_write_to_restore_tag = false;
        // commented away, because I think foreground dispatch on activation, actually pauses the activity. So this is not really needed.
        // Note: I think activity is paused on these situation: change scree, dialog, and foreground dispatch event.
        //resetForegroundDispatch();


        return;

    }


}
