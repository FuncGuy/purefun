/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.data.Sequence.listOf;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@HigherKind
@FunctionalInterface
public interface Function1<A, R> {

  R apply(A value);

  default <B> Function1<A, B> andThen(Function1<R, B> after) {
    return value -> after.apply(apply(value));
  }

  default <B> Function1<B, R> compose(Function1<B, A> before) {
    return value -> apply(before.apply(value));
  }

  default Function1<A, Optional<R>> liftOptional() {
    return value -> Optional.ofNullable(apply(value));
  }

  default Function1<A, Option<R>> liftOption() {
    return value -> Option.of(() -> apply(value));
  }

  default Function1<A, Try<R>> liftTry() {
    return value -> Try.of(() -> apply(value));
  }

  default Function1<A, Id<R>> liftId() {
    return value -> Id.of(apply(value));
  }

  default Function1<A, Future<R>> liftFuture() {
    return value -> Future.run(() -> apply(value));
  }

  default Function1<A, Future<R>> liftFuture(Executor executor) {
    return value -> Future.run(executor, () -> apply(value));
  }

  default Function1<A, Either<Throwable, R>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default <L> Function1<A, Either<L, R>> liftRight() {
    return value -> Either.right(apply(value));
  }

  default <L> Function1<A, Either<R, L>> liftLeft() {
    return value -> Either.left(apply(value));
  }

  default Function1<A, Sequence<R>> sequence() {
    return value -> listOf(apply(value));
  }

  default Function1<A, Stream<R>> stream() {
    return value -> Stream.of(apply(value));
  }

  default CheckedFunction1<A, R> checked() {
    return this::apply;
  }

  default Function1<A, R> memoized() {
    return new MemoizedFunction<>(this);
  }

  default PartialFunction1<A, R> partial(Matcher1<A> isDefined) {
    return new PartialFunction1<A, R>() {
      @Override
      public boolean isDefinedAt(A value) {
        return isDefined.match(value);
      }

      @Override
      public R apply(A value) {
        return Function1.this.apply(value);
      }
    };
  }

  static <A> Function1<A, A> identity() {
    return value -> value;
  }

  static <A, T> Function1<A, T> cons(T cons) {
    return ignore -> cons;
  }

  static <A, T> Function1<A, T> fail() {
    return ignore -> {
      throw new UnsupportedOperationException();
    };
  }

  static <A, R> Function1<A, R> of(Function1<A, R> reference) {
    return reference;
  }
}
