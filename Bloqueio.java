public class Bloqueio {

    public enum type {
        LEITURA, 
        ESCRITA, 
        CERTIFY, 
        INTENCIONAL_LEITURA, 
        INTENCIONAL_ESCRITA, 
        INTENCIONAL_CERTIFY
    }

    public type tipoBloqueio;
    
    private Integer transactionId;

    // Construtor
    public Bloqueio(type tipoBloqueio) {
        this.tipoBloqueio = tipoBloqueio;
    }

    // Define o ID da transação que detém o bloqueio
    public void setTransaction(Integer transactionId) {
        this.transactionId = transactionId;
    }

    // Retorna o ID da transação que detém o bloqueio
    public Integer getTransaction() {
        return transactionId;
    }
}
