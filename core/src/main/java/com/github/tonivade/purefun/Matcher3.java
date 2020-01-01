/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Matcher3<A, B, C> extends Recoverable {

  default boolean match(A a, B b, C c) {
    try {
      return run(a, b, c);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  boolean run(A a, B b, C c) throws Throwable;

  default Matcher1<Tuple3<A, B, C>> tupled() {
    return tuple -> match(tuple.get1(), tuple.get2(), tuple.get3());
  }

  static <A, B, C> Matcher3<A, B, C> invalid() {
    return (a, b, c) -> { throw new IllegalStateException(); };
  }

  static <A, B, C> Matcher3<A, B, C> otherwise() {
    return (a, b, c) -> true;
  }
}
