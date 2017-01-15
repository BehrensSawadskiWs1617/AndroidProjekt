package de.patrick_sawadski.reibungsversuch;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.TRANSPARENT;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;

public class DurchfuehrungActivity extends AppCompatActivity implements SensorEventListener {

    public static final String TAG = "Durchfuehrung";
    public static final int VERSUCHSTYP_HAFTREIBUNG = 1;
    public static final int VERSUCHSTYP_GLEITREIBUNG = 2;
    private static final int SENSOR_DELAY_US = 20000;
    private int versuchsTyp = 0;
    private String sVersuchsTyp = "";
    private boolean versuchGestartet = false;
    private float fSchwellenwert = 0.1F;                    // Schwelle für Rutscherkennung
    private float beschleunigungMax = 0.0F;                 // maximal erreichte Beschleunigung
    private float gravity[];                                // aktueller Vektor Gravity
    private float linAccel[];                               // aktueller Vektor Beschleunigung ohne Gravity
    private double aktuellerWinkel;                         // aktueller berechneter Winkel
    private Queue werteListe = new LinkedList();

    private SensorManager mSensorManager;
    private Sensor mGravitySensor, mLinearAccelSensor;
    private TextView tVx, tVy, tVz, tVwinkel, tVxA, tVyA, tVzA, tVschwelle;
    private Button btnStart;
    private LineGraphSeries<DataPoint> mSeriesX, mSeriesY, mSeriesZ;
    private double graphLastT = 5d;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_durchfuehrung, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onOptionsItemSelected():" + item);
        if(id == R.id.action_help){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if(versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) {
                builder.setTitle(R.string.dialog_help_durchfuehrung_gleit_title)
                        .setMessage(R.string.dialog_help_durchfuehrung_gleit)
                        .setPositiveButton("OK", null);
            } else if(versuchsTyp == VERSUCHSTYP_HAFTREIBUNG) {
                builder.setTitle(R.string.dialog_help_durchfuehrung_haft_title)
                        .setMessage(R.string.dialog_help_durchfuehrung_haft)
                        .setPositiveButton("OK", null);
            }
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_durchfuehrung);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_durchfuehrung);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);

        }

        Intent intent = getIntent();
        versuchsTyp = intent.getIntExtra("EXTRA_VERSUCHSTYP", 0);
        switch (versuchsTyp){
            case VERSUCHSTYP_HAFTREIBUNG:
                this.setTitle(R.string.title_activity_haftreibung);
                sVersuchsTyp = getString(R.string.title_activity_haftreibung);
                break;
            case VERSUCHSTYP_GLEITREIBUNG:
                this.setTitle(R.string.title_activity_gleitreibung);
                sVersuchsTyp = getString(R.string.title_activity_gleitreibung);
                break;
            default:
                finish();
                break;
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        tVx = (TextView) findViewById(R.id.textViewAccelX);
        tVy = (TextView) findViewById(R.id.textViewAccelY);
        tVz = (TextView) findViewById(R.id.textViewAccelZ);
        tVschwelle = (TextView) findViewById(R.id.textViewSchwelle);
        btnStart = (Button) findViewById(R.id.buttonHaftreibungStart);
        GraphView graph = (GraphView) findViewById(R.id.graphViewHaftreibung);

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
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(300);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-10);
        graph.getViewport().setMaxY(10);

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);


        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                starteVersuch();
            }
        });


        SeekBar seekBarSchwelle = (SeekBar) findViewById(R.id.seekBarSchwelle);
        seekBarSchwelle.setMax(100);
        seekBarSchwelle.setProgress((int)(fSchwellenwert*10));

        seekBarSchwelle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fSchwellenwert = (float) i / 100;
                tVschwelle.setText(String.format(Locale.GERMAN, "%.02f", fSchwellenwert));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        tVwinkel = (TextView) findViewById(R.id.textViewWinkel);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGravitySensor, SENSOR_DELAY_US);      // delay in µs
        mSensorManager.registerListener(this, mLinearAccelSensor, SENSOR_DELAY_US);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void starteVersuch(){
        versuchGestartet = true;
        beschleunigungMax = 0.0F;
        maxErreicht = false;
        btnStart.setClickable(false);
        btnStart.setText(R.string.button_durchfuehrung_running);
    }

    private void stoppeVersuch(){
        versuchGestartet = false;
        btnStart.setClickable(true);
        btnStart.setText(R.string.button_durchfuehrung_start);

        // Berechnungen anstellen
        float tanalpha = gravity[1]/gravity[2];
        float koeffizient = 0f;
        if(versuchsTyp == VERSUCHSTYP_HAFTREIBUNG) koeffizient = tanalpha;
        if(versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) koeffizient = tanalpha - (float)(beschleunigungMax/(9.81*cos(atan(tanalpha))));

        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // Datei mit Messwerten schreiben
        Calendar date = Calendar.getInstance();
        String FILENAME = DateFormat.format("yyyy-MM-dd-kk-mm-ss", date).toString() + "_" + sVersuchsTyp;
        File file;
        URI fileuri = null;
        try {
            file = File.createTempFile(FILENAME, ".csv", getApplicationContext().getCacheDir());
            fileuri = file.toURI();
            FileWriter writer = new FileWriter(file);
            // TODO: Winkel und Beschleunigung fehlt!!!
            writer.write(
                    String.format(Locale.ENGLISH,
                    "\"sep=;\"\n\r" +
                            "Reibungsversuch\n\r" +
                            "Datum;%s\n\r" +
                            "Ort;%s\n\r" +
                            "Temperatur;%.01f\n\r" +
                            "Luftdruck;%d\n\r" +
                            "Luftfeuchtigkeit;%.01f\n\r" +
                            "Teilnehmer 1;%s\n\r" +
                            "Teilnehmer 2;%s\n\r" +
                            "Typ;%s\n\r" +
                            "Oberfläche 1;%s\n\r" +
                            "Oberfläche 2;%s\n\r" +
                            "Koeffizient;%.02f\n\r",

                            DateFormat.format("yyyy-MM-dd-kk-mm-ss", date).toString(),
                            prefs.getString("ORT", ""),
                            prefs.getFloat("TEMPERATUR", 0.0f),
                            prefs.getInt("LUFTDRUCK", 0),
                            prefs.getFloat("LUFTFEUCHTE", 0.0f),
                            prefs.getString("TEILNEHMER1", ""),
                            prefs.getString("TEILNEHMER2", ""),
                            sVersuchsTyp,
                            prefs.getString("OBERFLAECHE1", ""),
                            prefs.getString("OBERFLAECHE2", ""),
                            koeffizient
                    )
            );
            writer.append("Zeitstempel in ms;Beschleunigung X;Beschleunigung Y;Beschleunigung Z\n\r");
            Log.d(TAG, "Schreibe " + werteListe.size() + " Werte");
            while(werteListe.size() > 0) {
                writer.append(((Messwert)werteListe.poll()).toCSVrow());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, DatenanzeigeActivity.class);
        intent.putExtra("EXTRA_FILEURI", fileuri);
        intent.putExtra("EXTRA_CACHED", true);

        startActivity(intent);


    }


    static final float ALPHA = 0.20f; // filter constant
    protected float[] lowPassFilter( float[] inputs, float[] outputs) {
        if(outputs == null) return inputs;
        for(int i = 0; i<inputs.length; i++){
            outputs[i] = outputs[i] + ALPHA * (inputs[i] - outputs[i]);
        }
        return outputs;
    }

    private class Messwert{
        public long time;
        public float[] acceleration;

        public Messwert(long time, float[] gravity, float[] linaccel){
            this.time = time;
            this.acceleration = new float[3];
            this.acceleration[0] = gravity[0] + linaccel[0];
            this.acceleration[1] = gravity[1] + linaccel[1];
            this.acceleration[2] = gravity[2] + linaccel[2];
        }

        public String toCSVrow(){
            return String.format(Locale.ENGLISH, "%d; %s; %s; %s\n\r", time, acceleration[0], acceleration[1], acceleration[2]);
        }

    }

    private boolean maxErreicht = false;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_GRAVITY:
                gravity = lowPassFilter(sensorEvent.values, gravity);
                tVx.setText(String.format(Locale.GERMAN,"%.02f", gravity[0])); // X
                tVy.setText(String.format(Locale.GERMAN,"%.02f", gravity[1])); // Y
                tVz.setText(String.format(Locale.GERMAN,"%.02f", gravity[2])); // Z
                aktuellerWinkel = Math.atan(gravity[1]/gravity[2])*180/Math.PI;
                tVwinkel.setText(String.format(Locale.GERMAN, "%.02f °", aktuellerWinkel));

                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linAccel = lowPassFilter(sensorEvent.values, linAccel);

                // TODO: Versuchsstopp etwas herauszögern, um mehr Daten im Graph zu sehen
                // TODO: Beschleunigungsmessung, Maximumerkennung optimieren
                if(versuchGestartet){
                    if(abs(linAccel[1]) > beschleunigungMax) beschleunigungMax = abs(linAccel[1]);
                    if(abs(linAccel[1]) > fSchwellenwert) maxErreicht = true;
                    if((versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) && maxErreicht && (abs(linAccel[1]) < (0.8*fSchwellenwert))) stoppeVersuch();
                    if((versuchsTyp == VERSUCHSTYP_HAFTREIBUNG) && abs(linAccel[1]) > fSchwellenwert) stoppeVersuch();
                }

                if(abs(linAccel[1]) > fSchwellenwert) tVwinkel.setBackgroundColor(GREEN);
                else tVwinkel.setBackgroundColor(TRANSPARENT);

                if(gravity != null && linAccel != null) {
                    werteListe.offer(new Messwert(SystemClock.elapsedRealtime(), gravity, linAccel));
                    if(werteListe.size() > 200 && !versuchGestartet){
                        werteListe.remove();
                    }
                }

                graphLastT += 1d;
                mSeriesX.appendData(new DataPoint(graphLastT, linAccel[0]), true, 400);
                mSeriesY.appendData(new DataPoint(graphLastT, linAccel[1]), true, 400);
                mSeriesZ.appendData(new DataPoint(graphLastT, linAccel[2]), true, 400);
                break;
            default:
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

}
