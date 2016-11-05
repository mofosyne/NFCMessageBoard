package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TextTagCreation extends AppCompatActivity {

    // Activity context
    Context ctx;

    // Information that we want to write to the tag
    public enum ActivityRequestCode_Enum {
        REQUEST_CODE_NEW_TAG
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_tag_creation);

        ctx = this;

        // Button Write With Return Results
        final Button button_write_tag_forresult = (Button) findViewById(R.id.button_create_new_tag);
        button_write_tag_forresult.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), WritingToTextTag.class);
                        startActivityForResult(
                                intent,
                                TestPage.ActivityRequestCode_Enum.REQUEST_CODE_NEW_TAG.ordinal()
                        );
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
        if (requestCode == TestPage.ActivityRequestCode_Enum.REQUEST_CODE_NEW_TAG.ordinal())
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
