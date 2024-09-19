public class Bloqueio {

    public enum TipoBloqueio {
        COMPARTILHADO, EXCLUSIVO, INTENCIONAL_LEITURA, INTENCIONAL_ESCRITA, INTENCIONAL_ATUALIZACAO
    }

    public TipoBloqueio tipoBloqueio;
    private Integer transactionId;

    // Construtor
    public Bloqueio(TipoBloqueio tipoBloqueio) {
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
