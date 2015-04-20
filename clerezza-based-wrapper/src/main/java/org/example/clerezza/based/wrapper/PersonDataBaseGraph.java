package org.example.clerezza.based.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.AbstractGraph;
import org.apache.clerezza.commons.rdf.impl.utils.LiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.example.wrapped.api.DataBase;
import org.example.wrapped.api.Person;

/**
 * Wraps a PersondataBase as a Graph, so that standard RDF tools can be used to 
 * query and serialize a PersonDataBase or to do other kind of RDF operations 
 * with it.
 */
public class PersonDataBaseGraph extends AbstractGraph {

    private final DataBase personDataBase;

    final static public IRI FIRST_NAME = new IRI("http://example.org/ontology/firstName");
    final static public IRI LAST_NAME = new IRI("http://example.org/ontology/lastName");
    final static public IRI DIARY = new IRI("http://example.org/ontology/diary");
    final static private Collection<IRI> supportedPredicates = Arrays.asList(FIRST_NAME, LAST_NAME, DIARY);
    final static private IRI XSD_STRING = new IRI("http://www.w3.org/2001/XMLSchema#string");

    public PersonDataBaseGraph(DataBase personDataBase) {
        this.personDataBase = personDataBase;
    }

    @Override
    protected Iterator<Triple> performFilter(BlankNodeOrIRI subject, IRI predicate, RDFTerm object) {
        //first handle the trivial empty-result cases
        if (predicate != null) {
            if (!supportedPredicates.contains(predicate)) {
                return Collections.emptyIterator();
            }
        }
        if (subject != null) {
            if (!(subject instanceof PersonNode)) {
                return Collections.emptyIterator();
            }
        }
        if (object != null) {
            if (!(object instanceof Literal)) {
                return Collections.emptyIterator();
            }
            final Literal objLiteral = (Literal) object;
            if (!objLiteral.getDataType().equals(XSD_STRING)) {
                return Collections.emptyIterator();
            }
        }
        final Iterator<Person> personIterator = getPersonsIterator((PersonNode) subject, predicate, (Literal) object);
        final Iterator<Triple> tripleIter = new Persons2Triples(personIterator);
        return new IteratorFilter(tripleIter, subject, predicate, object);
    }

    @Override
    protected int performSize() {
        int i = 0;
        Iterator<Triple> iter = performFilter(null, null, null);
        while (iter.hasNext()) {
            Triple next = iter.next();
            i++;
        }
        return i;
    }

    private Iterator<Person> getPersonsIterator(PersonNode subject, IRI predicate, Literal object) {
        if (subject != null) {
            return Collections.singleton((subject).person).iterator();
        } else {
            if (object != null) {
                final String objString = object.getLexicalForm();
                if (predicate != null) {
                    if (predicate.equals(LAST_NAME)) {
                        return personDataBase.filterByLastName(objString);
                    }
                    if (predicate.equals(FIRST_NAME)) {
                        return personDataBase.filterByFirstName(objString);
                    }
                }
            }
        }
        return personDataBase.list();
    }

    private static class Persons2Triples implements Iterator<Triple> {

        private Iterator<Triple> currentPerson;
        private final Iterator<Person> personIterator;

        private Persons2Triples(Iterator<Person> personIterator) {
            this.personIterator = personIterator;
            prepareNextPerson();

        }

        private void prepareNextPerson() {
            if (!personIterator.hasNext()) {
                currentPerson = Collections.emptyIterator();
            } else {
                currentPerson = getTriplesForPerson(personIterator.next());
            }
        }

        private static Iterator<Triple> getTriplesForPerson(Person person) {
            final Collection<Triple> triples = new ArrayList<>();
            if (person.getFirstName() != null) {
                triples.add(new TripleImpl(
                        new PersonNode(person),
                        FIRST_NAME,
                        new LiteralImpl(person.getFirstName(), XSD_STRING, null)));
            }
            if (person.getLastName() != null) {
                triples.add(new TripleImpl(
                        new PersonNode(person),
                        LAST_NAME,
                        new LiteralImpl(person.getLastName(), XSD_STRING, null)));
            }
            if (person.getDiary() != null) {
                triples.add(new TripleImpl(
                        new PersonNode(person),
                        DIARY,
                        new LiteralImpl(person.getDiary(), XSD_STRING, null)));
            }
            return triples.iterator();
        }

        @Override
        public boolean hasNext() {
            if (!currentPerson.hasNext()) {
                prepareNextPerson();
            }
            return currentPerson.hasNext();
        }

        @Override
        public Triple next() {
            if (!currentPerson.hasNext()) {
                prepareNextPerson();
            }
            return currentPerson.next();
        }

    }

    private static class IteratorFilter implements Iterator<Triple> {

        private final Iterator<Triple> base;
        private Triple nextMatching;
        private final BlankNodeOrIRI subject;
        private final IRI predicate;
        private final RDFTerm object;

        private IteratorFilter(Iterator<Triple> base, BlankNodeOrIRI subject, IRI predicate, RDFTerm object) {
            this.base = base;
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
            prepareNext();
        }

        private void prepareNext() {
            while (base.hasNext()) {
                final Triple candidate = base.next();
                if (((subject == null) || (candidate.getSubject().equals(subject)))
                        && ((predicate == null) || (candidate.getPredicate().equals(predicate)))
                        && ((object == null) || (candidate.getObject().equals(object)))) {
                    nextMatching = candidate;
                    return;
                }
            }
            nextMatching = null;
        }

        @Override
        public boolean hasNext() {
            return nextMatching != null;
        }

        @Override
        public Triple next() {
            if (nextMatching == null) {
                throw new NoSuchElementException();
            }
            final Triple result = nextMatching;
            prepareNext();
            return result;
        }
    }

    static class PersonNode extends BlankNode {

        Person person;

        public PersonNode(Person person) {
            this.person = person;
        }

        @Override
        public int hashCode() {
            return 83 + Objects.hashCode(this.person);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PersonNode other = (PersonNode) obj;
            return person.equals(other.person);
        }

    }

}
