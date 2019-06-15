package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Try;

public interface MonadDefer<F extends Kind> extends MonadError<F, Throwable>, Bracket<F>, Defer<F> {

  default <A> Higher1<F, A> later(Producer<A> later) {
    return defer(() -> Try.of(later::get).fold(this::raiseError, this::pure));
  }
}
