/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Equal;

public class LensTest {

  private final Lens<Employee, String> nameLens = Lens.of(Employee::getName, Employee::withName);
  private final Lens<Employee, Address> addressLens = Lens.of(Employee::getAddress, Employee::withAddress);
  private final Lens<Address, String> cityLens = Lens.of(Address::getCity, Address::withCity);
  private final Lens<Employee, String> cityAddressLens = addressLens.compose(cityLens);

  @Test
  public void lensTest() {
    Employee employee = new Employee("pepe", new Address("Madrid"));

    assertAll(() -> assertEquals("pepe", nameLens.get(employee)),
              () -> assertEquals(employee.withName("paco"), nameLens.set(employee, "paco")),
              () -> assertEquals("Madrid", cityAddressLens.get(employee)),
              () -> assertEquals(employee.withAddress(new Address("Alicante")),
                                 cityAddressLens.set(employee, "Alicante")));
  }

  @Test
  public void lensLaws() {
    Employee employee = new Employee("pepe", new Address("Madrid"));

    assertAll(() -> assertEquals(employee, nameLens.set(employee, nameLens.get(employee))),
              () -> assertEquals("paco", nameLens.get(nameLens.set(employee, "paco"))));
  }
}

class Employee {
  private final String name;
  private final Address address;

  public Employee(String name, Address address) {
    this.name = name;
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public Employee withName(String newName) {
    return new Employee(newName, address);
  }

  public Address getAddress() {
    return address;
  }

  public Employee withAddress(Address newAddress) {
    return new Employee(name, newAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, address);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Employee::getName)
        .comparing(Employee::getAddress)
        .applyTo(obj);
  }
}

class Address {
  private final String city;

  public Address(String city) {
    this.city = city;
  }

  public String getCity() {
    return city;
  }

  public Address withCity(String newCity) {
    return new Address(newCity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(city);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Address::getCity)
        .applyTo(obj);
  }
}