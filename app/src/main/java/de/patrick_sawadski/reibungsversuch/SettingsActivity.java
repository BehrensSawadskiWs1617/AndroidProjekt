package de.patrick_sawadski.reibungsversuch;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity{

    private SharedPreferences prefs;
    private EditText
            etTeilnehmer1,
            etTeilnehmer2,
            etOberflaeche1,
            etOberflaeche2,
            etTemperatur,
            etLuftdruck,
            etLuftfeuchte,
            etOrt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etTeilnehmer1   = ((EditText) findViewById(R.id.editTextTeilnehmer1));
        etTeilnehmer2   = ((EditText) findViewById(R.id.editTextTeilnehmer2));
        etOberflaeche1  = ((EditText) findViewById(R.id.editTextOberflaeche1));
        etOberflaeche2  = ((EditText) findViewById(R.id.editTextOberflaeche2));
        etTemperatur    = ((EditText) findViewById(R.id.editTextTemperatur));
        etLuftdruck     = ((EditText) findViewById(R.id.editTextLuftdruck));
        etLuftfeuchte   = ((EditText) findViewById(R.id.editTextLuftfeuchtigkeit));
        etOrt           = ((EditText) findViewById(R.id.editTextOrt));

        prefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            etTeilnehmer1.setText(prefs.getString("TEILNEHMER1", ""));
            etTeilnehmer2.setText(prefs.getString("TEILNEHMER2", ""));
            etOberflaeche1.setText(prefs.getString("OBERFLAECHE1", ""));
            etOberflaeche2.setText(prefs.getString("OBERFLAECHE2", ""));
            etTemperatur.setText(String.format(Locale.ENGLISH, "%.01f", prefs.getFloat("TEMPERATUR", 25.0f)));
            etLuftdruck.setText(String.format(Locale.ENGLISH, "%d", prefs.getInt("LUFTDRUCK", 1080)));
            etLuftfeuchte.setText(String.format(Locale.ENGLISH, "%.01f", prefs.getFloat("LUFTFEUCHTE", 35.0f)));
            etOrt.setText(prefs.getString("ORT", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("TEILNEHMER1", etTeilnehmer1.getText().toString());
        editor.putString("TEILNEHMER2", etTeilnehmer2.getText().toString());
        editor.putString("OBERFLAECHE1", etOberflaeche1.getText().toString());
        editor.putString("OBERFLAECHE2", etOberflaeche2.getText().toString());
        editor.putString("ORT", etOrt.getText().toString());

        // TODO: Benutzer auf falsche Eingaben hinweisen! Z.B. durch Einfärben vom Textfeld während der Eingabe
        try {
            editor.putFloat("TEMPERATUR", nf.parse(etTemperatur.getText().toString()).floatValue());
            editor.putInt("LUFTDRUCK", nf.parse(etLuftdruck.getText().toString()).intValue());
            editor.putFloat("LUFTFEUCHTE", nf.parse(etLuftfeuchte.getText().toString()).floatValue());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        editor.apply();

    }
}
