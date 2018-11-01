/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TrampolineTest {

  @Test
  public void sum() {
    assertEquals(5050, sum(100));
    assertEquals(20100, sum(200));
    assertEquals(45150, sum(300));
  }

  @Test
  public void fib() {
    assertEquals(1, fib(1));
    assertEquals(1, fib(2));
    assertEquals(2, fib(3));
    assertEquals(3, fib(4));
    assertEquals(5, fib(5));
    assertEquals(8, fib(6));
    assertEquals(13, fib(7));
    assertEquals(21, fib(8));
    assertEquals(55, fib(10));
    assertEquals(317811, fib(28));
  }

  int fib(int n) {
    return fibLoop(n).run();
  }

  int sum(int n) {
    return sumLoop(n, 0).run();
  }

  Trampoline<Integer> sumLoop(Integer counter, Integer sum) {
    if (counter == 0) {
      return Trampoline.done(sum);
    }
    return Trampoline.more(() -> sumLoop(counter - 1, sum + counter));
  }

  Trampoline<Integer> fibLoop(Integer n) {
    if (n < 2) {
      return Trampoline.done(n);
    }
    return Trampoline.more(() -> fibLoop(n - 1)).flatMap(x -> fibLoop(n - 2).map(y -> x + y));
  }
}
