/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.ProducerInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EitherKTest {

  @Test
  public void left() {
    EitherK<Option.µ, Try.µ, String> left = EitherK.left(Option.some("hello").kind1());

    assertAll(
        () -> assertTrue(left.isLeft()),
        () -> assertFalse(left.isRight()),
        () -> assertEquals(Option.some("hello"), left.getLeft()),
        () -> assertThrows(NoSuchElementException.class, left::getRight)
    );
  }

  @Test
  public void right() {
    EitherK<Option.µ, Try.µ, String> right = EitherK.right(Try.success("hello").kind1());

    assertAll(
        () -> assertFalse(right.isLeft()),
        () -> assertTrue(right.isRight()),
        () -> assertEquals(Try.success("hello"), right.getRight()),
        () -> assertThrows(NoSuchElementException.class, right::getLeft)
    );
  }

  @Test
  public void extract() {
    EitherK<Id.µ, Producer.µ, String> left = EitherK.left(Id.of("hola").kind1());
    EitherK<Id.µ, Producer.µ, String> right = EitherK.right(Producer.cons("hola").kind1());

    assertAll(
        () -> assertEquals("hola", left.extract(IdInstances.comonad(), ProducerInstances.comonad())),
        () -> assertEquals("hola", right.extract(IdInstances.comonad(), ProducerInstances.comonad()))
    );
  }

  @Test
  public void coflatMap() {
    EitherK<Id.µ, Producer.µ, String> left = EitherK.left(Id.of("hola").kind1());

    assertAll(
        () -> assertEquals(
                EitherK.left(Id.of("left").kind1()),
                left.coflatMap(IdInstances.comonad(), ProducerInstances.comonad(), eitherK -> "left")),
        () -> assertEquals(
                EitherK.right(Id.of("right").kind1()),
                left.swap().coflatMap(ProducerInstances.comonad(), IdInstances.comonad(), eitherK -> "right"))
    );
  }

  @Test
  public void mapLeft() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.left(Option.some("hello").kind1());

    EitherK<Option.µ, Try.µ, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Option.some(5), result.getLeft());
  }

  @Test
  public void mapRight() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.right(Try.success("hello").kind1());

    EitherK<Option.µ, Try.µ, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Try.success(5), result.getRight());
  }

  @Test
  public void mapK() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.right(Try.success("hello").kind1());

    EitherK<Option.µ, Option.µ, String> result = eitherK.mapK(new FunctionK<Try.µ, Option.µ>() {
      @Override
      public <T> Higher1<Option.µ, T> apply(Higher1<Try.µ, T> from) {
        return from.fix1(Try::narrowK).toOption().kind1();
      }
    });

    assertEquals(Option.some("hello"), result.getRight());
  }

  @Test
  public void mapLeftK() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.left(Option.some("hello").kind1());

    EitherK<Try.µ, Try.µ, String> result = eitherK.mapLeftK(new FunctionK<Option.µ, Try.µ>() {
      @Override
      public <T> Higher1<Try.µ, T> apply(Higher1<Option.µ, T> from) {
        return from.fix1(Option::narrowK).fold(Try::<T>failure, Try::success).kind1();
      }
    });

    assertEquals(Try.success("hello"), result.getLeft());
  }

  @Test
  public void swap() {
    EitherK<Option.µ, Try.µ, String> original = EitherK.left(Option.some("hello").kind1());
    EitherK<Try.µ, Option.µ, String> expected = EitherK.right(Option.some("hello").kind1());

    assertAll(
        () -> assertEquals(expected, original.swap()),
        () -> assertEquals(original, original.swap().swap())
    );
  }
}