package com.hondaafr.Libs.UI.Fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment implements PipAware {
    // Window insets are handled at the activity level by EdgeToEdgeHelper
    // Subclasses can override onViewCreated() directly for their setup
}

