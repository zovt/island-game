import java.util.Iterator;

import tester.Tester;

// an iterator over ILists
class IListIterator<T> implements Iterator<T> {
    IList<T> list;
    
    IListIterator(IList<T> list) {
        this.list = list;
    }
    
    // check if this iterator has a next element
    public boolean hasNext() {
        return this.list.isCons();
    }

    // get the current item
    public T next() {
        T item = list.asCons().item;
        list = list.asCons().next;
        return item;
    }
}

// a list
interface IList<T> extends Iterable<T> {
    Cons<T> asCons();
    boolean isCons();
}

// Abstract class containing an iterator common to both classes
abstract class AList<T> implements IList<T> {
    public Iterator<T> iterator() {
        return new IListIterator<T>(this);
    }
}

// A non-empty list
class Cons<T> extends AList<T> {
    T item;
    IList<T> next;
    
    Cons(T item, IList<T> next) {
        this.item = item;
        this.next = next;
    }
    
    // get this cons as a cons
    public Cons<T> asCons() {
        return this;
    }
    
    // check if this is a cons
    public boolean isCons() {
        return true;
    }
}

// an empty list
class Empty<T> extends AList<T> {
    // get this Empty as a Cons
    public Cons<T> asCons() {
        throw new RuntimeException("Empty is not Cons.");
    }
    
    // check if this is a Cons
    public boolean isCons() {
        return false;
    }
}

class ExamplesLists {
    IList<Integer> mt = new Empty<Integer>();
    IList<Integer> l1 = mt;
    IList<Integer> l2 = new Cons<Integer>(2, mt);
    IList<Integer> l3 = new Cons<Integer>(8, new Cons<Integer>(2, mt));
    
    // test iteration
    void testIteration(Tester t) {
        int res = 0;
        for (int i : l1) {
            res += i;
        }
        t.checkExpect(res, 0);
        
        for (int i : l2) {
            res += i;
        }
        t.checkExpect(res, 2);
        
        res = 0;
        for (int i : l3) {
            res += i;
        }
        t.checkExpect(res, 10);
    }
}