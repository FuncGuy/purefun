/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface Writer<L, A> extends FlatMap2<Writer.µ, L, A> {

  final class µ implements Kind {}

  Monoid<L> monoid();
  Tuple2<L, A> value();

  default A getValue() {
    return value().get2();
  }

  default L getLog() {
    return value().get1();
  }

  @Override
  default <B> Writer<L, B> map(Function1<A, B> mapper) {
    return bimap(monoid(), identity(), mapper);
  }

  default <R> Writer<R, A> mapLog(Monoid<R> monoidR, Function1<L, R> mapper) {
    return bimap(monoidR, mapper, identity());
  }

  default Writer<L, A> append(L log2) {
    return mapLog(monoid(), log1 -> monoid().combine(log1, log2));
  }

  default Writer<L, A> reset() {
    return mapLog(monoid(), cons(monoid().zero()));
  }

  default <V, R> Writer<V, R> bimap(Monoid<V> monoidV, Function1<L, V> mapper1, Function1<A, R> mapper2) {
    return writer(monoidV, value().map(mapper1, mapper2));
  }

  @Override
  default <B> Writer<L, B> flatMap(Function1<A, ? extends Higher2<Writer.µ, L, B>> mapper) {
    Writer<L, B> apply = mapper.andThen(Writer::narrowK).apply(value().get2());
    Tuple2<L, A> combine = value().map1(log -> monoid().combine(log, apply.getLog()));
    return writer(monoid(), Tuple.of(combine.get1(), apply.getValue()));
  }

  static <L, A> Writer<L, A> pure(Monoid<L> monoid, A value) {
    return writer(monoid, Tuple.of(monoid.zero(), value));
  }

  static <L, A> Writer<L, A> writer(Monoid<L> monoid, Tuple2<L, A> value) {
    return new Writer<L, A>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Tuple2<L, A> value() { return value; }
    };
  }

  static <L, T> Writer<L, T> narrowK(Higher2<Writer.µ, L, T> hkt) {
    return (Writer<L, T>) hkt;
  }

  static <L, T> Writer<L, T> narrowK(Higher1<Higher1<Writer.µ, L>, T> hkt) {
    return (Writer<L, T>) hkt;
  }
}