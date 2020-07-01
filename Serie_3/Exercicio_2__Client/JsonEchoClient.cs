/**
 * ISEL, LEIC, Concurrent Programming
 *
 * TCP echo server using JSON request/responses.
 *
 * Note: When using Visual Studio, add the Newtonsoft.Json package, consulting:
 *    https://docs.microsoft.com/en-us/nuget/quickstart/install-and-use-a-package-in-visual-studio
 * 
 * Carlos Martins, June 2020
 */

using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using System.IO;
using System.Net.Sockets;
using System.Diagnostics;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Exercicio_2__Client {

    /**
     * Client for a JSON echo server.
     */
    class JsonEchoClient {
        private const int SERVER_PORT = 13000;
        private static volatile int requestCount = 0;

        // The JSON serializer object.
        private static JsonSerializer serializer = new JsonSerializer();

        /**
         * Send a request to the server and display its response.
         */
        static async Task SendRequestAndReceiveResponseAsync(string server, RequestPayload payload) {
            /**
             * Create a TcpClient socket in order to connect to the echo server.
             */
            using (TcpClient connection = new TcpClient()) {
                try {
                    // Start a stop watch timer
                    Stopwatch sw = Stopwatch.StartNew();

                    // connect socket to the echo server.
                    await connection.ConnectAsync(server, SERVER_PORT);

                    // Create and fill the Request with "payload" as Payload
                    Request request = new Request {
                        Method = $"echo-{payload.Number}",
                        Headers = new Dictionary<String, String>(),
                        Payload = (JObject)JToken.FromObject(payload),
                    };

                    // Add some headers for test purposes 
                    request.Headers.Add("agent", "json-client");
                    request.Headers.Add("timeout", "10000");

                    /**
                     * Translate the message to JSON and send it to the echo server.
                     */
                    JsonTextWriter writer = new JsonTextWriter(new StreamWriter(connection.GetStream()));
                    serializer.Serialize(writer, request);
                    Console.WriteLine($"-->{payload.ToString()}");
                    await writer.FlushAsync();

                    /**
                     * Receive the server's response and display it.
                     */
                    JsonTextReader reader = new JsonTextReader(new StreamReader(connection.GetStream())) {
                        // Configure reader to support reading multiple top-level objects
                        SupportMultipleContent = true
                    };
                    try {
                        // Consume any bytes until start of JSON object ('{')
                        do {
                            await reader.ReadAsync();
                        } while (reader.TokenType != JsonToken.StartObject &&
                                 reader.TokenType != JsonToken.None);
                        if (reader.TokenType == JsonToken.None) {
                            Console.WriteLine("***error: reached end of input stream, ending.");
                            return;
                        }

                        /**
                         * Read the response JSON object
                        */
                        JObject jresponse = await JObject.LoadAsync(reader);
                        sw.Stop();

                        /**
                         * Back to the .NET world
                         */
                        Response response = jresponse.ToObject<Response>();
                        request = response.Payload.ToObject<Request>();
                        RequestPayload recoveredPayload = request.Payload.ToObject<RequestPayload>();
                        Console.WriteLine($"<--{recoveredPayload.ToString()}, elapsed: {sw.ElapsedMilliseconds} ms");
                    }
                    catch (JsonReaderException jre) {
                        Console.WriteLine($"***error: error reading JSON: {jre.Message}");
                    }
                    catch (Exception e) {
                        Console.WriteLine($"-***error: exception: {e.Message}");
                    }
                    sw.Stop();
                    Interlocked.Increment(ref requestCount);
                }
                catch (Exception ex) {
                    Console.WriteLine($"--***error:[{payload}] {ex.Message}");
                }
            }
        }

        /**
         * Send continuously batches of requests until a key is pressed.
         */
        private const int REQS_PER_BATCH = 10;

        // use explicitly created tasks
        public static async Task Main(string[] args) {
            bool executeOnce = false;
            string text = (args.Length > 0) ? args[0] : "--default text--";

            Task[] requests = new Task[REQS_PER_BATCH];
            Stopwatch sw = Stopwatch.StartNew();
            do {
                for (int i = 0; i < REQS_PER_BATCH; i++)
                    requests[i] = SendRequestAndReceiveResponseAsync("localhost", new RequestPayload { Number = i, Text = text });
                await Task.WhenAll(requests);
            } while (!(executeOnce || Console.KeyAvailable));
            Console.WriteLine($"--completed requests: {requestCount} / {sw.ElapsedMilliseconds} ms");
        }
    }
}
