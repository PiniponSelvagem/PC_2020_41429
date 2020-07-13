
# Sincronizadores

## _Implementação do tipo `TimeoutHolder` em *Java*_

```Java
import java.util.concurrent.TimeUnit;

public class TimeoutHolder {
	private final long deadline;		// timeout deadline: non-zero if timed
	
	public TimeoutHolder(long millis) {
		deadline = millis > 0L ? System.currentTimeMillis() + millis: 0L;
	}
	
	public TimeoutHolder(long time, TimeUnit unit) {
		deadline = time > 0L ? System.currentTimeMillis() + unit.toMillis(time) : 0L;
	}
	
	// returns true if a timeout was defined
	public boolean isTimed() { return deadline != 0L; }
	
	// returns the remaining timeout
	public long value() {
		if (deadline == 0L)
			return Long.MAX_VALUE;
		long remainder = deadline - System.currentTimeMillis();
		return remainder > 0L ? remainder : 0L;
	}	
}
```

## _Monitores_

### _Monitor Implicito_

```Java
public class SingleResource {
	private final Object monitor; // the monitor
	private boolean busy;         // the synchronization state
	
	public SingleResource(boolean busy) {
		monitor = new Object();
		this.busy = busy;
	}
	
	public SingleResource() { this(false); }
  
	// acquire resource
	public void acquire() throws InterruptedException {
		synchronized(monitor) {	// this block is a critical section executed inside the monitor
			while (busy)
				monitor.wait();	// block current thread on monitor's condition variable
			busy = true;
		}
	}
  
	// release the previously acquire resource
	public void release() {
		synchronized(monitor) {	// this block is a critical section executed inside the monitor
			busy = false;
			monitor.notify();	// notify one thread blocked in the monitor by acquire
		}
	}
}
```

---

### _Monitor Explícito_

```Java

import java.util.concurrent.locks.*;

public class SingleResourceEx {
	private final Lock monitor;       // the lock
	private final Condition nonBusy;  // the condition variable
	private boolean busy;             // the synchronization state
	
	public SingleResourceEx(boolean busy) {
		monitor = new ReentrantLock();		// create the monitor
		nonBusy = monitor.newCondition();	// get a condition variable of the monitor
		this.busy = busy;
	}
	
	public SingleResourceEx() { this(false); }
  
	// acquire resource
	public void acquire() throws InterruptedException {
    	monitor.lock();		// enter the monitor, that is, acquire the monitor's lock
   		try { 	// this block is the critical section executed inside the monitor
			while(busy)
				nonBusy.await();	// block current thread on the onBusy conditon variable,
									// obviously leaving the monitor
			busy = true;	// acquire the resource
		} finally {
			monitor.unlock();	// release the lock, that is, leave the monitor
		}
	}
  
	// release the previously acquire resource 
	public void release() {
		monitor.lock();
		try {
			busy = false;		// mark resource as free
			nonBusy.signal();	// notify one thread blocked on onBusy condition variable; if there
								// is at least one thread blocked, it will reenter the monitor and
								// try to acquire the resource
		} finally {
			monitor.unlock();
		}
	}
}
```

---

### _Pseudo-código para o Sincronizador Genérico com Base num Monitor Explícito do *Java*_

