/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Alternative;

public class AlternativeLaws {

  private static final Function1<Integer, Integer> twoTimes = a -> a * 2;
  private static final Function1<Integer, Integer> plusFive = a -> a + 5;

  public static <F extends Kind> void verifyLaws(Alternative<F> instance) {
    assertAll(
        () -> rightAbsorption(instance, String::valueOf),
        () -> leftDistributivity(instance, 1, 2, String::valueOf),
        () -> rightDistributivity(instance, 3, twoTimes, plusFive));
  }

  private static <F extends Kind, A, B> void rightAbsorption(Alternative<F> instance, Function1<A, B> f) {
    assertEquals(
        instance.<String>zero(),
        instance.ap(instance.zero(), instance.pure(f)),
        "right absorption");
  }

  private static <F extends Kind, A, B> void leftDistributivity(Alternative<F> instance,
                                                                A a1, A a2,
                                                                Function1<A, B> f) {
    Higher1<F, A> fa1 = instance.pure(a1);
    Higher1<F, A> fa2 = instance.pure(a2);
    assertEquals(
        instance.map(instance.combineK(fa1, fa2), f),
        instance.combineK(instance.map(fa1, f), instance.map(fa2, f)),
        "left distributivity");
  }

  private static <F extends Kind, A, B> void rightDistributivity(Alternative<F> instance,
                                                                 A a,
                                                                 Function1<A, B> f,
                                                                 Function1<A, B> g) {
    Higher1<F, A> fa = instance.pure(a);
    Higher1<F, Function1<A, B>> ff = instance.pure(f);
    Higher1<F, Function1<A, B>> fg = instance.pure(g);
    assertEquals(
        instance.ap(fa, instance.combineK(ff, fg)),
        instance.combineK(instance.ap(fa, ff), instance.ap(fa, fg)),
        "right distributivity");
  }
}
