package org.example.clerezza.based.wrapper.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import junit.framework.Assert;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.example.clerezza.based.wrapper.PersonDataBaseGraph;
import org.example.wrapped.api.DataBase;
import org.example.wrapped.api.Person;
import org.junit.Test;

/**
 *
 * @author developer
 */
public class PersonDataBaseGraphTest {
    
    private static class PersonImpl implements Person  {
        String firstName, lastName, diary;

        public PersonImpl(String firstName, String lastName, String diary) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.diary = diary;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getLastName() {
            return lastName;
        }

        @Override
        public String getDiary() {
            return diary;
        }
        
    }
    
    @Test
    public void simpleTest() {
        final Person Alice = new PersonImpl("Alice", "Affentranger", null);
        final Person Bob = new PersonImpl("Bob", "Basinga", "Today I met Alice.");
        DataBase db = new DataBase() {

            @Override
            public Iterator<Person> list() {
                return Arrays.asList(Alice, Bob).iterator();
            }

            @Override
            public Iterator<Person> filterByLastName(String lastName) {
                if (lastName.equals("Affentranger")) {
                    return Arrays.asList(Alice).iterator();
                }
                if (lastName.equals("Basinga")) {
                    return Arrays.asList(Bob).iterator();
                }
                return Collections.emptyIterator();
            }

            @Override
            public Iterator<Person> filterByFirstName(String firstName) {
                if (firstName.equals("Alice")) {
                    return Arrays.asList(Alice).iterator();
                }
                if (firstName.equals("Bob")) {
                    return Arrays.asList(Bob).iterator();
                }
                return Collections.emptyIterator();
            }
        };
        final Graph graph = new PersonDataBaseGraph(db);
        BlankNodeOrIRI bob = graph.filter(null, PersonDataBaseGraph.FIRST_NAME, new PlainLiteralImpl("Bob")).next().getSubject();
        BlankNodeOrIRI mrBasinga = graph.filter(null, PersonDataBaseGraph.LAST_NAME, new PlainLiteralImpl("Basinga")).next().getSubject();
        Assert.assertEquals(bob, mrBasinga);
        BlankNodeOrIRI alice = graph.filter(null, PersonDataBaseGraph.FIRST_NAME, new PlainLiteralImpl("Alice")).next().getSubject();
        Assert.assertFalse(bob.equals(alice));
        //Alice is 2, Bob 3 triples
        Assert.assertEquals(5, graph.size());
                
    }
}
