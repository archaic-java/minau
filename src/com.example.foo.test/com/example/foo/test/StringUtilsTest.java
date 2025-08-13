package com.example.foo.test;

import com.example.foo.StringUtils;
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

public class StringUtilsTest implements TestSuite {
    
    private final StringUtils stringUtils = new StringUtils();
    
    @Test
    public void testReverse() {
        assert "olleh".equals(stringUtils.reverse("hello")) : "Reverse of 'hello' should be 'olleh'";
        assert "".equals(stringUtils.reverse("")) : "Reverse of empty string should be empty";
        assert "a".equals(stringUtils.reverse("a")) : "Reverse of single char should be itself";
        assert stringUtils.reverse(null) == null : "Reverse of null should be null";
        assert "321".equals(stringUtils.reverse("123")) : "Reverse of '123' should be '321'";
    }
    
    @Test
    public void testIsPalindrome() {
        assert stringUtils.isPalindrome("racecar") : "'racecar' is a palindrome";
        assert stringUtils.isPalindrome("A man a plan a canal Panama") : "Should ignore spaces and case";
        assert !stringUtils.isPalindrome("hello") : "'hello' is not a palindrome";
        assert stringUtils.isPalindrome("") : "Empty string is a palindrome";
        assert stringUtils.isPalindrome("a") : "Single character is a palindrome";
        assert !stringUtils.isPalindrome(null) : "null is not a palindrome";
    }
    
    @Test
    public void testCapitalize() {
        assert "Hello".equals(stringUtils.capitalize("hello")) : "Should capitalize first letter";
        assert "Hello".equals(stringUtils.capitalize("HELLO")) : "Should lowercase rest";
        assert "H".equals(stringUtils.capitalize("h")) : "Should work with single char";
        assert "".equals(stringUtils.capitalize("")) : "Empty string should remain empty";
        assert stringUtils.capitalize(null) == null : "null should return null";
    }
    
    @Test
    public void testCamelCase() {
        assert "HelloWorld".equals(stringUtils.camelCase("hello world")) : "Should camel case words";
        assert "TheQuickBrownFox".equals(stringUtils.camelCase("the quick brown fox")) : "Multiple words";
        assert "Test".equals(stringUtils.camelCase("test")) : "Single word";
        assert "".equals(stringUtils.camelCase("")) : "Empty string";
        assert stringUtils.camelCase(null) == null : "null should return null";
    }
    
    @Test
    public void testCountVowels() {
        assert stringUtils.countVowels("hello") == 2 : "'hello' has 2 vowels";
        assert stringUtils.countVowels("aeiou") == 5 : "'aeiou' has 5 vowels";
        assert stringUtils.countVowels("AEIOU") == 5 : "Should be case insensitive";
        assert stringUtils.countVowels("xyz") == 0 : "'xyz' has no vowels";
        assert stringUtils.countVowels("") == 0 : "Empty string has no vowels";
        assert stringUtils.countVowels(null) == 0 : "null has no vowels";
    }
    
    @Test
    public void testRemoveWhitespace() {
        assert "helloworld".equals(stringUtils.removeWhitespace("hello world")) : "Should remove spaces";
        assert "test".equals(stringUtils.removeWhitespace("  test  ")) : "Should remove all whitespace";
        assert "abc".equals(stringUtils.removeWhitespace("a\tb\nc")) : "Should remove tabs and newlines";
        assert "".equals(stringUtils.removeWhitespace("")) : "Empty string";
        assert "".equals(stringUtils.removeWhitespace("   ")) : "Only whitespace";
        assert stringUtils.removeWhitespace(null) == null : "null should return null";
    }
}