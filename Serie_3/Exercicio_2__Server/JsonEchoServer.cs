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
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Exercicio_2__Server {

    /**
     * The echo TCP server
     */
    class JsonEchoServer {
        private const int SERVER_PORT = 13000;

        private const int MIN_SERVICE_TIME = 10;
        private const int MAX_SERVICE_TIME = 500;

        // The JSON serializer object
        private static readonly JsonSerializer serializer = new JsonSerializer();

        // Process a client's connection
        private static async Task ServeConnectionAsync(int id, TcpClient connection, int service_time) {
            using (connection) {
                var stream = connection.GetStream();
                var reader = new JsonTextReader(new StreamReader(stream)) {
                    // Support reading multiple top-level objects
                    SupportMultipleContent = true
                };
                var writer = new JsonTextWriter(new StreamWriter(stream));
                try {
                    // Consume any bytes until start of object character ('{')
                    do {
                        await reader.ReadAsync();
                        //Console.WriteLine($"advanced to {reader.TokenType}");
                    } while (reader.TokenType != JsonToken.StartObject &&
                             reader.TokenType != JsonToken.None);

                    if (reader.TokenType == JsonToken.None) {
                        Console.WriteLine($"[{id}] reached end of input stream, ending.");
                        return;
                    }

                    // Load root JSON object
                    JObject json = await JObject.LoadAsync(reader);

                    // Retrive the Request object, and show its Method and Headers fields
                    Request request = json.ToObject<Request>();
                    Console.WriteLine($"Request {{\n  Method: {request.Method}");
                    Console.Write("  Headers: { ");
                    if (request.Headers != null) {
                        int i = 0;
                        foreach (KeyValuePair<String, String> entry in request.Headers) {
                            Console.Write($"{entry.Key}: {entry.Value}");
                            if (i < request.Headers.Count - 1)
                                Console.Write(", ");
                            i++;
                        }
                    }
                    Console.WriteLine(" }\n}");

                    // Simulate the service time
                    await Task.Delay(service_time);

                    // Build the response and send it to the client
                    var response = new Response {
                        Status = 200,
                        Payload = json,
                    };
                    serializer.Serialize(writer, response);
                    await writer.FlushAsync();
                }
                catch (JsonReaderException e) {
                    Console.WriteLine($"[{id}] Error reading JSON: {e.Message}, continuing");
                    var response = new Response { Status = 400, };
                    serializer.Serialize(writer, response);
                    await writer.FlushAsync();
                    // close the connection because an error may not be recoverable by the reader
                    return;
                }
                catch (Exception e) {
                    Console.WriteLine($"[{id}] Unexpected exception, closing connection {e.Message}");
                    return;
                }
            }
        }

        /**
         * Listen for connections, but without parallelizing multiple-connection processing
         */
        public static async Task ListenAsync(TcpListener listener) {
            int connId = 0;
            Random random = new Random(Environment.TickCount);
            listener.Start();
            Console.WriteLine($"Listening on port {SERVER_PORT}");
            do {
                try {
                    TcpClient connection = await listener.AcceptTcpClientAsync();
                    connId++;
                    Console.WriteLine($"--connection accepted with id: {connId}");
                    await ServeConnectionAsync(connId, connection, random.Next(MIN_SERVICE_TIME, MAX_SERVICE_TIME));
                }
                catch (ObjectDisposedException) {
                    // Exit the method normally, the listen socket was closed
                    break;
                }
                catch (InvalidOperationException) {
                    break;  // When AceptTcpClienteAsync() is calleda after Stop()
                }
            } while (true);
        }

        /**
         * Execute server and wait for <enter> to shutdown.
         */
        public static async Task Main() {
            var listener = new TcpListener(IPAddress.Loopback, SERVER_PORT);
            var listenTask = ListenAsync(listener);
            Console.Write("---hit <enter> to shutdown the server...");
            await Console.In.ReadLineAsync();
            // Shutdown the server graciously
            listener.Stop();
            // Wait until all accepted connections are served
            await listenTask;
        }
    }
}