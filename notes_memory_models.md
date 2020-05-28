
# Modelos de Memória

# _Visibilidade_

- Problema: se usa 2nd thread afectar `ready` com `true`, na 1st thread este while pode ficar em ciclo infinito porque não vai ver `ready` afectada. Isto porque não existe nenhuma garantia de que as operações realizadas numa **thread** serão realizadas pela ordem de programa.
```Java
private static boolean ready;

public void run() {
    while (!ready)
        ;
    System.out.println(number);
}
```

- Outro problema, o JIT pode compilar o codigo desta maneira, visto que `ready` não é afectado dentro do `while`
```Java
public void run() {
    if (!ready) {
        while (true)
            ;
    }
    System.out.println(number);
}	
```

## Soluções
- 1:
```Java
public void run() {
    while (!ready)
        Thread.yield();
    System.out.println(number);
}	
```

- 2:
```Java
private static volatile boolean ready;
```

---

# _Barreiras_
- Relacionado com reordenação de código

### *Acquire Barrier*
- Acquire -> seguinte nao pode passar para cima
- Impede que instruções que veem **depois** possam ser movidas para **antes** da barreira.
```
   |	  
===|======= *acquire barrier* 
   |   ^
   V   |
       |
```

### *Release Barrier*
- Release -> anterior nao pode passar para baixo
- Impede que instruções que veem **antes** possam ser movidas para **depois** da barreira.
```
   |
   |   ^
   v   |
=======|=== *release barrier*
       |
```

### *Full-Fence*
- Combina **acquire** com **release**, mas apenas se **acquire** e depois **release**, se não perde o efeito pois **release** e depois **acquire** pode ser reordenado.
```
   |	  
   V
============ *full-fence* 
       ^
       |
```

## Exemplo
```				
                             |
synchronized(monitor) {  ====|= acquire barrier
                          ^  V
                          |
	critical section;
	
    |
    |
    V  ^
} =====|== release barrier
       |
```
---

# _Reordenação parte 1_

- Problema:
```Java
public class PossibleReordering {
	static int x = 0, y = 0;
	static int a = 0, b = 0;
	
	public static void main(String... args) throws InterruptedException {
		Thread one = new Thread(() -> {
			a = 1;
			x = b;
		});
		Thread other = new Thread(() -> {
			b = 1;
			y = a;
		});
		one.start(); other.start();
		one.join(); other.join();
		System.out.printf("(x: %d, y: %d)\n", x, y);
	}
}
```
- Casos possiveis que print:
    - (x: 1, y: 0)
    - (x: 0, y: 1)
    - (x: 1, y: 1)
    - (x: 0, y: 0)

- Este último caso especial pois:
```
                   +----------+          reorder           +-------+   
Thread one:    --->| x = b(0) |--------------------------->| a = 1 |
                   +----------+                            +-------+

                                +-------+     +----------+
Thread other:  ---------------->| b = 1 |---->| y = a(0) |
                                +-------+     +----------+
```

## Soluções
- 1 - usar sincronização
- 2 - para o compilador nao reordenar, tambem se pode usar volatile e desta forma `'x: 0, y: 0'` não será possivel de acontecer.

## Exemplos
```Java
class StartAndTerminationThreadRuleDemo {
	
	private static class AddThread extends Thread {
		private int arg1, arg2;
		private int result;
	
		public AddThread(int arg1, int arg2) {
			this.arg1 = arg1;
			this.arg2 = arg2;
		}
	
		// executed by the started thread
		public void run() {	// Thread.start interposes an acquire barrier
			result = arg1 + arg2;
		} // on termination interposes a release barrier
	}

	public static void main(String... args) throws InterruptedException {
		var addt = new AddThread(20, 22);
		addt.start();		// interposes a release barrier
		addt.join();		// after return, Thread.join interposes an acquire barrier
		System.out.printf("sum is: %d\n", addt.result);
	}
}
```

```Java
class ThreadInterruptionRuleDemo {
	
	private static class AddOnIntrThread extends Thread {
		int intrArg1, intrArg2;
		
		public void run() {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ie) { // interposes an acquire barrier (action B)
				System.out.printf("sum is: %d\n", intrArg1 + intrArg2);
			}
		}
	}

	public static void main(String... args) throws InterruptedException {
		var addt = new AddOnIntrThread();
		addt.start();
		Thread.sleep(1000);
		addt.intrArg1 = 20;
		addt.intrArg2 = 22;
		addt.interrupt();		// interposes a release barrier (action A)
		System.out.println("...main exits");
	}
}
```
---

# _Reordenação parte 2_

