/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Higher3<F extends Kind, A, B, C> extends Higher2<Higher1<F, A>, B, C> {

  @Override
  default <R> R fix1(Function1<? super Higher1<Higher1<Higher1<F, A>, B>, C>, ? extends R> function) {
    return Higher2.super.fix1(function);
  }

  @Override
  default <R> R fix2(Function1<? super Higher2<Higher1<F, A>, B, C>, ? extends R> function) {
    return Higher2.super.fix2(function);
  }

  default <R> R fix3(Function1<? super Higher3<F, A, B, C>, ? extends R> function) {
    return function.apply(this);
  }
}