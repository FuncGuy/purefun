/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.monad.Writer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterInstances {

  static <L> Monad<Higher1<Writer.µ, L>> monad(Monoid<L> monoid) {
    return WriterMonad.instance(requireNonNull(monoid));
  }
}

interface WriterMonad<L> extends Monad<Higher1<Writer.µ, L>> {

  static <L> WriterMonad<L> instance(Monoid<L> monoid) {
    return () -> monoid;
  }

  Monoid<L> monoid();

  @Override
  default <T> Higher2<Writer.µ, L, T> pure(T value) {
    return Writer.pure(monoid(), value).kind2();
  }

  @Override
  default <T, R> Higher2<Writer.µ, L, R> flatMap(Higher1<Higher1<Writer.µ, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Writer.µ, L>, R>> map) {
    return Writer.narrowK(value).flatMap(map.andThen(Writer::narrowK)).kind2();
  }
}
