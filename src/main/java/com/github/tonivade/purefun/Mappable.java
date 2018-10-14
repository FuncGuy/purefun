/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Mappable<W extends Kind, T> extends Higher1<W, T> {

  <R> Mappable<W, R> map(Function1<T, R> map);

}
