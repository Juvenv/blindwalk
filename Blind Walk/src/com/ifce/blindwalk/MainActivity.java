package com.ifce.blindwalk;

import java.util.Locale;

import com.example.blindwalk.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnInitListener, OnGestureListener, OnDoubleTapListener {
	private Toolbar mToolbar;
	private TextToSpeech tts;
	private String fala, fala1;
	private GestureDetectorCompat mDetector;
	private Vibrator mVibrator;
	private TextView txt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.telainicial);
		mDetector = new GestureDetectorCompat(this, this);
		mDetector.setOnDoubleTapListener(this);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setTitle("Blind Walk Canindé");
		setSupportActionBar(mToolbar);
		tts = new TextToSpeech(this, this);
		fala = "Seja Bem Vindo ao App Blind Walk";
		fala1 = "Pressione a tela central por um segundo para pesquisar um local desejado";
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		txt = (TextView) findViewById(R.id.textView1);
		Typeface type = Typeface.createFromAsset(getAssets(), "fonts/robotobold.ttf");
		txt.setTypeface(type);
	}

	@Override

	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(new Locale("pt", "BR"));
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(MainActivity.this, "Seu dispositivo não suporta Text to Speech", Toast.LENGTH_SHORT)
						.show();
			} else {
				speak(fala);
				add(fala1);
			}
		} else {
			Toast.makeText(MainActivity.this, "Falha no Text to Speech", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mVibrator != null)
			mVibrator.cancel();
	}

	// TexttoSpeech
	@SuppressWarnings("deprecation")
	private void speak(String speech) {
		tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
	}

	// TexttoSpeech
	@SuppressWarnings("deprecation")
	public void add(String text) {
		tts.speak(text, TextToSpeech.QUEUE_ADD, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		mVibrator.vibrate(1000);
		Intent t = new Intent(MainActivity.this, rotas.class);
		startActivity(t);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			Intent i = new Intent(android.content.Intent.ACTION_VIEW);
			i.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.lars.blindwalk&hl=pt_BR"));
			startActivity(i);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