- Para garantir que a _thread_ que executa a **Acção B** pode ver os resultados da **Acção A** (sejam ou não as acções A e B executadas em _threads_ diferentes), deve existir uma relação **_happens-before_** entre a acção A e a acção B.

### _Happens-Before_ versus Barreiras de Memória
- Acções de Sincronização com semântica _release_ (referida acima por **acção A**):
	- Libertação de um _lock_;
	- Escrita num campo `volatile`;
	- Terminação de uma _thread_;
	- Interrupção de uma _thread_;
	- Fim da execução do construtor (relativamente ao _finalizer_);

- Acções de Sincronização com semântica _acquire_ (referida acima por **acção B**):
	- Acquisição de um _lock_;
	- Leitura de um campo `volatile`;
	- Detecção da terminação de uma _thread_;
	- Detecção da interrupção por parte da _thread_ interrompida;
	- Início da execução do _finalizer_ (relativamente ao fim do construtor);

### _Piggybacking_ na Sincronização da Biblioteca Standard
- Outras relações _happens-before_ garantidas pelo biblioteca de classes do _Java_:
	 - Colocar um item de dados numa colecção _thread-safe_ _happens-before_ outra _thread_ obter esse item da colecção;
	 - Decrementado um `CountDownLatch` _happens-before_ uma _thread_ retornar de `await` sobre esse _latch_;
	 - Devolver uma autorização a um `Semaphore` _happens-before_ adquirir uma autorização no mesmo semáforo;
	 - As acções realizadas por uma _task_ representada por um `Future` _happens-before_ outra _thread_ retornar com sucesso do método `Future.get`;
	 - Submeter um `Runnable` ou um `Callable` a um `Executor` _happend-before_ a _task_ começar a execução, e;
	 - Uma _thread_ chegando a uma `CyclicBarrier` ou `Exchanger` _happes-before_ as outras _threads_ serem libertadas da mesma barreira ou ponto de troca. Se a `CyclicBarrier` especifica uma acção para executar quando a barreira abre, a chegada da última _thread_ do grupo à barreira _happens-before_ a execução da acção da barreira que por sua vez _happens-before_ todas as _thread_ serem libertadas da barreira.

---

# _Publicação de Objectos_
- Se não for garantido que a publicação da referência partilhada _happens-before_:
    - "outra" thread - carregar a ref partilhada, pode acontecer que a escrita para aceder a esse objecto seja reordenada com as escritas nos campos do objecto, ficando o objecto parcialmente construido / estado invalido.
- Nete exemplo é facil de perceber que varias threads podem entrar no `if`, mas ignorando esse problema `resource` pode ficar parcialmente construido pois nao existe sincronização. 
```Java
public class UnsafeLazyInitialization {
	private static Resource resource = null;
	
	public static Resource getInstance() {
		// check-then-act - atomicity
		if (resource == null)
			resource = new Resource();
		else
			;
		return resource;
	}
}
```

### Soluções
- 1
    - Tornar o `getInstance()` -> **syncronized**
    - Novo problema: resource nao é alterado e `syncronized` a **return resource** quando o resultado é sempre o mesmo.
```Java
public class SafeLazyInitialization {
	private static Resource resource;
	
	public synchronized static Resource getInstance() {
		if (resource == null)
			resource = new Resource();
		return resource;	
	}
}
```

- 2
    - tirando partido da criação do objecto principal
```Java
public class EagerInitialization {
	private static Resource resource = new Resource();	// static initializer
	
	public static Resource getResource() { return resource; }
	
	//...
}
```

- 3
    - tirando partido da iniciação de apenas quando a class é acedida
    - a JVM irá inicializar a class **ResourceHolder**, não precisando de sincronização adicional
```Java
public class ResourceFactory {
	private static class ResourceHolder {
		static Resouce resource = new Resource();
	}
	
	public static Resource getResource() { return ResourceHolder.resource; }
	
	// ... other functionality
}
```

---

# _Initialization Safety_ 
- A _initialization safety_ significa que `SafeStates`, mostrado adiante, pode ser seguramente publicado mesmo através de inicialização _lazy_ não segura ou expondo a referência para `SafeStates` num campo publico estático sem qualquer sincronização, mesmo que a classe se baseie num ´HshSet´ que não é _thread safe_.
- Se o campo `states` não for `final`, ou se outro método que não o construtor, modificasse o seu conteúdo, _initialization safety_ não seria suficiente forte para garantir acesso seguro a `SafeStates` sem sincronização. Se `SafeStates` tivesse outros campos não `final`, outras _threads_ podem ainda ver valores incorretos nesses campos.
 ```Java
 public class SafeStates {
 	private final Map<String, String> states;
	
	public SafeStates() {
		states = new HashMap<String, String>();
		states.put("alaska", "AK");
		states.put("alabama", "AL");
		...
		states.put("wyoming", "WY");
	}
	
	public String getAbbreviation(String stateName) {
		return states.get(stateName);
	}
 }
 ```

 ---

