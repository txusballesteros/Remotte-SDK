package com.mobandme.remotte.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;


public class MenuActivity extends Activity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        ((ListView)findViewById(R.id.list)).setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, SensorsActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, RotationActivity.class));
                break;
            case 2:
                startActivity(new Intent(this, ShakeActivity.class));
                break;
        }
    }
}
