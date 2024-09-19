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
    public TipoOperacao tipoOperacao;

    // Enum para definir os tipos de operação
    public enum TipoOperacao {
        READ,
        WRITE,
        UPDATE,
        INTENTIONAL_READ,
        INTENTIONAL_WRITE,
        INTENTIONAL_UPDATE,
        COMMIT,
        ABORT
    }
}
class Read extends Operacao {
    
    public Read(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.READ;
    }
}

class Write extends Operacao {
    
    public Write(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.WRITE;
    }
}

class Update extends Operacao {
    
    public Update(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.UPDATE;
    }
}

class IntentionalRead extends Operacao {
    
    public IntentionalRead(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.INTENTIONAL_READ;
    }
}

class IntentionalWrite extends Operacao {
    
    public IntentionalWrite(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.INTENTIONAL_WRITE;
    }
}

class IntentionalUpdate extends Operacao {
    
    public IntentionalUpdate(Integer transaction, Registro registro) {
        super(transaction, registro);
        tipoOperacao = TipoOperacao.INTENTIONAL_UPDATE;
    }
}

class Commit extends Operacao {
    
    public Commit(Integer transaction) {
        super(transaction, null);
        tipoOperacao = TipoOperacao.COMMIT;
    }
}

class Abort extends Operacao {
    
    public Abort(Integer transaction) {
        super(transaction, null);
        tipoOperacao = TipoOperacao.ABORT;
    }
}