# _Publicação e Fuga de Objectos_
- **Publicar** um objecto significa torná-lo acessível a código fora do âmbito corrente.
    - Armazenar a referência para o objecto acessível a outro código.
    - Retornar a referência de um método não privado.
    - Passar a referência para um método de outra classe.

- **Fuga de objectos**
    - `states` pode ser modificado por quem chamar `getStates()`, podendo assim colocar `states` em estado invalido / parcialmente construido / etc.
```Java
class UnsafeStates {
	private String[] states = new String[] {
		"AK", "AL", ...
	};
	
	public String[] getStates() { return states; }
}
```

### Resolver **UnsafeStates**
- For array:
```Java
String[] copied = new String[states.length];
System.arraycopy(states, 0, copied, 0, states.length);
return copied;
```

- For List:
```Java
return Collections.unmodifiableList(internalList);
```

- For Object:
```Java
return internalObject.clone(); // Require deep clone
```

### Fuga do **This**
```Java
public class ThisEscape {
	public ThisEscape(EventSource source) {
		source.registerListener(
			new EventListener() {
				public void onEvent(Event e) {
					doSomething(e);
				}
		});
	}
}
```

### Resolver **ThisEscape**
- conclusão, não criar outros objectos dentro do constructor
```Java
public class SafeListener {
	private final EventListener listener;
	
	private SafeListener() {
		listener = new EventListener() {
			public void onEvent(Event e) {
				doSomething(e);
			}
		};
	}
	
	public static SafeListener newInstance(EventSource source) {
		SafeListener safe = new SafeListener();
		source.registerListener(safe.listener);
		return safe;
	}
}
```

---

# _Thread Confinement_
- O acesso a estado partilhado mutável requer sincronização; **uma forma de evitar este requisito é não partilhar objectos**. Se os objectos forem apenas acedidos por uma única _thread_, não é necessária sincronização.

---

# _Ad-hoc Thread Confinement_
- _Add-hoc thread confinement_ descreve as situações em que a responsabilidade de manter o _thread confinement_ é inteiramente da implementação.
- Uma possivel forma é `comentar` ou outro dizendo que aquela variavel/objecto nao deverá ser usada por outras threads.

---

# _Stack Confinement_
- O objecto em questao apenas pode ser alcançado por o metodo que o contem. Exemplo `numPairs` no método `loadTheArc`.

```Java
public int loadTheArc(Collection<Animal> candidates) {
	SortedSet<Animal> animals;
	int numPairs = 0;
	Animal candidate = null
	
	// animals confined to method, do not let them escape!
	animals = new TreeSet<Animal>(new SpeciesGenderComparator());
	animals.add(candidates);
	for (Animal a : animals) {
		if (candidate == null || !candidate.isPotentialMate(a))
			candidate = a;
		else {
			ark.load(new AnimalPair(candidate, a));
			numPairs++;
			++candidate = null;
		}
	}
	return numPairs;
}
```

---

# _ThreadLocal_
- Uma forma de manter _thread confinement_ é com `ThreadLocal`, permite associar valor <--> thread. Evitando o uso de valores globais e sincronizações.
- Exemplo, criar uma connecção por thread sem usar variaveis globais que irias necessitar de sincronização.
```Java
private static ThreadLocal<Connection> connectionHolder =
	new ThreadLocal<Connection> () {
		public Connect initialValue() {
			return DriverManager.getConnection(DB_URL);
		}
	};

public static Connection getConnection() {
	return connectionHolder.get();
}
```

---

# _Volatile e objectos imutaveis_
- Considere o seguinte "value holder" `OneValueCache`
```Java
class OneValueCache {
	private final BigInteger lastNumber;
	private final BigInteger[] lastFactors;
	
	public OneValueCache(BigInteger number, BigInteger[] factors) {
		lastNumber = number;
		lastFactors = Array.copyOf(factors, factors.length);
	}
	
	public BigInteger[] getFactors(BigInteger number) {
		if (lastNumber == null || !lastNumber.equals(number))
			return null;
		else
			return Arry.copyOf(lastFactors, lastFactors.length);
	}
}
```
- e a seguinte classe de utilização
```Java
public class VolatileCacheFactorizer implements Servlet {
	private volatile OneValueCache cache = new OneValueCache(null, null);
	
	public void service(ServletRequest req, ServletResponse resp) {
		BigInteger number = extractFromRequest(req);
		BigInteger[] factors = cache.getFactors(number);
		if (factors == null) {
			factors = factor(number);
			cache = new OneValueCache(number, factors);
		}
		encondeIntoResponse(resp, factors);
	}
}
```
- uma vez que `cache` é `volatile`, é possivel partilhar o objecto `OneValueCache` garantindo que os dados são vistos por outras threads e uma vez que os dados são `final` (iniciados no constructor), não haverá problemas de sincronização.

