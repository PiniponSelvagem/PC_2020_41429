using System;

namespace Exercicio_1 {
    class Program {
        static void Main(string[] args) {
            int max = 8;
            Console.WriteLine("Running testComputeAsync() {0} times...\n", max);

            for (int i = 0; i < max; ++i) {
                Exercicio1.testComputeAsync(i);
            }

            Console.WriteLine("Press Enter to terminate...");
            Console.Read();
        }
    }
}
