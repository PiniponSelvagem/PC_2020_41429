using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Exercicio_2__Client {
    /**
     * Represents the payload of the request message
     */
    public class RequestPayload {
        public int Number { get; set; }
        public String Text { get; set; }

        public override String ToString() {
            return $"[ Number: {Number}, Text: {Text} ]";
        }
    }
}