---

# _Publicação Segura de Objectos_
- O exemplo seguinte mostra uma publicação insegura do objecto `holder`
```Java
// Unsafe publication
public Holder holder;

public void initialize() {
	holder = new Holder(42);
}
```

- O exemplo seguinte mostra uma publicação insegura pois `n`não é `volatile`
```Java
public class Holder {
	private int n;  // 'volatile' resolve o problema e n==n garantidamente
	
	public Holder(int n) { this.n = n; }
	
	public void assertSanity() {
		if (n != n)
			throw new AssertionError("This statement is false")
	}
}
```

- Para publicar um objecto em segurança, ambos a referência para o objecto e o estado do objecto devem estar visíveis às outras _threads_ ao mesmo tempo. Um objecto adequadamente construído pode ser publicado em segurança por:

	- Iniciando a referência para o objecto num iniciador estático;
	
	- Armazenado a referência para o objecto num campo _volatile_ ou numa `AtomicReference`;
	
	- Armazenando a referência num campo `final` de um objecto adequadamente construído; ou
	
	- Armazenado a referência num campo que esteja adequadamente protegido por um _lock_.

---

# _Objectos Efectivamente Imutáveis_
- Se os ojectos são mútaveis, mas são usados de forma imutavel, entao não é necessário mais sincronização.
- Exemplo seguinte, depois de inserir um `Date` nunca mais modificar esse `Date`.
```Java
public Map<String, Date> lastLogin =
	Collections.synchronizedMap(new HashMap<String, Date>());
```

---

# _Objectos Mutaveis_
- Os requisitos de publicação de um objecto depende da sua mutabilidade:

	- Objectos **imutáveis** podem ser publicados usando qualquer dos mecanismos disponíveis.
	
	- Objectos **efectivamente imutáveis** devem ser publicados em segurança.
	
	- Objectos **mutáveis** devem ser publicados em segurança e devem ser ou _thread-safe_ ou protegidos por um _lock_.

---

# _Sincronização Non-Blocking_
- A class `Counter` é _thread-safe_ mas irá perder performance se muitas _threads_ chamarem `increment()` ao mesmo tempo, visto que irão ficar a espera no `syncronized`.
- `++value` sao 3 operacoes: read -> modify -> write, `value = value + 1`
```Java
public final class Counter {
	private long value = 0;
	
	public synchronized long get() { return value; }
	
	public synchronized long increment() {
		if (value == Long.MAX_VALUE)
			throw new IllegalStateException("counter overflow");
		return ++value;
	}
}
```

## _Compare and Swap_
- Em java `compareAndSet`
- Quando múltiplas _threads_ tentam actualizar a mesma variável simultaneamente usando CAS, uma ganha e actualiza o valor da variável, e o resto das _threads_ perde. Mas as _threads_ perdedoras não são punidas com o bloqueio, como aconteceria se falhassem a acquisição de um _lock_; em alternativa, elas são informadas de que não ganharam a corrida desta vez mas podem tentar de novo.
- Resumidamente, _Compare and Swap_:
    - tenho o valor `1` e quero actualizar `value` para `2` se ainda estiver lá `1`
    - chamo `compareAndSwap(1, 2)`
        - se estiver lá `1`, mete `value = 2` e retorna `1`
        - se estiver lá valor diferente de `1`, nao afecta `value` e retorna o que lá está
    - desta forma quando voltar do `compareAndSwap` irei saber que teve sucesso ou não na afectação, e decido se quero voltar a tentar ou outra decisão qualquer.
```Java
public class SimulatedCAS {
	private int value;
	
	public synchronized int get() { return value; }
	
	public synchronized void set(int value) { this.value = value; }
	
	public synchronized int compareAndSwap(int expectedValue, int newValue) {
		int oldValue = value;
		if (oldValue == expectedValue)
			value = newValue;
		return oldValue;
	}
	
	public boolean compareAndSet(int expectedValue, int newValue) {
		return compareAndSet(expectedValue, newValue) == expectedValue;
	}
}
```

