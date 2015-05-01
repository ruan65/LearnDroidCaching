package com.gmail.ruan65.learndroidcaching;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class FunPic extends ActionBarActivity implements View.OnClickListener {

    private ImageView mImageView;
    private File cacheDir;
    boolean cacheMode;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fun_pic);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        cacheMode = prefs.getBoolean(getString(R.string.prefs_cache), false);

        cacheDir = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                ? getExternalCacheDir()
                : getCacheDir();

        mImageView = (ImageView) findViewById(R.id.imageView);

        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_fun_pic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.cache) {

            prefs.edit().putBoolean(getString(R.string.prefs_cache), cacheMode = !cacheMode).apply();

            item.setTitle(cacheMode ? "Не кешировать" : "Кешировать");

            if (!cacheMode) clearCache();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        String url = getString(
                id == R.id.button  ? R.string.dog_url :
                id == R.id.button2 ? R.string.cat_url :
                                     R.string.miley_url
        );

        Bitmap bitmap = cacheMode ? BitmapFactory.decodeFile(createImageFile(url).getPath()) : null;

        if (null == bitmap) {

            new GetPicFromUrlThenShowTask().execute(url);
        } else {

            mImageView.setImageBitmap(bitmap);
        }
    }

    private void closeStream(Closeable stream) {
        try {
            if (null != stream) stream.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private File createImageFile(String url) {

        String fileName = Uri.parse(url).getLastPathSegment();

        return new File(cacheDir, fileName);
    }

    private void clearCache() {

        final File[] cachedImgs = cacheDir.listFiles();

        if (null != cachedImgs) {

            new Thread(new Runnable() {

                @Override
                public void run() {

                    for (File f : cachedImgs) {
                        f.delete();
                    }
                }
            }).start();
        }
    }

    private class GetPicFromUrlThenShowTask extends AsyncTask<String, Void, Bitmap> {

        String url;
        InputStream inStream;
        Bitmap bm;

        @Override
        protected Bitmap doInBackground(String... urls) {

            url = urls[0];

            try {
                inStream = new URL(url).openConnection().getInputStream();

                bm = BitmapFactory.decodeStream(inStream);

                TimeUnit.SECONDS.sleep(2);

                return bm;

            } catch (Exception e) {
                Log.d(getApplication().getPackageName(), e.getMessage());
            } finally {
                closeStream(inStream);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap img) {

            mImageView.setImageBitmap(img);

            if (cacheMode) new SaveBitmapToDiskCacheTask().execute(url, bm);
        }
    }

    private class SaveBitmapToDiskCacheTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... url_0_Bitmap_1) {

            File file = createImageFile((String) url_0_Bitmap_1[0]);

            OutputStream out = null;

            try {
                out = new FileOutputStream(file);

                ((Bitmap) url_0_Bitmap_1[1]).compress(Bitmap.CompressFormat.JPEG, 50, out);

                out.flush();

            } catch (Exception e) {
                Log.d(getApplication().getPackageName(), e.getMessage());
            } finally {
                closeStream(out);
            }
            return null;
        }
    }
}
