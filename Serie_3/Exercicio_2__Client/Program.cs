using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Exercicio_2__Client {
    class Program {
        static void Main(string[] args) {
            Console.WriteLine("Client Text input: ");
            string input = Console.ReadLine();
            string [] text;
            if (input.Equals("")) {
                text = new string[0];
            }
            else {
                text = new string[] { input };
            }
            JsonEchoClient.Main(text).Wait();
        }
    }
}
