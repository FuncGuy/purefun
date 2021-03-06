/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.free.EitherK;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.InjectK;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unchecked")
public interface EitherKInstances {

  static <F extends Kind, G extends Kind, T> Eq<Higher1<Higher1<Higher1<EitherK.µ, F>, G>, T>> eq(
      Eq<Higher1<F, T>> leftEq, Eq<Higher1<G, T>> rightEq) {
    return (a, b) -> Pattern2.<EitherK<F, G, T>, EitherK<F, G, T>, Boolean>build()
        .when((x, y) -> x.isLeft() && y.isLeft())
          .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
        .when((x, y) -> x.isRight() && y.isRight())
          .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
        .otherwise()
          .returns(false)
        .apply(EitherK.narrowK(a), EitherK.narrowK(b));
  }

  static <F extends Kind, G extends Kind> Functor<Higher1<Higher1<EitherK.µ, F>, G>> functor(
      Functor<F> functorF, Functor<G> functorG) {
    return EitherKFunctor.instance(requireNonNull(functorF), requireNonNull(functorG));
  }

  static <F extends Kind, G extends Kind> Contravariant<Higher1<Higher1<EitherK.µ, F>, G>> contravariant(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return EitherKContravariant.instance(requireNonNull(contravariantF), requireNonNull(contravariantG));
  }

  static <F extends Kind, G extends Kind> Comonad<Higher1<Higher1<EitherK.µ, F>, G>> comonad(
      Comonad<F> comonadF, Comonad<G> comonadG) {
    return EitherKComonad.instance(requireNonNull(comonadF), requireNonNull(comonadG));
  }

  static <F extends Kind, G extends Kind> InjectK<F, Higher1<Higher1<EitherK.µ, F>, G>> injectEitherKLeft() {
    return EitherKInjectKLeft.instance();
  }

  static <F extends Kind, R extends Kind, G extends Kind>
      InjectK<F, Higher1<Higher1<EitherK.µ, G>, R>> injectEitherKRight(InjectK<F, R> inject) {
    return EitherKInjectKRight.instance(requireNonNull(inject));
  }
}

interface EitherKFunctor<F extends Kind, G extends Kind> extends Functor<Higher1<Higher1<EitherK.µ, F>, G>> {

  static <F extends Kind, G extends Kind> EitherKFunctor<F, G> instance(Functor<F> functorF, Functor<G> functorG) {
    return new EitherKFunctor<F, G>() {
      @Override
      public Functor<F> f() { return functorF; }
      @Override
      public Functor<G> g() { return functorG; }
    };
  }

  Functor<F> f();
  Functor<G> g();

  @Override
  default <T, R> Higher3<EitherK.µ, F, G, R> map(
      Higher1<Higher1<Higher1<EitherK.µ, F>, G>, T> value, Function1<T, R> map) {
    return value.fix1(EitherK::narrowK).map(f(), g(), map).kind3();
  }
}

interface EitherKContravariant<F extends Kind, G extends Kind>
    extends Contravariant<Higher1<Higher1<EitherK.µ, F>, G>> {

  static <F extends Kind, G extends Kind> EitherKContravariant<F, G> instance(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return new EitherKContravariant<F, G>() {
      @Override
      public Contravariant<F> f() { return contravariantF; }
      @Override
      public Contravariant<G> g() { return contravariantG; }
    };
  }

  Contravariant<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> Higher3<EitherK.µ, F, G, B> contramap(
      Higher1<Higher1<Higher1<EitherK.µ, F>, G>, A> value, Function1<B, A> map) {
    return value.fix1(EitherK::narrowK).contramap(f(), g(), map).kind3();
  }
}

interface EitherKComonad<F extends Kind, G extends Kind>
    extends Comonad<Higher1<Higher1<EitherK.µ, F>, G>>, EitherKFunctor<F, G> {

  static <F extends Kind, G extends Kind> EitherKComonad<F, G> instance(Comonad<F> comonadF, Comonad<G> comonadG) {
    return new EitherKComonad<F, G>() {
      @Override
      public Comonad<F> f() { return comonadF; }
      @Override
      public Comonad<G> g() { return comonadG; }
    };
  }

  @Override
  Comonad<F> f();
  @Override
  Comonad<G> g();

  @Override
  default <A, B> Higher3<EitherK.µ, F, G, B> coflatMap(
      Higher1<Higher1<Higher1<EitherK.µ, F>, G>, A> value,
      Function1<Higher1<Higher1<Higher1<EitherK.µ, F>, G>, A>, B> map) {
    return value.fix1(EitherK::narrowK).coflatMap(f(), g(), eitherK -> map.apply(eitherK.kind1())).kind3();
  }

  @Override
  default <A> A extract(Higher1<Higher1<Higher1<EitherK.µ, F>, G>, A> value) {
    return value.fix1(EitherK::narrowK).extract(f(), g());
  }
}

interface EitherKInjectKRight<F extends Kind, G extends Kind, R extends Kind>
    extends InjectK<F, Higher1<Higher1<EitherK.µ, G>, R>> {

  static <F extends Kind, X extends Kind, R extends Kind> EitherKInjectKRight<F, X, R> instance(InjectK<F, R> injectK) {
    return () -> injectK;
  }

  InjectK<F, R> inject();

  @Override
  default  <T> Higher3<EitherK.µ, G, R, T> inject(Higher1<F, T> value) {
    return EitherK.<G, R, T>right(inject().inject(value)).kind3();
  }
}

@Instance
interface EitherKInjectKLeft<F extends Kind, G extends Kind> extends InjectK<F, Higher1<Higher1<EitherK.µ, F>, G>> {

  @Override
  default  <T> Higher3<EitherK.µ, F, G, T> inject(Higher1<F, T> value) {
    return EitherK.<F, G, T>left(value).kind3();
  }
}
