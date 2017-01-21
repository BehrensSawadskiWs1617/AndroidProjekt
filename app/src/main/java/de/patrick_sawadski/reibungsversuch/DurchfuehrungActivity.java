package de.patrick_sawadski.reibungsversuch;

import android.app.ActionBar;
import android.app.Activity;
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

    private static final int SENSOR_DELAY_US = 20000;

    private static final int REQUEST_WEG_ZEIT = 1;          // Intent Request Codes

    public static final int VERSUCHSTYP_HAFTREIBUNG = 1;
    public static final int VERSUCHSTYP_GLEITREIBUNG = 2;
    private int versuchsTyp = 0;
    private String sVersuchsTyp = "";

    private static final int VERSUCHSSTATUS_BEREIT         = 0;
    private static final int VERSUCHSSTATUS_GESTARTET      = 1;
    private static final int VERSUCHSSTATUS_RUTSCHEND      = 2;
    private static final int VERSUCHSSTATUS_GESTOPPT       = 3;
    private int versuchsStatus = VERSUCHSSTATUS_BEREIT;

    private long referenzzeit;
    private float fSchwellenwert = 0.1F;                    // Schwelle für Rutscherkennung
    private float maxBeschleunigung;                        // maximal erreichte Beschleunigung
    private float gravity[];                                // aktueller Vektor Gravity
    private float linAccel[];                               // aktueller Vektor Beschleunigung ohne Gravity
    private float winkelGravityVektor[];                    // Winkel für Versuchsberechnung

    private Queue werteListe;

    private class Messwert{
        long time;
        float[] acceleration;

        Messwert(long time, float[] gravity, float[] linaccel){
            this.time = time;
            this.acceleration = new float[3];
            this.acceleration[0] = gravity[0] + linaccel[0];
            this.acceleration[1] = gravity[1] + linaccel[1];
            this.acceleration[2] = gravity[2] + linaccel[2];
        }

        String toCSVrow(long startzeit){
            return String.format(Locale.ENGLISH, "%d; %s; %s; %s\n\r", time-startzeit, acceleration[0], acceleration[1], acceleration[2]);
        }
    }

    private SensorManager mSensorManager;
    private Sensor mGravitySensor, mLinearAccelSensor;
    private TextView tVx, tVy, tVz, tVwinkel, tVxA, tVyA, tVzA, tVschwelle;
    private Button btnStart;
    private LineGraphSeries<DataPoint> mSeriesX, mSeriesY, mSeriesZ;
    private double graphLastT = 5d;

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
        tVwinkel = (TextView) findViewById(R.id.textViewWinkel);
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

    /*********************************
     * Versuchssteuerung
     */

    private void starteVersuch(){
        versuchsStatus = VERSUCHSSTATUS_GESTARTET;
        referenzzeit = SystemClock.elapsedRealtime();
        maxBeschleunigung = 0.0F;
        werteListe = new LinkedList();
        btnStart.setClickable(false);
        btnStart.setText(R.string.button_durchfuehrung_running);
    }

    private double getWinkel(float[] gravityVect){
        return Math.atan(gravityVect[1]/gravityVect[2])*180/Math.PI;
    }

    private void messwertGravity(float[] values){
        gravity = lowPassFilter(values, gravity);
        tVx.setText(String.format(Locale.GERMAN,"%.02f", gravity[0])); // X
        tVy.setText(String.format(Locale.GERMAN,"%.02f", gravity[1])); // Y
        tVz.setText(String.format(Locale.GERMAN,"%.02f", gravity[2])); // Z
        tVwinkel.setText(String.format(Locale.GERMAN, "%.02f °", getWinkel(gravity)));
    }

    private float startAccel;       // Beschleunigung bei Schwellenwertüberschreitung zur Vorzeichenerkennung
    private long startzeitpunkt;
    private void messwertLinaccel(float[] values){
        linAccel = lowPassFilter(values, linAccel);

        graphLastT += 1d;
        mSeriesX.appendData(new DataPoint(graphLastT, linAccel[0]), true, 400);
        mSeriesY.appendData(new DataPoint(graphLastT, linAccel[1]), true, 400);
        mSeriesZ.appendData(new DataPoint(graphLastT, linAccel[2]), true, 400);

        if(gravity != null && linAccel != null && (versuchsStatus == VERSUCHSSTATUS_GESTARTET || versuchsStatus == VERSUCHSSTATUS_RUTSCHEND)) {
            werteListe.offer(new Messwert(SystemClock.elapsedRealtime(), gravity, linAccel));
            if(werteListe.size() > 2000){   // Größe begrenzen, nur letzte 2000 Messwerte ( bei 20 ms Messwertabstand = 40 Sekunden )
                werteListe.remove();
            }
        }

        if(versuchsStatus == VERSUCHSSTATUS_GESTARTET) {
            if (abs(linAccel[1]) > fSchwellenwert) ausloeseVersuch();
        }

        if(versuchsStatus == VERSUCHSSTATUS_RUTSCHEND) {
            if(versuchsTyp == VERSUCHSTYP_HAFTREIBUNG && abs(linAccel[1]) < 0.2 * fSchwellenwert) stoppeVersuch();
            if(versuchsTyp == VERSUCHSTYP_GLEITREIBUNG){
                if(startAccel < 0) {
                    if(linAccel[1] < maxBeschleunigung) maxBeschleunigung = linAccel[1];
                    if (linAccel[1] > 0) stoppeVersuch();
                }
                if(startAccel > 0){
                    if(linAccel[1] > maxBeschleunigung) maxBeschleunigung = linAccel[1];
                    if(linAccel[1] < 0) stoppeVersuch();
                }
            }
        }

        if(abs(linAccel[1]) > fSchwellenwert) tVwinkel.setBackgroundColor(GREEN);
        else tVwinkel.setBackgroundColor(TRANSPARENT);
    }

    private void ausloeseVersuch(){
        versuchsStatus = VERSUCHSSTATUS_RUTSCHEND;
        winkelGravityVektor = gravity.clone();
        startAccel = linAccel[1];
        startzeitpunkt = SystemClock.elapsedRealtime();
    }

    private void stoppeVersuch() {
        versuchsStatus = VERSUCHSSTATUS_GESTOPPT;
        if (versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) {
            Intent intent = new Intent(this, WegZeitDialog.class);
            intent.putExtra("EXTRA_ZEITVORGABE", SystemClock.elapsedRealtime() - startzeitpunkt);
            startActivityForResult(intent, REQUEST_WEG_ZEIT);
        } else if (versuchsTyp == VERSUCHSTYP_HAFTREIBUNG) {
            speichereVersuch(false, 0, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_WEG_ZEIT){
            Float weg = 0F, zeit = 0F;
            Boolean ext = false;
            if(resultCode == Activity.RESULT_OK){
                ext = true;
                weg = data.getFloatExtra("EXTRA_WEG", 0.0F);
                zeit = data.getFloatExtra("EXTRA_ZEIT", 0.0F);
            }
            speichereVersuch(ext, weg, zeit);
        }
    }

    private void speichereVersuch(boolean externeMess, float weg, float zeit) {
        float koeffizient = winkelGravityVektor[1] / winkelGravityVektor[2];
        float extBeschl = 0F, extKoeff = 0F;
        if (versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) {
            if(externeMess){
                extBeschl = 2 * weg / (zeit * zeit);
                extKoeff = koeffizient - (float) (extBeschl / (9.81 * cos(atan(koeffizient))));
            }
            koeffizient -= (float) (abs(maxBeschleunigung) / (9.81 * cos(atan(koeffizient))));
        }
        // Datei mit Messwerten schreiben
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Calendar date = Calendar.getInstance();
        String FILENAME = DateFormat.format("yyyy-MM-dd-kk-mm-ss", date).toString() + "_" + sVersuchsTyp;
        File file;
        URI fileuri = null;
        try {
            file = File.createTempFile(FILENAME, ".csv", getApplicationContext().getCacheDir());
            fileuri = file.toURI();
            FileWriter writer = new FileWriter(file);
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
                            "Koeffizient;%.02f\n\r" +
                            "Winkel;%.02f\n\r",

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
                            koeffizient,
                            getWinkel(winkelGravityVektor)
                    )
            );
            if(versuchsTyp == VERSUCHSTYP_GLEITREIBUNG){
                writer.append(String.format(Locale.ENGLISH, "Beschleunigung;%.02f\n\r", maxBeschleunigung));
                if(externeMess){
                    writer.append(String.format(Locale.ENGLISH, "Vergleich Beschleunigung;%.02f\n\r" +
                            "Vergleich Koeffizient;%.02f\n\r", extBeschl, extKoeff));
                }
            }
            writer.append("Zeitstempel in ms;Beschleunigung X;Beschleunigung Y;Beschleunigung Z\n\r");
            Log.d(TAG, "Schreibe " + werteListe.size() + " Werte");
            while(werteListe.size() > 0) {
                writer.append(((Messwert)werteListe.poll()).toCSVrow(referenzzeit));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        versuchsStatus = VERSUCHSSTATUS_BEREIT;
        btnStart.setClickable(true);
        btnStart.setText(R.string.button_durchfuehrung_start);

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


    /**************************************
     * Sensorcallback
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_GRAVITY:
                messwertGravity(sensorEvent.values);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                messwertLinaccel(sensorEvent.values);
                break;
            default:
                break;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    /*****************************************
     * Anzeige der Versuchshilfe
     */
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
}
