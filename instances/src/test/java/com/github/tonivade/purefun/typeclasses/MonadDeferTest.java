/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.effect.ZIO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class MonadDeferTest {

  private MonadDefer<IO.µ> ioMonadDefer = IOInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>> zioMonadDefer =
      ZIOInstances.monadDefer();
  private MonadDefer<Future.µ> futureMonadDefer = FutureInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>> eitherTMonadDeferFromMonad =
      EitherTInstances.monadDeferFromMonad(ioMonadDefer);
  private MonadDefer<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>> eitherTMonadDeferFromMonadThrow =
      EitherTInstances.monadDeferFromMonadThrow(ioMonadDefer);
  private MonadDefer<Higher1<OptionT.µ, IO.µ>> optionTMonadDefer =
      OptionTInstances.monadDefer(ioMonadDefer);

  private AutoCloseable resource = Mockito.mock(AutoCloseable.class);

  @Test
  public void ioBracket() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource).kind1(), r -> IO.pure("done").kind1());

    String result = bracket.fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void ioBracketAcquireError() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.<AutoCloseable>raiseError(new IllegalStateException()).kind1(), r -> IO.pure("done").kind1());

    assertThrows(IllegalStateException.class, () -> bracket.fix1(IO::narrowK).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void ioBracketUseError() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource).kind1(), r -> IO.<String>raiseError(new UnsupportedOperationException()).kind1());

    assertThrows(UnsupportedOperationException.class, () -> bracket.fix1(IO::narrowK).unsafeRunSync());

    verify(resource).close();
  }

  @Test
  public void eitherTBracket() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO.µ, Throwable, AutoCloseable>right(IOInstances.monad(), resource).kind3(),
                                           r -> EitherT.<IO.µ, Throwable, String>right(IOInstances.monad(), "done").kind3());

    String result = bracket.fix1(EitherT::narrowK).get().fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void eitherTBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonadThrow.bracket(EitherT.<IO.µ, Throwable, AutoCloseable>left(IOInstances.monad(), new IllegalStateException()).kind3(),
                                                r -> EitherT.<IO.µ, Throwable, String>right(IOInstances.monad(), "done").kind3());

    assertThrows(IllegalStateException.class,
                 () -> bracket.fix1(EitherT::narrowK).value().fix1(IO::narrowK).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketAcquireError2() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO.µ, Throwable, AutoCloseable>left(IOInstances.monad(), new IllegalStateException()).kind3(),
                                           r -> EitherT.<IO.µ, Throwable, String>right(IOInstances.monad(), "done").kind3());

    Throwable error = bracket.fix1(EitherT::narrowK).getLeft().fix1(IO::narrowK).unsafeRunSync();

    assertEquals(IllegalStateException.class, error.getClass());
    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO.µ, Throwable, AutoCloseable>right(IOInstances.monad(), resource).kind3(),
                                           r -> EitherT.<IO.µ, Throwable, String>left(IOInstances.monad(),
                                                             new UnsupportedOperationException()).kind3());

    Throwable error = bracket.fix1(EitherT::narrowK).getLeft().fix1(IO::narrowK).unsafeRunSync();

    assertEquals(UnsupportedOperationException.class, error.getClass());
    verify(resource).close();
  }

  @Test
  public void optionTBracket() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource).kind2(),
                                  r -> OptionT.some(IOInstances.monad(), "done").kind2());

    String result = bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void optionTBracketAcquireError() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.<IO.µ, AutoCloseable>none(IOInstances.monad()).kind2(),
                                  r -> OptionT.some(IOInstances.monad(), "done").kind2());

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync());

    assertEquals("could not acquire resource", error.getMessage());
    verify(resource, never()).close();
  }

  @Test
  public void optionTBracketUseError() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource).kind2(),
                                  r -> OptionT.<IO.µ, String>none(IOInstances.monad()).kind2());

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync());

    assertEquals("get() in none", error.getMessage());
    verify(resource).close();
  }

  @Test
  public void futureBracket() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.success(resource).kind1(), r -> Future.success("done").kind1());

    Future<String> result = bracket.fix1(Future::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("done"), result.await());
    verify(resource).close();
  }

  @Test
  public void futureBracketAcquireError() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.<AutoCloseable>failure(new IllegalStateException()).kind1(),
                                 r -> Future.success("done").kind1());

    Future<String> result = bracket.fix1(Future::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("fail"), result.await());
    verify(resource, never()).close();
  }

  @Test
  public void futureBracketUseError() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.success(resource).kind1(),
                                 r -> Future.<String>failure(new UnsupportedOperationException()).kind1());

    Future<String> result = bracket.fix1(Future::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("fail"), result.await());
    verify(resource).close();
  }

  @Test
  public void zioBracket() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>pure(resource).kind3(),
                              r -> ZIO.<Nothing, Throwable, String>pure("done").kind3());

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertEquals(Either.right("done"), result);
    verify(resource).close();
  }

  @Test
  public void zioBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>raiseError(new IllegalStateException()).kind3(),
                              r -> ZIO.<Nothing, Throwable, String>pure("done").kind3());

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource, never()).close();
  }

  @Test
  public void zioBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>pure(resource).kind3(),
                              r -> ZIO.<Nothing, Throwable, String>raiseError(new UnsupportedOperationException()).kind3());

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource).close();
  }
}
