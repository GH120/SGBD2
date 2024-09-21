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
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados vazio
        protocolo.database = database;
        protocolo.GrafoWaitFor = new HashMap<>();
        protocolo.BloqueiosAtivos = new ArrayList<>();
        protocolo.datacopies = new HashMap<>();
        protocolo.Escalonamento = new LinkedList<>();
        protocolo.OperacoesRestantes = new LinkedList<>();
    }

    public static Database criarBancoDeDadosExemplo1() {
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

        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

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

    @Test
    public void testEscalonamentoEscritas() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroX = database.buscarRegistro("X");
        Registro registroY = database.buscarRegistro("Y");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Write(1, registroX)); // Transação 1 escreve em Registro X
        operacoes.add(new Read(2, registroY));   // Transação 2 lê Registro Y
        operacoes.add(new Read(1, registroY));   // Transação 1 lê Registro Y
        operacoes.add(new Write(2, registroX));  // Transação 2 escreve em Registro X

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(3, protocolo.Escalonamento.size()); //O último write não vai ser permitido entrar
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        // Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Write);
        
        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(2).transaction);
        // Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction);
    }

    @Test
    public void testCommitDeadlock() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroX = database.buscarRegistro("X");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Write(1, registroX));  // W1(X)
        operacoes.add(new Read(1, registroX));   // R1(X)
        operacoes.add(new Read(2, registroX));   // R2(X)
        operacoes.add(new Commit(1));            // C1
        operacoes.add(new Write(2, registroX));  // W2(X)
        operacoes.add(new Commit(2));            // C2

        //Ordem esperada => c1 não consegue comitar pois r2(x) está lendo a cópia antiga do banco de dados
        // c1 espera a transação t2
        // w2 cria uma nova copia do banco de dados e é escalonado
        // c2 não consegue comitar pois R1 está lendo o estado do banco de dados
        //Deadlock

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(3, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        // Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Write);

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(2).transaction);
        // Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction);
    }

    @Test
    public void testCommitMultiplosRegistros() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroY = database.buscarRegistro("Y");
        Registro registroX = database.buscarRegistro("X");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Write(1, registroX));  // W1(X) - Transação 1 escreve em X
        operacoes.add(new Read(1, registroX));   // R1(X) - Transação 1 lê o novo valor de X
        operacoes.add(new Read(2, registroX));   // R2(X) - Transação 2 lê o valor antigo de X (antes do commit de T1)
        operacoes.add(new Commit(1));            // C1 - Transação 1 comita
        operacoes.add(new Write(2, registroY));  // W2(Y) - Transação 2 escreve em Y
        operacoes.add(new Commit(2));            // C2 - Transação 2 comita

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(6, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(4) instanceof Commit);
        Assert.assertTrue(protocolo.Escalonamento.get(5) instanceof Commit);

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction); // W1(X)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction); // R1(X)
        Assert.assertTrue(2 == protocolo.Escalonamento.get(2).transaction); // R2(X)
        Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction); // W2(Y)
        Assert.assertTrue(2 == protocolo.Escalonamento.get(4).transaction); // C2
        Assert.assertTrue(1 == protocolo.Escalonamento.get(5).transaction); // C1
    }



}
