package tw.voidchen.SCU_WiFiAuth;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.CheckBox;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.method.LinkMovementMethod;

public class MainActivity extends Activity
{
    private static final String SETTING_DATA = "PrefSetData";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Init MainToggle
        ToggleButton MainToggle = (ToggleButton) findViewById(R.id.MainToggle);
        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        MainToggle.setChecked(SetData.getBoolean("MainToggleState", false));

        if(SetData.getBoolean("MainToggleState", false))
            MainToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.maintoggleon));
        else
            MainToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.maintoggleoff));

        //Init TextUsername and TextPassword
        EditText TargetText;
        if(SetData.contains("username")){
            TargetText = (EditText) findViewById(R.id.TextUsername);
            TargetText.setText(SetData.getString("username", ""));
        }
        if(SetData.contains("password")){
            TargetText = (EditText) findViewById(R.id.TextPassword);
            TargetText.setText(SetData.getString("password", ""));
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    public void MainToggleClick(View view){
        boolean state = ((ToggleButton) view).isChecked();

        SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
        if(state){
            if(!SetData.contains("username") || !SetData.contains("password")){
                state = false;
                ((ToggleButton) view).setChecked(false);

                Context context = getApplicationContext();
                Toast.makeText(context, R.string.NeedAuthInfo, Toast.LENGTH_SHORT).show();
            }
        }
        SharedPreferences.Editor SetDataEditor = SetData.edit();
        SetDataEditor.putBoolean("MainToggleState", state);
        SetDataEditor.apply();

        if(state)
            ((ToggleButton) view).setBackgroundDrawable(getResources().getDrawable(R.drawable.maintoggleon));
        else
            ((ToggleButton) view).setBackgroundDrawable(getResources().getDrawable(R.drawable.maintoggleoff));
    }

    public void SaveConfig(View view){
        //save username and password
        EditText TextUsername = (EditText) findViewById(R.id.TextUsername);
        EditText TextPassword = (EditText) findViewById(R.id.TextPassword);
        String username = TextUsername.getText().toString();
        String password = TextPassword.getText().toString();

        if(username.isEmpty() || password.isEmpty()){
            Context context = getApplicationContext();
            Toast.makeText(context, R.string.BlankAuthInfo, Toast.LENGTH_SHORT).show();
        }
        else{
            SharedPreferences SetData = getSharedPreferences(SETTING_DATA, 0);
            SharedPreferences.Editor SetDataEditor = SetData.edit();
            SetDataEditor.putString("username", username);
            SetDataEditor.putString("password", password);
            SetDataEditor.apply();

            Context context = getApplicationContext();
            Toast.makeText(context, R.string.SaveAuthInfo, Toast.LENGTH_SHORT).show();

            //change MainToggle state
            ToggleButton MainToggle = (ToggleButton) findViewById(R.id.MainToggle);
            MainToggle.setChecked(true);
            MainToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.maintoggleon));
            SetDataEditor.putBoolean("MainToggleState", true);
            SetDataEditor.apply();
        }
    }

    public void ShowPassword(View view){
        //change TextPassword inputType
        EditText target = (EditText) findViewById(R.id.TextPassword);
        boolean state = ((CheckBox) view).isChecked();
        if(!state)
            target.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else
            target.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }
}
