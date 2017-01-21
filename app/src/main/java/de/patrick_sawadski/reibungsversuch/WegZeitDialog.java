package de.patrick_sawadski.reibungsversuch;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class WegZeitDialog extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_weg_zeit);

        setTitle("Vergleichsmessung");

        final EditText etWeg = (EditText)findViewById(R.id.etWeg);
        final EditText etZeit = (EditText)findViewById(R.id.etZeit);

        Intent intent = getIntent();
        long zeitvorgabe = intent.getLongExtra("EXTRA_ZEITVORGABE", 0);
        if(zeitvorgabe > 0) etZeit.setText(String.valueOf(zeitvorgabe / 1000.0));

        findViewById(R.id.btnHinzufuegen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
                Intent result = new Intent();
                try {
                    result.putExtra("EXTRA_WEG", nf.parse(etWeg.getText().toString()).floatValue());
                    result.putExtra("EXTRA_ZEIT", nf.parse(etZeit.getText().toString()).floatValue());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
        findViewById(R.id.btnUeberspringen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        etWeg.requestFocus();

    }
}
