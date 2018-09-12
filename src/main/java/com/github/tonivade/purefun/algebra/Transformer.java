/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Witness;

public interface Transformer<F extends Witness, T extends Witness> {

  <X> Higher1<T, X> apply(Higher1<F, X> from);

  default <B extends Witness> Transformer<B, T> compose(Transformer<B, F> before) {
    return new Transformer<B, T>() {
      @Override
      public <X> Higher1<T, X> apply(Higher1<B, X> from) {
        return Transformer.this.apply(before.apply(from));
      }
    };
  }

  default <A extends Witness> Transformer<F, A> andThen(Transformer<T, A> after) {
    return new Transformer<F, A>() {
      @Override
      public <X> Higher1<A, X> apply(Higher1<F, X> from) {
        return after.apply(Transformer.this.apply(from));
      }
    };
  }
}
