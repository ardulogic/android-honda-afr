package com.hondaafr;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    public MutableLiveData<Boolean> fuelConsumptionAvailable = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> showFuelConsumption = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> showTotalFuelConsumption = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> engineSoundsEnabled = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> showCluster = new MutableLiveData<>(true);
}