```Java
class GenericSynchronizerMonitorStyleExplicitMonitor {
	// explicit Java monitor that supports the synchronzation of shared data access
	// and supports also the control synchronization.
	private final Lock lock = new ReentrantLock();
	private final Condition okToAcquire = lock.newCondition(); 
	
	// synchronization state
	private SynchState synchState;
	
	/**
	 * Synchronizer dependent methods
	 */

	// initialize the synchronizer
	public GenericSynchronizerMonitorStyleExplicitMonitor(InitializeArgs initialState) {
        initialize "synchState" according to information specified by "initialState";
    }

	// returns true if synchronization state allows an immediate acquire
	private boolean canAcquire(AcquireArgs acquireArgs) {
        returns true if "syncState" satisfies an immediate acquire according to "acquireArgs";
    }

	// executes the processing associated with a successful acquire
	private AcquireResult acquireSideEffect(AcquireArgs acquireArgs) {
        update "synchState" according to "acquireArgs" after a successful acquire;
		returns "the-proper-acquire-result";
    }

	// update synchronization state due to a release operation
	private void updateStateOnRelease(ReleaseArgs releaseArgs) {
        // update "syncState" according to "releaseArgs";
    }

	/**
	 * Synchronizer independent methods
	 */

	// generic acquire operation; returns null when it times out
	public AcquireResult acquire(AcquireArgs acquireArgs, long millisTimeout)
		 					     throws InterruptedException {
		lock.lock();
		try {
			if (canAcquire(acquireArgs))
				return acquireSideEffect(acquireArgs);	
			boolean isTimed = millisTimeout >= 0;
			long nanosTimeout = isTimed ? TimeUnit.MILLISECONDS.toNanos(millisTimeout) : 0L;
			do {
				if (isTimed) {
					if (nanosTimeout <= 0)
						return null; // timeout
					nanosTimeout = okToAcquire.awaitNanos(nanosTimeout);
				} else
					okToAcquire.await();
			} while (!canAcquire(acquireArgs));
			// successful acquire after blocking
			return acquireSideEffect(acquireArgs);
		} finally {
			lock.unlock();
		}
	}

	// generic release operation 
	public void release(ReleaseArgs releaseArgs) {
		lock.lock();
		try {
			updateStateOnRelease(releaseArgs);
			okToAcquire.signalAll();
			// or okToAcquire.signal() if only one thread can have success in its acquire
		} finally {
			lock.unlock();
		}
	}
}
```

---

## Limitações na implementação de Sincronizadores ao "Estilo Monitor"

- Os monitores com a semântica de notificação de *Lampson* e *Redell* não garantem atomicidade entre o código que é executado dentro do monitor por uma *releaser thread* antes da notificação de uma *threads* bloqueadas e o código executado pela *acquirer thread* após retorno da operação de *wait* sobre uma variável condição. (Recorda-se que esta atomicidade era garantida pela semântica de sinalização proposta por *Brich Hansen* e *Hoare*).

