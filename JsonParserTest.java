import org.junit.Test;
import static org.junit.Assert.*;
import java.util.LinkedList;

public class JsonParserTest {

    @Test
    public void testJsonToDatabase() {
        // Configurar o parser com debug desativado
        JsonParser parser = new JsonParser(false);

        // Simular o caminho do arquivo JSON
        String path = "resources/dbs/database1.json"; // Substitua pelo caminho correto do arquivo

        // Chamar o método jsonToDatabase
        Database database = parser.jsonToDatabase(path);

        // Verificar se o database foi carregado corretamente
        assertNotNull(database);
        assertEquals(2, database.tabelas.size());

        // Verificar se a primeira tabela tem o número correto de páginas
        Tabela tabela = database.tabelas.get(0);
        assertEquals(2, tabela.paginas.size());

        // Verificar se a primeira página tem o número correto de registros
        Pagina pagina = tabela.paginas.get(0);
        assertEquals(2, pagina.registros.size());

        // Verificar se o nome do primeiro registro está correto
        Registro registro = pagina.registros.get(0);
        assertEquals("Registro1", registro.nome);
    }

    @Test
    public void testJsonToOperacoes() {
        // Configurar o parser com debug ativado para ver as mensagens
        JsonParser parser = new JsonParser(true);

        // Simular o caminho do arquivo JSON
        String databasePath = "resources/dbs/database1.json"; // Substitua pelo caminho correto do arquivo
        String operacoesPath = "resources/ops/database1.json"; // Substitua pelo caminho correto do arquivo

        // Chamar o método jsonToDatabase para carregar o database
        Database database = parser.jsonToDatabase(databasePath);
        assertNotNull(database);

        // Chamar o método jsonToOperacoes para carregar as operações
        LinkedList<Operacao> operacoes = parser.jsonToOperacoes(operacoesPath, database);
        assertNotNull(operacoes);

        // // Verificar o número de operações
        // assertEquals(2, operacoes.size());

        // // Verificar a primeira operação
        // Operacao operacao1 = operacoes.get(0);
        // assertEquals(1, operacao1.transaction.intValue());
        // assertEquals(Operacao.type.READ, operacao1.tipoOperacao);
        // assertEquals("Registro1", operacao1.registro.nome);

        // // Verificar a segunda operação
        // Operacao operacao2 = operacoes.get(1);
        // assertEquals(2, operacao2.transaction.intValue());
        // assertEquals(Operacao.type.WRITE, operacao2.tipoOperacao);
        // assertEquals("Registro2", operacao2.registro.nome);
    }
}
