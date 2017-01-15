package de.patrick_sawadski.reibungsversuch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.content.FileProvider;
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
            View Touchzone = view.findViewById(R.id.touchzoneViewData);
            Touchzone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent;
                    String filename= list.get(i).toString();
                    String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
                    if(extension.equals("csv")) {
                        intent = new Intent(getApplicationContext(), DatenanzeigeActivity.class);
                        intent.putExtra("EXTRA_CACHED", false);
                        intent.putExtra("EXTRA_FILEURI", list.get(i).toURI());
                        startActivity(intent);
                    } else if(extension.equals("jpg")){
                        Uri imageURI = FileProvider.getUriForFile(getApplicationContext(), "de.patrick_sawadski.fileprovider", list.get(i));
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

            ((Button)view.findViewById(R.id.buttonListItemDelete)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Zweite Abfrage einfügen (Wirklich löschen?)
                    // Hier sind deine Changes
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
