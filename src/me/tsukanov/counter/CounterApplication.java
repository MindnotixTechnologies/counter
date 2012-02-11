package me.tsukanov.counter;

import java.util.LinkedHashMap;

import android.app.Application;

public class CounterApplication extends Application {

	public LinkedHashMap<String, Integer> counters;
	public int activePosition;
	public String activeKey;
	public static int theme;
	public boolean isUpdateNeeded;
	
	@Override
	public void onCreate() {
		super.onCreate();
		theme = R.style.Theme_Sherlock_Light_DarkActionBar;
		activePosition = 0;
		isUpdateNeeded = false;
	}
}