package uk.co.thomasrynne.swisher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PodcastBookmark {

    public static final int NOTHING = -1;
    public static final int WATCHED = -2;
    Executor executor = Executors.newSingleThreadExecutor();

    public static class Bookmark {
        public String episodeName;
        public int millis;
        Bookmark(String episodeName, int millis) {
            this.episodeName = episodeName;
            this.millis = millis;
        }
    }

    private final PodcastBookmarkDbHelper db;

    public PodcastBookmark(Context context) {
        db = new PodcastBookmarkDbHelper(context);
    }

    public Bookmark read(String feed) {
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "select episode, offsetMillis from PodcastBookmarks where feed = ? order by _id desc limit 1",
                new String[] { feed }
        );
        if (cursor.moveToNext()) {
            return new Bookmark(cursor.getString(0), cursor.getInt(1));
        }
        return null;
    }

    public int read(String feed, String episode) {
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "select offsetMillis from PodcastBookmarks where feed = ? and episode = ? order by _id desc limit 1",
                new String[] { feed, episode }
        );
        if (cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public void save(final String podcastUrl, final String episodeName, final int millis) {
        executor.execute(new Runnable() { public void run() {
            ContentValues values = new ContentValues();
            values.put("feed", podcastUrl);
            values.put("episode", episodeName);
            values.put("offsetMillis", millis);
            long id = db.getWritableDatabase().insert("PodcastBookmarks", null, values);
            Log.i("SWISHER", "saved " + episodeName + " " + millis);
        } });
    }
}
class PodcastBookmarkDbHelper extends SQLiteOpenHelper {
    public PodcastBookmarkDbHelper(Context context) {
        super(context, "PodcastBookmark2.db", null, 3);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE PodcastBookmarks(_id integer primary key, feed varchar(1024), episode varchar(512), offsetMillis integer)");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("SWISHER", "from " + oldVersion + " to " + newVersion);
        db.execSQL("drop TABLE PodcastBookmarks");
        db.execSQL("CREATE TABLE PodcastBookmarks(_id integer primary key, feed varchar(1024), episode varchar(512), offsetMillis integer)");
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
