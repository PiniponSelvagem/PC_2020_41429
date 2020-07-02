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
            cancellationTakeHandler = new Action<object>((node) => RequestCancellationTakeHandler(node, true));
            timeoutTakeHandler = new TimerCallback((node) => RequestCancellationTakeHandler(node, false));
            cancellationTransferHandler = new Action<object>((node) => RequestCancellationTransferHandler(node, true));
            timeoutTransferHandler = new TimerCallback((node) => RequestCancellationTransferHandler(node, false));

            pendingMessages = new LinkedList<Message>();
            asyncTakes = new LinkedList<AsyncTake>();
        }




        private List<AsyncTake> SatisfyPendingAsyncTakes() {
            List<AsyncTake> satisfied = null;
            while (asyncTakes.Count > 0) {
                AsyncTake take = asyncTakes.First.Value;

                if (pendingMessages.Count == 0) {
                    break;
                }

                asyncTakes.RemoveFirst();

                Message msg = pendingMessages.First.Value;
                take.message = msg.message;
                if (msg.transfer != null) {
                    msg.transfer.done = true;
                }
                pendingMessages.RemoveFirst();

                take.done = true;

                if (satisfied == null) {
                    satisfied = new List<AsyncTake>();
                }

                satisfied.Add(take);
            }
            return satisfied;
        }

        private void CompleteSatisfiedAsyncTakes(List<AsyncTake> toComplete) {
            if (toComplete != null) {
                foreach (AsyncTake take in toComplete) {
                    T msg = take.message;
                    take.Dispose();
                    take.SetResult(msg);
                }
            }
        }

        private void RequestCancellationTakeHandler(object _takeNode, bool cancelling) {
            LinkedListNode<AsyncTake> takeNode = (LinkedListNode<AsyncTake>) _takeNode;
            AsyncTake take = takeNode.Value;
            bool complete = false;
            List<AsyncTake> satisfied = null;

            lock (theLock) {
                if (!take.done) {
                    asyncTakes.Remove(takeNode);
                    complete = take.done = true;

                    if (pendingMessages.Count > 0) {
                        satisfied = SatisfyPendingAsyncTakes();
                    }
                }
            }

            if (complete) {
                if (satisfied != null) {
                    CompleteSatisfiedAsyncTakes(satisfied);
                }

                take.Dispose(cancelling);

                if (cancelling) {
                    take.SetCanceled();
                }
                else {
                    take.SetResult(null);
                }
            }
        }




        private List<Message> SatisfyPendingMessages() {
            List<Message> satisfied = null;
            while (pendingMessages.Count > 0) {
                Message msg = pendingMessages.First.Value;

                if (asyncTakes.Count == 0) {
                    break;
                }

                pendingMessages.RemoveFirst();

                AsyncTake take = asyncTakes.First.Value;
                take.message = msg.message;

                asyncTakes.RemoveFirst();

                if (msg.transfer != null) {
                    msg.transfer.done = true;
                }

                if (satisfied == null) {
                    satisfied = new List<Message>();
                }
                satisfied.Add(msg);
            }
            return satisfied;
        }

        private void CompleteSatisfiedMessages(List<Message> toComplete) {
            if (toComplete != null) {
                foreach (Message msg in toComplete) {
                    msg.transfer.Dispose();
                    msg.transfer.SetResult(true);
                }
            }
        }

        private void RequestCancellationTransferHandler(object _msgNode, bool cancelling) {
            LinkedListNode<Message> msgNode = (LinkedListNode<Message>) _msgNode;
            Message msg = msgNode.Value;
            bool complete = false;
            List<Message> satisfied = null;

            lock (theLock) {
                if (!msg.transfer.done) {
                    pendingMessages.Remove(msgNode);
                    complete = msg.transfer.done = true;

                    if (pendingMessages.Count > 0) {
                        satisfied = SatisfyPendingMessages();
                    }
                }
            }

            if (complete) {
                if (satisfied != null) {
                    CompleteSatisfiedMessages(satisfied);
                }

                msg.transfer.Dispose(cancelling);

                if (cancelling) {
                    msg.transfer.SetCanceled();
                }
                else {
                    msg.transfer.SetResult(false);
                }
            }
        }





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
