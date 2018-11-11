/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.algebra.Monad;

public class OptionTTest {

  final Monad<IO.µ> monad = IO.monad();

  @Test
  public void map() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> map = some.map(String::toUpperCase);

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void filter() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> filter = some.filter(String::isEmpty);
    OptionT<IO.µ, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(IO.narrowK(orElse.get()).unsafeRunSync(), IO.narrowK(filter.orElse("not empty")).unsafeRunSync());
  }

  @Test
  public void none() {
    OptionT<IO.µ, String> none = OptionT.none(monad);

    assertAll(
        () -> assertTrue(IO.narrowK(none.isEmpty()).unsafeRunSync()),
        () -> assertEquals("empty", IO.narrowK(none.orElse("empty")).unsafeRunSync()));
  }

  @Test
  public void some() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertFalse(IO.narrowK(some.isEmpty()).unsafeRunSync()),
        () -> assertEquals("abc", IO.narrowK(some.orElse("empty")).unsafeRunSync()));
  }
}
