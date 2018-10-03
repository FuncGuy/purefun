/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.Objects;

import com.github.tonivade.purefun.Pattern1.CaseBuilder;

public class Pattern2<A, B, R> implements Function2<A, B, R> {

  private final Pattern1<Tuple2<A, B>, R> pattern;

  private Pattern2() {
    this(Pattern1.build());
  }

  private Pattern2(Pattern1<Tuple2<A, B>, R> pattern) {
    this.pattern = Objects.requireNonNull(pattern);
  }

  @Override
  public R apply(A a, B b) {
    return pattern.apply(Tuple.of(a, b));
  }

  public static <A, B, R> Pattern2<A, B, R> build() {
    return new Pattern2<>();
  }

  public CaseBuilder2<Pattern2<A, B, R>, A, B, R> when(Matcher2<A, B> matcher) {
    return new CaseBuilder2<>(this::add).when(matcher);
  }

  public CaseBuilder2<Pattern2<A, B, R>, A, B, R> otherwise() {
    return new CaseBuilder2<>(this::add).when(Matcher2.otherwise());
  }

  private Pattern2<A, B, R> add(Matcher1<Tuple2<A, B>> matcher, Function1<Tuple2<A, B>, R> handler) {
    return new Pattern2<>(pattern.add(matcher, handler));
  }
  
  public static class CaseBuilder2<B, T, V, R> extends CaseBuilder<B, Tuple2<T, V>, R> {

    CaseBuilder2(Function2<Matcher1<Tuple2<T, V>>, Function1<Tuple2<T, V>, R>, B> finisher) {
      super(finisher);
    }
    
    CaseBuilder2(Function2<Matcher1<Tuple2<T, V>>, Function1<Tuple2<T, V>, R>, B> finisher, 
                 Matcher1<Tuple2<T, V>> matcher) {
      super(finisher, matcher);
    }
    
    public CaseBuilder2<B, T, V, R> when(Matcher2<T, V> matcher) {
      return new CaseBuilder2<>(finisher, matcher.tupled());
    }
    
    public B then(Function2<T, V, R> handler) {
      return super.then(handler.tupled());
    }

    public B returns(R value) {
      return then((a, b) -> value);
    }
  }
}
