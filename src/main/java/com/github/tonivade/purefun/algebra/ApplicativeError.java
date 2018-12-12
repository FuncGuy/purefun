/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.monad.EitherT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public interface ApplicativeError<F extends Kind, E> extends Applicative<F> {
  
  <A> Higher1<F, A> raiseError(E error);

  <A> Higher1<F, A> handleErrorWith(Higher1<F, A> value, Function1<E, Higher1<F, A>> handler);

  default <A> Higher1<F, A> handleError(Higher1<F, A> value, Function1<E, A> handler) {
    return handleErrorWith(value, handler.andThen(this::pure));
  }

  default <A> Higher1<F, A> recoverWith(Higher1<F, A> value, PartialFunction1<E, Higher1<F, A>> handler) {
    return handleErrorWith(value, error -> handler.applyOrElse(error, this::raiseError));
  }

  default <A> Higher1<F, A> recover(Higher1<F, A> value, PartialFunction1<E, A> handler) {
    return recoverWith(value, handler.andThen(this::pure));
  }
  
  default <A> Higher1<F, Either<E, A>> attemp(Higher1<F, A> value) {
    return handleErrorWith(map(value, Either::right), e -> pure(Either.left(e)));
  }
  
  default <A> EitherT<F, E, A> attempT(Monad<F> monad, Higher1<F, A> value) {
    return EitherT.of(monad, attemp(value));
  }

  default <A> Higher1<F, A> fromTry(Try<A> value, Function1<Throwable, E> recover) {
    return value.fold(recover.andThen(this::raiseError), this::pure);
  }

  default <A> Higher1<F, A> fromEither(Either<E, A> value) {
    return value.fold(this::raiseError, this::pure);
  }
}