### _Contador Non-Blocking_
```Java
public class CASCounter {
	private SimulatedCAS value;
	
	public int getValue() { return value.get(); }

	// increment and get : an unconditional operation
	public int increment() {
		int observedValue, updatedValue;
		do {
			observedValue = value.get();
			updatedValue = observedValue + 1;
		} while (!value.compareAndSet(observedValue, updatedValue))
		return updatedValue;
	}
	
	// a conditional operation
	public boolean decrementIfGraterThanZero() {
		int observedValue, updatedValue;
		do {
			observedValue = value.get();
			if (observedValue == 0)
				return false;	// decrement is not possible
			updatedValue = observedValue - 1;
		} while (!value.compareAndSet(observedValue, updatedValue));
		return true;
	}
}
```

### _Variáveis Atómicas como "**Better Volatiles**"_
- Usando `AtomicReference`, para guardar um objecto.
```Java
public class CasNumberRange {

	private static class IntPair {
		final int lower;	// Invariant: lower <= upper
		final int upper;
		...
	}
	
	private final AtomicReference<IntPair> values =
		new AtomicReference<IntPair>(new IntPair(0, 0));
		
	public void setLower(int newLower) {
		while (true) {
			IntPair observedPair = values.get();
			if (newLower > observedPair.upper)
				throw new IllegalArgumentException(
					"Can't set lower to " + newLower + " > upper");
			IntPair newPair = new IntPair(newLower, obdervedPair.upper);
			if (values.compareAndSet(observedPair, newPair))
				break;
		}
	}
	
	// similarly to setUpper
}
```

## Algoritmos _Nonblocking_
1. É obtida uma cópia do estado partilhado mutável (`observedValue`);
2. Em função do valor da cópia `observedValue`, podemos ter uma de três situações:
    - i. se for possível prosseguir com a operação, determinar o novo valor do estado partilhado (`updatedValue`) e passar ao passo 3;
    - ii. no caso da operação não ser possível, proceder adequadamente, isto é, aguardar algum tempo e repetir 1 (_spin wait_ ou _backoff_), devolver a indicação de que a operação não é possível ou lançar uma excepção;
    - iii. o valor de `observedValue` indica já ter sido alcançado um estado final inalterável (por exemplo, na inicialização _lazy_ após ter sido criada a instância do recurso subjacente), a operação é dada como concluída normalmente;
3. Invocar CAS para alterar o estado partilhado para `updatedValue` se o seu valor ainda for `observedValue`. Pode ocorrer uma de três situações:
    - i. o CAS tem sucesso, concluindo a operação;
    - ii. o CAS falha devido a colisão com outra _thread_ (situação comum), repetir 1, podendo eventualmente esperar algum tempo (_spin wait_ ou _backoff_);
    - iii. o CAS falha, mas devido a outra _thread_ já ter feita a operação que se pretendia (por exemplo, na inicialização _lazy_ quando mais do que uma _thread_ cria instâncias do resurso subjacente no passo 2.i), a operação e dada como concluída, após eventual _cleanup_ da instância do recurso criado especulativamente no passo 2.i.

### **LazyInit**
- `LazyInit` é um caso, pouco comum, onde a falha do CAS não implica fazer uma nova tentativa, pois indica que foi atingido um estado final inalterável; isto é, a instância do recurso subjacente foi criada por uma das _threads_ que o solicitaram e, a partir disso, todas as outras _threads_ vão obter essa mesma instância.
```Java	
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;
	
public final class LazyInit<E> {
	private final Supplier<E> supplier;
	private final Consumer<E> cleanup;
	 
	private AtomicReference<E> resource;
	
	public LazyInit(Supplier<E> supplier, Consumer<E> cleanup) {
		this.supplier = supplier;
		this.cleanup = cleanup;
		resource = new AtomicReference<>(null);
	}
	
	// returns the instance of E, creating it at first time
	public E getInstance() {
		// step 1
		E observedResource = resource.get();
		
		// step 2
		if (observedResource != null)
			// outcome 2.iii
			return observedResource;
		
		// outcome 2.i
		E updatedResource = supplier.get();
		
		// step 3.
		if (resource.compareAndSet(null, updatedResource))
			// outcome 3.i
			return updatedResource;
		
		// outcome 3.iii: do cleanup, if sepecified, and return the resource
		// created by some other thread
		
		if (cleanup != null)
			cleanup.accept(updatedResource);
		
		return resource.get();
	}
}
```

