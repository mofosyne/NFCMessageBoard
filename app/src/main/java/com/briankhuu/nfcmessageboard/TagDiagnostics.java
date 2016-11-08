package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TagDiagnostics extends AppCompatActivity {
    private static final String LOGGER_TAG = WritingToTextTag.class.getSimpleName();

    // Activity context
    Context ctx;

    // Tag reference
    Tag tag;
    private NfcAdapter mNfcAdapter; // Sets up an empty object of type NfcAdapter

    // Haptic Feedback
    Vibrator vibrator;

    // Display diagnostic information here
    public static TextView textview_diagnostics_output;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_diagnostics);

        /* Activity Context For Toast to work easier!
        * */

        // Record the activity context pointer
        ctx = this;

        //setup vibrate
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);


        // Setup the TextView display ( ::bk:: This looks very much like calling DOM objects in javascript )
        textview_diagnostics_output = (TextView) findViewById(R.id.textView_tag_diagnostics);


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



        /* Install Button Listeners
        * */

        // Cancel Button
        final Button button_write_tag = (Button) findViewById(R.id.button_cancel);
        button_write_tag.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        finish();
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
        {  // We expect that a tag has been detected, if not then don't process this intent.
            Log.e( LOGGER_TAG, "handle_NfcAdapter_Intent:"
                    +"Intent Extra NfcAdapter.EXTRA_TAG Missing"
            );
            return;
        }

        display_tag_diagnostics(intent);

        return;
    }

    /***********************************************************************************************
     *  INTENT HANDLING
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

    private void display_tag_diagnostics(Intent intent)
    {
        String diagStr = "";

        /* Header */
        diagStr += "\n# Tag Diagnostics";

        /* Intent */
        diagStr += "\n## ANDROID INCOMING INTENT";

        if ( intent.getAction() != null)
            diagStr += "\n - Intent Action: " + intent.getAction();

        if ( intent.getPackage() != null)
            diagStr += "\n - Intent Package: " + intent.getPackage();

        if ( intent.getScheme() != null)
            diagStr += "\n - Intent Scheme: " + intent.getScheme();

        if ( intent.getData() != null)
            diagStr += "\n - Intent Data: " + intent.getData();

        if ( intent.getType() != null)
            diagStr += "\n - Intent Type: " + intent.getType();

        diagStr += "\n - NfcAdapter.EXTRA_ID: " + bytesToHex(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

        diagStr += "\n";



        /* Tag Information*/
        diagStr += "\n## PROCESSING TAG\n";
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null)
        {   // Tag object is empty... huh?
            diagStr += "\nERROR: Not A Tag?";
        }
        else
        {   // Checking Tag
            diagStr += "\n### Tag (android.nfc.Tag)";
            diagStr += "\n - Tag ID: 0x" + bytesToHex(tag.getId());
            diagStr += "\n - Tech List";
            String[] techList_str_array = tag.getTechList();
            for (int i = 0; i < techList_str_array.length ; i++)
            {
                diagStr += "\n    - "+techList_str_array[i];
            }

            diagStr += "\n";

            /*********************************************
             * Display NDEF info
             */
            diagStr += "\n### NDEF Formatable (android.nfc.tech.NdefFormatable)";
            diagStr += "\n - ClassPath: (android.nfc.tech.NdefFormatable)";
            NdefFormatable ndefFormattableTag = NdefFormatable.get(tag);
            if (ndefFormattableTag == null)
            {
                diagStr += "\n - NdefFormatable.get(tag) == null ";
                diagStr += "\n - This tag cannot be formatted to NDEF ";
            }
            else
            {
                diagStr += "\n - Can Enumerate Formattable NDEF Driver ";
            }

            diagStr += "\n";

            /*********************************************
             * Display NDEF info
             */
            diagStr += "\n### NDEF ";
            diagStr += "\n - ClassPath: (android.nfc.tech.Ndef)";
            Ndef ndefTag = Ndef.get(tag);
            if (ndefTag == null)
            {
                diagStr += "\n - Ndef.get(tag) == null";
                diagStr += "\n - ERROR: Tag has not been formatted for NDEF data yet";
            }
            else
            {
                diagStr += "\n - Tag Size (bytes): " + ndefTag.getMaxSize();
                diagStr += "\n - Writable: " + (ndefTag.isWritable() ? "Yes" : "No");
                diagStr += "\n - Tag Type: " + ndefTag.getType();

                NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
                NdefRecord[] ndefRecords = ndefMesg.getRecords();

                diagStr += "\n - Size of NDEF message (Bytes): " + ndefMesg.getByteArrayLength();
                diagStr += "\n - Number of NDEF records: " + ndefRecords.length;
                diagStr += "\n - NDEF records: " + ndefRecords.length;

                // Record
                for (int i = 0; i < ndefRecords.length; i++) {
                    diagStr += "\n    * " + ndefRecords[i].getType().toString();
                }
            }
        }

        diagStr += "\n";

        // Display the diagnostics
        // textview_diagnostics_output.setText(diagStr);
        updateMainDisplay(diagStr, textview_diagnostics_output);

        return;
    }

    /**
     *  Psudo Markdown Parser
     * @param text
     * @param mTextView
     */

    private void updateMainDisplay(String text, TextView mTextView) {
        // Let's update the main display
        // Needs to set as spannable otherwise http://stackoverflow.com/questions/16340681/fatal-exception-string-cant-be-cast-to-spannable
        mTextView.setText(text, TextView.BufferType.SPANNABLE);
        // Let's prettify it!
        changeLineinView_TITLESTYLE(mTextView, "# ", 0xfff4585d, 2f); // Primary Header
        changeLineinView(mTextView, "\n# ", 0xFFF4A158, 1.5f); // Secondary Header
        changeLineinView(mTextView, "\n## ", 0xFFF4A158, 1.2f); // Secondary Header
        changeLineinView(mTextView, "\n### ", 0xFFF4A158, 1.0f); // Third Header
        changeLineinView(mTextView, "\n---", 0xFFF4A158, 1.2f); // Horizontal Rule
        changeLineinView(mTextView, "\n>",   0xFF89e24d, 0.9f); // Block Quotes
        changeLineinView(mTextView, "\n - ", 0xFFA74DE3, 1f);   // Classic Markdown List
        changeLineinView(mTextView, "\n- ", 0xFFA74DE3, 1f);   // NonStandard List

        //spanSetterInView(String startTarget, String endTarget, int typefaceStyle, String fontFamily,TextView tv, int colour, float size)
        // Limitation of spanSetterInView. Well its not a regular expression... so can't exactly have * list, and *bold* at the same time.
        spanSetterInView(mTextView,  " **",   "** ",   Typeface.BOLD,        "",  0xFF89e24d,  1f, true); // Bolding
        spanSetterInView(mTextView,   " *",    "* ",   Typeface.ITALIC,      "",  0xFF4dd8e2,  1f, true); // Italic
        spanSetterInView(mTextView, " ***",  "*** ",   Typeface.BOLD_ITALIC, "",  0xFF4de25c,  1f, true); // Bold and Italic
        spanSetterInView(mTextView,   " `",    "` ",   Typeface.BOLD,      "monospace",  0xFF45c152,  1.1f, true); // inline code
        spanSetterInView(mTextView,"\n    ","\n",      Typeface.BOLD,      "monospace",  0xFF45c152,  0.9f, true); // classic indented code
        spanSetterInView(mTextView,"\n```\n","\n```\n",Typeface.BOLD,      "monospace",  0xFF45c152,  1.1f, false); // fenced code Blocks ( endAtLineBreak=false since this is a multiline block operator)
    }

    private void changeLineinView(TextView tv, String target, int colour, float size) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        while (true) {
            startSpan = vString.indexOf(target, endSpan-1);     // (!@#$%) I want to check a character behind in case it is a newline
            endSpan = vString.indexOf("\n", startSpan+1);       // But at the same time, I do not want to read the point found by startSpan. This is since startSpan may point to a initial newline.
            ForegroundColorSpan foreColour = new ForegroundColorSpan(colour);
            // Need a NEW span object every loop, else it just moves the span
            // Fix: -1 in startSpan or endSpan, indicates that the indexOf has already searched the entire string with not valid match (Lack of endspan check, occoured because of the inclusion of endTarget, which added extra complications)
            if ( (startSpan < 0) || ( endSpan < 0 ) ) break;// Need a NEW span object every loop, else it just moves the span
            // Need to make sure that start range is always smaller than end range. (Solved! Refer to few lines above with (!@#$%) )
            if (endSpan > startSpan) {
                //endSpan = startSpan + target.length();
                spanRange.setSpan(foreColour, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Also wannna bold the span too
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(Typeface.BOLD), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(spanRange);
    }

    private void changeLineinView_TITLESTYLE(TextView tv, String target, int colour, float size) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        /*
        * Had to do this, since there is something wrong with this overlapping the "##" detection routine
        * Plus you only really need one title.
         */
        //while (true) {
        startSpan = vString.substring(0,target.length()).indexOf(target, endSpan-1); //substring(target.length()) since we only want the first line
        endSpan = vString.indexOf("\n", startSpan+1);
        ForegroundColorSpan foreColour = new ForegroundColorSpan(colour);
        // Need a NEW span object every loop, else it just moves the span
            /*
            if (startSpan < 0)
                break;
                */
        if ( !(startSpan < 0) ) { // hacky I know, but its to cater to the case where there is no header text
            // Need to make sure that start range is always smaller than end range.
            if (endSpan > startSpan) {
                //endSpan = startSpan + target.length();
                spanRange.setSpan(foreColour, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Also wannna bold the span too
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        //}
        tv.setText(spanRange);
    }


    private void spanSetterInView(TextView tv, String startTarget, String endTarget, int typefaceStyle, String fontFamily, int colour, float size, boolean endAtLineBreak) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        while (true) {
            startSpan = vString.indexOf(startTarget, endSpan-1);     // (!@#$%) I want to check a character behind in case it is a newline
            endSpan = vString.indexOf(endTarget, startSpan+1+startTarget.length());     // But at the same time, I do not want to read the point found by startSpan. This is since startSpan may point to a initial newline. We also need to avoid the first patten matching a token from the second pattern.
            // Since this is pretty powerful, we really want to avoid overmatching it, and limit any problems to a single line. Especially if people forget to type in the closing symbol (e.g. * in bold)
            if (endAtLineBreak){
                int endSpan_linebreak = vString.indexOf("\n", startSpan+1+startTarget.length());
                if ( endSpan_linebreak < endSpan ) { endSpan = endSpan_linebreak; }
            }
            // Fix: -1 in startSpan or endSpan, indicates that the indexOf has already searched the entire string with not valid match (Lack of endspan check, occoured because of the inclusion of endTarget, which added extra complications)
            if ( (startSpan < 0) || ( endSpan < 0 ) ) break;// Need a NEW span object every loop, else it just moves the span
            // We want to also include the end "** " characters
            endSpan += endTarget.length();
            // If all is well, we shall set the styles and etc...
            if (endSpan > startSpan) {// Need to make sure that start range is always smaller than end range. (Solved! Refer to few lines above with (!@#$%) )
                spanRange.setSpan(new ForegroundColorSpan(colour), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(typefaceStyle), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Default to normal font family if settings is empty
                if( !fontFamily.equals("") )  spanRange.setSpan(new TypefaceSpan(fontFamily), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(spanRange);
    }

}
