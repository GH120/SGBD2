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
        String resultado = controle.runEscalonamento("w1(Registro1) w2(Registro3 with pagelock) r3(Registro3) r3(Registro1) r2(Registro3) w3(Registro1) a3 c1 ");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(Registro1)W2(Registro3)R2(Registro3)A3C1"));
    }

    @Test
    public void testCommitAbort2() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com operações de commit e abort
        String resultado = controle.runEscalonamento("w1(Registro1) w2(Registro3 with pagelock) r3(Registro3) r3(Registro1) r2(Registro3) w3(Registro1) a2 c1 ");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(Registro1)A2C1"));
    }

    @Test
    public void testMultiplasTransacoes() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com múltiplas transações
        controle.runEscalonamento("w1(Registro1) r2(Registro1) u1(Registro2) w2(Registro3) c1 c2");
        
        // Assert.assertTrue(resultado.equals("W1(Registro1)R1(Registro2)C1"));
    }

    @Test
    public void testDeadlock() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database1.json", "resources/ops/database1.json");

        // Teste com operações que podem gerar deadlock
        String resultado = controle.runEscalonamento("w1(Registro1 with tablelock) r2(Registro1) w2(Registro2 with tablelock) r1(Registro2) c1 c2");

        Assert.assertTrue(resultado.equals("W1(Registro1)R1(Registro2)C1"));
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
        String resultado = controle.runEscalonamento("w1(x with tablelock) w2(z) c1 c2");

        Assert.assertTrue(resultado.equals("W1(x)C1W2(z)C2"));
    }

    @Test
    public void testBloqueioPagina() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("w1(x with pagelock) w2(z) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(x)W2(z)C1C2"));
    }

    @Test
    public void testBloqueioPaginaTabela() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("w1(x with pagelock) w2(z with tablelock) c1 c2");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("W1(x)C1W2(z)C2"));
    }

    @Test
    public void testBloqueioPaginaTabela2() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("r1(x with pagelock) w1(y) r2(x with pagelock) c1 w2(z) r3(z) c2 w3(y) c3");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("R1(x)W1(y)R2(x)W2(z)C2C1"));
    }

    @Test
    public void testBloqueioPaginaTabela3() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("r1(x with pagelock) w1(y) r2(x with pagelock) c1 w2(z) r3(z) c2 w3(y) c3");
        
        // Verificar se o escalonamento foi bem-sucedido
        Assert.assertTrue(resultado.equals("R1(x)W1(y)R2(x)W2(z)C2C1"));
    }

    @Test
    public void testBloqueioPaginaTabela4() throws IOException {
        // Inicializa o controle com o caminho para a base de dados e operações
        Controle controle = new Controle("resources/dbs/database2.json", "resources/ops/database1.json");

        // Deve funcionar pois estão em páginas diferentes
        String resultado = controle.runEscalonamento("r1(x) r2(x) w1(x) r1(z) c1 w2(y) r3(y) c2 w3(z) c3");
        
        // Verificar se o escalonamento foi bem-sucedido
        // Assert.assertTrue(resultado.equals("R1(x)W1(y)R2(x)W2(z)C2C1"));
    }
}
