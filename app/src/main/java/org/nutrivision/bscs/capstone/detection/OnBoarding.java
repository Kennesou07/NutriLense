package org.nutrivision.bscs.capstone.detection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import org.nutrivision.bscs.capstone.detection.adapter.SliderAdapter;

public class OnBoarding extends AppCompatActivity {
    ViewPager viewpager;
    LinearLayout dotsLayout;
    SliderAdapter sliderAdapter;
    TextView[] dots;
    Button btnGetStarted;
    Animation animation;
    int currentPos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.on_boarding);

        viewpager = findViewById(R.id.slider);
        dotsLayout = findViewById(R.id.dots);
        btnGetStarted = findViewById(R.id.btnGetStarted);

        sliderAdapter = new SliderAdapter(this);
        viewpager.setAdapter(sliderAdapter);
        addDots(0);
        viewpager.addOnPageChangeListener(changeListener);
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OnBoarding.this,Welcome.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void skip(View view){
        startActivity(new Intent(this,Welcome.class));
        finish();
    }

    public void next(View view){
        viewpager.setCurrentItem(currentPos + 1);
    }

    private void addDots(int position){
        dots = new TextView[4];
        dotsLayout.removeAllViews();
        for(int i=0; i<dots.length; i++){
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setLineSpacing(0, .5f);
            dots[i].setPadding(0, 0, 0, 0);
            dots[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            dotsLayout.addView(dots[i]);
        }

        if(dots.length >0){
            dots[position].setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    ViewPager.OnPageChangeListener changeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDots(position);
            currentPos = position;
            if(position == 0){
                btnGetStarted.setVisibility(View.GONE);
            }
            else if(position == 1){
                btnGetStarted.setVisibility(View.GONE);
            }
            else if(position == 2){
                btnGetStarted.setVisibility(View.GONE);
            }
            else{
                animation = AnimationUtils.loadAnimation(OnBoarding.this,R.anim.button_animation);
                btnGetStarted.setAnimation(animation);
                btnGetStarted.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
;