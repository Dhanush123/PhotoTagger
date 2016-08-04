package com.x10host.dhanushpatel.phototagger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    Toolbar toolbar;
    RadioGroup radioGroup;
    RadioButton tiles, wood, linen;
    Button feedbackButton;
    RelativeLayout rl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.my_toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        feedbackButton = (Button) findViewById(R.id.feedbackButton);
        tiles = (RadioButton) findViewById(R.id.tilesBack);
        wood = (RadioButton) findViewById(R.id.woodBack);
        linen = (RadioButton) findViewById(R.id.linenBack);

        rl = (RelativeLayout) findViewById(R.id.settingsPage);
        getBackground();
        buttonListeners();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tilesBack) {
                    Toast.makeText(getApplicationContext(), "New background selected: Tiles",
                            Toast.LENGTH_SHORT).show();
                    updateBackground("tiles");
                } else if (checkedId == R.id.woodBack) {
                    Toast.makeText(getApplicationContext(), "New background selected: Wood",
                            Toast.LENGTH_SHORT).show();
                    updateBackground("wood");
                } else {
                    Toast.makeText(getApplicationContext(), "New background selected: Linen",
                            Toast.LENGTH_SHORT).show();
                    updateBackground("linen");
                }
            }

        });

    }

    private void buttonListeners(){
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //code taken from Android dev docs for intent & intent filters
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL,"dhanush.patel@ymail.com");
                intent.putExtra(Intent.EXTRA_SUBJECT,"Feedback on Photo Tagger app");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                Log.i("Feedback button", "pressed");
            }
        });
    }
    private void updateBackground(String code){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("background", code);
        editor.commit();

        getBackground();
    }

    public void getBackground(){
            SharedPreferences sharedPreference= PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String codeGot = sharedPreference.getString("background", "tiles");
            switch (codeGot){
                case "tiles":
                    rl.setBackgroundResource(R.drawable.tiles);
                    break;
                case "wood":
                    rl.setBackgroundResource(R.drawable.darkwood);
                    break;
                case "linen":
                    rl.setBackgroundResource(R.drawable.blacklinen);
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Error: could not set background", Toast.LENGTH_SHORT).show();
            }
    }
}
