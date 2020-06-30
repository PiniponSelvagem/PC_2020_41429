
# Tasks

.NET _Framework Task Parallel Library_ (TPL)

- Os principais tipos definidos pela TPL são:

	- `Task`, `Task<TResult>`, `TaskCompletionSource<TResult>`, `TaskCreationOptions` e `TaskStatus`

	- `TaskFactory` e `TaskFactory<TResult>`
	
	- `TaskCanceledException`

	- `TaskScheduler`, ConcurrentExclusiveSchedulerPair e `TaskSchedulerException`


## _Criação de Tasks_

```C#
for (int i = 0; i < 10; i++) {
	int capturedI = i;      // captura do closure. Se não, apenas um 'i' seria criado e a "referencia" passada às tasks vendo sempre 10.
	Task.Run(() => Console.WriteLine(capturedI));
}
Console.ReadLine();
```

- No ficheiro [tasks.cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/tree/master/src/tasks) está um programa de demonstração que ilustra as várias formas de criar _tasks_ assim como as duas forma de passar dados a uma nova _task_ (através de uma argumento do método `TaskFactory.StartNew` e através da captura de estado pela _closure_).

---

## _Retornando Dados de uma Task_

### Sem usar Tasks:
```C#
BigInteger n = 49000;
BigInteger r = 600;

BigInteger part1 = Factorial(n);
BigInteger part2 = Factorial(n - r);
BigInteger part3 = Factorial(r);

BigInteger chances = part1 / (part2 * part3);
Console.WriteLine(chances);
```

### Usando Tasks:
```C#
BigInteger n = 49000;
BigInteger r = 600;

Task<BigInteger> part1 = Task.Run(() => Factorial(n));
Task<BigInteger> part2 = Task.Run(() => Factorial(n - r));
Task<BigInteger> part3 = Task.Run(() => Factorial(r));

BigInteger chances = part1.Result / (part2.Result * part3.Result);
Console.WriteLine(chances);
```

- No ficheiro [returning-data.cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/blob/master/src/tasks/returning-data.cs) está um programa de demonstração que faz o cálculo das chances da lotaria de forma síncrona e de forma assíncrona medindo o tempo de cálculo nos dois casos.

---

## _Tratamento de Erros_

```C#
Task task = Task.Run(() => DoSomething());

// do something else

// synchronize with task outcome 
try {
	task.Wait();
} catch (AggregateException errors) {
	foreach (Exception error in errors.InnerExceptions)
		Console.WriteLine($"{error.getType().Name}: {error.Message}");
}
```

- Uma vez que uma excepção interna pode ter outra excepção agregada, requerendo outro nível de iteração. É melhor usar o `.Flatten()` para por todas as excepções ao mesmo nível.
```C#
catch (AggregateException errors) {
	foreach (Exception error in errors.Flatten().InnerExceptions)
		Console.WriteLine($"{error.GetType().Name}: {error.Message}");
}
```

- O tipo `AggregateException` também define o método `Handle` para auxiliar no tratamento de excepções reduzindo a quantidade de código que é necessário escrever. O método `Handle` aceita um _delegate_ predicado que é aplicado a cada uma das excepções agrupadas na `AggregateException`. O predicado deve devolver `true` se a excepção é pode ser tratada e `false` no caso contrário. No fim de processar todas as excepções, qualquer excepção não reconhecida como tratada será relançada de novo como parte de uma nova instância de `AggregateException` contendo apenas as excepções que foram consideradas não tratadas.

- Por exemplo, o seguinte excerto de código, ilusta uma situação em que queiramos ignorar a `TaskCanceledException` mas considerar as outra excepções eventualmente agrupadas na `AggregateException`.

```C#
...
var loopTask = ...;
try {
	long result = loopTask.Result;
		Console.WriteLine($"\n-- Successful execution of {result} loop iterations");
} catch (AggregateException ae) {
	try {
		ae.Handle((ex) => {
			if (ex is TaskCanceledException) {
				Console.WriteLine($"\n** The task was cancelled by user with: \"{ex.Message}\"");
				return true;
			}
			return false;
		});
	} catch (AggregateException ae2) {
		foreach (Exception ex in ae2.Flatten().InnerExceptions)
			Console.WriteLine($"\n** Exception type: {ex.GetType().Name}: ; Message: {ex.Message}");
	}
}
...
```

- Os ficheiros [error-handling.cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/blob/master/src/tasks/error-handling.cs) e [cancellation.cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/blob/master/src/tasks/cancellation.cs) contêm código que faz o tratamento das excepções lançadas por _tasks_.

---

## _Cancelamento de Tasks_

- `CancellationTokenSource` e `CancellationToken`. Estes dois tipos coordenam o cancelamento.
- O tipo `CancellationTokenSource` é usado pela parte que pretende solicitar o cancelamento; o tipo `CancellationToken` é passado a cada operação assíncrona que se pretende poder ser cancelada.

