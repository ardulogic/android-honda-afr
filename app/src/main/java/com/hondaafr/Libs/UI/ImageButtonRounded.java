package com.hondaafr.Libs.UI;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;

import com.hondaafr.R;

public class ImageButtonRounded extends androidx.appcompat.widget.AppCompatImageButton {

    private boolean isIconActive = false;
    private boolean state = false;

    public ImageButtonRounded(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setState(boolean enabled) {
        this.state = enabled;

        if (state) {
            setBackgroundResource(R.drawable.image_button_on);
        } else {
            setBackgroundResource(R.drawable.image_button_off);
        }
    }

    public void setIconState(boolean isActive) {
        Log.d("ImageButtonRounded", "Intermediate state:" + isActive);

        if (isActive) {
            setColorFilter(Color.parseColor("#00FF66"), PorterDuff.Mode.SRC_IN);
        } else {
            setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN);
        }

        isIconActive = isActive;
    }

    public boolean getState() {
        return state;
    }

    public void toggleState() {
        setState(!state);
    }

}
