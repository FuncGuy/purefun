/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import static java.util.Objects.requireNonNull;

final class Cons<F extends Kind, T> implements Stream<F, T> {

  private final MonadDefer<F> monad;
  private final Higher1<F, T> head;
  private final Stream<F, T> tail;

  Cons(MonadDefer<F> monad, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.head = requireNonNull(head);
    this.tail = requireNonNull(tail);
  }

  @Override
  public Higher1<F, Option<T>> headOption() {
    return monad.map(head, Option::some);
  }

  @Override
  public Higher1<F, Option<Tuple2<Higher1<F, T>, Stream<F, T>>>> split() {
    return monad.pure(Option.some(Tuple.of(head, tail)));
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return suspend(() -> cons(head, tail.concat(other)));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return suspend(() -> cons(head, tail.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return suspend(() -> cons(other, tail.prepend(head)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return n > 0 ? suspend(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public Stream<F, T> drop(int n) {
    return n > 0 ? suspend(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return suspendF(() -> monad.map(head,
        t -> matcher.match(t) ? cons(head, tail.takeWhile(matcher)) : empty()));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher)));
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<T, R> partial) {
    return suspendF(() ->
            monad.map(head, t -> partial.isDefinedAt(t) ?
                cons(monad.map(head, partial::apply), tail.collect(partial)) : tail.collect(partial)));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator) {
    return monad.flatMap(head, h -> tail.foldRight(combinator.apply(h, begin), combinator));
  }

  @Override
  public Higher1<F, Boolean> exists(Matcher1<T> matcher) {
    return foldRight(monad.pure(false), (t, acc) -> matcher.match(t) ? monad.pure(true) : acc);
  }

  @Override
  public Higher1<F, Boolean> forall(Matcher1<T> matcher) {
    return foldRight(monad.pure(true), (t, acc) -> matcher.match(t) ? acc : monad.pure(false));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return suspend(() -> cons(monad.map(head, map), suspend(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return suspend(() -> cons(monad.flatMap(head, mapper), suspend(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, Stream<F, R>> map) {
    return suspendF(() ->
        monad.map(
            monad.map(head, map),
            s -> s.concat(tail.flatMap(map))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(value, tail.intersperse(value)))));
  }

  @Override
  public StreamModule getModule() { throw new UnsupportedOperationException(); }

  private <R> Stream<F, R> cons(Higher1<F, R> head, Stream<F, R> tail) {
    return new Cons<>(monad, head, tail);
  }

  private <R> Stream<F, R> suspend(Producer<Stream<F, R>> stream) {
    return suspendF(stream.map(monad::<Stream<F, R>>pure));
  }

  private <R> Stream<F, R> suspendF(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, monad.defer(stream));
  }

  private Stream<F, T> empty() {
    return new Nil<>(monad);
  }
}
