public class TabelaConflitos {

    // Definindo a matriz de compatibilidade como static final
    private static final boolean[][] MATRIZ_CONFLITOS = {
        // LEITURA  ESCRITA  CERTIFY  INT_LEIT  INT_ESCR  INT_CERT
        {  true ,   true ,   false ,   true ,   true ,    false },  // LEITURA
        {  true ,  false ,   false ,   true ,   false ,   false },  // ESCRITA
        { false ,  false ,    true ,   false,   false ,    true },  // CERTIFY
        {  true ,   true ,   false ,   true ,   true ,     true },  // INTENCIONAL_LEITURA
        {  true ,  false ,   false ,   true ,   true ,     true },  // INTENCIONAL_ESCRITA
        { false ,  false ,   false ,   true ,   true ,     true },  // INTENCIONAL_CERTIFY
    };

    // Método para verificar se dois tipos de bloqueio são compatíveis
    public static boolean podeConcederBloqueio(
        Bloqueio.type bloqueioAtual, 
        Bloqueio.type novoBloqueio
    ) {
        return MATRIZ_CONFLITOS[bloqueioAtual.ordinal()][novoBloqueio.ordinal()];
    }

    // Sobrecarga para verificar compatibilidade entre um bloqueio existente e uma nova operação
    public static boolean podeConcederBloqueio(
        Bloqueio bloqueioExistente, 
        Operacao novaOperacao
    ) {

        if(bloqueioExistente == null) return true;
        
        Bloqueio novoBloqueio = obterBloqueio(novaOperacao);

        return podeConcederBloqueio(
            bloqueioExistente.tipo, 
            novoBloqueio.tipo
        );
    }

    public static boolean podeConcederBloqueio(
        Operacao novaOperacao
    ) {

        Bloqueio bloqueioExistente = novaOperacao.registro.bloqueio;
        Bloqueio novoBloqueio = obterBloqueio(novaOperacao);

        if(bloqueioExistente == null) return true;

        return podeConcederBloqueio(
            bloqueioExistente.tipo, 
            novoBloqueio.tipo
        );
    }


    // Método auxiliar para mapear uma operação ao tipo de bloqueio correspondente
    public static Bloqueio obterBloqueio(Operacao operacao) {

        switch (operacao.tipoOperacao) {
            case READ:
                return new Bloqueio(Bloqueio.type.LEITURA, operacao.registro);
            case WRITE:
                return new Bloqueio(Bloqueio.type.ESCRITA, operacao.registro);
            case COMMIT:
                return new Bloqueio(Bloqueio.type.CERTIFY, operacao.registro);
            default:
                throw new IllegalArgumentException("Tipo de operação desconhecido.");
        }

    }


    // Testando
    public static void main(String[] args) {
        // Exemplo de uso
        System.out.println(podeConcederBloqueio(Bloqueio.type.LEITURA, Bloqueio.type.ESCRITA)); // true
        System.out.println(podeConcederBloqueio(Bloqueio.type.ESCRITA, Bloqueio.type.CERTIFY)); // false
    }
}
