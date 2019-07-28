/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public interface Kleisli<F extends Kind, Z, A> extends FlatMap3<Kleisli.µ, F, Z, A> {

  Monad<F> monad();
  Higher1<F, A> run(Z value);

  @Override
  default <R> Kleisli<F, Z, R> map(Function1<A, R> map) {
    return Kleisli.of(monad(), value -> monad().map(run(value), map));
  }

  @Override
  default <R> Kleisli<F, Z, R> flatMap(Function1<A, ? extends Higher3<Kleisli.µ, F, Z, R>> map) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), a -> map.andThen(Kleisli::narrowK).apply(a).run(value)));
  }

  default <B> Kleisli<F, Z, B> compose(Kleisli<F, A, B> other) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), other::run));
  }

  default <X> Kleisli<F, X, A> local(Function1<X, Z> map) {
    return Kleisli.of(monad(), map.andThen(this::run)::apply);
  }

  static <F extends Kind, A, B> Kleisli<F, A, B> lift(Monad<F> monad, Function1<A, B> map) {
    return Kleisli.of(monad, map.andThen(monad::pure)::apply);
  }

  static <F extends Kind, A, B> Kleisli<F, A, B> pure(Monad<F> monad, B value) {
    return Kleisli.of(monad, a -> monad.pure(value));
  }

  static <F extends Kind, A, B> Kleisli<F, A, B> of(Monad<F> monad, Function1<A, Higher1<F, B>> run) {
    requireNonNull(monad);
    requireNonNull(run);
    return new Kleisli<F, A, B>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Higher1<F, B> run(A value) { return run.apply(value); }
    };
  }
}