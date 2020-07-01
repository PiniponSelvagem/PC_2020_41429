package pc.serie2.ex2;

import java.util.concurrent.atomic.AtomicReference;

public class TransferQueue<T> {

    // the queue node
    private static class Node<V> {
        final AtomicReference<Node<V>> next;
        final V item;

        Node(V item) {
            next = new AtomicReference<>(null);
            this.item = item;
        }
    }

    // the head and tail references
    private final AtomicReference<Node<T>> head;
    private final AtomicReference<Node<T>> tail;

    public TransferQueue() {
        Node<T> sentinel = new Node<>(null);
        head = new AtomicReference<>(sentinel);
        tail = new AtomicReference<>(sentinel);
    }

    // enqueue a datum
    public void put(T item) {
        Node<T> newNode = new Node<>(item);

        while (true) {
            Node<T> observedTail = tail.get();
            Node<T> observedTailNext = observedTail.next.get();
            if (observedTail == tail.get()) {	// confirm that we have a good tail, to prevent CAS failures
                if (observedTailNext != null) { /** step A **/
                    // queue in intermediate state, so advance tail for some other thread
                    tail.compareAndSet(observedTail, observedTailNext);		/** step B **/
                } else {
                    // queue in quiescent state, try inserting new node
                    if (observedTail.next.compareAndSet(null, newNode)) {	/** step C **/
                        // advance the tail
                        tail.compareAndSet(observedTail, newNode);	/** step D **/
                        break;
                    }
                }
            }
        }
    }

    // try to dequeue a datum
    public T take() {
        while (true) {
            Node<T> observedHead = head.get();
            Node<T> observedTail = tail.get();
            Node<T> observedHeadNext = observedHead.next.get();

            if (observedHead == head.get()) {   // confirm that we have a good head, to prevent CAS failures
                if (observedHead == observedTail) {
                    if (observedHeadNext == null) { // check if queue is empty
                        return null;
                    }
                    // queue in intermediate state, so advance tail for some other thread
                    tail.compareAndSet(observedTail, observedHeadNext);
                } else {
                    if (head.compareAndSet(observedHead, observedHeadNext)) {   // advance head to next position
                        return observedHeadNext.item;
                    }
                }
            }
        }
    }
}