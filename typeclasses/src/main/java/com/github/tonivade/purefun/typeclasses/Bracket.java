package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Bracket<F extends Kind> extends MonadError<F, Throwable> {

  <A, B> Higher1<F, B> bracket(Higher1<F, A> acquire, Function1<A, ? extends Higher1<F, B>> use, Consumer1<A> release);
}
