package de.patrick_sawadski.reibungsversuch;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.TRANSPARENT;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;

public class DurchfuehrungActivity extends AppCompatActivity implements SensorEventListener {


    // Definition von Versuchstypen
    public static final int VERSUCHSTYP_HAFTREIBUNG = 1;
    public static final int VERSUCHSTYP_GLEITREIBUNG = 2;
    private int versuchsTyp = 0;
    private String sVersuchsTyp = "";


    // Zeit zwischen den Sensorwerten in µs
    private static final int SENSOR_DELAY_US = 10000;

    private boolean versuchGestartet = false;

    private SensorManager mSensorManager;
    private Sensor mGravitySensor, mLinearAccelSensor;
    private TextView tVx, tVy, tVz, tVwinkel, tVxA, tVyA, tVzA, tVschwelle;
    private Button btnStart;
    private LineGraphSeries<DataPoint> mSeriesX, mSeriesY, mSeriesZ;
    private double graphLastT = 5d;

    private float fSchwellenwert = 0.1F;
    private float beschleunigungMax = 0.0F; // maximal erreichte Beschleunigung
    private double aktuellerWinkel;
    private float gravity[];            // aktueller Vektor Gravity
    private float linAccel[];           // aktueller Vektor Beschleunigung ohne Gravity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_durchfuehrung);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_durchfuehrung);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        graph.getViewport().setMaxX(400);
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
            writer.write(
                    String.format(Locale.ENGLISH,
                    "\"sep=;\"\n\r" +
                            "Reibungsversuch\n\r" +
                            "Datum;%s\n\r" +
                            "Teilnehmer 1;%S\n\r" +
                            "Teilnehmer 2;%S\n\r" +
                            "Typ;%s\n\r" +
                            "Oberfläche 1;%s\n\r" +
                            "Oberfläche 2;%s\n\r" +
                            "Koeffizient;%.02f",
                    DateFormat.format("yyyy-MM-dd-kk-mm-ss", date).toString(),
                    prefs.getString("TEILNEHMER1", ""),
                    prefs.getString("TEILNEHMER2", ""),
                    sVersuchsTyp,
                    prefs.getString("OBERFLAECHE1", ""),
                    prefs.getString("OBERFLAECHE2", ""),
                    koeffizient
                    )
            );
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

            // TODO: restliche Versuchsbedingungen
            // TODO: raw Datenarray anhängen (Beschleunigungen, Gravityvektor)

        Intent intent = new Intent(this, DatenanzeigeActivity.class);
        intent.putExtra("EXTRA_FILEURI", fileuri);
        intent.putExtra("EXTRA_CACHED", true);

        // TODO: Restliche Extras entfernen, wenn der Parser geht... werden dann aus csv gelesen

//        intent.putExtra("EXTRA_VERSUCHSTYP", versuchsTyp);
//        intent.putExtra("EXTRA_KOEFFIZIENT", koeffizient);
//        intent.putExtra("EXTRA_WINKEL", aktuellerWinkel);
//        intent.putExtra("EXTRA_MAXBESCHLEUNIGUNG", beschleunigungMax);
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


                if(versuchGestartet){
                    if(abs(linAccel[1]) > beschleunigungMax) beschleunigungMax = abs(linAccel[1]);
                    if(abs(linAccel[1]) > fSchwellenwert) maxErreicht = true;
                    if((versuchsTyp == VERSUCHSTYP_GLEITREIBUNG) && maxErreicht && (linAccel[1] < (0.9*fSchwellenwert))) stoppeVersuch();
                    if((versuchsTyp == VERSUCHSTYP_HAFTREIBUNG) && abs(linAccel[1]) > fSchwellenwert) stoppeVersuch();
                }

                if(abs(linAccel[1]) > fSchwellenwert) tVwinkel.setBackgroundColor(GREEN);
                else tVwinkel.setBackgroundColor(TRANSPARENT);



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
