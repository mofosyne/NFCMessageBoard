package com.briankhuu.nfcmessageboard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DisplayTextTag extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_text_tag);


        final Button button_write_tag = (Button) findViewById(R.id.button_write_tag);
        button_write_tag.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), WritingToTextTag.class);
                startActivity(intent);
            }
        });

        final Button button_readme = (Button) findViewById(R.id.button_readme);
        button_readme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ReadMe.class);
                startActivity(intent);
            }
        });
    }
}
