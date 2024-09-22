import java.util.ArrayList;
import java.util.List;

public class TabelaConflitos {

    // Definindo a matriz de compatibilidade como static final
    private static final boolean[][] MATRIZ_CONFLITOS = {
        // LEITURA  ESCRITA  CERTIFY  UPDATE   INT_LEIT  INT_ESCR  INT_CERT  INT_UPDT
        {  true ,   true ,   false ,  true   ,   true ,   true ,    false,    true  },  // LEITURA
        {  true ,  false ,   false ,  false  ,   true ,   false ,   false,    false },  // ESCRITA
        { false ,  false ,   false ,  false  ,   false,   false ,   false,    false },  // CERTIFY
        {  true ,  false ,   false ,  false  ,   true ,   false ,   false,    false },  // UPDATE
        {  true ,   true ,   false ,  true   ,   true ,   true ,     true,    true  },  // INTENCIONAL_LEITURA
        {  true ,  false ,   false ,  false  ,   true ,   true ,     true,    true  },  // INTENCIONAL_ESCRITA
        { false ,  false ,   false ,  false  ,   true ,   true ,     true,    true  },  // INTENCIONAL_CERTIFY
        {  true ,  false ,   false ,  false  ,   true ,   true ,     true,    true  },  // INTENCIONAL_UPDATE
    };

    //Uma bela duma bagunça, necessita de refatoração, espaguetti gigantesco

    // Método para verificar se dois tipos de bloqueio são compatíveis
    public static boolean compativeis(
        Bloqueio.type bloqueioAtual, 
        Bloqueio.type novoBloqueio
    ) {
        return MATRIZ_CONFLITOS[bloqueioAtual.ordinal()][novoBloqueio.ordinal()];
    }

    public static boolean podeConcederBloqueio(
        Operacao novaOperacao,
        List<Bloqueio> bloqueios
    ) {

        if(bloqueios.size() == 0){

            return bloqueioPermitidoParaTodosPais(
                    novaOperacao.registro.getPai(), 
                    obterBloqueio(novaOperacao)
            );
        } 
                
        return bloqueios.stream()
                        .allMatch(bloqueio -> podeConcederBloqueio(bloqueio, novaOperacao));
    }

    // Sobrecarga para verificar compatibilidade entre um bloqueio existente e uma nova operação
    public static boolean podeConcederBloqueio(
        Bloqueio bloqueioExistente, 
        Operacao novaOperacao
    ) {

        if(bloqueioExistente == null) return true;

        if(bloqueioExistente.transaction == novaOperacao.transaction) return true;
        
        Bloqueio novoBloqueio = obterBloqueio(novaOperacao);

        Boolean bloqueioPermitido = compativeis(bloqueioExistente.tipo, novoBloqueio.tipo);

        //Retorna se permitiu o bloqueio dele e de todos os seus ascendentes
        return bloqueioPermitido && bloqueioPermitidoParaTodosPais(novaOperacao.registro.getPai(), novoBloqueio);
    }

    public static boolean bloqueioPermitidoParaTodosPais(Data pai, Bloqueio bloqueio){

        boolean bloqueioPermitido = true;

        while(pai != null){

            //Copio o bloqueio e vejo se é intencional ou não baseado no escopo
            Bloqueio copia = (TabelaConflitos.bloqueioEmSeuEscopo(pai, bloqueio.escopo))? bloqueio.clonar() : bloqueio.intencional();

            bloqueioPermitido = bloqueioPermitido && bloqueiosCompativeis(copia, pai.bloqueios);

            pai = pai.getPai();
        }

        return bloqueioPermitido;
    }

    public static boolean bloqueiosCompativeis(
        Bloqueio bloqueioNovo,
        List<Bloqueio> bloqueios
    ) {

        if(bloqueios.size() == 0) return true;

        return bloqueios.stream().allMatch(bloqueio -> compativeis(bloqueio.tipo, bloqueioNovo.tipo));
    }

    public static boolean bloqueioEmSeuEscopo(Data data, Operacao.lock escopo){

        boolean isDatabase = data instanceof Database;
        boolean isTabela   = data instanceof Tabela;
        boolean isPagina   = data instanceof Pagina;
        boolean isRegistro = data instanceof Registro;

        if(escopo == Operacao.lock.tablelock){
            return isTabela || isPagina || isRegistro;
        }

        if(escopo == Operacao.lock.pagelock){
            return isPagina || isRegistro;
        }

        if(escopo == Operacao.lock.rowlock){
            return isRegistro;
        }

        return false;
    }


    // Método auxiliar para mapear uma operação ao tipo de bloqueio correspondente
    public static Bloqueio obterBloqueio(Operacao operacao) {

        switch (operacao.tipoOperacao) {
            case READ:
                return new Bloqueio(Bloqueio.type.LEITURA, operacao.registro, operacao.transaction, operacao.escopoLock);
            case WRITE:
                return new Bloqueio(Bloqueio.type.ESCRITA, operacao.registro, operacao.transaction, operacao.escopoLock);
            case UPDATE:
                return new Bloqueio(Bloqueio.type.UPDATE , operacao.registro, operacao.transaction, operacao.escopoLock);
            case COMMIT:
                return new Bloqueio(Bloqueio.type.CERTIFY, operacao.registro, operacao.transaction, operacao.escopoLock);
            default:
                throw new IllegalArgumentException("Tipo de operação desconhecido.");
        }

    }
}
