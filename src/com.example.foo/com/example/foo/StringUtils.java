package com.example.foo;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringUtils {
    
    public String reverse(String input) {
        if (input == null) {
            return null;
        }
        return new StringBuilder(input).reverse().toString();
    }
    
    public boolean isPalindrome(String input) {
        if (input == null) {
            return false;
        }
        String cleaned = input.replaceAll("\\s+", "").toLowerCase();
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }
    
    public String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }
    
    public String camelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Arrays.stream(input.split("\\s+"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining());
    }
    
    public int countVowels(String input) {
        if (input == null) {
            return 0;
        }
        return (int) input.toLowerCase()
            .chars()
            .filter(ch -> "aeiou".indexOf(ch) >= 0)
            .count();
    }
    
    public String removeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s+", "");
    }
}