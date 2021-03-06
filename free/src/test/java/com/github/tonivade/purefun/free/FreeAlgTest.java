/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKLeft;
import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKRight;
import static com.github.tonivade.purefun.typeclasses.InjectK.injectReflexive;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreeAlgTest {

  private static Free<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, String> read() {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.ReadLine().kind1());
  }

  private static Free<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, Unit> write(String value) {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.WriteLine(value).kind1());
  }

  private static Free<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, Unit> send(String to, String content) {
    return Free.inject(injectEitherKRight(injectReflexive()), new EmailAlg.SendEmail(to, content).kind1());
  }

  @Test
  public void algebra() {
    Free<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, Unit> hello =
        read().flatMap(name -> write("hello " + name))
            .andThen(send("toni@home", "hello"));

    ConsoleExecutor executor = new ConsoleExecutor().read("toni");

    executor.run(hello.foldMap(IOInstances.monad(), interpreter()).fix1(IO::narrowK));

    assertEquals("hello toni\nemail to toni@home with content hello\n", executor.getOutput());
  }

  private static FunctionK<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, IO.µ> interpreter() {
    final Console<IO.µ> console = IOInstances.console();
    return new FunctionK<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, IO.µ>() {
      @Override
      public <T> Higher1<IO.µ, T> apply(Higher1<Higher1<Higher1<EitherK.µ, ConsoleAlg.µ>, EmailAlg.µ>, T> from) {
        return from.fix1(EitherK::narrowK).foldK(
          new FunctionK<ConsoleAlg.µ, IO.µ>() {
            @Override
            public <X> Higher1<IO.µ, X> apply(Higher1<ConsoleAlg.µ, X> from) {
              ConsoleAlg<X> consoleAlg = from.fix1(ConsoleAlg::narrowK);
              if (consoleAlg instanceof ConsoleAlg.ReadLine) {
                return (Higher1<IO.µ, X>) console.readln();
              }
              if (consoleAlg instanceof ConsoleAlg.WriteLine) {
                ConsoleAlg.WriteLine writeLine = (ConsoleAlg.WriteLine) consoleAlg;
                return (Higher1<IO.µ, X>) console.println(writeLine.getLine());
              }
              throw new IllegalStateException();
            }
          },
            new FunctionK<EmailAlg.µ, IO.µ>() {
              @Override
              public <X> Higher1<IO.µ, X> apply(Higher1<EmailAlg.µ, X> from) {
                EmailAlg<X> emailAlg = from.fix1(EmailAlg::narrowK);
                if (emailAlg instanceof EmailAlg.SendEmail) {
                  EmailAlg.SendEmail sendEmail = (EmailAlg.SendEmail) emailAlg;
                  return (Higher1<IO.µ, X>) console.println(
                      "email to " + sendEmail.getTo() + " with content " + sendEmail.getContent());
                }
                throw new IllegalStateException();
              }
            }
        );
      }
    };
  }
}

