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

    public Operacao.lock escopo;
    
    public Integer transaction;

    public Data data;

    // Construtor
    public Bloqueio(type tipoBloqueio, Data data, Integer transaction, Operacao.lock escopo) {
        this.tipo = tipoBloqueio;
        this.data = data;
        this.transaction = transaction;
        this.escopo = escopo;
    }

    // Define o ID da transação que detém o bloqueio
    public void setTransaction(Integer transactionId) {
        this.transaction = transactionId;
    }

    // Retorna o ID da transação que detém o bloqueio
    public Integer getTransaction() {
        return transaction;
    }

    // Retorna a versão intencional desse bloqueio para ser 
    public Bloqueio intencional(){

        System.out.println(this.tipo);

        switch(this.tipo){
            case LEITURA:{
                return new Bloqueio(type.INTENCIONAL_LEITURA, null, transaction, escopo);
            }
            case ESCRITA:{
                return new Bloqueio(type.INTENCIONAL_ESCRITA, null, transaction, escopo);
            }
            case CERTIFY:{
                return new Bloqueio(type.INTENCIONAL_CERTIFY, null, transaction, escopo);
            }
            default:{
                return new Bloqueio(this.tipo, null, transaction, escopo);
            }
        }
    }

    public Bloqueio clonar(){
        return new Bloqueio(tipo, data ,transaction, escopo);
    }
}
