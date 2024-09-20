//Quem tem o bloqueio é o registro/linha/tabela
//Ajustar essa lógica

public abstract class Operacao{

    Integer  transaction;
    Registro registro;
    Integer  timestamp;

    public Operacao(Integer transaction, Registro registro){
        this.transaction  = transaction;
        this.registro     = registro;
    }

    public Operacao setTimestamp(Integer tempo){
        
        timestamp = tempo;

        return this;
    }

    // Propriedade para armazenar o tipo da operação
    public type tipoOperacao;

    // Enum para definir os tipos de operação
    public enum type {
        READ,
        WRITE,
        // UPDATE,
        COMMIT,
        ABORT
    }
}
class Read extends Operacao {
    
    public Read(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = type.READ;
    }
}

class Write extends Operacao {
    
    public Write(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = type.WRITE;
    }
}

// class Update extends Operacao {
    
//     public Update(Integer transaction, Registro registro) {
//         super(transaction, registro);
//         tipoOperacao = type.UPDATE;
//     }
// }

class Commit extends Operacao {
    
    public Commit(Integer transaction) {
        super(transaction, null);
        tipoOperacao = type.COMMIT;
    }
}

class Abort extends Operacao {
    
    public Abort(Integer transaction) {
        super(transaction, null);
        tipoOperacao = type.ABORT;
    }
}
