/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.collectingAndThen;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;

public interface ImmutableSet<E> extends Sequence<E> {

  Set<E> toSet();

  @Override
  ImmutableSet<E> append(E element);
  @Override
  ImmutableSet<E> remove(E element);
  @Override
  ImmutableSet<E> appendAll(Sequence<E> other);
  @Override
  ImmutableSet<E> removeAll(Sequence<E> other);

  @Override
  ImmutableSet<E> reverse();

  ImmutableSet<E> union(ImmutableSet<E> other);
  ImmutableSet<E> intersection(ImmutableSet<E> other);
  ImmutableSet<E> difference(ImmutableSet<E> other);

  @Override
  default <R> ImmutableSet<R> map(Function1<E, R> mapper) {
    return ImmutableSet.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableSet<R> flatMap(Function1<E, Sequence<R>> mapper) {
    return ImmutableSet.from(stream().flatMap(mapper.andThen(Sequence::stream)::apply));
  }

  @Override
  default ImmutableSet<E> filter(Matcher1<E> matcher) {
    return ImmutableSet.from(stream().filter(matcher::match));
  }

  @Override
  default ImmutableSet<E> filterNot(Matcher1<E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableSet<T> from(Iterable<T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableSet<T> from(Stream<T> stream) {
    return new JavaBasedImmutableSet<>(stream.collect(Collectors.toSet()));
  }

  @SafeVarargs
  static <T> ImmutableSet<T> of(T... elements) {
    return new JavaBasedImmutableSet<>(new LinkedHashSet<>(Arrays.asList(elements)));
  }

  static <T> ImmutableSet<T> empty() {
    return new JavaBasedImmutableSet<>(emptySet());
  }

  static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return collectingAndThen(Collectors.toSet(), JavaBasedImmutableSet::new);
  }

  final class JavaBasedImmutableSet<E> implements ImmutableSet<E>, Serializable {

    private static final long serialVersionUID = -4111867323115030715L;

    private static final Equal<JavaBasedImmutableSet<?>> EQUAL = 
        Equal.<JavaBasedImmutableSet<?>>of().comparing(x -> x.backend);

    private final Set<E> backend;

    private JavaBasedImmutableSet(Set<E> backend) {
      this.backend = unmodifiableSet(backend);
    }

    @Override
    public int size() {
      return backend.size();
    }

    @Override
    public boolean contains(E element) {
      return backend.contains(element);
    }

    @Override
    public ImmutableSet<E> reverse() {
      return this;
    }

    @Override
    public ImmutableSet<E> append(E element) {
      Set<E> newSet = toSet();
      newSet.add(element);
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public ImmutableSet<E> remove(E element) {
      Set<E> newSet = toSet();
      newSet.remove(element);
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public ImmutableSet<E> appendAll(Sequence<E> other) {
      Set<E> newSet = toSet();
      newSet.addAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public ImmutableSet<E> removeAll(Sequence<E> other) {
      Set<E> newSet = toSet();
      newSet.removeAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public ImmutableSet<E> union(ImmutableSet<E> other) {
      return appendAll(other);
    }

    @Override
    public ImmutableSet<E> intersection(ImmutableSet<E> other) {
      Set<E> newSet = toSet();
      newSet.retainAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public ImmutableSet<E> difference(ImmutableSet<E> other) {
      Set<E> newSet = toSet();
      newSet.removeAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableSet<>(newSet);
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public Set<E> toSet() {
      return new LinkedHashSet<>(backend);
    }

    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }

    @Override
    public boolean equals(Object obj) {
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "ImmutableSet(" + backend + ")";
    }
  }
}
