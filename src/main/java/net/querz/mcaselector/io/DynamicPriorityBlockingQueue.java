package net.querz.mcaselector.io;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class DynamicPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

	private Node<E> first;
	private Node<E> last;
	private int size;

	private final ReentrantLock lock = new ReentrantLock();

	private final Condition notEmpty = lock.newCondition();

	public DynamicPriorityBlockingQueue() {
	}

	@Override
	public Iterator<E> iterator() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return new ElementIterator<>(first);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			boolean removed = false;
			Iterator<Node<E>> itr = new NodeIterator<>(first);
			while (itr.hasNext()) {
				Node<E> node = itr.next();
				if (filter.test(node.element)) {
					node.remove();
					removed = true;
				}
			}
			return removed;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return size;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void put(E e) throws InterruptedException {
		offer(e);
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) {
		return offer(e);
	}

	@Override
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ((result = dequeue()) == null) {
				notEmpty.await();
			}
		} finally {
			lock.unlock();
		}
		return result;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ((result = dequeue()) == null && nanos > 0) {
				nanos = notEmpty.awaitNanos(nanos);
			}
		} finally {
			lock.unlock();
		}
		return result;
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = Math.min(size, maxElements);
			for (int i = 0; i < n; i++) {
				c.add(dequeue());
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean offer(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (first == null) {
				first = new Node<>(e);
				last = first;
			} else {
				last.next = new Node<>(e, last);
				last = last.next;
			}
			size++;
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return dequeue();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (first == null) {
				return null;
			}
			return first.element;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Iterator<Node<E>> itr = new NodeIterator<>(first);
			while (itr.hasNext()) {
				Node<E> node = itr.next();
				node.remove();
			}
			first = null;
			last = null;
			size = 0;
		} finally {
			lock.unlock();
		}
	}

	private E dequeue() {
		if (first == null) {
			return null;
		}

		// get element with highest priority
		Node<E> current = first;
		Iterator<Node<E>> itr = new NodeIterator<>(first);
		while (itr.hasNext()) {
			Node<E> node = itr.next();
			if (node.compareTo(current) < 0) {
				current = node;
			}
		}
		current.remove();
		return current.element;
	}

	private class Node<V> implements Comparable<Node<V>> {

		V element;
		Node<V> next;
		Node<V> previous;

		Node(V element) {
			this.element = element;
		}

		Node(V element, Node<V> previous) {
			this.element = element;
			this.previous = previous;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Node<V> o) {
			return ((Comparable<V>) element).compareTo(o.element);
		}

		@SuppressWarnings("unchecked")
		public void remove() {
			if (previous != null) {
				previous.next = next;
			}
			if (next != null) {
				next.previous = previous;
			}
			if (this == first) {
				first = (Node<E>) next;
			}
			if (this == last) {
				last = (Node<E>) previous;
			}
			next = null;
			previous = null;
			size--;
		}
	}

	private class NodeIterator<V> implements Iterator<Node<V>> {

		Node<V> current;

		NodeIterator(Node<V> first) {
			this.current = first;
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public Node<V> next() {
			if (current == null) {
				throw new NoSuchElementException();
			}
			Node<V> n = current;
			current = current.next;
			return n;
		}
	}

	private class ElementIterator<V> implements Iterator<V> {

		Node<V> current;

		ElementIterator(Node<V> first) {
			this.current = first;
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public V next() {
			if (current == null) {
				throw new NoSuchElementException();
			}
			V e = current.element;
			current = current.next;
			return e;
		}
	}
}