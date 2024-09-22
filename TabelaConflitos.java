import java.util.ArrayList;
import java.util.List;

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

    public static boolean podeConcederBloqueio(
        Operacao novaOperacao,
        List<Bloqueio> bloqueios
    ) {

        if(bloqueios.size() == 0) return true;

        return bloqueios.stream().allMatch(bloqueio -> podeConcederBloqueio(bloqueio, novaOperacao));
    }

    public static boolean podeConcederBloqueio(
        Bloqueio bloqueioNovo,
        List<Bloqueio> bloqueios
    ) {

        if(bloqueios.size() == 0) return true;

        return bloqueios.stream().allMatch(bloqueio -> podeConcederBloqueio(bloqueio.tipo, bloqueioNovo.tipo));
    }

    // Sobrecarga para verificar compatibilidade entre um bloqueio existente e uma nova operação
    public static boolean podeConcederBloqueio(
        Bloqueio bloqueioExistente, 
        Operacao novaOperacao
    ) {

        if(bloqueioExistente == null) return true;

        if(bloqueioExistente.transaction == novaOperacao.transaction) return true;
        
        Bloqueio novoBloqueio = obterBloqueio(novaOperacao);

        Boolean bloqueioPermitido =  podeConcederBloqueio(bloqueioExistente.tipo, novoBloqueio.tipo);

        Data pai = bloqueioExistente.data.getPai();

        while(pai != null){

            bloqueioPermitido = bloqueioPermitido && podeConcederBloqueio(novoBloqueio.intencional(), pai.bloqueios);

            pai = pai.getPai();
        }

        //Retorna se permitiu o bloqueio dele e de todos os seus ascendentes
        return bloqueioPermitido;
    }

    // Método para verificar se dois tipos de bloqueio são compatíveis
    public static boolean podeConcederBloqueio(
        Bloqueio.type bloqueioAtual, 
        Bloqueio.type novoBloqueio
    ) {
        return MATRIZ_CONFLITOS[bloqueioAtual.ordinal()][novoBloqueio.ordinal()];
    }


    // Método auxiliar para mapear uma operação ao tipo de bloqueio correspondente
    public static Bloqueio obterBloqueio(Operacao operacao) {

        switch (operacao.tipoOperacao) {
            case READ:
                return new Bloqueio(Bloqueio.type.LEITURA, operacao.registro, operacao.transaction);
            case WRITE:
                return new Bloqueio(Bloqueio.type.ESCRITA, operacao.registro, operacao.transaction);
            case COMMIT:
                return new Bloqueio(Bloqueio.type.CERTIFY, operacao.registro, operacao.transaction);
            default:
                throw new IllegalArgumentException("Tipo de operação desconhecido.");
        }

    }
}
