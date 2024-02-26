package org.nutrivision.bscs.capstone.detection;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.adapter.CategoriesAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.CategoriesClass;

import java.util.ArrayList;
import java.util.Locale;

public class Categories extends AppCompatActivity {
    ImageView arrowBack;
    Button vegetablesBtn, fruitsBtn, rawBtn, processedBtn, snacksBtn, drinksBtn;
    SearchView search;
    CategoriesAdapter adapter;
    ArrayList<CategoriesClass> categoriesList;
    LinearLayout dynamicLayout;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        /*------------- HOOKS -------------*/
        arrowBack = findViewById(R.id.arrowBackBtn);
        vegetablesBtn = findViewById(R.id.vegetablesBtnSeeAll);
        fruitsBtn = findViewById(R.id.fruitsBtnSeeAll);
        rawBtn = findViewById(R.id.rawBtnSeeAll);
        processedBtn = findViewById(R.id.processedFoodsBtnSeeAll);
        snacksBtn = findViewById(R.id.snacksBtnSeeAll);
        drinksBtn = findViewById(R.id.drinksBtnSeeAll);
        search = findViewById(R.id.searchView);
        dynamicLayout = findViewById(R.id.dynamicLayout);

        /*------------- USE -------------*/
        search.clearFocus();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
//                filterData(newText);
                return true;
            }
        });

        arrowBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Categories.super.onBackPressed();
            }
        });
        vegetablesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Vegetables");
                intent.putExtra("Image",R.drawable.vegetable);
                intent.putExtra("Description",R.string.vegetable_desc);
                intent.putExtra("Background",R.drawable.vegetable_backdrop);
                startActivity(intent);
            }
        });
        fruitsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Fruits");
                intent.putExtra("Image",R.drawable.fruit);
                intent.putExtra("Description",R.string.fruit_desc);
                intent.putExtra("Background",R.drawable.fruit_backdrop);
                startActivity(intent);
            }
        });
        rawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Raw");
                intent.putExtra("Image",R.drawable.raw);
                intent.putExtra("Description",R.string.raw_desc);
                intent.putExtra("Background",R.drawable.raw_backdrop);
                startActivity(intent);
            }
        });
        processedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Processed Foods");
                intent.putExtra("Image",R.drawable.processfood);
                intent.putExtra("Description",R.string.processfoods_desc);
                intent.putExtra("Background",R.drawable.processfood_backdrop);
                startActivity(intent);
            }
        });
        snacksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Snacks");
                intent.putExtra("Image",R.drawable.snack);
                intent.putExtra("Description",R.string.snacks_desc);
                intent.putExtra("Background",R.drawable.snack_backdrop);
                startActivity(intent);
            }
        });
        drinksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Categories.this, categoriesList.class);
                intent.putExtra("Title","Drinks");
                intent.putExtra("Image",R.drawable.drinks);
                intent.putExtra("Description",R.string.drinks_desc);
                intent.putExtra("Background",R.drawable.drinks_backdrop);
                startActivity(intent);
            }
        });

//        categoriesList = new ArrayList<>();
//        categoriesList.add(new CategoriesClass(R.drawable.vegetable_backdrop, R.drawable.vegetable, "Vegetables",R.string.vegetable_desc));
//        categoriesList.add(new CategoriesClass(R.drawable.fruit_backdrop, R.drawable.fruit, "Fruit",R.string.fruit_desc));
//        categoriesList.add(new CategoriesClass(R.drawable.raw_backdrop, R.drawable.raw, "Raw",R.string.raw_desc));
//        categoriesList.add(new CategoriesClass(R.drawable.processfood_backdrop, R.drawable.processfood, "Processed Foods",R.string.processfoods_desc));
//        categoriesList.add(new CategoriesClass(R.drawable.snack_backdrop, R.drawable.snack, "Snacks",R.string.snacks_desc));
//        categoriesList.add(new CategoriesClass(R.drawable.drinks_backdrop, R.drawable.drinks, "Drinks",R.string.drinks_desc));
    }
}
