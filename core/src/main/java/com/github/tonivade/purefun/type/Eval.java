/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

import java.util.Stack;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

/**
 * <p>This is a monad that allows to control the evaluation of a computation or a value.</p>
 * <p>There are 3 basic strategies:</p>
 * <ul>
 *   <li>Eval.now(): evaluated immediately</li>
 *   <li>Eval.later(): the computation is evaluated later, but only the first time, the result is memoized.</li>
 *   <li>Eval.always(): the computation is evaluated later, but is always executed.</li>
 * </ul>
 * @param <A> result of the computation
 */
@HigherKind
public interface Eval<A> {

  Eval<Boolean> TRUE = now(true);
  Eval<Boolean> FALSE = now(false);
  Eval<Unit> UNIT = now(unit());
  Eval<Integer> ZERO = now(0);
  Eval<Integer> ONE = now(1);

  A value();

  default <R> Eval<R> map(Function1<A, R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  <R> Eval<R> flatMap(Function1<A, Eval<R>> map);

  EvalModule getModule();

  static <T> Eval<T> now(T value) {
    return new Done<>(cons(value));
  }

  static <T> Eval<T> later(Producer<T> later) {
    return new Done<>(later.memoized());
  }

  static <T> Eval<T> always(Producer<T> always) {
    return new Done<>(always);
  }

  static <T> Eval<T> defer(Producer<Eval<T>> eval) {
    return new Defer<>(eval);
  }

  static <T> Eval<T> raiseError(Throwable error) {
    return new Done<>(() -> { throw error; });
  }

  final class Done<A> implements Eval<A> {

    private final Producer<A> producer;

    protected Done(Producer<A> producer) {
      this.producer = requireNonNull(producer);
    }

    @Override
    public A value() {
      return producer.get();
    }

    public <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
      return new FlatMapped<>(cons(this), map::apply);
    }

    @Override
    public EvalModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Done(?)";
    }
  }

  final class Defer<A> implements Eval<A> {

    private final Producer<Eval<A>> deferred;

    protected Defer(Producer<Eval<A>> deferred) {
      this.deferred = requireNonNull(deferred);
    }

    @Override
    public A value() {
      return EvalModule.collapse(this).value();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
      return new FlatMapped<>(deferred::get, map::apply);
    }

    @Override
    public EvalModule getModule() {
      throw new UnsupportedOperationException();
    }

    protected Eval<A> next() {
      return deferred.get();
    }

    @Override
    public String toString() {
      return "Defer(?)";
    }
  }

  final class FlatMapped<A, B> implements Eval<B> {

    private final Producer<Eval<A>> start;
    private final Function1<A, Eval<B>> run;

    protected FlatMapped(Producer<Eval<A>> start, Function1<A, Eval<B>> run) {
      this.start = requireNonNull(start);
      this.run = requireNonNull(run);
    }

    @Override
    public B value() {
      return EvalModule.evaluate(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Eval<R> flatMap(Function1<B, Eval<R>> map) {
      return new FlatMapped<>(() -> (Eval<B>) start(), b -> new FlatMapped<>(() -> run((A) b), map::apply));
    }

    @Override
    public EvalModule getModule() {
      throw new UnsupportedOperationException();
    }

    protected Eval<A> start() {
      return start.get();
    }

    protected Eval<B> run(A value) {
      return run.apply(value);
    }

    @Override
    public String toString() {
      return "FlatMapped(?, ?)";
    }
  }
}

interface EvalModule {

  @SuppressWarnings("unchecked")
  static <A, X> Eval<A> collapse(Eval<A> eval) {
    Eval<A> current = eval;
    while (true) {
      if (current instanceof Eval.Defer) {
        Eval.Defer<A> defer = (Eval.Defer<A>) current;
        current = defer.next();
      } else if (current instanceof Eval.FlatMapped) {
        Eval.FlatMapped<X, A> flatMapped = (Eval.FlatMapped<X, A>) current;
        return new Eval.FlatMapped<>(flatMapped::start, a -> collapse(flatMapped.run(a)));
      } else break;
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <A> A evaluate(Eval<A> eval) {
    Stack<Function1<Object, Eval>> stack = new Stack<>();
    Eval<A> current = eval;
    while (true) {
      if (current instanceof Eval.FlatMapped) {
        Eval.FlatMapped currentFlatMapped = (Eval.FlatMapped) current;
        Eval<A> next = currentFlatMapped.start();
        if (next instanceof Eval.FlatMapped) {
          Eval.FlatMapped nextFlatMapped = (Eval.FlatMapped) next;
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = (Eval<A>) currentFlatMapped.run(next.value());
        }
      } else if (!stack.isEmpty()) {
        current = (Eval<A>) stack.pop().apply(current.value());
      } else break;
    }
    return current.value();
  }
}
