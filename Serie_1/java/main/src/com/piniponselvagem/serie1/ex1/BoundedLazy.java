package com.piniponselvagem.serie1.ex1;

import java.util.Optional;
import java.util.function.Supplier;

import static com.piniponselvagem.serie1.ex1.MonitorState.*;

public class BoundedLazy<T> {

    private final Object monitor = new Object();
    private final int lives;              // numero de vezes que pode ser utilizado

    private int currLives = 0;
    private MonitorState state = NOTCREATED;
    private Optional<T> value;

    private Supplier<T> supplier;

    public BoundedLazy(Supplier<T> supplier, int lives) {
        this.supplier = supplier;
        this.lives = lives;
    }

    public Optional<T> get(long timeout) throws Exception {
        synchronized (monitor) {
            if (state == CREATED && currLives > 0) {
                --currLives;
                return value;
            }
        }

        synchronized (monitor) {
            monitor.wait(timeout);
            if (state == CREATING)
                return Optional.empty();
        }

        Optional<T> v = Optional.empty();
        Exception ex = null;
        try {
            state = CREATING;
            v = Optional.of(supplier.get());
        } catch(Exception e) {
            ex = e;
        }

        synchronized(monitor) {
            if (ex != null) {
                state = ERROR;
                monitor.notifyAll();
                throw ex;
            }
            else {
                value = v;
                currLives = lives-1;    // -1 since this call counts as 1 live
                state = CREATED;
                monitor.notify();
                return value;
            }
        }
    }




    /*
        synchonized(monitor) {
            // state == CREATED, entao decrementar 'lives' e retornar o valor
        }

        // compute value: state == CREATING
        T v = null;
        Exception ex = null;
        try {
            V = supplier.get();
        } catch(Exception e) {
            ex = e;
        }

        synchonized(monitor) {
            //save computed value and notify waiters
            if (ex != null) {
                exception = ex;
                // set state to ERROR
                // notify all blocked threads
                throw ex;
            )
            else {
                value = v;
                currLives = lives;
                state = CREATED;
                // notify all blocked threads
                return Optional.of(value);
            }
        }
        */


        /*
        value = Supplier.value; //???

        if (value.wasCalculated == true && lives.used == 0)
            return value;                           // naturezza de operacao Aquire

        if (value.wasCalculated == false || lives.used == lives)
            value = supplier.calculateNewValue();   // chamado na propria thread invocante (depois de sair do monitor, tirar a posse do lock)
            return value;                           // e depois retorna esse valor

        if (SomeThread.isCalculatingValue)
            waitForCalculation();                   // tenho que bloquear a thread invocante ate obter o calculo

        if (timeout.reached)
            return Optional.empty();

        if (thread.wasInterrupted)
            throw new InterruptedException()

        if (Supplier.throwedException == true)
            object goes to error state throwing its exception on ALL get calls

        */


        /*
        - Sincronizador está toda dentro do metodo get.
        - Este sincronizador tem que ser implementado como uma maquina de estados, com os seguintes estados:
            > NOTCREATED: o valor nao esta disponivel pq nao foi ainda calculado ou pq ja foram consumidas todas as vidas
            > CREATING:   ja existe uma thread a calcular o valor
            > CREATED:    o valor esta disponivel 'lives' vezes
            > ERROR:      ocorreu uma excepcao na chamada ao 'supplier'

        - Parametros imutaveis: (podem ser final) (sendo imutaveis o java garante a visibilidade fora do objecto)
            > 'supplier'
            > 'lives'

        - Estado de sincronizacao:
            > 'state'
            > 'value'
            > 'currLives'
            > 'exception'

        - Sugestoes:
            > criar o diagrama de maquina de estados
         */
}
