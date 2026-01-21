package com.hondaafr;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.hondaafr.Libs.UI.Fragments.ClusterFragment;
import com.hondaafr.Libs.UI.Fragments.MapFragment;
import com.hondaafr.Libs.UI.Fragments.ObdLogFragment;
import com.hondaafr.Libs.UI.Fragments.ScientificFragment;

public class MainPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_SCIENTIFIC = 0;
    public static final int PAGE_CLUSTER = 1;
    public static final int PAGE_MAP = 2;
    public static final int PAGE_OBD_LOG = 3;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_CLUSTER:
                return new ClusterFragment();
            case PAGE_MAP:
                return new MapFragment();
            case PAGE_OBD_LOG:
                return new ObdLogFragment();
            case PAGE_SCIENTIFIC:
            default:
                return new ScientificFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}

