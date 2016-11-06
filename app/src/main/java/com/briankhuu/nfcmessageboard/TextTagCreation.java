package com.briankhuu.nfcmessageboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TextTagCreation extends AppCompatActivity {

    // Activity and TextView context
    Context ctx;
    TextView textView_new_tag_title;

    // Request Codes
    public enum ActivityRequestCode_Enum {
        REQUEST_CODE_NEW_TAG
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_tag_creation);


        // Get all context
        ctx = this;
        textView_new_tag_title = (TextView) findViewById(R.id.textView_new_tag_title);

        // Button Write With Return Results
        final Button button_write_tag_forresult = (Button) findViewById(R.id.button_create_new_tag);
        button_write_tag_forresult.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), WritingToTextTag.class);
                        intent.putExtra("tag_type","txt");
                        intent.putExtra("tag_content","# " + textView_new_tag_title.getText().toString() + "\n");
                        startActivityForResult(
                                intent,
                                ActivityRequestCode_Enum.REQUEST_CODE_NEW_TAG.ordinal()
                        );
                    }
                }
        );

        // Button Write With Return Results
        final Button button_cancel = (Button) findViewById(R.id.button_cancel);
        button_cancel.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v) {
                        finish();
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
                    finish();
                    break;
                case (Activity.RESULT_CANCELED):
                    Toast.makeText(ctx, "Tag write failed", Toast.LENGTH_LONG ).show();
                    break;
                default:
            }
        }
    }

}
