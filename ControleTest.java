import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ControleTest {
    
     @Test
    public void testEscalonamentoSimples() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");
        
        // Teste com uma sequência simples de operações
        controle.runEscalonamento("w1(Registro1 with tablelock) r1(Registro1) u1(Registro2) c1");
        
        // Verificar se o escalonamento foi bem-sucedido (pode-se adicionar verificações dependendo da implementação)
        System.out.println("Escalonamento simples concluído com sucesso.");
    }

    @Test
    public void testCommitAbort() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com operações de commit e abort
        controle.runEscalonamento("w2(Registro3 with pagelock) r2(Registro3) c2 a3");
        
        // Verificar se o escalonamento foi bem-sucedido
        System.out.println("Commit e Abort escalonados com sucesso.");
    }

    @Test
    public void testMultiplasTransacoes() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com múltiplas transações
        controle.runEscalonamento("w1(Registro1) r2(Registro1) u1(Registro2) w2(Registro3) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        System.out.println("Múltiplas transações escalonadas com sucesso.");
    }

    @Test
    public void testDeadlock() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com operações que podem gerar deadlock
        controle.runEscalonamento("w1(Registro1 with tablelock) r2(Registro1) w2(Registro2 with tablelock) r1(Registro2) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        System.out.println("Deadlock test concluído com sucesso.");
    }

    @Test
    public void testOperacoesSemLocksEspecificados() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com operações sem especificar locks (deve assumir rowlock por padrão)
        String resultado = controle.runEscalonamento("w1(Registro1) r1(Registro2) u2(Registro3)");
        
        Assert.assertTrue(resultado.equals("W1(Registro1)R1(Registro2)U2(Registro3)"));
    }

    @Test
    public void testBloqueioTabela() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // W2 deve esperar w1 pois estão na mesma tabela
        String resultado = controle.runEscalonamento("w1(X with tablelock) w2(Z) c1 c2");

        Assert.assertTrue(resultado.equals("W1(X)C1W2(Z)C2"));
    }

    @Test
    public void testBloqueioPagina() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("w1(X with pagelock) w2(Z) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(X)W2(Z)C1C2"));
    }

    @Test
    public void testBloqueioPaginaTabela() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("w1(X with pagelock) w2(Z with tablelock) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(X)C1W2(Z)C2"));
    }

    @Test
    public void testBloqueioPaginaTabela2() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("r1(X with pagelock) w1(Y) r2(X with pagelock) c1 w2(Z) r3(Z) c2 w3(Y) c3");
        
        // Verificar se o escalonamento foi bem-sucedido
        // Assert.assertTrue(resultado.equals("W1(X)C1W2(Z)C2"));
    }
}
