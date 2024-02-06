package org.nutrivision.bscs.capstone.detection.adapter;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

public class CategoriesClass {
    int image;
    int background;
    String title;
    int desc;

    public CategoriesClass(int background, int image, String title, int desc) {
        this.background = background;
        this.image = image;
        this.title = title;
        this.desc = desc;
    }

    public int getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public int getDesc() {
        return desc;
    }

    public int getBackground() {
        return background;
    }
}
