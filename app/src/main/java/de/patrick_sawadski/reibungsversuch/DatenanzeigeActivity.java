package de.patrick_sawadski.reibungsversuch;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.Arrays;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_datenanzeige);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LineGraphSeries<DataPoint> mSeriesX, mSeriesY, mSeriesZ;
        GraphView graph = (GraphView) findViewById(R.id.graphViewDatenanzeige);

        mSeriesX = new LineGraphSeries<>();
        mSeriesX.setTitle("Acc X");
        mSeriesX.setColor(ContextCompat.getColor(this, R.color.colorAxisX));
        mSeriesX.setThickness(2);
        mSeriesY = new LineGraphSeries<>();
        mSeriesY.setTitle("Acc Y");
        mSeriesY.setColor(ContextCompat.getColor(this, R.color.colorAxisY));
        mSeriesY.setThickness(2);
        mSeriesZ = new LineGraphSeries<>();
        mSeriesZ.setTitle("Acc Z");
        mSeriesZ.setColor(ContextCompat.getColor(this, R.color.colorAxisZ));
        mSeriesZ.setThickness(2);

        graph.addSeries(mSeriesX);
        graph.addSeries(mSeriesY);
        graph.addSeries(mSeriesZ);
        graph.getViewport().setScalable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-20);
        graph.getViewport().setMaxY(20);

        // TODO: Graphenbeschriftung X Y Z
        // TODO: Achsenbeschriftung verbessern

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);


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

                List<String> rows = Arrays.asList(builder.toString().split("\n\r"));
                boolean rohdatenErreicht = false;
                for(String string : rows){
                    String[] part = string.split(";");
                    if(!rohdatenErreicht){
                        String value = "";
                        if(part.length > 1) value = part[1];

                        switch(part[0]) {
                            case "Typ":
                                ((TextView)findViewById(R.id.tvVersuchstyp)).setText(value);
                                break;
                            case "Oberfläche 1":
                                ((TextView)findViewById(R.id.tvOberflaeche1)).setText(value);
                                break;
                            case "Oberfläche 2":
                                ((TextView)findViewById(R.id.tvOberflaeche2)).setText(value);
                                break;
                            case "Datum":
                                ((TextView)findViewById(R.id.tvDatum)).setText(value);
                                break;
                            case "Ort":
                                ((TextView)findViewById(R.id.tvOrt)).setText(value);
                                break;
                            case "Teilnehmer 1":
                                ((TextView)findViewById(R.id.tvTeilnehmer1)).setText(value);
                                break;
                            case "Teilnehmer 2":
                                ((TextView)findViewById(R.id.tvTeilnehmer2)).setText(value);
                                break;
                            case "Temperatur":
                                ((TextView)findViewById(R.id.tvTemperatur)).setText(value);
                                break;
                            case "Luftdruck":
                                ((TextView)findViewById(R.id.tvLuftdruck)).setText(value);
                                break;
                            case "Luftfeuchtigkeit":
                                ((TextView)findViewById(R.id.tvLuftfeuchtigkeit)).setText(value);
                                break;
                            case "Koeffizient":
                                ((TextView)findViewById(R.id.tvKoeffizient)).setText(value);
                                break;
                            case "Winkel":
                                ((TextView)findViewById(R.id.tvWinkel)).setText(value+"°");
                                break;
                            case "Beschleunigung":
                                findViewById(R.id.tableRowBeschleunigung).setVisibility(View.VISIBLE);
                                ((TextView)findViewById(R.id.tvBeschleunigung)).setText(value);
                                break;
                            case "Vergleich Beschleunigung":
                                findViewById(R.id.tableRowExtBeschl).setVisibility(View.VISIBLE);
                                ((TextView)findViewById(R.id.tvExtBeschleunigung)).setText(value);
                                break;
                            case "Vergleich Koeffizient":
                                findViewById(R.id.tableRowExtKoeff).setVisibility(View.VISIBLE);
                                ((TextView)findViewById(R.id.tvExtKoeff)).setText(value);
                                break;
                            case "Zeitstempel in ms":
                                Log.d(TAG, "ZEITSTEMPEL");
                                rohdatenErreicht = true;
                                break;
                        }
                    } else {
                        mSeriesX.appendData(new DataPoint(Integer.valueOf(part[0]), Float.valueOf(part[1])), true, 400);
                        mSeriesY.appendData(new DataPoint(Integer.valueOf(part[0]), Float.valueOf(part[2])), true, 400);
                        mSeriesZ.appendData(new DataPoint(Integer.valueOf(part[0]), Float.valueOf(part[3])), true, 400);
                    }
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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

        Button btnZeigeBild = (Button) findViewById(R.id.buttonZeigeBild);

        if(!photoavaiable) {
            btnZeigeBild.setVisibility(View.INVISIBLE);
        }
        btnZeigeBild.setOnClickListener(new View.OnClickListener() {
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
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_datenanzeige, menu);
        if(datenCached) {
            menu.findItem(R.id.action_speichern).setVisible(true);
            menu.findItem(R.id.action_teilen).setVisible(false);
            menu.findItem(R.id.action_foto_hinzufuegen).setVisible(false);
        } else {
            menu.findItem(R.id.action_speichern).setVisible(false);
            menu.findItem(R.id.action_teilen).setVisible(true);
            menu.findItem(R.id.action_foto_hinzufuegen).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onOptionsItemSelected():" + item);
        if(id == android.R.id.home){
            finish();
            return true;
        }
        if(id == R.id.action_foto_hinzufuegen) {
            Intent intent;
            File imageFile = new File(getApplicationContext().getFilesDir(), filebase + ".jpg");
            Uri imageURI = FileProvider.getUriForFile(getApplicationContext(), "de.patrick_sawadski.fileprovider", imageFile);
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
            return true;
        }
        if(id == R.id.action_speichern){
            if(datenCached) {
                File savedFile = new File(getApplicationContext().getFilesDir(), file.getName());
                try {
                    copy(file, savedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, savedFile.toString());
                Toast.makeText(this, "Messung gespeichert!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, DatenanzeigeActivity.class);
                intent.putExtra("EXTRA_FILEURI", savedFile.toURI());
                intent.putExtra("EXTRA_CACHED", false);
                startActivity(intent);
                finish();
            }
            return true;
        }
        if(id == R.id.action_teilen){
            // TODO: Teilen funktion
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            findViewById(R.id.buttonZeigeBild).setVisibility(View.VISIBLE);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(datenCached && isFinishing()){
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

