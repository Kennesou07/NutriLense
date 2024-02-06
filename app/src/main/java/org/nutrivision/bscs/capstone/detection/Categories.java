package org.nutrivision.bscs.capstone.detection;

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
        categoriesList = new ArrayList<>();
        categoriesList.add(new CategoriesClass(R.drawable.vegetable_backdrop, R.drawable.vegetable, "Vegetables",R.string.vegetable_desc));
        categoriesList.add(new CategoriesClass(R.drawable.fruit_backdrop, R.drawable.fruit, "Fruit",R.string.fruit_desc));
        categoriesList.add(new CategoriesClass(R.drawable.raw_backdrop, R.drawable.raw, "Raw",R.string.raw_desc));
        categoriesList.add(new CategoriesClass(R.drawable.processfood_backdrop, R.drawable.processfood, "Processed Foods",R.string.processfoods_desc));
        categoriesList.add(new CategoriesClass(R.drawable.snack_backdrop, R.drawable.snack, "Snacks",R.string.snacks_desc));
        categoriesList.add(new CategoriesClass(R.drawable.drinks_backdrop, R.drawable.drinks, "Drinks",R.string.drinks_desc));
    }
}
