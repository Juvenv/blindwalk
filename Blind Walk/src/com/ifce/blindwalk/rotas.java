package com.ifce.blindwalk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.blindwalk.R;
import com.google.android.gms.maps.model.LatLng;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class rotas extends ActionBarActivity implements OnInitListener, LocationListener, SensorEventListener {

	private TextView edittext2;
	private ImageButton btnSpeak;
	private Toolbar mToolbar;
	private final int REQ_CODE_SPEECH_INPUT = 100;
	private List<LatLng> list;
	private TextToSpeech tts;
	private double lat_atual, lng_atual;
	private String local;
	private ArrayList<LatLng> pontosfinais = new ArrayList<LatLng>();
	private ArrayList<Boolean> pontosfinaisboleanos = new ArrayList<Boolean>();
	private ArrayList<String> palavras = new ArrayList<String>();
	private Timer timer;
	private Vibrator mVibrator;
	private float azimuth;
	private double r1, r2;
	private SensorManager mSensorManager;
	private String origin = "nada";
	private TimerTask tarefa;
	private LocationManager locationManager;
	private Button btn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rotas);
		tts = new TextToSpeech(this, this);
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		timer = new Timer();
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setTitle("Blind Walk Canindé");
		setSupportActionBar(mToolbar);
		edittext2 = (TextView) findViewById(R.id.editText2);
		final ProgressDialog ringProgressDialog = ProgressDialog.show(rotas.this, "Verificando conexão á internet...",
				"Espere um momento ...", true);
		ringProgressDialog.setCancelable(true);
		ringProgressDialog.setCanceledOnTouchOutside(false);
		add("");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(7000);
				} catch (Exception e) {
				}
				if (verificaConexao() == false) {
					speak("Sem acesso á internet , impossível de pesquisar rota , voltando a  pagína principal");

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(6000);
							} catch (Exception e) {
							}
						}
					}).start();

					Intent t = new Intent(rotas.this, MainActivity.class);
					startActivity(t);
				} else {
					speak("com Acesso á internet");
					add("Clique no centro da tela para falar a rota desejada");
				}
				ringProgressDialog.dismiss();
			}
		}).start();
		btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
		btnSpeak.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				promptSpeechInput();
			}
		});

		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				rotabutton(String.valueOf(edittext2.getText()).toLowerCase(new Locale("pt", "BR")));
			}
		});
		locationManager = (LocationManager) getSystemService(rotas.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void destroy() {
       timer= new Timer();
       timer.cancel();
	}

	@Override
	public void onResume() {
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
		super.onResume();
	}

	@Override
	public void onPause() {
		edittext2.setText("");
	destroy();
		mSensorManager.unregisterListener(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		edittext2.setText("");
		destroy();
		mSensorManager.unregisterListener(this);
		super.onDestroy();
	}

	// text to speech
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(new Locale("pt", "BR"));
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(rotas.this, "Seu dispositivo não suporta Text to Speech", Toast.LENGTH_SHORT).show();
			} else {
				speak("Tela de pesquisa de Rotas de canindé");
				add("Verificando conexão com a internet");
			}
		} else {
			Toast.makeText(rotas.this, "Falha no Text to Speech", Toast.LENGTH_SHORT).show();
		}
	}

	// verifica conexão com a internet
	public boolean verificaConexao() {
		boolean conectado = false;
		ConnectivityManager conectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conectivityManager.getActiveNetworkInfo() != null && conectivityManager.getActiveNetworkInfo().isAvailable()
				&& conectivityManager.getActiveNetworkInfo().isConnected()) {
			conectado = true;
		} else {
			conectado = false;
		}
		return conectado;
	}

	// speech recognizer
	private void promptSpeechInput() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
		try {
			destroy();
			speak("fale por favor");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);

		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT)
					.show();
		}
	}

	// resultados da activity
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQ_CODE_SPEECH_INPUT: {
			if (resultCode == RESULT_OK && null != data) {
				ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				speak(result.get(0));
				edittext2.setText(String.valueOf(result.get(0)));
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				add("clique abaixo da tela para pesquisar a rota");
			}
			break;
		}
		}
	}

	// botão de pesquisa
	@SuppressLint("DefaultLocale")
	public void rotabutton(String r) {
		pontosfinais.clear();
		pontosfinaisboleanos.clear();
		palavras.clear();
		origin = local;
		speak("Pesquisando rotas");
		destroy();
		if (r.toLowerCase().contains("prefeitura") || r.toLowerCase().contains("prefeitura de canindé")) {
			pegarRota(origin, "-4.350640,-39.313339");
		} else if (r.toLowerCase().contains("ceo")
				|| r.toLowerCase().contains("centro de especialidades odontológicas")) {
			pegarRota(origin, "-4.350265,-39.311633");
		} else {
			pegarRota(origin, r);
		}
	}

	// metodo pra pesquisar rotas
	private void pegarRota(final String origin, final String destination) {
		new Thread() {
			public void run() {
				String des = null;
				try {
					des = URLEncoder.encode(destination, "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination="
						+ des + "&mode=WALKING&language=pt-br";
				HttpResponse response;
				HttpGet request;
				AndroidHttpClient client = AndroidHttpClient.newInstance("route");
				request = new HttpGet(url);
				try {
					response = client.execute(request);
					final String answer = EntityUtils.toString(response.getEntity());
					runOnUiThread(new Runnable() {
						public void run() {
							try {
								list = JSONRoute(answer);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							speak("espere um momento");
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					client.close();
				}
			}
		}.start();
	}

	// capturando dados do arquivo json
	private List<LatLng> JSONRoute(String json) throws JSONException {
		JSONObject result = new JSONObject(json);
		if (!result.get("status").equals("OK")) {
			speak("Rota não encontrada,tente pesquisar novamente");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			edittext2.setText("");
			return null;
		} else {
			List<LatLng> lines = new ArrayList<LatLng>();
			double lat, lng;
			JSONArray routes = result.getJSONArray("routes");
			JSONArray steps = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
			for (int i = 0; i < steps.length(); i++) {
				lat = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
						.getJSONObject(i).getJSONObject("end_location").getDouble("lat");
				lng = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
						.getJSONObject(i).getJSONObject("end_location").getDouble("lng");
				pontosfinais.add(new LatLng(lat, lng));
				pontosfinaisboleanos.add(false);
				palavras.add(decodifica(routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0)
						.getJSONArray("steps").getJSONObject(i).getString("html_instructions")));
			}
			tarefa = new TimerTask() {
				public void run() {
					try {
						verificalocal();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			timer = new Timer();
			timer.scheduleAtFixedRate(tarefa, 8500, 8500);
			return (lines);
		}
	}

	private String decodifica(String texto) {
		texto = texto.replaceAll("\\<(/?[^\\>]+)\\>", "");
		return texto;
	}

	private void verificalocal() {
		for (int i = 0; i < pontosfinaisboleanos.size(); i++) {
			speak(" ");
			add(" ");
			while (pontosfinaisboleanos.get(i).booleanValue() == false) {
				r1 = pontosfinais.get(i).latitude;
				r2 = pontosfinais.get(i).longitude;
				if (pontosfinaisboleanos.get(pontosfinaisboleanos.size() - 1) == false) {
					if (distancia(new LatLng(lat_atual, lng_atual),
							new LatLng(pontosfinais.get(i).latitude, pontosfinais.get(i).longitude)) <= 5) {
						add("Seu destino está há cinco metros");
						add(palavras.get(i));
						pontosfinaisboleanos.set(i, true);
					} else {
						add("Distância Atual "
								+ String.valueOf(distancia(new LatLng(lat_atual, lng_atual),
										new LatLng(pontosfinais.get(i).latitude, pontosfinais.get(i).longitude)))
								+ " metros");
						add("Continue seguindo em frente de acordo com a vibração ");
						try {
							Thread.sleep(7000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else if (pontosfinaisboleanos.get(pontosfinaisboleanos.size() - 1) == true) {
					add("Chegada com sucesso");
					timer.cancel();
				}
			}
		}
	}

	private void speak(String speech) {
		tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
	}

	public void add(String text) {
		tts.speak(text, TextToSpeech.QUEUE_ADD, null);
	}

	private double bearing(double lat1, double lng1, double lat2, double lng2) {
		double latitude1 = Math.toRadians(lat1);
		double latitude2 = Math.toRadians(lat2);
		double longitude1 = Math.toRadians(lng1);
		double longitude2 = Math.toRadians(lng2);
		double dLong = longitude2 - longitude1;
		double dphi = Math.log(Math.tan(latitude2 / 2.0 + Math.PI / 4.0) / Math.tan(latitude1 / 2.0 + Math.PI / 4.0));
		if (Math.abs(dLong) > Math.PI) {
			if (dLong > 0.0) {
				dLong = -(2.0 * Math.PI - dLong);
			} else {
				dLong = (2.0 * Math.PI + dLong);
			}
		}
		double bearing = (Math.toDegrees(Math.atan2(dLong, dphi)) + 360.0) % 360.0;
		return bearing;
	}

	private static int distancia(LatLng StartP, LatLng EndP) {
		double lat1 = StartP.latitude;
		double lat2 = EndP.latitude;
		double lon1 = StartP.longitude;
		double lon2 = EndP.longitude;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return (int) ((int) 6366000 * c);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		azimuth = event.values[0];
		if (pontosfinais.size() != 0) {
			if ((bearing(lat_atual, lng_atual, r1, r2) - 20) <= azimuth
					&& (bearing(lat_atual, lng_atual, r1, r2) + 20) >= azimuth) {
				mVibrator.vibrate(100);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {
		Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
		Toast.makeText(getBaseContext(), "Gps desativado!!! ", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationChanged(Location location) {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, this);
		if (locationManager != null) {
			if (location == null) {
				location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				lat_atual = location.getLatitude();
				lng_atual = location.getLongitude();
				r1 = location.getLatitude();
				r2 = location.getLongitude();
			} else {
				local = location.getLatitude() + "," + location.getLongitude();
				lat_atual = location.getLatitude();
				lng_atual = location.getLongitude();
				r1 = location.getLatitude();
				r2 = location.getLongitude();
			}
		}
	}
}
