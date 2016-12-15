package de.patrick_sawadski.reibungsversuch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.List;

public class DatenanzeigeActivity extends AppCompatActivity {

    private static final String TAG = "Datenanzeige";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private File file = null;
    private Boolean datenCached;
    private Boolean photoavaiable = false;
    private String filebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datenanzeige);

        Intent intent = getIntent();
        datenCached = intent.getBooleanExtra("EXTRA_CACHED", false);
        URI fileuri = (URI)intent.getExtras().get("EXTRA_FILEURI");

        if(fileuri != null){

            try {
                file = new File(fileuri);
                filebase = file.getName().substring(0, file.getName().lastIndexOf("."));

                FileReader reader = new FileReader(file);

                StringBuilder builder = new StringBuilder();
                builder.append(file.toString()).append("\n\r");
                int ch;
                while((ch = reader.read()) != -1){
                    builder.append((char)ch);
                }

                // TODO: Übergebene csv parsen und anzuzeigende Werte extrahieren
                // TODO: Verlaufsgraphen anzeigen

                ((TextView) findViewById(R.id.textViewTest)).setText(builder.toString());
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Button zum Speichern anzeigen wenn Daten nicht im Speicher
            final Button btnSpeichern = (Button) findViewById(R.id.buttonSpeichern);
            if(datenCached) {
                btnSpeichern.setVisibility(View.VISIBLE);
                btnSpeichern.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        File savedFile = new File(getApplicationContext().getFilesDir(), file.getName());
                        try {
                            copy(file, savedFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), "Messung gespeichert", Toast.LENGTH_SHORT).show();
                        btnSpeichern.setClickable(false);
                        btnSpeichern.setEnabled(false);
                        btnSpeichern.setText(R.string.button_ergebnis_gespeichert);
                        Log.d(TAG, savedFile.toString());
                    }
                });
            } else {
                btnSpeichern.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.e(TAG, "Fileuri ist leer");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        File[] filesBuf = getFilesDir().listFiles();
        for(File fileN : filesBuf){
            if(fileN.getName().equals(filebase+".jpg")) {
                photoavaiable = true;
            }
        }

        Button btnAddFoto = (Button) findViewById(R.id.buttonAddFoto);

        if(photoavaiable) {
            btnAddFoto.setText("Foto ansehen");
        }
        btnAddFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                File imageFile = new File(getApplicationContext().getFilesDir(), filebase + ".jpg");
                Uri imageURI = FileProvider.getUriForFile(getApplicationContext(), "de.patrick_sawadski.fileprovider", imageFile);

                if (photoavaiable) {
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(imageURI, "image/*");
                    List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        getApplicationContext().grantUriPermission(packageName, imageURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    startActivity(intent);
                } else {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        try {
                            if (imageFile.createNewFile()) {
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                                List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                                for (ResolveInfo resolveInfo : resInfoList) {
                                    String packageName = resolveInfo.activityInfo.packageName;
                                    getApplicationContext().grantUriPermission(packageName, imageURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(datenCached){
            Boolean deleted = file.delete();
            Log.d(TAG, "file.delete() -> " + deleted.toString());
        }
    }

    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }
}

