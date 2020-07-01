using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;

namespace Exercicio_2__Client {
    /**
     * Type that represents a JSON request
     */
    public class Request {
        public String Method { get; set; }
        public Dictionary<String, String> Headers { get; set; }
        public JObject Payload { get; set; }

        public override String ToString() {
            return $"Method: {Method}, Headers: {Headers}, Payload: {Payload}";
        }
    }
}
