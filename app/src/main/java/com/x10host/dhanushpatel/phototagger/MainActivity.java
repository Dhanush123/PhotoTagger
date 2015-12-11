package com.x10host.dhanushpatel.phototagger;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.x10host.dhanushpatel.phototagger.api.AlchemyAPI;
import com.x10host.dhanushpatel.phototagger.api.AlchemyAPI_ImageParams;

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
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    static int TAKE_PICTURE = 1;
    static int PICK_PHOTO = 2;
    boolean pickOrChoose = true;
    Button takePhotoButton;
    Button choosePhotoButton;
    ImageView photoShow;
    TextView photoTags;
    Bitmap chosenBitmap;
    String mTakenPhotoPath;
    String mChoosenPhotoPath;
    String tags;
    String AlchemyAPI_Key = Constants.API_KEY;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePhotoButton = (Button) findViewById(R.id.takePhotoButton);
        choosePhotoButton = (Button) findViewById(R.id.choosePhotoButton);
        photoShow = (ImageView) findViewById(R.id.photoShow);
        photoTags = (TextView) findViewById(R.id.photoTags);
        buttonListeners();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode ==  PICK_PHOTO && resultCode == RESULT_OK && intent != null) {
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
            scaleBitmap();
            SendAlchemyCall("imageClassify");
        }

        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK && intent != null) {
            // get bundle
            Bundle extras = intent.getExtras();
            //Convert bitmap to byte array
            Bitmap bitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
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

    private void SendAlchemyCallInBackground(final String call)
    {
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
            //textview.setText("Error: " + e.getMessage());
        }
    }

    private void ShowTagInTextView(final Document doc, final String tag)
    {
        Log.d(getString(R.string.app_name), doc.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photoTags.setText("Analyzing photo...");
                Element root = doc.getDocumentElement();
                NodeList items = root.getElementsByTagName(tag);

                ExifInterface exif = null;
                try {
                    if(pickOrChoose) {
                        exif = new ExifInterface(mTakenPhotoPath);
                    }
                    else{
                        exif = new ExifInterface(mChoosenPhotoPath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ArrayList<String> tags = new ArrayList<String>();
                for (int i=0;i<items.getLength();i++) {
                    Node concept = items.item(i);
                    String astring = concept.getNodeValue();
                    astring = concept.getChildNodes().item(0).getNodeValue();
                    //tags.add(astring);
                    if(i==0){
                        photoTags.setText("Tags: " + astring);
                        tags = astring;
                        exif.setAttribute("UserComment",astring);
                    }
                    else {
                        photoTags.append(", " + astring);
                        tags = tags + ", " + astring;
                        exif.setAttribute("UserComment",exif.getAttribute("UserComment")+", " + astring);
                    }
                }
                Log.i("photo tags are",tags);

                try {
                    exif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    private void scaleBitmap(){
        Bitmap myBitmap = chosenBitmap;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.e("Screen width ", " "+width);
        Log.e("Screen height ", " "+height);
        Log.e("img width ", " "+myBitmap.getWidth());
        Log.e("img height ", " "+myBitmap.getHeight());
        float scaleHt =(float) width/myBitmap.getWidth();
        Log.e("Scaled percent ", " "+scaleHt);
        Bitmap scaled = Bitmap.createScaledBitmap(myBitmap, width, (int) (myBitmap.getWidth()*scaleHt), true);
        photoShow.setImageBitmap(scaled);
    }

}
