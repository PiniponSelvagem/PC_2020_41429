using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Exercicio_2__Server {
    
    /*
     * ------------------------------------------------------------------------------
     * DEVELOPER NOTE
     * ------------------------------------------------------------------------------
     * 
     * Esta classe 'TransferQueueAsync' foi realizada em conjunto com o
     * meu colega Guilherme Salvador [43543] de PC.
     * Por essa razão deverá haver partes do código nesta classe que estarão
     * parecidas/iguais.
     * O meu colega foi quem estava a perceber melhor esta parte final da
     * matéria, 'Sincronizadores Async', e eu apenas limitei-me a tentar perceber
     * o código, a comentar, questionar e contra propor a direcção desta
     * implementação.
     * 
     * Posto isto, os exercícios das Série 1 e 2 mais o exercício 1 desta Série 3,
     * são da minha autoria e deste modo estarei mais a par das suas implementações.
     * 
     * ------------------------------------------------------------------------------
     */

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
            public T message;
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
        private readonly Action<object> cancellationTakeHandler;
        private readonly TimerCallback timeoutTakeHandler;

        private readonly Action<object> cancellationTransferHandler;
        private readonly TimerCallback timeoutTransferHandler;

        /**
	     *  Completed tasks use to return constant results from the TransferAsync method
	     */
        private static readonly Task<bool> trueTask = Task.FromResult<bool>(true);
        private static readonly Task<bool> falseTask = Task.FromResult<bool>(false);


        //////////////////////////////////////////////////////////////////


        public TransferQueueAsync() {
            // Construct delegates used to describe the cancellation handlers.
            cancellationTakeHandler = new Action<object>((node) => RequestCancellationTakeHandler(node, true));
            timeoutTakeHandler = new TimerCallback((node) => RequestCancellationTakeHandler(node, false));
            cancellationTransferHandler = new Action<object>((node) => RequestCancellationTransferHandler(node, true));
            timeoutTransferHandler = new TimerCallback((node) => RequestCancellationTransferHandler(node, false));

            // Initialize the shared mutable state
            pendingMessages = new LinkedList<Message>();
            asyncTakes = new LinkedList<AsyncTake>();
        }



        /**
	     * Auxiliary methods, related to 'Take'
	     */

        /**
         * Returns the list of all pending async takes that can be satisfied.
         *
         * Note: This method is called when the current thread owns the lock.
         */
        private List<AsyncTake> SatisfyPendingAsyncTakes() {
            List<AsyncTake> satisfied = null;
            while (asyncTakes.Count > 0) {
                AsyncTake take = asyncTakes.First.Value;

                // Check if available pendingmessages allow satisfy this async take
                if (pendingMessages.Count == 0)
                    break;

                // Remove the async take from the queue
                asyncTakes.RemoveFirst();

                // Update take and mark message as done
                Message msg = pendingMessages.First.Value;
                take.message = msg.message;
                if (msg.transfer != null) {
                    msg.transfer.done = true;
                }
                pendingMessages.RemoveFirst();
                take.done = true;

                // Add the async take to the result list
                if (satisfied == null)
                    satisfied = new List<AsyncTake>();
                satisfied.Add(take);
            }
            return satisfied;
        }

        /**
	     * Complete the tasks associated to the satisfied async takes.
	     *
	     *  Note: This method is called when the current thread **does not own the lock**.
	     */
        private void CompleteSatisfiedAsyncTakes(List<AsyncTake> toComplete) {
            if (toComplete != null) {
                foreach (AsyncTake take in toComplete) {
                    T msg = take.message;
                    // Dispose the resources associated with the async take and
                    // complete its task with success.
                    take.Dispose();
                    take.SetResult(msg);    // complete the associated take's task
                }
            }
        }

        /**
	     * Try to cancel an async acquire request
	     */
        private void RequestCancellationTakeHandler(object _takeNode, bool cancelling) {
            LinkedListNode<AsyncTake> takeNode = (LinkedListNode<AsyncTake>) _takeNode;
            AsyncTake take = takeNode.Value;
            bool complete = false;
            List<AsyncTake> satisfied = null;

            // To access shared mutable state we must acquire the lock
            lock (theLock) {

                /**
			     * Here, the async take can be already satisfied or cancelled.
			     */
                if (!take.done) {
                    // Remove the async acquire take from queue and mark it as done.
                    asyncTakes.Remove(takeNode);
                    complete = take.done = true;

                    // If after removing the async take is possible to satisfy any
                    // pending async take(s) do it 
                    if (pendingMessages.Count > 0) {
                        satisfied = SatisfyPendingAsyncTakes();
                    }
                }
            }

            // If we cancelled the async take, release the resources associated with it,
            // and complete the underlying task.
            if (complete) {
                // Complete any satisfied async takes
                if (satisfied != null) {
                    CompleteSatisfiedAsyncTakes(satisfied);
                }

                // Dispose the resources associated with the cancelled async take
                take.Dispose(cancelling);

                // Complete the TaskCompletionSource to RanToCompletion with false (timeout)
                // or Canceled final state (cancellation).
                if (cancelling) {
                    take.SetCanceled();     // cancelled
                }
                else {
                    take.SetResult(null);   // timeout
                }
            }
        }



        /**
	     * Auxiliary methods, related to 'Message'
	     */

        /**
         * Returns the list of all pending async messages that can be satisfied.
         *
         * Note: This method is called when the current thread owns the lock.
         */
        private List<Message> SatisfyPendingMessages() {
            List<Message> satisfied = null;
            while (pendingMessages.Count > 0) {
                Message msg = pendingMessages.First.Value;

                // Check if available asyncTakes allow satisfy this async message
                if (asyncTakes.Count == 0)
                    break;

                // Remove the pending message from the queue
                pendingMessages.RemoveFirst();

                // Update take and mark message as done
                AsyncTake take = asyncTakes.First.Value;
                take.message = msg.message;

                asyncTakes.RemoveFirst();

                if (msg.transfer != null)
                    msg.transfer.done = true;

                // Add the async acquire to the result list
                if (satisfied == null)
                    satisfied = new List<Message>();
                satisfied.Add(msg);
            }
            return satisfied;
        }

        /**
	     * Complete the messages associated to the satisfied async transfers.
	     *
	     *  Note: This method is called when the current thread **does not own the lock**.
	     */
        private void CompleteSatisfiedMessages(List<Message> toComplete) {
            if (toComplete != null) {
                foreach (Message msg in toComplete) {
                    // Dispose the resources associated with the async message and
                    // complete its task with success.
                    msg.transfer.Dispose();
                    msg.transfer.SetResult(true);   // complete the associated message's task
                }
            }
        }

        /**
	     * Try to cancel an async acquire request
	     */
        private void RequestCancellationTransferHandler(object _msgNode, bool cancelling) {
            LinkedListNode<Message> msgNode = (LinkedListNode<Message>) _msgNode;
            Message msg = msgNode.Value;
            bool complete = false;
            List<Message> satisfied = null;

            // To access shared mutable state we must acquire the lock
            lock (theLock) {
                /**
			     * Here, the async transfer can be already satisfied or cancelled.
			     */
                if (!msg.transfer.done) {
                    // Remove the async transfer request from queue and mark it as done.
                    pendingMessages.Remove(msgNode);
                    complete = msg.transfer.done = true;

                    // If after removing the async acquire is possible to satisfy any
                    // pending async transfer(s) do it 
                    if (pendingMessages.Count > 0) {
                        satisfied = SatisfyPendingMessages();
                    }
                }
            }

            // If we cancelled the async transfer, release the resources associated with it,
            // and complete the underlying task.
            if (complete) {
                // Complete any satisfied async transfer
                if (satisfied != null) {
                    CompleteSatisfiedMessages(satisfied);
                }

                // Dispose the resources associated with the cancelled async transfer
                msg.transfer.Dispose(cancelling);

                // Complete the TaskCompletionSource to RanToCompletion with false (timeout)
                // or Canceled final state (cancellation).
                if (cancelling) {
                    msg.transfer.SetCanceled();     // cancelled
                }
                else {
                    msg.transfer.SetResult(false);  // timeout
                }
            }
        }



        /**
	     * Asynchronous Task-based Asynchronous Pattern (TAP) interface.
	     */
         
        public void PutAsync(T message) {
            lock (theLock) {
                if (asyncTakes.Count > 0) {
                    AsyncTake take = asyncTakes.First.Value;
                    asyncTakes.RemoveFirst();
                    take.message = message;
                    take.done = true;
                    return;
                }
                Message msg = new Message(message);
                LinkedListNode<Message> messageNode = pendingMessages.AddLast(msg);
            }
        }

        public Task<T> TakeAsync(int timeout = Timeout.Infinite, CancellationToken cToken = default(CancellationToken)) {
            lock (theLock) {
                if (pendingMessages.Count > 0) {
                    Message msg = pendingMessages.First.Value;
                    pendingMessages.RemoveFirst();
                    T message = msg.message;
                    if (msg.transfer != null) {
                        msg.transfer.done = true;
                    }
                    return Task.FromResult(message);
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
                    take.timer = new Timer(timeoutTakeHandler, takeNode, timeout, Timeout.Infinite);

                /**
                 * If the cancellation token is already in the canceled state, the cancellation
                 * handler will run immediately and synchronously, which *causes no damage* because
                 * this processing is terminal and the implicit locks can be acquired recursively.
                 */
                if (cToken.CanBeCanceled)
                    take.cTokenRegistration = cToken.Register(cancellationTakeHandler, takeNode);

                // Return the Task<T> that represents the async take
                return take.Task;
            }
        }

        public Task<Boolean> TransferAsync(T message, int timeout = Timeout.Infinite, CancellationToken cToken = default(CancellationToken)) {
            lock (theLock) {
                if (asyncTakes.Count > 0) {
                    AsyncTake take = asyncTakes.First.Value;
                    asyncTakes.RemoveFirst();
                    take.message = message;
                    take.done = true;
                    return trueTask;
                }

                if (timeout <= 0)
                    return falseTask;

                if (cToken.IsCancellationRequested)
                    return Task.FromCanceled<Boolean>(cToken);

                Message msg = new Message(message);
                LinkedListNode<Message> msgNode = pendingMessages.AddLast(msg);

                if (timeout != Timeout.Infinite)
                    msg.transfer.timer = new Timer(timeoutTransferHandler, msgNode, timeout, Timeout.Infinite);

                if (cToken.CanBeCanceled)
                    msg.transfer.cTokenRegistration = cToken.Register(cancellationTakeHandler, msgNode);

                return msg.transfer.Task;
            }
        }
    }
}
