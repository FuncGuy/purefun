/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.type.Const;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class TraverseTest {

  @Test
  public void seuence() {
    Sequence<Option<String>> seq = listOf(Option.some("a"), Option.some("b"), Option.some("c"));

    Traverse<Sequence.µ> instance = SequenceInstances.traverse();

    Higher1<Option.µ, Higher1<Sequence.µ, String>> result =
        instance.traverse(OptionInstances.applicative(), seq.kind1(), x -> x.map(String::toUpperCase).kind1());

    assertEquals(Option.some(listOf("A", "B", "C")), result);
  }

  @Test
  public void either() {
    Traverse<Higher1<Either.µ, Throwable>> instance = EitherInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Either.right("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Either.<Throwable, Option<String>>right(Option.some("hello!")).kind1(),
                t -> t.map(String::toUpperCase).kind1())),
        () -> assertEquals(Option.some(Either.left(error)),
            instance.traverse(OptionInstances.applicative(), Either.<Throwable, Option<String>>left(error).kind1(),
                t -> t.map(String::toUpperCase).kind1())));
  }

  @Test
  public void composed() {
    Traverse<Nested<Option.µ, Id.µ>> composed = Traverse.compose(OptionInstances.traverse(), IdInstances.traverse());

    assertEquals(Try.success(Option.some(Id.of("HOLA!"))),
        composed.traverse(TryInstances.applicative(), nest(Option.some(Id.of(Try.success("hola!")).kind1()).kind1()),
            t -> t.map(String::toUpperCase).kind1()));
  }

  @Test
  public void id() {
    Traverse<Id.µ> instance = IdInstances.traverse();

    assertAll(
        () -> assertEquals(Option.some(Id.of("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Id.of(Option.some("hello!")).kind1(),
                t -> t.map(String::toUpperCase).kind1())));
  }

  @Test
  public void testConst() {
    Traverse<Higher1<Const.µ, String>> instance = ConstInstances.traverse();

    assertAll(
        () -> assertEquals(Option.some(Const.of("hello!")),
            instance.traverse(OptionInstances.applicative(), Const.<String, String>of("hello!").kind1(),
                t -> Option.some(t).kind1())));
  }

  @Test
  public void option() {
    Traverse<Option.µ> instance = OptionInstances.traverse();

    assertAll(
        () -> assertEquals(Try.success(Option.some("HELLO!")),
            instance.traverse(TryInstances.applicative(), Option.some(Try.success("hello!")).kind1(),
                t -> t.map(String::toUpperCase).kind1())),
        () -> assertEquals(Try.success(Option.none()),
            instance.traverse(TryInstances.applicative(), Option.<Try<String>>none().kind1(),
                t -> t.map(String::toUpperCase).kind1())));
  }

  @Test
  public void testTry() {
    Traverse<Try.µ> instance = TryInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Try.success("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Try.success(Option.some("hello!")).kind1(),
                t -> t.map(String::toUpperCase).kind1())),
        () -> assertEquals(Option.some(Try.failure(error)),
            instance.traverse(OptionInstances.applicative(), Try.<Option<String>>failure(error).kind1(),
                t -> t.map(String::toUpperCase).kind1())));
  }
}
