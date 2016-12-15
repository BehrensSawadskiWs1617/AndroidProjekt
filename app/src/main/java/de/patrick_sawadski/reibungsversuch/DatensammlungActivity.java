package de.patrick_sawadski.reibungsversuch;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatensammlungActivity extends AppCompatActivity {

    private ListView listview;
    private ArrayList<File> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datensammlung);
        listview = (ListView) findViewById(R.id.activity_datensammlung);

    }

    @Override
    protected void onResume() {
        super.onResume();
        File[] filesBuf = getFilesDir().listFiles();
        files = new ArrayList<>();
        for(File inFile : filesBuf){
            if(!inFile.isDirectory()){
                files.add(inFile);
            }
        }
        CustomAdapter adapter = new CustomAdapter(files, this);
        //ArrayAdapter<File> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        listview.setAdapter(adapter);
    }

    public class CustomAdapter extends BaseAdapter implements ListAdapter {

        private ArrayList<File> list = new ArrayList<File>();
        private Context context;

        public CustomAdapter(ArrayList<File> list, Context context){
            this.list = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.listitem_datensammlung,null);
            }

            TextView itemText = (TextView)view.findViewById(R.id.textViewListItemText);
            itemText.setText(list.get(i).getName());
            view.findViewById(R.id.touchzoneViewData).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), DatenanzeigeActivity.class);
                    intent.putExtra("EXTRA_CACHED", false);
                    intent.putExtra("EXTRA_FILEURI", list.get(i).toURI());
                    startActivity(intent);
                }
            });
            ((Button)view.findViewById(R.id.buttonListItemDelete)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Zweite Abfrage einfügen (Wirklich löschen?)
                    if(list.get(i).delete()){
                        Toast.makeText(getApplicationContext(), "Gelöscht:" + list.get(i).getName(), Toast.LENGTH_SHORT).show();
                        list.remove(i);
                        notifyDataSetChanged();
                    }
                }
            });
            return view;
        }
    }
}
