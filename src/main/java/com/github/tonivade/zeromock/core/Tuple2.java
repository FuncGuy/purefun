/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Objects;

public final class Tuple2<A, B> {

  private final A value1;
  private final B value2;

  private Tuple2(A value1, B value2) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public A get1() {
    return value1;
  }

  public B get2() {
    return value2;
  }
  
  public <C> Tuple2<C, B> map1(Handler1<A, C> mapper) {
    return map(mapper, Handler1.identity());
  }
  
  public <C> Tuple2<A, C> map2(Handler1<B, C> mapper) {
    return map(Handler1.identity(), mapper);
  }
  
  public <C, D> Tuple2<C, D> map(Handler1<A, C> mapper1, Handler1<B, D> mapper2) {
    return Tuple2.of(mapper1.handle(value1), mapper2.handle(value2));
  }

  public static <A, B> Tuple2<A, B> of(A value1, B value2) {
    return new Tuple2<>(value1, value2);
  }

  public static <A, B> Tuple2<A, B> from(Map.Entry<A, B> entry) {
    return Tuple2.of(entry.getKey(), entry.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2);
  }

  @Override
  public boolean equals(Object obj) {
    return equal(this)
        .append(comparing(Tuple2::get1))
        .append(comparing(Tuple2::get2))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple2(" + value1 + ", " + value2 + ")";
  }
}