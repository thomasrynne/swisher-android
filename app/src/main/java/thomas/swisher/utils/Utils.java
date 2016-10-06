package thomas.swisher.utils;

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
