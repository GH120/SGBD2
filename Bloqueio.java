public class Bloqueio {

    public enum type {
        LEITURA, 
        ESCRITA, 
        CERTIFY, 
        INTENCIONAL_LEITURA, 
        INTENCIONAL_ESCRITA, 
        INTENCIONAL_CERTIFY
    }

    public type tipo;
    
    public Integer transaction;

    public Registro registro;

    // Construtor
    public Bloqueio(type tipoBloqueio, Registro data, Integer transaction) {
        this.tipo = tipoBloqueio;
        this.registro = data;
        this.transaction = transaction;
    }

    // Define o ID da transação que detém o bloqueio
    public void setTransaction(Integer transactionId) {
        this.transaction = transactionId;
    }

    // Retorna o ID da transação que detém o bloqueio
    public Integer getTransaction() {
        return transaction;
    }
}
