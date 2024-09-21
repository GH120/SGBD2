import org.junit.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Protocolo2V2PLTest {

    private Protocolo2V2PL protocolo;
    private Database database;

    @Before
    public void setUp() {
        protocolo = new Protocolo2V2PL();
        database = criarBancoDeDadosExemplo(); // Inicializa o banco de dados vazio
        protocolo.database = database;
        protocolo.GrafoWaitFor = new HashMap<>();
        protocolo.BloqueiosAtivos = new ArrayList<>();
        protocolo.datacopies = new HashMap<>();
        protocolo.Escalonamento = new LinkedList<>();
        protocolo.OperacoesRestantes = new LinkedList<>();
    }

    public static Database criarBancoDeDadosExemplo() {
        // Criar registros
        Registro registro1 = new Registro("X", 100, null);
        Registro registro2 = new Registro("Y", 200, null);
        Registro registro3 = new Registro("Z", 300, null);
        
        // Criar páginas
        Pagina pagina1 = new Pagina(new ArrayList<>(List.of(registro1, registro2)), null);
        Pagina pagina2 = new Pagina(new ArrayList<>(List.of(registro3)), null);
        
        // Associar páginas aos registros
        registro1.pagina = pagina1;
        registro2.pagina = pagina1;
        registro3.pagina = pagina2;
        
        // Criar tabelas
        Tabela tabela1 = new Tabela(new ArrayList<>(List.of(pagina1)), null);
        Tabela tabela2 = new Tabela(new ArrayList<>(List.of(pagina2)), null);
        
        // Associar tabelas às páginas
        pagina1.tabela = tabela1;
        pagina2.tabela = tabela2;
        
        // Criar o banco de dados
        Database bancoDeDados = new Database(new ArrayList<>(List.of(tabela1, tabela2)));
        
        return bancoDeDados;
    }

    @Test
    public void testEscalonamentoLeituras() {
        // Configurar registros
        Registro registro1 = database.buscarRegistro("X");
        Registro registro2 = database.buscarRegistro("Y");

        // Criar operações de leitura
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Read(1, registro1)); // Transação 1 lê Registro1
        operacoes.add(new Read(2, registro2)); // Transação 2 lê Registro2
        operacoes.add(new Read(1, registro2)); // Transação 1 lê Registro2
        operacoes.add(new Read(2, registro1)); // Transação 2 lê Registro1

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações de leitura foram escalonadas corretamente
        Assert.assertEquals(4, protocolo.Escalonamento.size());
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Read);
        
        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(2).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction);
    }
}
