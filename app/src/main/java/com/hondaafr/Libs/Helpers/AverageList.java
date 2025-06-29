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

    public void add(double number) {
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

        return sum / numbers.size();
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

    public double getMaxValue() {
        if (numbers.isEmpty()) {
            return 0; // Avoid division by zero
        }

        double maxValue = Double.NEGATIVE_INFINITY;
        for (Double number : numbers) {
            if (number > maxValue) {
                maxValue = number;
            }
        }

        return maxValue;
    }

    public double getMinValue() {
        if (numbers.isEmpty()) {
            return 0; // Or throw an exception if that fits your use case better
        }

        double minValue = Double.POSITIVE_INFINITY;
        for (Double number : numbers) {
            if (number < minValue) {
                minValue = number;
            }
        }

        return minValue;
    }

    // Clear all numbers and reset the sum
    public void clear() {
        numbers.clear();
        sum = 0D;
    }

    public boolean isEmpty() {
        return numbers.isEmpty();
    }
}
