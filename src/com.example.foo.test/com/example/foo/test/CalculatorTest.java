package com.example.foo.test;

import com.example.foo.Calculator;
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

public class CalculatorTest implements TestSuite {
    
    private final Calculator calculator = new Calculator();
    
    @Test
    public void testAddition() {
        assert calculator.add(2, 3) == 5 : "2 + 3 should equal 5";
        assert calculator.add(-1, 1) == 0 : "-1 + 1 should equal 0";
        assert calculator.add(0, 0) == 0 : "0 + 0 should equal 0";
        assert calculator.add(Integer.MAX_VALUE, 0) == Integer.MAX_VALUE : "MAX + 0 should equal MAX";
    }
    
    @Test
    public void testSubtraction() {
        assert calculator.subtract(5, 3) == 2 : "5 - 3 should equal 2";
        assert calculator.subtract(0, 5) == -5 : "0 - 5 should equal -5";
        assert calculator.subtract(-3, -3) == 0 : "-3 - (-3) should equal 0";
    }
    
    @Test
    public void testMultiplication() {
        assert calculator.multiply(3, 4) == 12 : "3 * 4 should equal 12";
        assert calculator.multiply(0, 100) == 0 : "0 * 100 should equal 0";
        assert calculator.multiply(-2, 3) == -6 : "-2 * 3 should equal -6";
        assert calculator.multiply(-2, -3) == 6 : "-2 * -3 should equal 6";
    }
    
    @Test
    public void testDivision() {
        assert calculator.divide(10, 2) == 5 : "10 / 2 should equal 5";
        assert calculator.divide(7, 3) == 2 : "7 / 3 should equal 2 (integer division)";
        assert calculator.divide(-10, 2) == -5 : "-10 / 2 should equal -5";
    }
    
    @Test
    public void testDivisionByZero() {
        boolean exceptionThrown = false;
        try {
            calculator.divide(10, 0);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
            assert e.getMessage().contains("Cannot divide by zero") : "Exception message should mention division by zero";
        }
        assert exceptionThrown : "Division by zero should throw IllegalArgumentException";
    }
    
    @Test
    public void testFactorial() {
        assert calculator.factorial(0) == 1 : "0! should equal 1";
        assert calculator.factorial(1) == 1 : "1! should equal 1";
        assert calculator.factorial(5) == 120 : "5! should equal 120";
        assert calculator.factorial(6) == 720 : "6! should equal 720";
    }
    
    @Test
    public void testFactorialNegative() {
        boolean exceptionThrown = false;
        try {
            calculator.factorial(-1);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
            assert e.getMessage().contains("negative") : "Exception message should mention negative numbers";
        }
        assert exceptionThrown : "Factorial of negative number should throw IllegalArgumentException";
    }
    
    @Test
    public void testIsPrime() {
        assert !calculator.isPrime(0) : "0 is not prime";
        assert !calculator.isPrime(1) : "1 is not prime";
        assert calculator.isPrime(2) : "2 is prime";
        assert calculator.isPrime(3) : "3 is prime";
        assert !calculator.isPrime(4) : "4 is not prime";
        assert calculator.isPrime(5) : "5 is prime";
        assert calculator.isPrime(17) : "17 is prime";
        assert !calculator.isPrime(20) : "20 is not prime";
        assert calculator.isPrime(23) : "23 is prime";
        assert !calculator.isPrime(100) : "100 is not prime";
    }
}