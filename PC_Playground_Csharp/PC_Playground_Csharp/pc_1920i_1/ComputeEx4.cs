using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace PC_Playground_Csharp.pc_1920i_1 {
    class ComputeEx4 {

        /*
        public R[] Compute(T[] elems) {
            var res = R[elems.Length];
            for (int i=0; i<elems.Length; ++i) {
                res[i] = Oper(elems[i]);        //OperAsync(maxRetries, elems[i]);
            }
            return res;
        }
        */

        internal class CommException : Exception {
            public CommException(string item, string message = "communication error when computing: ") : base(message + item) { }
        }

        internal class InvalidMaxRetriesException : Exception {
            public InvalidMaxRetriesException(string message = "invalid maxRetries, it should be maxRetries >= 1") : base(message) { }
        }


        public async Task<int[]> ComputeAsync(string[] elems, int maxRetries, CancellationToken cToken) {
            CancellationTokenSource linkedCts = CancellationTokenSource.CreateLinkedTokenSource(cToken);
            Task<int>[] tasks = new Task<int>[elems.Length];

            for (int i = 0; i<elems.Length; ++i) {
                tasks[i] = OperRetryAsync(elems[i], maxRetries, linkedCts);
            }

            return await Task.WhenAll(tasks);
        }

        private async Task<int> OperRetryAsync(string item, int maxRetries, CancellationTokenSource lcts) {
            Exception ex = new InvalidMaxRetriesException();

            for (int i = 0; i<maxRetries; ++i) {
                return await OperAsync(item, lcts.Token);
            }

            lcts.Cancel();
            throw ex;
        }
        
        const int MIN_OPER_TIME = 100;
        const int MAX_OPER_TIME = 1000;
        private async Task<int> OperAsync(string item, CancellationToken cToken) {
            Random rnd = new Random(Environment.TickCount);

            try {
                await Task.Delay(rnd.Next(MIN_OPER_TIME, MAX_OPER_TIME), cToken);
                if (rnd.Next(0, 100) >= 50) {
                    throw new CommException(item);
                }
                return item.Length;
            }
            catch (OperationCanceledException ex) {
                Console.WriteLine("*** delay canceled ***");
                throw;
            }
        }
    }
}
