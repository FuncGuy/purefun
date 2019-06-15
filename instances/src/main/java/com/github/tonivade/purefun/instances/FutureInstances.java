/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;

public interface FutureInstances {

  static Functor<Future.µ> functor() {
    return new FutureFunctor() {};
  }

  static Applicative<Future.µ> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Future.µ> applicative(Executor executor) {
    return new FutureApplicative() {

      @Override
      public Executor executor() {
        return executor;
      }
    };
  }
  
  static Monad<Future.µ> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Future.µ> monad(Executor executor) {
    return new FutureMonad() {

      @Override
      public Executor executor() {
        return executor;
      }
    };
  }

  static MonadError<Future.µ, Throwable> monadError() {
    return monadError(Future.DEFAULT_EXECUTOR);
  }

  static MonadError<Future.µ, Throwable> monadError(Executor executor) {
    return new FutureMonadError() {
      
      @Override
      public Executor executor() {
        return executor;
      }
    };
  }

  static MonadDefer<Future.µ> monadDefer() {
    return monadDefer(Future.DEFAULT_EXECUTOR);
  }
  
  static MonadDefer<Future.µ> monadDefer(Executor executor) {
    return new FutureMonadDefer() {

      @Override
      public Executor executor() {
        return executor;
      }
    };
  }
}

interface FutureFunctor extends Functor<Future.µ> {

  @Override
  default <T, R> Future<R> map(Higher1<Future.µ, T> value, Function1<T, R> mapper) {
    return Future.narrowK(value).map(mapper);
  }
}

interface FuturePure extends Applicative<Future.µ> {
  
  Executor executor();

  @Override
  default <T> Future<T> pure(T value) {
    return Future.success(executor(), value);
  }
}

interface FutureApplicative extends FuturePure {

  @Override
  default <T, R> Future<R> ap(Higher1<Future.µ, T> value, Higher1<Future.µ, Function1<T, R>> apply) {
    return Future.narrowK(value).flatMap(t -> Future.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface FutureMonad extends FuturePure, Monad<Future.µ> {

  @Override
  default <T, R> Future<R> flatMap(Higher1<Future.µ, T> value,
      Function1<T, ? extends Higher1<Future.µ, R>> map) {
    return Future.narrowK(value).flatMap(map);
  }
}

interface FutureMonadError extends FutureMonad, MonadError<Future.µ, Throwable> {
  
  Executor executor();

  @Override
  default <A> Future<A> raiseError(Throwable error) {
    return Future.failure(executor(), error);
  }

  @Override
  default <A> Future<A> handleErrorWith(Higher1<Future.µ, A> value,
      Function1<Throwable, ? extends Higher1<Future.µ, A>> handler) {
    return Future.narrowK(value).fold(handler.andThen(Future::narrowK), success -> Future.success(executor(), success)).flatten();
  }
}

interface FutureDefer extends Defer<Future.µ> {
  
  Executor executor();

  @Override
  default <A> Future<A> defer(Producer<Higher1<Future.µ, A>> defer) {
    return Future.defer(executor(), defer.andThen(Future::narrowK)::get);
  }
}

interface FutureBracket extends Bracket<Future.µ> {

  @Override
  default <A, B> Future<B> bracket(Higher1<Future.µ, A> acquire, Function1<A, ? extends Higher1<Future.µ, B>> use, Consumer1<A> release) {
    return Future.narrowK(acquire)
      .flatMap(resource -> use.andThen(Future::narrowK).apply(resource)
          .onComplete(result -> release.accept(resource)));
  }
}

interface FutureMonadDefer extends MonadDefer<Future.µ>, FutureMonadError, FutureDefer, FutureBracket { }
