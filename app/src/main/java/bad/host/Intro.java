/**
 *  BadHost  by  https://github.com/Burning-machine    2020 - 2021.
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */
package bad.host;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.widget.NestedScrollView;
import static bad.host.MainActivity.completedFirstRun;

public class Intro extends Activity {
    public static boolean unzip;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Button skip = findViewById(R.id.close);
       setAnimation();
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            Intent intent = new Intent(Intro.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            unzip=true;
            completedFirstRun(getApplicationContext());
            Intro.this.finish();
            }
            });
    }
    private void setAnimation() {
        NestedScrollView scrollLayout = (NestedScrollView) findViewById(R.id.scroll);
        AnimationDrawable animationDrawable = (AnimationDrawable) scrollLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);
        animationDrawable.start();
    }
}
