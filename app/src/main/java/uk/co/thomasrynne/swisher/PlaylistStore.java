package uk.co.thomasrynne.swisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class PlaylistStoreDbHelper extends SQLiteOpenHelper {
    public PlaylistStoreDbHelper(Context context) {
        super(context, "Playlists.db", null, 1);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Playlists (_id integer primary key, name varchar(255))");
        db.execSQL("CREATE TABLE PlaylistsItems (_id integer primary key, playlist integer, position integer, data varchar(5000))");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
public class PlaylistStore {
	private final PlaylistStoreDbHelper db; 
	
	public PlaylistStore(Context context) {
		db = new PlaylistStoreDbHelper(context);
	}
	
	public List<String> playlists() {
		List<String> playlists = new LinkedList<String>();
		Cursor cursor = db.getReadableDatabase().rawQuery(
		    "select name from Playlists order by _id desc",
		    new String[] { }
		);
		while (cursor.moveToNext()) {
			playlists.add(cursor.getString(0));
		}
		return playlists;	
	}
	
	public List<JSONObject> read(String name) {
		List<JSONObject> playlist = new LinkedList<JSONObject>();
		Cursor cursor = db.getReadableDatabase().rawQuery(
		    "select i.data from Playlists p, PlaylistItems i where p._id = i.playlist and p.name = ? order by i.position desc",
		    new String[] { name }
		);
		try {
			while (cursor.moveToNext()) {
				playlist.add(new JSONObject(cursor.getString(0)));
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return playlist;
	}
	public void store(final String playlist, final List<JSONObject> items) {
		try {
			ContentValues values = new ContentValues();
			values.put("name", playlist);
			long id = db.getWritableDatabase().insert("Playlists", null, values);
			for (JSONObject json: items) {
				ContentValues valuesx = new ContentValues();
				valuesx.put("playlist", id);
				valuesx.put("data", json.toString(2));
				db.getWritableDatabase().insert("PlaylistItems", null, valuesx);
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
}
