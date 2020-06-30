using System;
using System.Threading;
using System.Threading.Tasks;

namespace Serie3 {
    class Exercicio1 {

        public static void start() {
            testComputeAsync();     // Test solution proposed for our problem, might run a couple of times to see multiple cases since its using random
        }
        
        
        public static void testComputeAsync() {
            Console.WriteLine(" --- COMPUTEASYNC TEST ---");

            string[] elems = {"a", "bc", "def", "ghij", "jihg", "fed", "cd", "a"};

            // the source of cancellation
            CancellationTokenSource cts = new CancellationTokenSource();
            // task receives the underlying CancellationToken
            CancellationToken ctoken = cts.Token;
            Task<int[]> task = ComputeAsync(elems, 3, ctoken);
            
            try {
                int[] computed = task.Result;
                for (int i = 0; i<computed.Length; ++i) {
                    Console.WriteLine(computed[i] + " -> " + elems[i]);
                }
            } catch(AggregateException errors) {
                foreach (Exception error in errors.Flatten().InnerExceptions)
                    Console.WriteLine("{0}: {1}", error.GetType().Name, error.Message);
            }
            //Console.WriteLine("{0}, retry number: {1} - {2}", e.Message, i+1, argument);
                        
            Console.WriteLine();
        }



        internal class CommException : Exception {
            public CommException(string argument, string message = "communication error when computing: ") : base(message + argument) { }
        }

        internal class InvalidMaxRetriesException : Exception {
            public InvalidMaxRetriesException(string message = "invalid maxRetries, it should be maxRetries >= 1") : base(message) { }
        }

        const int MIN_OPER_TIME = 100;
        const int MAX_OPER_TIME = 1000;

        static async Task<int> OperAsync(string argument, CancellationToken ctoken) {
            Random rnd = new Random(Environment.TickCount);

            try {
                await Task.Delay(rnd.Next(MIN_OPER_TIME, MAX_OPER_TIME), ctoken);
                if (rnd.Next(0, 100) >= 50) {
                    throw new CommException(argument);
                }
                return argument.Length;
            } catch (OperationCanceledException e) {
                Console.WriteLine("*** delay canceled ***");
                throw;
            }
        }
        
        static async Task<int> OperRetryAsync(string argument, int maxRetries, CancellationTokenSource lcts) {
            Exception ex = new InvalidMaxRetriesException();
            for (int i=0; i<maxRetries; ++i) {
                try {
                    return await OperAsync(argument, lcts.Token);
                } catch (CommException e) {
                    ex = e;
                }
            }

            lcts.Cancel();
            throw ex;
        }

        static async Task<int[]> ComputeAsync(string[] elems, int maxRetries, CancellationToken ctoken) {
            CancellationTokenSource linkedCts = CancellationTokenSource.CreateLinkedTokenSource(ctoken);
            Task<int>[] tasks = new Task<int>[elems.Length];

            for (int i=0; i<elems.Length; ++i) {
                string arguement = elems[i];
                tasks[i] = OperRetryAsync(arguement, maxRetries, linkedCts);
            }

            return await Task.WhenAll(tasks);
        }
    }
}
