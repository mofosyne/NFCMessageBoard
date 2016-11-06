package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TestPage extends AppCompatActivity {

    // Activity context
    Context ctx;


    // Information that we want to write to the tag
    public enum ActivityRequestCode_Enum {
        REQUEST_CODE_NEW_TAG
    }


    /***********************************************************************************************
        Activity Lifecycle
        https://developer.android.com/reference/android/app/Activity.html
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        ctx = this;


        /* Install Button Listeners
        * */

        // Button Write With Return Results
        final Button button_write_tag_forresult = (Button) findViewById(R.id.button_write_tag_startActivityForResult);
        button_write_tag_forresult.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), WritingToTextTag.class);
                        startActivityForResult(
                                intent,
                                ActivityRequestCode_Enum.REQUEST_CODE_NEW_TAG.ordinal()
                        );
                    }
                }
        );

        // Button Write Tag
        final Button button_write_tag = (Button) findViewById(R.id.button_write_tag);
        button_write_tag.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), WritingToTextTag.class);
                        startActivity(intent);
                    }
                }
        );

        // button_open_new_tag_creation Write Tag
        final Button button_open_new_tag_creation = (Button) findViewById(R.id.button_open_new_tag_creation);
        button_open_new_tag_creation.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), TextTagCreation.class);
                        startActivity(intent);
                    }
                }
        );

        // Button Readme
        final Button button_readme = (Button) findViewById(R.id.button_readme);
        button_readme.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), ReadMe.class);
                        startActivity(intent);
                    }
                }
        );
    }

    @Override
    protected void onStart(){
        super.onStart();
    };

    @Override
    protected void onRestart(){
        super.onStart();
    };

    @Override
    protected void onResume(){
        super.onResume();
    };

    @Override
    protected void onPause(){
        super.onPause();
    };

    @Override
    protected void onStop(){
        super.onStop();
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
    };

    /**********************************************************************************************/

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ActivityRequestCode_Enum.REQUEST_CODE_NEW_TAG.ordinal())
        {
            switch (resultCode)
            {
                case (Activity.RESULT_OK):
                    Toast.makeText(ctx, "Tag was successfully written to ", Toast.LENGTH_LONG ).show();
                    break;
                case (Activity.RESULT_CANCELED):
                    Toast.makeText(ctx, "Tag write failed", Toast.LENGTH_LONG ).show();
                    break;
                default:
            }
        }
    }

}
