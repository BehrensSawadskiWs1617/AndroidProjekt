package de.patrick_sawadski.reibungsversuch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

import static java.lang.Float.NaN;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences prefs;
    private TextView
            tvTeilnehmer,
            tvOberflaechen,
            tvTemperatur,
            tvLuftdruck,
            tvLuftfeuchte,
            tvStandort;  // TODO: Ort fehlt!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        findViewById(R.id.linearLayoutVersuchsbedingungen).setOnClickListener(this);
        findViewById(R.id.btnHaftreibung).setOnClickListener(this);
        findViewById(R.id.btnGleitreibung).setOnClickListener(this);
        findViewById(R.id.btnDatensammlung).setOnClickListener(this);

        tvTeilnehmer = ((TextView) findViewById(R.id.textViewTeilnehmer));
        tvOberflaechen = ((TextView) findViewById(R.id.textViewMaterialien));
        tvTemperatur = ((TextView) findViewById(R.id.textViewTemp));
        tvLuftdruck = ((TextView) findViewById(R.id.textViewLuftdruck));
        tvLuftfeuchte = ((TextView) findViewById(R.id.textViewLuftfeuchte));


    }

    @Override
    protected void onResume() {
        super.onResume();
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
        try {
            tvTeilnehmer.setText(prefs.getString("TEILNEHMER1", "") + ", " + prefs.getString("TEILNEHMER2", ""));
            tvOberflaechen.setText(prefs.getString("OBERFLAECHE1", "") + ", " + prefs.getString("OBERFLAECHE2", ""));
            tvTemperatur.setText(String.format(Locale.GERMAN, "%.01f °C", prefs.getFloat("TEMPERATUR", NaN)));
            tvLuftdruck.setText(String.format(Locale.GERMAN, "%d hPa", prefs.getInt("LUFTDRUCK", 0)));
            tvLuftfeuchte.setText(String.format(Locale.GERMAN, "%.01f %% rel.", prefs.getFloat("LUFTFEUCHTE", NaN)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch(view.getId()) {
            case R.id.linearLayoutVersuchsbedingungen:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.btnHaftreibung:
                intent = new Intent(this, DurchfuehrungActivity.class);
                intent.putExtra("EXTRA_VERSUCHSTYP", DurchfuehrungActivity.VERSUCHSTYP_HAFTREIBUNG);
                startActivity(intent);
                break;
            case R.id.btnGleitreibung:
                intent = new Intent(this, DurchfuehrungActivity.class);
                intent.putExtra("EXTRA_VERSUCHSTYP", DurchfuehrungActivity.VERSUCHSTYP_GLEITREIBUNG);
                startActivity(intent);
                break;
            case R.id.btnDatensammlung:
                intent = new Intent(this, DatensammlungActivity.class);
                startActivity(intent);
                break;
            default: break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
