package com.hondaafr;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.hondaafr.Libs.UI.Fragments.ClusterFragment;
import com.hondaafr.Libs.UI.Fragments.MapFragment;
import com.hondaafr.Libs.UI.Fragments.AdaptiveAfrFragment;
import com.hondaafr.Libs.UI.Fragments.ObdLogFragment;
import com.hondaafr.Libs.UI.Fragments.ScientificFragment;

public class MainPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_CLUSTER = 0;
    public static final int PAGE_ADAPTIVE_AFR = 1;
    public static final int PAGE_MAP = 2;
    public static final int PAGE_SCIENTIFIC = 3;
    public static final int PAGE_OBD_LOG = 4;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_CLUSTER:
                return new ClusterFragment();
            case PAGE_ADAPTIVE_AFR:
                return new AdaptiveAfrFragment();
            case PAGE_MAP:
                return new MapFragment();
            case PAGE_SCIENTIFIC:
                return new ScientificFragment();
            case PAGE_OBD_LOG:
            default:
                return new ObdLogFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}

