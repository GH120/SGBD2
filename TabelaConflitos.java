import java.util.HashMap;
import java.util.Map;

public class TabelaConflitos {

    // Tabela de conflitos - um mapa que define a compatibilidade entre tipos de bloqueios e operações
    private static final Map<Bloqueio.TipoBloqueio, Map<Operacao.TipoOperacao, Boolean>> tabelaConflitos = new HashMap<>();

    static {
        // Inicializa a tabela de conflitos para cada tipo de bloqueio

        // Bloqueio compartilhado
        Map<Operacao.TipoOperacao, Boolean> compart = new HashMap<>();
        compart.put(Operacao.TipoOperacao.READ, true);  // Bloqueio compartilhado permite leitura
        compart.put(Operacao.TipoOperacao.WRITE, false); // Bloqueio compartilhado não permite escrita
        compart.put(Operacao.TipoOperacao.UPDATE, false); // Bloqueio compartilhado não permite atualização
        compart.put(Operacao.TipoOperacao.INTENTIONAL_READ, true); // Compatível com bloqueio intencional de leitura
        compart.put(Operacao.TipoOperacao.INTENTIONAL_WRITE, false); // Não compatível com intenção de escrita
        compart.put(Operacao.TipoOperacao.INTENTIONAL_UPDATE, false); // Não compatível com intenção de atualização

        // Bloqueio exclusivo
        Map<Operacao.TipoOperacao, Boolean> exclus = new HashMap<>();
        exclus.put(Operacao.TipoOperacao.READ, false);  // Não compatível com leitura
        exclus.put(Operacao.TipoOperacao.WRITE, true); // Compatível com escrita
        exclus.put(Operacao.TipoOperacao.UPDATE, true); // Compatível com atualização
        exclus.put(Operacao.TipoOperacao.INTENTIONAL_READ, false); // Não compatível com intenção de leitura
        exclus.put(Operacao.TipoOperacao.INTENTIONAL_WRITE, false); // Não compatível com intenção de escrita
        exclus.put(Operacao.TipoOperacao.INTENTIONAL_UPDATE, false); // Não compatível com intenção de atualização

        // Bloqueio intencional de leitura
        Map<Operacao.TipoOperacao, Boolean> intencionalLeitura = new HashMap<>();
        intencionalLeitura.put(Operacao.TipoOperacao.READ, true);  // Permite leitura
        intencionalLeitura.put(Operacao.TipoOperacao.WRITE, false); // Não permite escrita
        intencionalLeitura.put(Operacao.TipoOperacao.UPDATE, false); // Não permite atualização
        intencionalLeitura.put(Operacao.TipoOperacao.INTENTIONAL_READ, true); // Compatível com outro bloqueio intencional de leitura
        intencionalLeitura.put(Operacao.TipoOperacao.INTENTIONAL_WRITE, false); // Não compatível com intenção de escrita
        intencionalLeitura.put(Operacao.TipoOperacao.INTENTIONAL_UPDATE, false); // Não compatível com intenção de atualização

        // Bloqueio intencional de escrita
        Map<Operacao.TipoOperacao, Boolean> intencionalEscrita = new HashMap<>();
        intencionalEscrita.put(Operacao.TipoOperacao.READ, false);  // Não compatível com leitura
        intencionalEscrita.put(Operacao.TipoOperacao.WRITE, true); // Permite escrita
        intencionalEscrita.put(Operacao.TipoOperacao.UPDATE, false); // Não permite atualização
        intencionalEscrita.put(Operacao.TipoOperacao.INTENTIONAL_READ, false); // Não compatível com intenção de leitura
        intencionalEscrita.put(Operacao.TipoOperacao.INTENTIONAL_WRITE, true); // Compatível com outro bloqueio intencional de escrita
        intencionalEscrita.put(Operacao.TipoOperacao.INTENTIONAL_UPDATE, false); // Não compatível com intenção de atualização

        // Bloqueio intencional de atualização
        Map<Operacao.TipoOperacao, Boolean> intencionalAtualizacao = new HashMap<>();
        intencionalAtualizacao.put(Operacao.TipoOperacao.READ, false);  // Não compatível com leitura
        intencionalAtualizacao.put(Operacao.TipoOperacao.WRITE, true); // Permite escrita
        intencionalAtualizacao.put(Operacao.TipoOperacao.UPDATE, true); // Permite atualização
        intencionalAtualizacao.put(Operacao.TipoOperacao.INTENTIONAL_READ, false); // Não compatível com intenção de leitura
        intencionalAtualizacao.put(Operacao.TipoOperacao.INTENTIONAL_WRITE, false); // Não compatível com intenção de escrita
        intencionalAtualizacao.put(Operacao.TipoOperacao.INTENTIONAL_UPDATE, true); // Compatível com outro bloqueio intencional de atualização

        // Adiciona todas as configurações de bloqueio à tabela
        tabelaConflitos.put(Bloqueio.TipoBloqueio.COMPARTILHADO, compart);
        tabelaConflitos.put(Bloqueio.TipoBloqueio.EXCLUSIVO, exclus);
        tabelaConflitos.put(Bloqueio.TipoBloqueio.INTENCIONAL_LEITURA, intencionalLeitura);
        tabelaConflitos.put(Bloqueio.TipoBloqueio.INTENCIONAL_ESCRITA, intencionalEscrita);
        tabelaConflitos.put(Bloqueio.TipoBloqueio.INTENCIONAL_ATUALIZACAO, intencionalAtualizacao);
    }

