/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Producer.failure;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class FutureTest {

  @Mock
  private Consumer1<Try<String>> tryConsumer;
  @Mock
  private Consumer1<String> consumerSuccess;
  @Mock
  private Consumer1<Throwable> consumerFailure;

  @Test
  public void onCompleteSuccess() {
    Future<String> future = Future.success("Hello World!");

    Promise<String> promise = future.apply().onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onCompleteFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    Promise<String> promise = future.apply().onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.failure(error), promise.get()));
  }

  @Test
  public void onSuccess() {
    Future<String> future = Future.success("Hello World!");

    Promise<String> promise = future.apply().onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onSuccessTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), cons("Hello World!"));

    Promise<String> promise = future.apply().onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    Promise<String> promise = future.apply().onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.failure(error), promise.get()));
  }

  @Test
  public void onFailureTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), failure(UnsupportedOperationException::new));

    Promise<String> promise = future.apply().onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(UnsupportedOperationException.class, promise.get().getCause().getClass()));
  }

  @Test
  public void map() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.map(String::toUpperCase);

    assertEquals(Try.success("HELLO WORLD!"), result.apply().get());
  }

  @Test
  public void flatMap() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.flatMap(string -> Future.async(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.apply().get());
  }

  @Test
  public void filter() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.filter(string -> string.contains("Hello"));

    assertEquals(Try.success("Hello world!"), result.apply().get());
  }

  @Test
  public void orElse() {
    Future<String> future = Future.failure(new IllegalArgumentException());

    Future<String> result = future.orElse(Future.success("Hello world!"));

    assertEquals(Try.success("Hello world!"), result.apply().get());
  }

  @Test
  public void await() {
    Promise<String> future = Future.success("Hello world!").apply();

    assertEquals(Try.success("Hello world!"), future.get(Duration.ofSeconds(1)));
  }

  @Test
  public void awaitTimeout() {
    Future<String> future = Future.delay(Duration.ofSeconds(10), cons("Hello world!"));

    Try<String> result = future.apply().get(Duration.ofMillis(100));

    assertAll(
        () -> assertTrue(result.isFailure()),
        () -> assertTrue(result.getCause() instanceof TimeoutException));
  }

  @Test
  public void cancelled() throws InterruptedException {
    Future<String> future = Future.delay(Duration.ofSeconds(1), cons("Hello world!"));

    Promise<String> promise = future.apply();
    Thread.sleep(50);
    future.cancel(false);

    assertTrue(promise.isCompleted());
    assertTrue(promise.get().getCause() instanceof CancellationException);
  }

  @Test
  public void interrupt(@Mock Producer<String> producer) throws InterruptedException {
    Future<String> future = Future.delay(Duration.ofSeconds(1), producer);

    Promise<String> promise = future.apply();
    Thread.sleep(50);
    future.cancel(true);

    assertTrue(promise.isCompleted());
    assertTrue(promise.get().getCause() instanceof CancellationException);
    Thread.sleep(1500);
    verifyZeroInteractions(producer);
  }

  @Test
  public void notCancelled() {
    Future<String> future = Future.success("Hello world!");

    Promise<String> promise = future.apply();
    future.cancel(false);

    assertTrue(promise.isCompleted());
    assertEquals(Try.success("Hello world!"), promise.get());
  }

  @Test
  public void noDeadlock() {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    List<String> result = Collections.synchronizedList(new ArrayList<>());

    currentThread(result).andThen(
        currentThread(result).andThen(
            currentThread(result).andThen(
                currentThread(result)))).apply(executor).get(Duration.ofSeconds(5));

    assertEquals(4, result.size());
  }

  @Test
  public void alternative() {
    Future<String> choiceTrue = Future.alternative(Future.success(true), Future.success("is true"), Future.success("not true"));
    Future<String> choiceFalse = Future.alternative(Future.success(false), Future.success("is true"), Future.success("not true"));

    assertEquals(Try.success("is true"), choiceTrue.await());
    assertEquals(Try.success("not true"), choiceFalse.await());
  }

  @Test
  public void choiceArray() {
    ImmutableArray<Future<String>> options = Sequence.arrayOf(Future.success("zero"), Future.success("one"));

    Future<String> choiceTrue = Future.choiceArray(Future.success(1), options);
    Future<String> choiceFalse = Future.choiceArray(Future.success(0), options);
    Future<String> noChoice = Future.choiceArray(Future.success(3), options);

    assertEquals(Try.success("one"), choiceTrue.await());
    assertEquals(Try.success("zero"), choiceFalse.await());
    assertTrue(noChoice.await().getCause() instanceof NoSuchElementException);
  }

  @Test
  public void choiceMap() {
    ImmutableMap<Integer, Future<String>> options =
      ImmutableMap.<Integer, Future<String>>builder()
        .put(0, Future.success("zero")).put(1, Future.success("one")).build();

    Future<String> choiceTrue = Future.choiceMap(Future.success(1), options);
    Future<String> choiceFalse = Future.choiceMap(Future.success(0), options);
    Future<String> noChoice = Future.choiceMap(Future.success(3), options);

    assertEquals(Try.success("one"), choiceTrue.await());
    assertEquals(Try.success("zero"), choiceFalse.await());
    assertTrue(noChoice.await().getCause() instanceof NoSuchElementException);
  }

  @Test
  public void choice() {
    Future<String> choiceTrue = Future.choice(Future.success(1), zeroOrOne());
    Future<String> choiceFalse = Future.choice(Future.success(0), zeroOrOne());
    Future<String> noChoice = Future.choice(Future.success(3), zeroOrOne());

    assertEquals(Try.success("1"), choiceTrue.await());
    assertEquals(Try.success("0"), choiceFalse.await());
    assertTrue(noChoice.await().getCause() instanceof NoSuchElementException);
  }

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  private Future<Unit> currentThread(List<String> result) {
    return Future.exec(() -> result.add(Thread.currentThread().getName()));
  }

  private PartialFunction1<Integer, Future<String>> zeroOrOne() {
    return PartialFunction1.of(value -> value > -1 && value < 2, integer -> Future.success(integer.toString()));
  }
}