### **SpinSemaphore**
- `SpinSemaphore`, uma implementação de um semáforo, em que espera é feita com _spin wait_, usando um algoritmo _nonblocking_ será:
```Java
import java.util.concurrent.atomic.AtomicInteger;

public class SpinSemaphore {
	// shared mutable state: the number of permits available
	private final AtomicInteger permits;
	private final int maxPermits;
	
	public SpinSemaphore(int initialPermits, int maxPermits) {
		if (initialPermits < 0 || initialPermits > maxPermits)
			throw new IllegalArgumentException();
		permits = new AtomicInteger(initialPermits);
		this.maxPermits = maxPermits;
	}
	
	public SpinSemaphore(int initialPermits) { this(initialPermits, Integer.MAX_VALUE); }
		
	// acquire the specified number of permits
	public boolean acquire(int acquires, long millisTimeout) {
		TimeoutHolder th = new TimeoutHolder(millisTimeout);
		
		while (true) {
			// step 1.
			int observedPermits = permits.get();	// must be a volatile read in order to get the most recent value
			// step 2
			if (observedPermits >= acquires) {
				// outcome 2.i
				int updatedPermits = observedPermits - acquires;
				// step 3
				if (permits.compareAndSet(observedPermits, updatedPermits))
					return true;	// outcome 3.i
				else
					// outcome 3.ii
					continue;
			}
			// outcome 2.ii
			if (th.value() <= 0)
				return false;
			Thread.yield();			// yield processor before retry
		}
	}
	
	// try to acquire one permit immediately
	public boolean tryAcquire(int acquires) { return acquire(acquires, 0L); }
	
	// releases the speciified number of permits, checking maximum value and overflow
	public void release(int releases) {
		while (true) {
			// step 1
			int observedPermits = permits.get();
			// step 2
			int updatedPermits = observedPermits + releases;
			if (updatedPermits > maxPermits || updatedPermits < observedPermits)
				// outcome 2.ii
				throw new IllegalStateException();
			// outcome 2.i
			// step 3
			if (permits.compareAndSet(observedPermits, updatedPermits))
				// outcome 3.i
				return;
			// outcome 3.ii
		}
	}
	
	// releases the speciified number of permits, unchecked
	public void uncheckedRelease(int releases) {
		// this is an unconditionl operation, so we can use the method AtomicInteger.addAndGet()
		permits.addAndGet(releases);
	}
}
```

### **TreiberStack**
- Os _stacks_ são as estruturas de dados ligadas mais simples que existem: cada elemento refere-se apenas a outro elemento e cada elemento é referido apenas por uma única referência. A classe `TreiberStack`, apresentada a seguir, mostra com se constrói um _stack_ usando referências atómicas.
```Java
public class TreiberStack<E> {
	// the node
	private static class Node<V> {
		Node<V> next;	// next node
		final V item;
			
		Node(V item) {
			this.item = item;
		}
	}
	
	// the stack top
	private final AtomicReference<Node<E>> top = new AtomicReference<>(null);
	
	// push an item onto stack
	public void push(E item) {
		Node<E> updatedTop = new Node<E>(item);
		while (true) {
			// step 1
			Node<E> observedTop = top.get();	// volatile read
			// step 2.i - link the new top node to the previous top node
			updatedTop.next = observedTop;
			// step 3.
			if (top.compareAndSet(observedTop, updatedTop))	// volatile write
				// outcome 3.i
				break;
			// outcome 3.ii
		}
	}
	
	// try to pop an item from the stack
	public E tryPop() {
		Node<E> observedTop;
		while (true) {
			// step 1
			observedTop = top.get();	// volatile read
			// step 2
			if (observedTop == null)
				// outcome 2.ii: the stack is empty
				return null;
			// outcome 2.i - compute the updated stack top
			Node<E> updatedTop = observedTop.next;
			// step 3.
			if (top.compareAndSet(observedTop, updatedTop))	// volatile write
				// outcome 3.i: success
				break;
			// outcome 3.ii: retry
		}
		return observedTop.item;
	}
}
```

### **MichaelScottQueue**
- Uma _linked queue_ é mais complicada que o _stack_ porque deve suportar acesso rápido a ambos os extremos, cabeça e cauda. Para fazer isto, é necessário manter ponteiros para os nós que se encontram à cabeça na cauda da fila.
```Java
public class MichaelScottQueue<E> {

	// the queue node
	private static class Node<V> {
		final AtomicReference<Node<V>> next;
		final V data;

		Node(V data) {
			next = new AtomicReference<Node<V>>(null);
			this.data = data;
		}
	}

	// the head and tail references
	private final AtomicReference<Node<E>> head;
	private final AtomicReference<Node<E>> tail;

	public MichaelScottQueue() {
		Node<E> sentinel = new Node<E>(null);
		head = new AtomicReference<Node<E>>(sentinel);
		tail = new AtomicReference<Node<E>>(sentinel);
	}

	// enqueue a datum
	public void enqueue(E data) {
		Node<E> newNode = new Node<E>(data);

		while (true) {
			Node<E> observedTail = tail.get();
			Node<E> observedTailNext = observedTail.next.get();
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
	public E tryDequeue() {
		// TODO
	}
}
```

