package ar.pelapp.soundboard;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private final String perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private final int MY_WRITE_EXTERNAL_STORAGE = 123;

    private String path;
    private MediaPlayer mp;
    private ArrayAdapter<String> mListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        // Clase que se encarga de reproducir media.
        mp = new MediaPlayer();

        // El Adapter es el puente entre lo programado y lo visible.
        // Guarda la lista de strings de archivos que debe aparecer.
        mListAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_sound,
                R.id.list_item_textview,
                new ArrayList<String>()
        );
        // Clase que representa a la lista como ente visible.
        ListView mListView = (ListView) findViewById(R.id.listview_soundboard);
        // Se relacionan ambas cosas.
        mListView.setAdapter(mListAdapter);

        // Si no tengo permiso para escribir y leer, lo pido.
        if (ContextCompat.checkSelfPermission(this, perm)
                 != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                Snackbar.make(mListView, R.string.permission_rationale_acceptance, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            @TargetApi(Build.VERSION_CODES.M)
                            public void onClick(View v) {
                                requestPermissions(
                                        new String[] { perm },
                                        MY_WRITE_EXTERNAL_STORAGE);
                            }
                        }).show();

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { perm },
                        MY_WRITE_EXTERNAL_STORAGE);
            }
        }

        // Obtengo el path de donde leeré los archivos.
        path = Uri.parse(Environment.getExternalStorageDirectory().getPath()).buildUpon()
                .appendPath("Pelapp").appendPath("Soundboard").build()
                .toString();

        // Qué debe pasar si se cliquea un ítem de la lista.
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String state = Environment.getExternalStorageState();
                if (!state.equals(Environment.MEDIA_MOUNTED)
                        && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
                    return;

                String fileName = mListAdapter.getItem(position);
//                String pathToFile = path + "/" + fileName;
                Uri uriPathToFile = Uri.fromFile(new File(path, fileName));
//                playFile(Uri.parse(pathToFile));
                playFile(uriPathToFile);
            }
        });

        // Qué debe pasar si se mantiene presionado un ítem de la lista.
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long id) {

                String state = Environment.getExternalStorageState();
                if (!state.equals(Environment.MEDIA_MOUNTED)
                        && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
                    return false;

                Uri uri = Uri.parse(path + "/" + mListAdapter.getItem(position));
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("audio/*");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(share, "Compartir"));
                return true;
            }
        });

        populate();
    }

    // Función para poblar la lista.
    private void populate() {

        // No sigue si no tiene el permiso.
        if (ContextCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Chequeo de que la memoria no se encuentre inaccesible.
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)
                && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(this, "Media not mounted, fer", Toast.LENGTH_LONG);
            return;
        }

        // File también refiere a directorios.
        File files = new File(path);

        //  Si no existe el directorio, lo crea.
        if (!files.exists()) {
            if (files.mkdirs()) {
                Toast.makeText(this, "Directorio creado en " + files.getPath(), Toast.LENGTH_LONG)
                        .show();
            } else {
                // Esto no debería poder ocurrir.
                Toast.makeText(this, "Directorio no existe y no fue creado " + files.getPath(), Toast.LENGTH_LONG)
                        .show();
                return;
            }
        } else {
            Toast.makeText(this, "Pon tus audios en "+ files.getPath(), Toast.LENGTH_LONG)
                    .show();
        }

        // Obtiene la lista de archivos.
        File list[] = files.listFiles();
        if (list == null) {
            return;
        }

        // Limpio el adaptador y agrego la nueva lista de archivos.
        mListAdapter.clear();
        for (File auxFile : list) {
            mListAdapter.add(auxFile.getName());
        }
        mListAdapter.notifyDataSetChanged();
    }

    // Corre tras pedir permiso para escribir/leer en disco.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                populate();
            }
        }
    }

    // Intenta reproducir un archivo en path fileUri. Frena y libera los recursos del anterior.
    // https://developer.android.com/reference/android/media/MediaPlayer.html#StateDiagram
    private void playFile(Uri fileUri) {

        if (mp != null) {
            try {
                mp.stop();
                mp.reset();

                mp.setDataSource(this, fileUri);
                mp.prepare();

            } catch (IllegalStateException
                    | IllegalArgumentException
                    | java.io.IOException
                    | SecurityException e) {

                mp.release();
                mp = null;
                mp = MediaPlayer.create(this, fileUri);
            }
        }

        if (mp == null) {
            mp = MediaPlayer.create(this, fileUri);
        }

        try {
            mp.stop();
            mp.start();

        } catch (IllegalStateException e1) {
            Toast.makeText(this, "No pude reproducir este archivo.", Toast.LENGTH_LONG)
                    .show();
        } catch (NullPointerException e2) {
            Toast.makeText(this, "No pude encontrar o reproducir este archivo. (NPE)", Toast.LENGTH_LONG)
                    .show();
        }

        try {
            if (!mp.isPlaying()) {
                MediaPlayer mpAux = MediaPlayer.create(this, fileUri);
                mpAux.start();
                Toast.makeText(this, "Fuck it", Toast.LENGTH_SHORT);

                mp.reset();
                mp.release();
                mp = null;
                mp = mpAux;
            }
        } catch (IllegalStateException e) {
            Toast.makeText(this, "What did you DO", Toast.LENGTH_LONG)
                    .show();
        }
    }


//    Esto sirve para crear el botón de settings

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onPause() {
        super.onPause();
        if (mp != null) {
            mp.release();
        }
    }
}
