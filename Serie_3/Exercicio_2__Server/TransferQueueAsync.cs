using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Exercicio_2__Server {
    class TransferQueueAsync<T> where T : class {

        // Base type for the two async requests
        private class AsyncRequest<V> : TaskCompletionSource<V> {
            internal readonly CancellationToken cToken;     // cancellation token
            internal CancellationTokenRegistration cTokenRegistration;  // used to dispose the cancellation handler 
            internal Timer timer;
            internal bool done;     // true when the async request is completed or canceled

            internal AsyncRequest(CancellationToken cToken) : base() {
                this.cToken = cToken;
            }

            /**
             * Disposes resources associated with this async acquire.
             *
             * Note: when this method is called we are sure that the field "timer" is correctly affected,
             *		 but we are not sure if the "cTokenRegistration" field is.
             * 		 However, this does not cause any damage, because when this method is called by
             *	     cancellation handler this field is not used, as the resources mobilized to register
             *		 the handler are released after its invocation.
             */
            internal void Dispose(bool canceling = false) {
                if (!canceling && cToken.CanBeCanceled)
                    cTokenRegistration.Dispose();
                timer?.Dispose();
            }
        }

        // Type used by async take requests
        private class AsyncTake : AsyncRequest<T> {
            internal AsyncTake(CancellationToken cToken) : base(cToken) { }
        }

        // Type used by async transfer take request
        private class AsyncTransfer : AsyncRequest<bool> {
            internal AsyncTransfer(CancellationToken cToken) : base(cToken) { }
        }

        // Type used to hold each message sent with put or with transfer 
        private class Message {
            internal readonly T message;
            internal readonly AsyncTransfer transfer;   // reference to this AsyncTransfer when message was sent with transfer, null otherwise.

            internal Message(T message, AsyncTransfer transfer = null) {
                this.message = message;
                this.transfer = transfer;
            }
        }

        // Queue of messages pending for reception
        private readonly LinkedList<Message> pendingMessages;

        // Queue of pending async take requests
        private readonly LinkedList<AsyncTake> asyncTakes;

        // The lock - we do not use the monitor functionality
        private readonly object theLock = new object();

        /**
	     * Delegates used as cancellation handlers for asynchrounous requests 
	     */
        private readonly Action<object> cancellationHandler;
        private readonly TimerCallback timeoutHandler;


        //////////////////////////////////////////////////////////////////


        public TransferQueueAsync() {
            /**
             * 'cancelation token', 'cancelamento do transfer', 'cancelamento do take por timeout'
             * fazer metodo generico para cancelar o 'take' e o 'transfer' e depois passam um param se é cancelamento por 'timeout' ou por 'cancelation token'
             *      a logica é igual, mas o que vao fazer ao codigo é diferente
             *      
             * aula 25 (semaphore)
             */
            
            cancellationHandler = new Action<object>((acquireNode) => AcquireCancellationHandler(acquireNode, true));
            timeoutHandler = new TimerCallback((acquireNode) => AcquireCancellationHandler(acquireNode, false));
            pendingMessages = new LinkedList<Message>();
            asyncTakes = new LinkedList<AsyncTake>();
        }

        public Task<T> TakeAsync(int timeout = Timeout.Infinite, CancellationToken cToken = default(CancellationToken)) {
            lock (theLock) {
                if (pendingMessages.Count > 0) {
                    Message msg = pendingMessages.First.Value;
                    T message = msg.message;
                    if (msg.transfer != null) {
                        msg.transfer.done = true;
                        return Task.FromResult(message);
                    }
                }
                
                // if timeout reached, return completed task with 'null'.
                if (timeout <= 0)
                    return Task.FromResult<T>(null);

                // If the cancellation was already requested return a a completed task in
                // the Canceled state
                if (cToken.IsCancellationRequested)
                    return Task.FromCanceled<T>(cToken);

                // Create a request node and insert it in requests queue
                AsyncTake take = new AsyncTake(cToken);
                LinkedListNode<AsyncTake> takeNode = asyncTakes.AddLast(take);

                /**
                 * Activate the specified cancelers when owning the lock.
                 */

                /**
                 * Since the timeout handler, that runs on a thread pool's worker thread,
                 * that acquires the lock before access the fields "take.timer" and
                 * "take.cTokenRegistration" these assignements will be visible to the
                 * timeout handler.
                 */
                if (timeout != Timeout.Infinite)
                    take.timer = new Timer(timeoutHandler, takeNode, timeout, Timeout.Infinite);

                /**
                 * If the cancellation token is already in the canceled state, the cancellation
                 * handler will run immediately and synchronously, which *causes no damage* because
                 * this processing is terminal and the implicit locks can be acquired recursively.
                 */
                if (cToken.CanBeCanceled)
                    take.cTokenRegistration = cToken.Register(cancellationHandler, takeNode);

                // Return the Task<T> that represents the async take
                return take.Task;
            }
        }
    }
}
