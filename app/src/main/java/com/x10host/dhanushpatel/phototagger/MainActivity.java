package com.x10host.dhanushpatel.phototagger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.x10host.dhanushpatel.phototagger.alchemy_api.AlchemyAPI;
import com.x10host.dhanushpatel.phototagger.alchemy_api.AlchemyAPI_ImageParams;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static int TAKE_PICTURE = 1;
    static int PICK_PHOTO = 2;
    boolean pickOrChoose = true;
    Button takePhotoButton;
    Button choosePhotoButton;
    Button retryIDButton;
    ImageView photoShow;
    TextView photoTags;
    Bitmap chosenBitmap;
    String mTakenPhotoPath;
    String mChoosenPhotoPath;
    String tags;
    ClarifaiClient clarifai;
    String AlchemyAPI_Key = Constants.API_KEY;
    List<RecognitionResult> results;
    byte[] photoBytes;
    int limit;
    int newNumCalls;
    String month_name;
    private Toolbar toolbar;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        getSupportActionBar().setTitle("Photo Tagger");
        takePhotoButton = (Button) findViewById(R.id.takePhotoButton);
        choosePhotoButton = (Button) findViewById(R.id.choosePhotoButton);
        retryIDButton = (Button) findViewById(R.id.retryIDButton);
        photoShow = (ImageView) findViewById(R.id.photoShow);
        photoTags = (TextView) findViewById(R.id.photoTags);
        photoTags.setMovementMethod(ScrollingMovementMethod.getInstance());
        retryIDButton.setVisibility(View.GONE);
        clarifai = new ClarifaiClient(Constants.APP_ID,Constants.APP_SECRET);
        buttonListeners();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        if(!isNetworkAvailable()){
            createNetworkErrorDialog();
        }
    }

    private void buttonListeners() {
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickOrChoose = true;
                // create intent with ACTION_IMAGE_CAPTURE action
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // start camera activity
                startActivityForResult(intent, TAKE_PICTURE);
            }
        });
        choosePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickOrChoose = false;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_PHOTO);
            }
        });
        retryIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoTags.setText("Analyzing photo...");
                if (!reachedMonthlyClarifaiLimit()){
                    updateClarifaiLimit();

                    Bitmap bitmap = ((BitmapDrawable) photoShow.getDrawable()).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
                    photoBytes = stream.toByteArray();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(final Void... params) {
                            // something you know that will take a few seconds
                            results = clarifai.recognize(new RecognitionRequest(photoBytes));
                            return null;
                        }

                        @Override
                        protected void onPostExecute(final Void result) {
                            // continue what you are doing...
                            clarifaiUIUpdate();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                }
                else{
                    photoTags.setText("Sorry, advanced scan monthly limit has been reached ("+limit+"/"+limit+").");
                    //Toast.makeText(getApplicationContext(),"Sorry, advanced scan monthly limit has been reached ("+limit+"/"+limit+").",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean reachedMonthlyClarifaiLimit(){
        boolean reachedLimit = false;

        Calendar now = Calendar.getInstance();
        // month start from 0 to 11
        int currentMonth = now.get(Calendar.MONTH) + 1;
        SharedPreferences sp = getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
        int retrievedMonth = sp.getInt("currentMonth",-1);
        if (retrievedMonth!=currentMonth){
            SharedPreferences sp2 = getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp2.edit();
            editor.putInt("currentMonth",currentMonth);
            editor.commit();
            retrievedMonth = currentMonth;
        }

        limit = 0;
        if(currentMonth==1 || currentMonth==3 || currentMonth==5 || currentMonth==7 || currentMonth==8 || currentMonth==10 || currentMonth==12){
            limit = 31;
        }
        else if(currentMonth==4 || currentMonth==6 || currentMonth==9 || currentMonth==11){
            limit = 30;
        }
        else{ //currentMonth==2, February
            if(isLeapYear(now.get(Calendar.YEAR))){
                limit = 29;
            }
            else{
                limit = 28;
            }
        }

        SharedPreferences sp2 = getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
        int userClarifaiCalls = sp2.getInt("numAPICalls",0);
        if(userClarifaiCalls==limit && retrievedMonth==currentMonth){
            reachedLimit = true;
        }

        return reachedLimit;
    }

    public static boolean isLeapYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365;
    }

    private void updateClarifaiLimit(){
        SharedPreferences sp = getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
        int gotUserClarifaiCalls = sp.getInt("numAPICalls", 0);
        newNumCalls = ++gotUserClarifaiCalls;
        SharedPreferences sp2 = getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp2.edit();

        editor.putInt("numAPICalls",newNumCalls);
        editor.commit();

        //runOnUiThread(new Runnable() {
        //    @Override
         //   public void run() {
         //  }
      //  });
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    protected void createNetworkErrorDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You need a network connection to use this application. Please turn on mobile network or Wi-Fi in Settings.")
                .setTitle("Unable to connect")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                startActivity(i);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.this.finish();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
    }


    private void clarifaiUIUpdate(){

        Calendar cal=Calendar.getInstance();
        SimpleDateFormat month_date = new SimpleDateFormat("MMMM");
        month_name = month_date.format(cal.getTime());
        Toast.makeText(getApplicationContext(), "Remaining advanced scans for " + month_name + ": " + (limit - newNumCalls) + "/" + limit, Toast.LENGTH_LONG).show();

        if(results.get(0).getTags() != null){
        List<Tag> tagsFound = results.get(0).getTags();
        String tag ="";
            for(int i=0; i < tagsFound.size();i++){
                tag = tagsFound.get(i).getName();
                if (i == 0) {
                    photoTags.setText("Tags: " + tag);
                    tags = tag;
                    // exif.setAttribute("UserComment", astring);
                } else {
                    photoTags.append(", " + tag);
                    tags = tags + ", " + tag;
                    // exif.setAttribute("UserComment", exif.getAttribute("UserComment") + ", " + astring);
                }
            }
            Log.i("New photo tags are", tags);
        }
        else{
            photoTags.setText("No detailed tags could be found...");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode ==  PICK_PHOTO && resultCode == RESULT_OK && intent != null) {
            photoTags.setText("Analyzing photo...");

            Uri selectedImage = intent.getData();
            InputStream inputStream = null;
            mChoosenPhotoPath = selectedImage.getPath();
            if (ContentResolver.SCHEME_CONTENT.equals(selectedImage.getScheme())) {
                try {
                    inputStream = this.getContentResolver().openInputStream(selectedImage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                if (ContentResolver.SCHEME_FILE.equals(selectedImage.getScheme())) {
                    try {
                        inputStream = new FileInputStream(selectedImage.getPath());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            chosenBitmap = BitmapFactory.decodeStream(inputStream);

            int currentBitmapWidth = chosenBitmap.getWidth();
            int currentBitmapHeight = chosenBitmap.getHeight();

            int ivWidth = photoShow.getWidth();
            int ivHeight = photoShow.getHeight();
            int newWidth = ivWidth;

            int newHeight = (int) Math.floor((double) currentBitmapHeight * ((double) newWidth / (double) currentBitmapWidth));

            Bitmap newbitMap = Bitmap.createScaledBitmap(chosenBitmap, newWidth, newHeight, true);

            photoShow.setImageBitmap(newbitMap);

            SendAlchemyCall("imageClassify");
        }

        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK && intent != null) {
            photoTags.setText("Analyzing photo...");
            // get bundle
            Bundle extras = intent.getExtras();
            //Convert bitmap to byte array
            Bitmap bitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,100, bos);
            byte[] bitmapdata = bos.toByteArray();
            //write the bytes in file
            File file = null;
            try {
                file = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                fos.write(bitmapdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
                galleryAddPic();
            } catch (IOException e) {
                e.printStackTrace();
            }
            photoShow.setImageBitmap(BitmapFactory.decodeFile(String.valueOf(new File(mTakenPhotoPath))));
            SendAlchemyCall("imageClassify");

        }
    }

    private void SendAlchemyCall(final String call)
    {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    SendAlchemyCallInBackground(call);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void SendAlchemyCallInBackground(final String call) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //textview.setText("Making call: "+call);
            }
        });

        Document doc = null;
        AlchemyAPI api = null;
        try
        {
            api = AlchemyAPI.GetInstanceFromString(AlchemyAPI_Key);
        }
        catch( IllegalArgumentException ex )
        {
           // textview.setText("Error loading AlchemyAPI.  Check that you have a valid AlchemyAPI key set in the AlchemyAPI_Key variable.  Keys available at alchemyapi.com.");
            return;
        }

       // String someString = urlText.getText().toString();
        try{
            if( "imageClassify".equals(call))
            {
                Bitmap bitmap = ((BitmapDrawable)photoShow.getDrawable()).getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageByteArray = stream.toByteArray();

                AlchemyAPI_ImageParams imageParams = new AlchemyAPI_ImageParams();
                imageParams.setImage(imageByteArray);
                imageParams.setImagePostMode(AlchemyAPI_ImageParams.RAW);
                doc = api.ImageGetRankedImageKeywords(imageParams);
                ShowTagInTextView(doc, "text");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //photoTags.setText("Error: " + e.getMessage());
        }
    }

    private void ShowTagInTextView(final Document doc, final String tag) {
        Log.d(getString(R.string.app_name), doc.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Element root = doc.getDocumentElement();
                NodeList items = root.getElementsByTagName(tag);
                /**
                 ExifInterface exif = null;
                 try {
                 if (pickOrChoose) {
                 exif = new ExifInterface(mTakenPhotoPath);
                 } else {
                 exif = new ExifInterface(mChoosenPhotoPath);
                 }
                 } catch (IOException e) {
                 e.printStackTrace();
                 }
                 **/
                for (int i = 0; i < items.getLength(); i++) {
                    Node concept = items.item(i);
                    String aString = concept.getChildNodes().item(0).getNodeValue();
                    if (i == 0 && aString.equals("NO_TAGS")) {
                        photoTags.setText("No simple tags could be found...");
                    } else if (i == 0 && !aString.equals("NO_TAGS")) {
                        photoTags.setText("Tags: " + aString);
                        tags = aString;
                        // exif.setAttribute("UserComment", aString);
                    } else {
                        photoTags.append(", " + aString);
                        tags = tags + ", " + aString;
                        // exif.setAttribute("UserComment", exif.getAttribute("UserComment") + ", " + aString);
                    }
                }

                if(tags==null){
                    tags = "No simple tags found";
                }
                retryIDButton.setVisibility(View.VISIBLE);
                Log.i("photo tags are", tags);
                /**
                 try {
                 exif.saveAttributes();
                 } catch (IOException e) {
                 e.printStackTrace();
                 }
                 **/
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String folder_main = "Photo Tagger";
        File storageDir = new File(Environment.getExternalStorageDirectory(),
                folder_main);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mTakenPhotoPath = image.getAbsolutePath();
        Log.e("Success, file path", mTakenPhotoPath);
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mTakenPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Log.i("added pic to gallery", "yes");
    }


}