### **TreiberStack**
- A listagem seguinte ilustra uma parte do algoritmo usado pelo `TreiberStack`, mas a implementação é um pouco diferente da apresentada anteriormente. Em vez de representar `top` com uma referência atómica, usa-se uma simples referência _volatile_ e fazem-se as actualizações atómicas usando a funcionalidade da classe `AtomicReferenceFiedUpdater`, cuja implementação se baseia em reflexão.
```Java
 public class TreiberStack<E> {
 	// the node
 	private static class Node<V> {
 		Node<V> next;
 		final V item;
			
 		Node(V item) {
 			this.item = item;
 		}
 	}
	
 	// the stack top
 	private volatile Node<E> top = null;
	
 	// the atomic field updater that allows execute atomic operation on the "top" volatile field
 	private static AtomicReferenceFieldUpdater<TreiberStack, Node> topUpdater =
 		AtomicReferenceFieldUpdater.newUpdater(TreiberStack.class, Node.class, "top");
	
 	// push an item onto stack
 	public void push(E item) {
 		Node<E> updatedTop = new Node<E>(item);
 		while (true) {
 			// step 1
 			Node<E> observedTop = top;
 			// step 2.i - link the new top node to the previous top node
 			updatedTop.next = observedTop;
 			// step 3.
 			if (topUpdater.compareAndSet(this, observedTop, updatedTop))
 				// outcome 3.i
 				break;
 			// outcome 3.ii
 		}
 	}
	...
 }
  ```

---

## O Problema ABA
- O problema ABA é uma anomalia que pode surgir com o uso ingénuo do _compare-and-swap_ em algoritmos onde os nós das estruturas ligadas possam ser reciclados (principalmente em ambientes sem _garbage collection_).
- Este problema consiste em:
    - 1 - value = `A`
    - 2 - thread 1 prepara para fazer CAS(expected: `A`, newValue: `B`)
    - 3 - thread 2 faz CAS(expected: `A`, newValue: `B`)
    - 4 - thread 2 faz CAS(expected: `B`, newValue: `A`)
    - 5 - thread 1 nao se apercebe da alteracao do valor quando for fazer CAS(expected: `A`, newValue: `B`)

- A classe `AtomicStampedReference` (e a sua prima `AtomicMarkableReference`) providencia actualizações atómicas num par de variáveis. `AtomicStampedReference` actualiza um par referência-inteiro, permitindo referências "versionadas" que são imunes ao problema ABA (embora teoricamente o contador possa dar a volta).

---

# _Optimização em Sincronizadores Implementados com Base em Monitor_
- Para aplicar esta optimização nos _fast-path_ da operações com as semânticas _acquire_ e _release_ em sincronizadores implementados com base em monitor, é necessário que se verifiquem as duas condições seguintes:
	- A implementação do sincronizador deve ser feita usando o "estilo monitor";
	- Ser possível implementar as operações elementares `tryAcquire` e `doRelease` usando técnicas _nonblocking_.

## _Semáforo_
```Java
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

public final class Semaphore {

	private final AtomicInteger permits;
	private volatile int waiters;
	private final Lock lock;
	private final Condition okToAcquire;
	 
	// Constructor
	public Semaphore(int initial) {
		if (initial < 0)
			throw new IllegalArgumentException();
		lock = new ReentrantLock();
		okToAcquire = lock.newCondition();
		permits = new AtomicInteger(initial);
	}
	
	public Semaphore() { this(0); }
	
	// tries to acquire one permit
	public boolean tryAcquire() {
		while (true) {
			int observedPermits = permits.get(); 
			if (observedPermits == 0)
				return false;
			if (permits.compareAndSet(observedPermits, observedPermits - 1))
				return true;
		}
	}
	
	// releases the specified number of permits
	private void doRelease(int releases) {
		permits.addAndGet(releases);
		// Java guarantees that this write is visible before any subsequent reads
	}
	
	// Acquire one permit from the semaphore
	public boolean acquire(long timeout, TimeUnit unit) throws InterruptedException {
		// try to acquire one permit, if available
		if (tryAcquire())
			return true;
		
		// no permits available; if a null time out was specified, return failure.
		if (timeout == 0)
			return false;

		// if a time out was specified, get a time reference
		boolean timed = timeout > 0;
		long nanosTimeout = timed ? unit.toNanos(timeout) : 0L;
		
		lock.lock();
		try {
			
			// the current thread declares itself as a waiter..
			waiters++;
			/**
			 * Java: JMM guarantees non-ordering of previous volatile write of "waiters"
			 * with the next volatile read of "permits"
			 */
			try {		
				do {
					// after increment waiters, we must recheck if acquire is possible!
					if (tryAcquire())
						return true;
					// check if the specified timeout expired
					if (timed && nanosTimeout <= 0)
						return false;
					if (timed)
						nanosTimeout = okToAcquire.awaitNanos(nanosTimeout);
					else
						okToAcquire.await();
				} while (true);
			} finally {
				// the current thread is no longer a waiter
				waiters--;
			}	
		} finally {
			lock.unlock();
		}
	}
	
	public void acquire() throws InterruptedException {
		acquire(-1, TimeUnit.MILLISECONDS);
	}

	public boolean acquire(int timeoutMillis) throws InterruptedException {
		return acquire(timeoutMillis, TimeUnit.MILLISECONDS);
	}
	
	// Release the specified number of permits
	public void release(int releases) {
		doRelease(releases);	// this has volatile write semantics so, it is visible before read waiters
		if (waiters > 0) {	
			lock.lock();
			try  {
				// We must recheck waiters, after enter the monitor in order
				// to avoid unnecessary notifications 
				if (waiters > 0) {
					if (waiters == 1 || releases == 1)
						okToAcquire.signal(); // only one thread can proceed execution
					else
						okToAcquire.signalAll(); // more than only one thread can proceed  execution
				}
			} finally {
				lock.unlock();
			}
		}
	}

	// Release one permit
	public void release() { release(1); }
}
```