```C#
public static void Main() {
	// the source of cancellation
	CancellationTokenSource cts = new CancellationTokenSource();
	// task receives the underlying CancellationToken
	CancellationToken ctoken = cts.Token;
				
	var loopTask = LoopRandomAsync(ctoken);
		
	while (!loopTask.IsCompleted) {
		if (Console.KeyAvailable && Console.ReadKey(true).Key == ConsoleKey.Q) {
			// cancel through CancellationTokenSource
			cts.Cancel();
		}
		Thread.Sleep(50);
	}
	...
}
```

- Para informar a TPL que uma operação assincrona responde ao cancelamento, a operação deve terminar lançando uma `OperationCancelledException` especificando o _cancellation token_ através do qual foi comunicado o cancelamento. Isto pode ser feito excplicitamente testando directamente a propriedade `CancellationToken.IsCancellationRequested` ou invocando  o método `CancellationToken.ThrowIfCancellationRequested` que lança a `OperationCancelledException` no caso se ter sido solicitado o cancelamento. O seguinte código exemplifica:

```C#
private static Task<int> LoopRandomAsync(CancellationToken ctoken) {
		
	return Task<int>.Run(() => {
		Random rnd = new Random(Environment.TickCount);
		int loopCount = rnd.Next(100);
			
		// 25% failures!
		if (loopCount > 75)
			throw new InvalidOperationException(loopCount.ToString() + " are too much loops!");
			
		Console.Write($"[{loopCount}]");
			
		for (int i = 0; i < loopCount; i++) {
				
			// ctoken.ThrowIfCancellationRequested();
			// or
			if (ctoken.IsCancellationRequested) {
				// do some necessary cleanup!
				throw new OperationCanceledException("LoopRandom task cancelled!", ctoken);
			}
			// show progress
			Console.Write('.');
			// yield processor for a random time between 10 and 100 ms 				
			Thread.Sleep(rnd.Next(10, 100));
		}
		return loopCount;
	}, ctoken);		// specify cancellation token
}
```

- Além do cancelamento conduzido pelo utilizador, é razoável que as operações assíncronas possa ser canceladas porque estão a demorar demasiado tempo a concluir. Quando criamos uma instância de `CancellationTokenSource` é possível especificar um período de tempo depois do qual o cancelamento é disparado automaticaente. Existe também um método, `CancelAfter`, que pode ser usado numa instância de `CancellationTokenSource` para definir um intervalo de tempo para solicitar o cancelamento após a criação.

```C#
public static void Main() {
	// the source of cancellation
	CancellationTokenSource cts = new CancellationTokenSource(2500);	// cancel automatically after 2500 ms
	// task receives the underlying CancellationToken
	CancellationToken ctoken = cts.Token;
				
	var loopTask = LoopRandomAsync(ctoken);
		
	while (!loopTask.IsCompleted) {
		if (Console.KeyAvailable && Console.ReadKey(true).Key == ConsoleKey.Q) {
			// cancel through CancellationTokenSource
			cts.Cancel();
		}
		Thread.Sleep(50);
	}
	...
}
```

---

## _Reportar Progresso de uma Task_

- Forma standard de reportar progresso através da interface `IProgress`

```C#
public interface IProgress<in T> {
	void Report(T value);
}
```

- No ficheiro [progress-report.cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/blob/master/src/tasks/progress-report.cs) encontra-se um programa que ilusta a utilização da interface `IProgress<T>` e da classe `Progress<T>`. 

---

## _Encadeamento de Tasks (continuation)_

```C#
Task<int> firstTask = Task.Run<int>(() => { Console.WriteLine("First Task"); return 42; });

Task secondTask = firstTask.ContinueWith(ascendent => Console.WriteLine($"Second Task, First task returned {ascendent.Result}"));

secondTask.Wait();
```

- Problema: `secondTask` irá executar sempre que `firstTask` acabar, mas poderemos querer que `secondTask` se execute apenas se `firstTask` terminou com successo, até ao fim.

```C#
// firstTask terminou até ao fim com successo
Task secondTask = firstTask.ContinueWith(ProcessResult, TaskContinuationOptions.OnlyOnRanToCompletion);

// firstTask lançou uma excepção nao tratada
Task errorHandler = firstTask.ContinueWith(ProcessResult, TaskContinuationOptions.OnlyOnFaulted);

secondTask.Wait();
```

- Exemplo de uma possiveis utilizacoes de continuacoes:

```C#
private static Task<string> DownloadWebPageAsync(string url) {
	WebRequest request = WebRequest.Create(url);
	Task<WebResponse> response = request.GetResponseAsync();
		
	return response.ContinueWith<string>(antecedent => {
		using (var reader = new StreamReader(antecedent.Result.GetResponseStream())) {
			return reader.ReadToEnd();
		}
	});
}
```

```C#
Task[] algoritmTasks = new Task[4];
for (int nTask = 0; nTask < algorithmTasks.Length; nTask++) {
	int partToProcess = nTask;
	algotithmTasks[nTask] = Task.Run(() => ProcessPart(partToProcess));
}
Task.Factory.ContinueWhenAll(algorithmTasks, antecedents = > ProduceSummary());
```

---

## _Tasks aninhadas e Tasks filhas_

