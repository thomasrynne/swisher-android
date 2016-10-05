package uk.co.thomasrynne.swisher;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.google.common.base.Optional;

import org.json.JSONException;
import org.json.JSONObject;

public class Songs {

    private static Map<Integer,Uri> albumImages = new HashMap<Integer,Uri>();

    public static Optional<Uri> imageForAlbum(int albumId) {
        return Optional.fromNullable(albumImages.get(albumId));
    }

    public static void init(ContentResolver contentResolver) {

        String[] projection = new String[] {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);
        final int albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
        final int albumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
        final Map<Integer,Uri> images = new HashMap<Integer,Uri>();
        if (cursor.moveToFirst()) {
            do {
                int albumId = cursor.getInt(albumIdIndex);
                String albumArtPath = cursor.getString(albumArtIndex);
                if (albumArtPath!=null) {
                    images.put(albumId, Uri.fromFile(new File(albumArtPath)));
                }
            } while (cursor.moveToNext());
        }
        Log.i("S", "album images: " + images);
        cursor.close();
        albumImages = images;
    }
}
