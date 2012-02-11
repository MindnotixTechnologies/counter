package me.tsukanov.counter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class CounterActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener {

	private static final String DATA_FILE = "data_dev_12";
	private static final int DIALOG_ADD = 100;
	private static final int DIALOG_EDIT = 101;
	private static final int DIALOG_DELETE = 102;

	CounterApplication app;
	ActionBar actionBar;
	CounterFragment currentFragment;
	SharedPreferences data, settings;
	List<String> keys;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		String savedTheme = settings.getString("theme", "light");
		if (savedTheme.equals("dark")) {
			CounterApplication.theme = R.style.Theme_Sherlock;
		} else if (savedTheme.equals("light")) {
			CounterApplication.theme = R.style.Theme_Sherlock_Light_DarkActionBar;
		}
		setTheme(CounterApplication.theme);
		super.onCreate(savedInstanceState);

		app = (CounterApplication) getApplication();
		actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);

		app.counters = new LinkedHashMap<String, Integer>();
		data = getBaseContext().getSharedPreferences(DATA_FILE,
				Context.MODE_PRIVATE);
		Map<String, ?> prefsMap = data.getAll();
		if (prefsMap.isEmpty()) {
			app.counters.put(
					(String) getResources().getText(
							R.string.default_counter_name),
					CounterFragment.DEFALUT_VALUE);
		} else {
			for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
				app.counters.put(entry.getKey(), (Integer) entry.getValue());
			}
		}

		keys = new ArrayList<String>();
		for (String key : app.counters.keySet()) {
			keys.add(key);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, keys);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);
		// Restore previously selected element
		actionBar.setSelectedNavigationItem(app.activePosition);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (app.isUpdateNeeded) {
			app.isUpdateNeeded = false;
			refreshActivity();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		data = getBaseContext().getSharedPreferences(DATA_FILE,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = data.edit();
		for (String name : app.counters.keySet()) {
			Log.v("SharedPreferences", "Saving!");
			editor.putInt(name, app.counters.get(name));
			editor.commit();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		app.activeKey = keys.get(itemPosition);
		app.activePosition = itemPosition;
		currentFragment = new CounterFragment();
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, currentFragment).commit();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			showDialog(DIALOG_ADD);
			return true;
		case R.id.menu_edit:
			showDialog(DIALOG_EDIT);
			return true;
		case R.id.menu_delete:
			showDialog(DIALOG_DELETE);
			return true;
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_ADD:
			dialog = getAddDialog().create();
			break;
		case DIALOG_EDIT:
			dialog = getEditDialog().create();
			break;
		case DIALOG_DELETE:
			dialog = getDeleteDialog().create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private Builder getAddDialog() {
		AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(this);
		addDialogBuilder.setTitle(getResources().getText(
				R.string.dialog_add_title));

		LinearLayout addDialogLayout = (LinearLayout) getLayoutInflater()
				.inflate(R.layout.editor_layout, null);

		// Name input label
		TextView nameInputLabel = new TextView(this);
		nameInputLabel.setText(getResources()
				.getText(R.string.dialog_edit_name));
		addDialogLayout.addView(nameInputLabel);

		// Name input
		final EditText nameInput = new EditText(this);
		nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
		addDialogLayout.addView(nameInput);

		// Value input label
		TextView valueInputLabel = new TextView(this);
		valueInputLabel.setText(getResources().getText(
				R.string.dialog_edit_value));
		addDialogLayout.addView(valueInputLabel);

		// Value input
		final EditText valueInput = new EditText(this);
		valueInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		InputFilter[] valueFilter = new InputFilter[1];
		valueFilter[0] = new InputFilter.LengthFilter(getCharLimit());
		valueInput.setFilters(valueFilter);
		valueInput.setText(String.valueOf(CounterFragment.DEFALUT_VALUE));
		addDialogLayout.addView(valueInput);

		addDialogBuilder.setView(addDialogLayout);
		addDialogBuilder.setPositiveButton(
				getResources().getText(R.string.dialog_button_add),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						app.counters.put(nameInput.getText().toString(),
								Integer.parseInt(valueInput.getText()
										.toString()));
						refreshActivity();
					}
				});
		addDialogBuilder.setNegativeButton(
				getResources().getText(R.string.dialog_button_cancel), null);
		return addDialogBuilder;
	}

	private Builder getEditDialog() {
		AlertDialog.Builder editDialogBuilder = new AlertDialog.Builder(this);
		editDialogBuilder.setTitle(getResources().getText(
				R.string.dialog_edit_title));

		LinearLayout editDialogLayout = (LinearLayout) getLayoutInflater()
				.inflate(R.layout.editor_layout, null);

		// Name input label
		TextView nameInputLabel = new TextView(this);
		nameInputLabel.setText(getResources()
				.getText(R.string.dialog_edit_name));
		editDialogLayout.addView(nameInputLabel);

		// Name input
		final EditText nameInput = new EditText(this);
		nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
		editDialogLayout.addView(nameInput);

		// Value input label
		TextView valueInputLabel = new TextView(this);
		valueInputLabel.setText(getResources().getText(
				R.string.dialog_edit_value));
		editDialogLayout.addView(valueInputLabel);

		// Value input
		final EditText valueInput = new EditText(this);
		valueInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		InputFilter[] valueFilter = new InputFilter[1];
		valueFilter[0] = new InputFilter.LengthFilter(getCharLimit());
		valueInput.setFilters(valueFilter);
		valueInput.setText(String.valueOf(CounterFragment.DEFALUT_VALUE));
		editDialogLayout.addView(valueInput);

		editDialogBuilder.setView(editDialogLayout);
		editDialogBuilder.setPositiveButton(
				getResources().getText(R.string.dialog_button_apply),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Delete current counter, add modified
						refreshActivity(); // TODO Rewrite
					}
				});
		editDialogBuilder.setNegativeButton(
				getResources().getText(R.string.dialog_button_cancel), null);
		return editDialogBuilder;
	}

	private Builder getDeleteDialog() {
		AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(this);
		deleteDialogBuilder
				.setMessage(
						getResources().getText(R.string.dialog_delete_title))
				.setCancelable(false)
				.setPositiveButton(
						getResources().getText(R.string.dialog_button_delete),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// TODO Delete counter
								app.counters.remove(app.activeKey);
								Toast.makeText(
										getBaseContext(),
										getResources().getText(
												R.string.toast_remove_sucess_1)
												+ " \""
												+ app.activeKey
												+ "\" "
												+ getResources()
														.getText(
																R.string.toast_remove_sucess_2),
										Toast.LENGTH_SHORT).show();
								refreshActivity();
							}
						})
				.setNegativeButton(
						getResources().getText(R.string.dialog_button_cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return deleteDialogBuilder;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (prefs.getBoolean("hardControlOn", true)) {
				currentFragment.increment();
				return true;
			}
			return false;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (prefs.getBoolean("hardControlOn", true)) {
				currentFragment.decrement();
				return true;
			}
			return false;
		case KeyEvent.KEYCODE_CAMERA:
			if (prefs.getBoolean("hardControlOn", true)) {
				currentFragment.refresh();
				return true;
			}
			return false;
		case KeyEvent.KEYCODE_BACK:
			finish();
			return true;
		default:
			return false;
		}
	}

	private int getCharLimit() {
		return String.valueOf(CounterFragment.MAX_VALUE).length();
	}

	private void refreshActivity() {
		Intent intent = getIntent();
		finish();
		startActivity(intent);
	}

}
