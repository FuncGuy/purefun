/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.algebra.Functor;
import com.github.tonivade.purefun.algebra.Monad;

@FunctionalInterface
public interface Kleisli<F extends Witness, Z, A> {

  Higher1<F, A> run(Z value);

  /*
   public <R1> Kleisli<W,T,R1> flatMap(Function<? super R, ? extends Higher<W,? extends R1>> mapper){
      Function<R,Higher<W,R1>> fn = (Function<R,Higher<W,R1>>)mapper;
      Kleisli<W, T, R1> x = kleisliK(monad, andThen(am -> monad.flatMap(fn, am)));
      return x;
   }
   */
  default <B> Kleisli<F, Z, B> flatMap(Monad<F> monad, Function1<A, Kleisli<F, Z, B>> map) {
    return value -> monad.flatMap(run(value), a -> map.apply(a).run(value));
  }

  default <B> Kleisli<F, Z, B> map(Functor<F> functor, Function1<A, B> map) {
    return value -> functor.map(run(value), map);
  }

  default <X> Kleisli<F, X, A> local(Function1<X, Z> map) {
    return map.andThen(this::run)::apply;
  }

  static <F extends Witness, Z, A> Kleisli<F, Z, A> of(Function1<Z, Higher1<F, A>> function) {
    return function::apply;
  }
}