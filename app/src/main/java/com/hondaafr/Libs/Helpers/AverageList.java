package com.hondaafr.Libs.Helpers;

import java.util.LinkedList;
import java.util.Queue;

public class AverageList {

    private Queue<Double> numbers;
    private int maxCount;
    private Double sum;

    public AverageList(int maxCount) {
        numbers = new LinkedList<>();
        this.maxCount = maxCount;
        this.sum = 0D;
    }

    public void addNumber(double number) {
        if (numbers.size() == maxCount) {
            Double removedNumber = numbers.poll(); // Remove the oldest number
            sum -= removedNumber;
        }

        numbers.add(number);
        sum += number;
    }

    public double getAvg() {
        if (numbers.isEmpty()) {
            return 0; // Avoid division by zero
        }

        return (double) sum / numbers.size();
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int max) {
        maxCount = max;
    }

    public int getCurrentCount() {
        return numbers.size();
    }

    // Calculate the average distance from a target value
    public double getAverageDistanceFromTarget(double target) {
        if (numbers.isEmpty()) {
            return 0; // Avoid division by zero
        }

        double totalDistance = 0;
        for (Double number : numbers) {
            totalDistance += Math.abs(number - target);
        }

        return totalDistance / numbers.size();
    }

    // Clear all numbers and reset the sum
    public void clear() {
        numbers.clear();
        sum = 0D;
    }
}
