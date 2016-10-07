package thomas.swisher.todo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.common.base.Optional;

import thomas.swisher.ui.UIBackendEvents;

/**
 * Consider using a library for the http and db calls
 * This is currently called from the background thread but performs slowish IO
 */

class CardStoreDbHelper extends SQLiteOpenHelper {
    public CardStoreDbHelper(Context context) {
        super(context, "Cards.db", null, 1);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Cards (_id integer primary key, card varchar(10), data varchar(5000))");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
public class CardStore {

    private final EventBus eventBus = EventBus.getDefault();
	private final CardStoreDbHelper db; 
	
	public CardStore(Context context) {
		db = new CardStoreDbHelper(context);
	}
	
	public Optional<JSONObject> read(String card) {
		JSONObject value = readLocal(card);
		if (value == null) {
            eventBus.post(new UIBackendEvents.ToastEvent("Looking up card..."));
			value = readRemote(card);
			if (value != null) { store(card, value); }
			return Optional.fromNullable(value);
		} else {
			return Optional.fromNullable(value);
		}
	}
	
	private JSONObject readRemote(String card) {
		try {
			String buffer = readURL("http://swisher.herokuapp.com/cardservice/read?cardnumber="+card);
			if (buffer != null) {
			    JSONArray list = new JSONArray(buffer);
			    return list.getJSONObject(0);
		    } else {
                eventBus.post(new UIBackendEvents.ToastEvent("Nothing found"));
		    	return null;
		    }
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String readURL(String url) throws IOException {
		return readURL(url, null);
	}

	private static String readURL(String url, String token) throws IOException {
		DefaultHttpClient defaultClient = new DefaultHttpClient();
		HttpGet httpGetRequest = new HttpGet(url);
		if (token != null) {
			httpGetRequest.setHeader("Authorization", "Bearer " + token);
		}
		Log.i("SWISHER", "Request " + token);
		HttpResponse httpResponse = defaultClient.execute(httpGetRequest);
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
		StringBuilder buffer = new StringBuilder();
		String line = null;
		while( (line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append('\n');
		}
		if (httpResponse.getStatusLine().getStatusCode() == 200) {
			return buffer.toString();
		} else {
			Log.i("SWISHER", "remote query failed: " + httpResponse.getStatusLine() + "\n" + buffer);
			return null;
		}
	}


	private void storeRemote(String card, JSONObject json) {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
		    HttpPost httpPostRequest = new HttpPost("http://swisher.herokuapp.com/cardservice/store");
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("cardnumber", card));
	        nameValuePairs.add(new BasicNameValuePair("value", json.toString(2)));
	        httpPostRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        HttpResponse response = httpClient.execute(httpPostRequest);
	        boolean success = response.getStatusLine().getStatusCode() == 200;
	        if (success) {
	        	Log.i("SWISHER", "remotely stored cardL " + card);
	        } else {
	        	Log.i("SWISHER", "remote storage failed for card: " + card);
	        }
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private JSONObject readLocal(String card) {
		try {
			Cursor cursor = db.getReadableDatabase().rawQuery(
			    "select data from Cards where card = ? order by _id desc",
			    new String[] { card }
			);
			if (cursor.moveToFirst()) {
				return new JSONObject(cursor.getString(0));
			} else {
				return null;
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	public void store(final String card, final JSONObject json) {
		storeLocal(card, json);
		new Thread(new Runnable() { public void run() {
			storeRemote(card, json);
		} }).start(); //Async for faster feedback after waving card
	}
	private void storeLocal(String card, JSONObject json) {
		try {
			ContentValues values = new ContentValues();
			values.put("card", card);
			values.put("data", json.toString(2));
			db.getWritableDatabase().insert("Cards", null, values);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
}
