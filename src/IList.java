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
    
    // remove
    public void remove() {
        throw new RuntimeException("Unimplemented");
    }
}

// a list
interface IList<T> extends Iterable<T> {
    // get this IList as a Cons
    Cons<T> asCons();

    // check if this IList is a Cons
    boolean isCons();

    // get the element at the given index
    T get(int idx);
    
    // get size
    int size();
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

    // get the item at the given index
    public T get(int idx) {
        if (idx == 0) {
            return this.item;
        }
        return this.next.get(idx - 1);
    }

    // returns size
    public int size() {
        return 1 + this.next.size();
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

    // get an item in an empty list
    public T get(int idx) {
        throw new RuntimeException("Index out of bounds");
    }

    // returns size of list
    public int size() {
        return 0;
    }
}

class ExamplesLists {
    IList<Integer> mt = new Empty<Integer>();
    IList<Integer> l1 = mt;
    IList<Integer> l2 = new Cons<Integer>(2, mt);
    IList<Integer> l3 = new Cons<Integer>(8, new Cons<Integer>(2, mt));

    // test asCons and isCons
    void testAsIsCons(Tester t) {
        t.checkExpect(l1.isCons(), false);
        t.checkExpect(l2.isCons(), true);
        t.checkException(new RuntimeException("Empty is not Cons."), mt,
                "asCons");
        t.checkExpect(l2.asCons(), l2);
    }

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

    // test get
    void testGet(Tester t) {
        t.checkExpect(l2.get(0), 2);
        t.checkExpect(l3.get(1), 2);
        t.checkExpect(l3.get(0), 8);
        t.checkException(new RuntimeException("Index out of bounds"), l1, "get",
                0);
        t.checkException(new RuntimeException("Index out of bounds"), l2, "get",
                3);
        t.checkException(new RuntimeException("Index out of bounds"), l3, "get",
                4);
    }
}