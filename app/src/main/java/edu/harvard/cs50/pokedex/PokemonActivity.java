package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private String url;
    private RequestQueue requestQueue;

    private boolean catched;
    private TextView catchButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private ImageView pokeSprite;
    private String spriteUrl;

    private String urlDesc;
    private TextView description;
    private RequestQueue descRequest;

    public void toggleCatch(View view) {
        if (catched){
            releasePokemon();

        } else {
            catchPokemon();
        }
    }

    private void catchPokemon(){
        catched = true;
        catchButton.setText("Release !");
        editor.putBoolean(nameTextView.getText().toString(), true);
        editor.commit();
    }

    private void releasePokemon(){
        catched = false;
        catchButton.setText("Catch !");
        editor.remove(nameTextView.getText().toString());
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);

        catchButton = findViewById(R.id.catch_button);
        sharedPreferences = getApplicationContext()
                .getSharedPreferences("Pokecatcher", 0);
        editor = sharedPreferences.edit();
        catched = false;

        pokeSprite = findViewById(R.id.pkm_img);
        description = findViewById(R.id.Desc);
        descRequest = Volley.newRequestQueue(getApplicationContext());


        load();
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }

                    if (sharedPreferences.getBoolean(response.getString("name"), false)) {
                        catchPokemon();
                    } else {
                        releasePokemon();
                    }

                    JSONObject spritesEntries = response.getJSONObject("sprites");
                    spriteUrl = spritesEntries.getString("front_default");

                    new DownloadSpriteTask().execute(spriteUrl);

                    urlDesc = "https://pokeapi.co/api/v2/pokemon-species/";
                    urlDesc = urlDesc.concat(Integer.toString(response.getInt("id")));

                    JsonObjectRequest request2 = new JsonObjectRequest(Request.Method.GET, urlDesc,
                            null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray txtEntries = response.getJSONArray("flavor_text_entries");
                                for (int i = 0; i < txtEntries.length(); i++){
                                    JSONObject txtEntry = txtEntries.getJSONObject(i);
                                    String language = txtEntry.getJSONObject("language").getString("name");
                                    String desc = txtEntry.getString("flavor_text");

                                    if (language.equals("en")){
                                        description.setText(desc);
                                        break;
                                    }
                                }

                            } catch (JSONException e2) {
                                Log.e("cs50", "Desc json error", e2);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error2) {
                            Log.e("cs50", "Desc details error", error2);
                        }
                    });
                    descRequest.add(request2);

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });
        requestQueue.add(request);
    }

    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
            catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            pokeSprite.setImageBitmap(bitmap);
        }
    }
}
