/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.ComonadLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Id;

public class ComonadTest {

  @Test
  public void idTest() {
    verifyLaws(Id.comonad(), Id.of("hola mundo"));
  }
}
