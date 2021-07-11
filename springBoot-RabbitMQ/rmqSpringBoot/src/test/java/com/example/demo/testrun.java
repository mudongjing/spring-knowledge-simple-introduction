package com.example.demo;

import org.junit.Test;

import java.util.stream.IntStream;

public class testrun {
    @Test
    public void test(){
        IntStream.rangeClosed(0, 10).boxed().forEach(System.out::println);
    }
}
