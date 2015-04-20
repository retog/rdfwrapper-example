package org.example.wrapped.api;

import java.util.Iterator;

/**
 *
 * @author developer
 */
public interface DataBase {

    Iterator<Person> list();

    Iterator<Person> filterByLastName(String lastName);

    Iterator<Person> filterByFirstName(String firstName);

}