## _Manual-Reset Event_
- A seguir apresenta-se a implementação em C# de um _manual-reset event_.
- Esta implementação ilustar em duas situações a necessidade de invocar o método `Interlocked.Memory` para garantir imediatamente a visibilidade a todos os processadores das escritas nas variáveis `signaled` e `waiters`.
```C#
using System;
using System.Threading;

public sealed class ManualResetEventSlim_ {
	private volatile bool signaled;		// true when the event is signaled
	private volatile int waiters;		// the current number of waiter threads - atomicity granted by monitor
	private int setVersion;				// the version of set operation - atomicty granted by monitor
	private readonly object monitor;
	
	// Constructor
	public ManualResetEventSlim_(bool initialState) {
		monitor = new object();
		signaled = initialState;
	}
	
	// return true when tha Wait must return
	private bool tryAcquire() { return signaled; }
	
	// set signaled to true and make it visible to all processors
	private void DoRelease() {
		signaled = true;
		/**
		 * In order to guarantee that this write is visible to all processors, before
		 * any subsequente read, notably the volatile read of "waiters" we must
		 * interpose a full-fence barrier.
		 */
		Interlocked.MemoryBarrier();
	}
	
	// Wait until the event is signalled
	public bool Wait(int timeout = Timeout.Infinite) {
	
		// If the event is signalled, return true
		if (tryAcquire())
			return true;
		
		// the event is not signalled; if a null time out was specified, return failure.
		if (timeout == 0)
			return false;

		// if a time out was specified, get a time reference
		TimeoutHolder th  = new TimeoutHolder(timeout);
		
		lock(monitor) {
		
			// get the current setVersion and declare the current thread as a waiter.						
			int sv = setVersion;
			waiters++;
			
			/**
			 * before we read the "signaled" volatile variable, we need to make sure that the increment
			 * of *waiters* is visible to all processors.
			 * In .NET this means interpose a full-fence memory barrier.
			 */			
			Interlocked.MemoryBarrier();
			
			try {
				/**
			 	 * after declare this thread as waiter, we must recheck the "signaled" in order
			 	 * to capture a check that ocorred befor we increment the waiters.
			 	 */
				if (tryAcquire())
					return true;

				// loop until the event is signalled, the specified timeout expires or
				// the thread is interrupted.
				do {				
					// check if the wait timed out
					if ((timeout = th.Value) == 0)
						// the specified time out elapsed, so return failure
						return false;
				
					Monitor.Wait(monitor, timeout);
				} while (sv == setVersion);
				return true;
			} finally {
				// at the end, decrement the number of waiters
				waiters--;
			}
		}
	}
		
	// Set the event to the signalled state
	public void Set() {
		DoRelease();
		// after set the "signaled" to true and making sure that it is visble to all
		// processors, check if there are waiters
		if (waiters > 0) {		
			lock(monitor) {
				// We must recheck waiters after acquire the lock in order
				// to avoid unnecessary notifications
				if (waiters > 0) {
					setVersion++;
					Monitor.PulseAll(monitor);
				}
			}
		}
	}

	// Reset the event
	public void Reset() { signaled = false; }
}
```