using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;

namespace Exercicio_2__Client {
    /**
     * Type that representes a JSON response
     */
    public class Response {
        public int Status { get; set; }
        public Dictionary<String, String> Headers { get; set; }
        public JObject Payload { get; set; }
    }
}
