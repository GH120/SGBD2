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

    public Data data;

    // Construtor
    public Bloqueio(type tipoBloqueio, Data data, Integer transaction) {
        this.tipo = tipoBloqueio;
        this.data = data;
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
