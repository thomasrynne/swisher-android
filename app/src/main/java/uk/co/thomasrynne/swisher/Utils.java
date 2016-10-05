package uk.co.thomasrynne.swisher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.annimon.stream.Stream;
import com.google.common.collect.ImmutableList;

import lombok.Value;
import lombok.val;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Utils {
    private Utils() {}
	
	public static final String YOUTUBE_DEVELOPER_KEY = "AIzaSyCLhKjwrLP6FtlQ8GYBcuelPGZ8hMwokYU";

	public static <T> List<T> list(Stream<T> stream) {
		ArrayList<T> list = new ArrayList<T>();
		stream.forEach((item) -> list.add(item));
		return list;
	}
	
	public static String readURL(String url) throws IOException {
		return readURL(url, null);
	}
	public static String readURL(String url, String token) throws IOException {
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

	@Value
	public static class FlatJson {
		public final JSONObject json;

		public boolean has(String name) {
			return json.has(name);
		}

		public String get(String name) {
			try {
				return json.getString(name);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}

		public ImmutableList<FlatJson> getList(String name) {
			try {
				val array = json.getJSONArray(name);
				val result = new ArrayList<FlatJson>(array.length());
				for (int i = 0; i < array.length(); i++) {
					result.add(new FlatJson(array.getJSONObject(i)));
				}
				return ImmutableList.copyOf(result);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static FlatJsonBuilder json() {
		return new FlatJsonBuilder();
	}

	public static class FlatJsonBuilder {
		private final JSONObject json = new JSONObject();
		public FlatJsonBuilder add(String name, String value) {
			try {
				json.put(name, value);
				return this;
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		public FlatJsonBuilder add(String name, List<FlatJson> items) {
			try {
				json.put(name, new JSONArray(items));
				return this;
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		public FlatJson build() {
			return new FlatJson(json);
		}
	}
}