- A semântica de *Lampson* e *Redell* permite que entre a alteração ao estado de sincronização feita numa operação *release* antes da notificação de uma *thread* bloqueada no monitor e a reacção a essa alteração de estado por parte da *acquirer thread* notificada, **terceiras** *threads* possam entrar no monitor (devido ao *barging*) e possam modificar o estado de sincronização, interferindo com a *acquirer thread* notificada no *realese*. (Para efeitos de simplificação do texto, refere-se a notificação apenas de uma *acquirer thread*; se forem notificadas várias *acquirer threads* nem sequer poderíamos falar em atomicidade, a menos que, com acontece com a semântica de *Brinch Hansen* e *Hoare* fosse estabelecida uma cadeia, onde cada as *threads* notificadas recebia o estdado do monitor deixado pela anterior.

- Nos sincronizadores em que o estado de sincronização reflete sempre o resultado das operações *acquire* e *release* realizadas anteriormente, como é o caso do semáforo ou de uma *message queue* esta  falta de atomicidade não torna inviável a implementação de sincronizadores usando o "estilo monitor". Não é possível garantir a ordem com que são realizadas as operações *acquire* viabilizadas por um *release*, mas não existe quebra da semântica de sincronização. No caso do semáforo, o seu estado de sincronização reflecte sempre o número de autorizações sob custódia do semáforo; no caso da *message queue* o seu estado de sincronização reflete sempre as mensagens que foram enviadas e ainda não foram recebidas.

- Contudo, em sincronizadore onde a semântica é definida em termos de transições de estado ou onde existem operação de *reset* do estado de sincronização, a interferência de **terceiras *threads*** desaconselha implementações segundo o "estilo monitor". São exemplos:

	- No *manual-reset event* ou no *auto-reset event* que por suportarem um operação de *reset*, é possível ocorrer uma operação de *reset* entre uma operação de *set* e a *thread(s)* notificada(s) reentrar(em) no monitor. Nesta situação, a semântica da operação *set* (libertar todas as *threads* no *manual-reset event* ou uma *thread* no *auto-reset event*) pode não se verificar.
		
	- Para evitar *starvation* das *threads* leitoras e escritoras na implementação do *read/write lock*, *Hoare* propôs a seguinte semântica: (a) quando existirem *threads* escritoras bloqueadas no *lock*, não são concedidos mais *read locks*, até que todos os existentes seja libertados - isto impede *starvation* das *threads* escritoras por parte das *threads* leitoras -, e; (b) quando é libertado o *write lock*, é garantido o *read lock* a todas as *threads* leitoras que se encontrem <ins>nesse momento</ins> bloqueadas, mas não às *threads* leitoras que tentem adquirir o *read lock* posteriromente - assim, é necessário distinguir entre as *threads* leitoras que se encontram bloqueadas e aquelas que tentem adquirir o *read lock* posteriormente - isto garante, que as *threads* escritoras não provocam *startvation* às *threads* leitoras;
	
	- O sincronizador *exchanger* que suporta a troca de mensagens entre pares de *threads*, também so pode ser implementado ao "estilo monitor" usando uma máquina de estados. Neste sincronizador, a primeira *thread* do par tem que se bloquear a aguardar a chegada da segunda *thread*; quando isto acontece, é preciso consumar a troca se a intervenção de **terceira(s)** *thread(s)*. Neste caso, a "terceira" *thread* deverá ser a primeira do próximo par.
	
	- Quando existe a necessidade de implementar disciplinas de fila de espera especícifas, é sempre necessário implementar explicitamente as filas de espera.

---

## Soluções

- Alguns dos problemas enunciados anteriormente podem ser resolvidos implementando as operações *acquire* com base em máquinas de estados de modo a impedir que uma terceira *thread* acedam ao estado de sincronização antes da conclusão das operações *acquire* viabilizadas por uma operação *release*. Exemplos:

	- A implementação do *exchanger* com base numa máquina de estados, poderia considerar três estados: *idle* quando aguarda a chegada da primeira *thread*; *exchanging*, depois da chegada da primeira *thread* de um par; *closing* depois da chegada da segunda *thread* até que a primeira *thread* reentre no monitor após notificação. Quando a primeira *thread* reentra no monitor completa a troca e coloca o *exchanger* no estado *idle*. Qualquer *thread* que entre no monitor quando o *exchanger* está no estado *closing*, bloqueia-se a aguardar que o *exchanger* transite para o estado *idle* (tornando a primeira *thread* de uma próxima troca), ou *exchanging* (tornando-se a segunda *thread* de uma próxima troca já iniciada por outra *thread*).

- As solução em que as operações *acquire* são baseadas em máquinas de estados não permitem resolver as situações em que a semântica de sincronização é definida em função de transições de estado como acontece no caso do *read/write lock*.

## Estilo kernel

``` Java
/**
 * Semaphore following the kernel style, using an *implicit Java monitor*, with
 * support for timeout on the acquire operation.
 */

class SemaphoreKernelStyleImplicitMonitor {
	// implicit Java monitor that synchronizes access to the mutable shared state
	// and supports also the control synchronization on its condition variable.
	private final Object monitor = new Object();
	
	// the request type
	private static class Request {
		final int acquires;     // the number of requested permits
		boolean done;           // true when completed
		
		Request(int acquires) { this.acquires = acquires; }
	}
	
	// the queue of pending acquire requests
	private final LinkedList<Request> reqQueue = new LinkedList<Request>();
	
	// the synchronization state: the number of available permits
	private int permits;
		
	// initialize the semaphore
	public SemaphoreKernelStyleImplicitMonitor(int initialPermits) {
		if (initial < 0)
			throw new IllegalArgumentException("initialPermits")
		permits = initialPermits;
	}
	
	// if there are sufficient permits, return true; false otherwise.
	private boolean canAcquire(int acquires) { return permits >= acquires; }
    
	// if there are threads in the queue, return whether the number of available
	// permits is sufficient to satisfy the request of the thread that
	// is at the front of the queue
	private boolean currentSynchStateAllowsAcquire() {
		return reqQueue.size() > 0 && permits >= reqQueue.peek().acquires;
	}
	
	// after acquire deduct the permissions granted
	private void acquireSideEffect(int acquires) { permits -= acquires; }
	
	// update the available permits in accordance with the permits released
	private void updateStateOnRelease(int releases) { permits += releases; }

	// acquires the specified number of permits; return false when it times out
	public boolean acquire(int acquires, long millisTimeout)  throws InterruptedException {
		synchronized(monitor) {
			// if the was previously interrupted, throw the appropriate exception
			if (Thread.interrupted())
				throw new InterruptedException();
			// if the queue is empty and there are sufficient permits, decrement the
			// number of available permits, and return success
			if (reqQueue.size() == 0 && canAcquire(acquires)) {
				acquireSideEffect(acquires);
				return true;
			}
			
			// the queue is not empty or there are not sufficient permits, so the
			// current thread must enqueue a request and wait on the monitor
			// condition variavel that a releaser thread complete the request
			// on its behalf.
			Request request = new Request(acquires);
			reqQueue.addLast(request);

			TimeoutHolder th = new TimeoutHolder(millisTimeout);
			do {
				try {
					if (th.isTimed()) {
						if ((millisTimeout = th.value()) <= 0) {
							// the specified time limit has expired
							reqQueue.remove(request);

							// if the request was at the head of the queue and there are more blocked
							// threads and some permits available, it is possible that this withdrawal
							// creates conditions to satisfy requests from the next thread.
							// for example, if after the remove of the request of the current thread,
							// if there are two permits available and there are two threads in the queue
							// requesting one permits each, there are mow conditions to satisfy the
							// requests of those threads.
							if (currentSynchStateAllowsAcquire())
								performPossibleAcquires();
							
							// return failure
							return false;
						}
						monitor.wait(millisTimeout);
					} else
						monitor.wait();
				} catch (InterruptedException ie) {
					// if the acquire operation was already done, re-assert interrupt
					// and return normally; else remove request from queue and throw
					// InterruptedException.
					if (request.done) {
						Thread.currentThread().interrupt();
						return true;
					}
					reqQueue.remove(request);

					// if this request was at the head of the queue and there are more blocked
					// threads and some permits available, it is possible that this withdrawal
					// creates conditions to satisfy requests from the next thread.
					// for example, if after the remove of the request of the current thread,
					// if there are two permits available and there are two threads in the queue
					// requesting one permits each, there are mow conditions to satisfy the
					// requests of those threads.
					if (currentSynchStateAllowsAcquire())
						performPossibleAcquires();
					
					// propagate InterrupedException
					throw ie;
				}
			} while (!request.done);
			// return success
			return true;
		}
	}

	// perform the possible pending acquires
	private void performPossibleAcquires() {
		boolean notify = false;
		while (reqQueue.size() > 0) {
			Request request = reqQueue.peek();
			if (!canAcquire(request.acquires))
				break;
			
			// complete a pending aqcuire request: (i) remove the request from the queue;
			// (ii) consume the requested permits; (iii) mark the request as completed;
			// (iv) ensure that that acquirer thread is notified. 
			reqQueue.removeFirst();
			acquireSideEffect(request.acquires);
			request.done = true;
			notify = true;
		}
		if (notify) {
			// even if we release only one thread, we do not know its position of the queue
			// of the condition variable, so it is necessary to notify all blocked threads,
			// to make sure that the thread(s) in question is notified.
			monitor.notifyAll();
		}
	}

	//releases the specified number of permits
	public void release(int releases) {
		synchronized(monitor) { 
			// return the release permits to the semaphore
			updateStateOnRelease(releases);
			// satisfies as many acquires as is possible
			performPossibleAcquires();
		}
	}
}
```