    // Método para verificar se um bloqueio é compatível com uma operação
    public static boolean podeConcederBloqueio(Bloqueio.TipoBloqueio tipoBloqueio, Operacao.TipoOperacao tipoOperacao) {
        return tabelaConflitos.get(tipoBloqueio).get(tipoOperacao);
    }

    public static Bloqueio getBloqueio(Operacao operacao) {
        // Baseado no tipo da operação, retorna o tipo de bloqueio
        switch (operacao.tipoOperacao) {
            case READ:
                return new Bloqueio(Bloqueio.TipoBloqueio.COMPARTILHADO);
            case WRITE:
                return new Bloqueio(Bloqueio.TipoBloqueio.EXCLUSIVO);
            case UPDATE:
                return new Bloqueio(Bloqueio.TipoBloqueio.EXCLUSIVO);
            case INTENTIONAL_READ:
                return new Bloqueio(Bloqueio.TipoBloqueio.INTENCIONAL_LEITURA);
            case INTENTIONAL_WRITE:
                return new Bloqueio(Bloqueio.TipoBloqueio.INTENCIONAL_ESCRITA);
            case INTENTIONAL_UPDATE:
                return new Bloqueio(Bloqueio.TipoBloqueio.INTENCIONAL_ATUALIZACAO);
            default:
                throw new IllegalArgumentException("Tipo de operação desconhecido.");
        }
    }

    public static boolean bloqueioDisponivel(Operacao operacao, Bloqueio bloqueioExistente) {
        if (bloqueioExistente == null) {
            // Se não há bloqueio existente, o novo bloqueio pode ser concedido
            return true;
        }
    
        // Obtém o tipo de bloqueio necessário para a operação
        Bloqueio novoBloqueio = getBloqueio(operacao);
        
        if (novoBloqueio == null) {
            // Se não puder criar o bloqueio, não pode conceder
            return false;
        }
    
        // Verifica se o bloqueio existente é compatível com o novo bloqueio
        if (TabelaConflitos.podeConcederBloqueio(bloqueioExistente.tipoBloqueio, operacao.tipoOperacao)) {
            // Se o novo bloqueio pode coexistir com o existente
            return true;  // O bloqueio compartilhado pode ser concedido, retorno 'false'
        }
    
        // Se não for compatível, o bloqueio não está disponível
        return true;
    }
}
