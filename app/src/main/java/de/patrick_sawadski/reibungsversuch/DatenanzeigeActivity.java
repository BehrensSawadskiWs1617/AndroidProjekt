package de.patrick_sawadski.reibungsversuch;

import android.content.Intent;
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

public class DatenanzeigeActivity extends AppCompatActivity {

    private static final String TAG = "Datenanzeige";
    private File file = null;
    private Boolean datenCached;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datenanzeige);

        Intent intent = getIntent();
        datenCached = intent.getBooleanExtra("EXTRA_CACHED", false);
        URI fileuri = (URI)intent.getExtras().get("EXTRA_FILEURI");

        if(fileuri != null){
            // TODO: Wenn datei im Cache, dann Button zum Speichern anbieten
            // TODO: Wenn Button gedrückt, ausgrauen und Toast über Speichervorgang
            // TODO: Wenn nicht gecached oder gespeichert, dann Dateipfad anzeigen


            try {
                file = new File(fileuri);
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
            Button btnSpeichern = (Button) findViewById(R.id.buttonSpeichern);
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
                        Log.d(TAG, savedFile.toString());
                    }
                });
            } else {
                btnSpeichern.setVisibility(View.VISIBLE);
            }


        } else {
            Log.e(TAG, "Fileuri ist leer");
        }

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

