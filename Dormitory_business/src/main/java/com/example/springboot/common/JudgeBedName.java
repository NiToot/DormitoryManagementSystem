package com.example.springboot.common;

public class JudgeBedName {

    private static String bedName;

    public static String getBedName(int num) {
        switch (num) {
            case 1:
                return bedName = "first_bed";
            case 2:
                return bedName = "second_bed";
            case 3:
                return bedName = "third_bed";
            case 4:
                return bedName = "fourth_bed";
            default:
                return null;
        }
    }
}