- O codigo seguinte, tem poucas probabilidades de printar `"Nested..."`, pois a Task exterior pode terminar antes de a filha começar.

```C#
Task.Run(() => {
	Task nested = Task.Factory.StartNew(() => Console.WriteLine("Nested..."));
}).Wait();
```

- Resolução:

```C#
Task.Run(() => {
	Task child = Task.Factory.StartNew(() => Console.WriteLine("Nested..."), TaskCreationOptions.AttachToParent);
}).Wait();
```

### _Porquê usar Tasks filhas_

```C#
public Task ImportXmlFilesAsync(string dataDir, CancellationToken, ctoken) {
	return Tsk.Run(() => {
		foreach (FileInfo file in new DirectoryInfo(dataDir).GetFiles("*.xml")) {
			XElement doc = XElement.Load(file.FullName);
			InternalProcessXml(doc);
		}
	}, ctoken);
}
```

- Para melhorar o desempenho:

```C#
public Task ImportXmlFilesAsync(string dataDir, CancellationToken, ctoken) {
	return Tsk.Run(() => {
		foreach (FileInfo file in new DirectoryInfo(dataDir).GetFiles("*.xml")) {
			string fileToProcess = file.FullName;	// captured state..
			Task.Factory.StartNew(() => {
				// convenient point to check cancellation
			
				XElement doc = XElement.Load(fileToProcess);
				InternalProcessXml(doc, ctoken);
			}, ctoken, TaskCreationOptions.AttachedToParent);
		}
	}, ctoken);
}
```

- NO ficheiro [why-child-tasks-cs](https://github.com/carlos-martins/isel-leic-pc-s1920v-li51n/blob/master/src/tasks/why-child-tasks.cs) encontra-se um programa completo que usa esta técnica.

---

## _Task-based Asynchronous Pattern_ (TAP)

- Em aulas anteriores abordámos um dos modelos de invocacação assíncrona definidos pelo .NET _Framework_, designado _Asynchronous Programming Model_ (APM). Neste modelo a uma operação cuja API síncrona seja:

```C#
T Xxx(U u, ..., V v);
```

- Corresponde uma API assícrona definida pelos seguintes dois métodos:

```C#
IAsyncResult BeginXxx(U u, ..., V v, AsyncCallback completionCallback, object callbackState);

T EndXxx(IAsyncResult asyncResult);
```

- O método `BeginXxx` lança a operação assícrona e devolve um objecto que implementa a interface `IAsyncResult` que representa a operação assíncrona. O método `EndXxx` permite obter o resultado da operação assíncrona representada pelo valor do argumento `asyncResult`.

- O _rendezvous_ com a conclusão da operação assíncrona pode ser feito usando técnicas de _polling_ (usando a interface `IAsyncResult`) ou usando a técnica de _callback_ especificando um `completionCallback` na chamada ao método `BeginXxx`.

- Com a introdução da _Task Parallel Library_ (TPL), o .NET _Framework_ definiu um novo modelo de invocação assíncrona designado por _Task-based Asynchronous Pattern_ (TAP), que como o nome indica usa _tasks_ para representar as operações assíncronas. A API segundo o modelo TAP para a API síncrona anterior é:

```C#
Task<T> XxxAsync(U u, ..., V v);
```

- Existe apenas um método que recebe o nome do método síncrono correspondente com o sufixo `Async`. Este método em vez de devolver o resultado da operação assíncrona devolve uma instância de `Task<TResult>` que representa a operção assícrona em curso.

- O _rendezvous_ com a conclusão da operação assíncrona pode ser feita usando técnicas de _polling_ (usando a propriedade `Task.IsCompleted` ou `Task.Result` ou os métodos `Task.Wait`, `Task.WaitAll` ou `Task.WaitAny`) ou usando a técnica de _callback_ através do agendamento de continuação na _task_ que representa a operação assíncrona (usando os métodos `Task.ContinueWith`, `TaskFactory.ContinueWhenAny` ou `TaskFactory.ContinueWhenAny`).

- O modelo TAP é mais simples de usar do que o modelo APM pois não tem o problema da chamada síncrona ao _completion callback_ que pode ocorrer aquando da chamada ao método `BeginXxx`.

- Usando o tipo `TaskCompletionSource<TResult>` é simples implementar uma interface ao modelo TAP quando se dispões de uma interface segundo o modelo APM. Usando os método `BeginXxx` e `EndXxx`, definidos anteriormente, a implementação do método `XxxAsync` será:

```C#

// TAP asynchronous API
Task<T> XxxAsync(U u, ..., V v) {
	TaskCompletionSource<T> tcs = new TaskCompletionSource<T>;
		
	BeginXxx(u, ...,v, (asyncResult) => {
		try {
			tcs.SetResult(EndXxx(asyncResult));
		} catch (Exception ex) {
			tcs.SetException(ex);
		}
	}, null);
	
	return tcs.Task;
}
```

- O tipo `TaskFactory` define o método `TaskFactory.FromAsync` que permite implementar interfaces segundo o estilo TAP com base em interfaces segundo o estilo APM.

