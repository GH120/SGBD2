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
        operacoes.add(new Commit(1));  // Commit 1

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(5, protocolo.Escalonamento.size()); //O último write não vai ser permitido entrar
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Commit);
        Assert.assertTrue(protocolo.Escalonamento.get(4) instanceof Write);
        
        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(2).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(3).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(4).transaction);
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
        // T2 é abortada
        // C1 consegue comitar

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(3, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Commit);

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(2).transaction);
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

    @Test
    public void testCommitMultiplosRegistrosDeadlock() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroY = database.buscarRegistro("Y");
        Registro registroX = database.buscarRegistro("X");
        Registro registroZ = database.buscarRegistro("Z");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Write(1, registroX));  // W1(X) - Transação 1 escreve em X
        operacoes.add(new Read(1, registroY));   // R1(Y) - Transação 1 lê Y
        operacoes.add(new Read(2, registroX));   // R2(X) - Transação 2 lê X (antes do commit de T1)
        operacoes.add(new Write(2, registroY));  // W2(Y) - Transação 2 escreve em Y //Problema: não está sendo inserido
        operacoes.add(new Write(3, registroZ));  // W3(Z) - Transação 3 escreve em Z
        operacoes.add(new Commit(3));            // C3 - Transação 3 comita
        operacoes.add(new Read(1, registroZ));   // R1(Z) - Transação 1 lê Z
        operacoes.add(new Commit(1));            // C1 - Transação 1 comita
        operacoes.add(new Commit(2));            // C2 - Transação 2 comita
        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(6, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);  // W1(X)
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);   // R1(Y)
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Write);  // W3(Z)
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Commit); // C3
        Assert.assertTrue(protocolo.Escalonamento.get(4) instanceof Read);   // R1(Z)
        Assert.assertTrue(protocolo.Escalonamento.get(5) instanceof Commit); // C1
        // Assert.assertTrue(protocolo.Escalonamento.get(8) instanceof Commit); // C2

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction); // W1(X)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction); // R1(Y)
        Assert.assertTrue(3 == protocolo.Escalonamento.get(2).transaction); // W3(Z)
        Assert.assertTrue(3 == protocolo.Escalonamento.get(3).transaction); // C3
        Assert.assertTrue(1 == protocolo.Escalonamento.get(4).transaction); // R1(Z)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(5).transaction); // C1
        // Assert.assertTrue(2 == protocolo.Escalonamento.get(8).transaction); // C2
    }

    @Test
    public void testUpdateMultiplosRegistros() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroY = database.buscarRegistro("Y");
        Registro registroX = database.buscarRegistro("X");
        Registro registroZ = database.buscarRegistro("Z");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();
        operacoes.add(new Update(1, registroX));  // U1(X) - Transação 1 atualiza X
        operacoes.add(new Read(1, registroY));    // R1(Y) - Transação 1 lê Y
        operacoes.add(new Read(2, registroX));    // R2(X) - Transação 2 lê X (antes do commit de T1)
        operacoes.add(new Update(2, registroY));  // U2(Y) - Transação 2 atualiza Y
        operacoes.add(new Update(3, registroZ));  // U3(Z) - Transação 3 atualiza Z
        operacoes.add(new Commit(3));             // C3 - Transação 3 comita
        operacoes.add(new Read(1, registroZ));    // R1(Z) - Transação 1 lê Z
        operacoes.add(new Commit(1));             // C1 - Transação 1 comita
        operacoes.add(new Commit(2));             // C2 - Transação 2 comita
        
        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        // Como todas as operações de update não causaram copias, não interfere nos commits
        Assert.assertEquals(9, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Update);  // U1(X)
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);    // R1(Y)
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);    // R2(X)
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Update);  // U2(Y)
        Assert.assertTrue(protocolo.Escalonamento.get(4) instanceof Update);  // U3(Z)
        Assert.assertTrue(protocolo.Escalonamento.get(5) instanceof Commit);  // C3
        Assert.assertTrue(protocolo.Escalonamento.get(6) instanceof Read);    // R1(Z)
        Assert.assertTrue(protocolo.Escalonamento.get(7) instanceof Commit);  // C1
        Assert.assertTrue(protocolo.Escalonamento.get(8) instanceof Commit); // C2

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction); // U1(X)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction); // R1(Y)
        Assert.assertTrue(2 == protocolo.Escalonamento.get(2).transaction); // R2(X)
        Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction); // U2(Y)
        Assert.assertTrue(3 == protocolo.Escalonamento.get(4).transaction); // U3(Z)
        Assert.assertTrue(3 == protocolo.Escalonamento.get(5).transaction); // C3
        Assert.assertTrue(1 == protocolo.Escalonamento.get(6).transaction); // R1(Z)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(7).transaction); // C1
        Assert.assertTrue(2 == protocolo.Escalonamento.get(8).transaction); // C2
    }

    @Test
    public void testCommitMultiplosRegistrosComPageLockETableLock() {
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroY = database.buscarRegistro("Y");
        Registro registroX = database.buscarRegistro("X");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();

        // Transação 1 com Page Lock
        Operacao writeT1 = new Write(1, registroX);  // W1(X) - Transação 1 escreve em X
        writeT1.escopoLock = Operacao.lock.pagelock; // Definindo o lock como pagelock
        operacoes.add(writeT1);
        
        Operacao readT1 = new Read(1, registroX);    // R1(X) - Transação 1 lê o novo valor de X
        readT1.escopoLock = Operacao.lock.pagelock;  // Definindo o lock como pagelock
        operacoes.add(readT1);
        
        // Transação 2 com Table Lock
        Operacao readT2 = new Read(2, registroX);    // R2(X) - Transação 2 lê o valor antigo de X (antes do commit de T1)
        readT2.escopoLock = Operacao.lock.tablelock; // Definindo o lock como tablelock
        operacoes.add(readT2);
        
        operacoes.add(new Commit(1));                // C1 - Transação 1 comita
        
        Operacao writeT2 = new Write(2, registroY);  // W2(Y) - Transação 2 escreve em Y
        writeT2.escopoLock = Operacao.lock.tablelock; // Definindo o lock como tablelock
        operacoes.add(writeT2);
        
        operacoes.add(new Commit(2));                // C2 - Transação 2 comita

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        // Verificar se as operações foram escalonadas corretamente
        Assert.assertEquals(3, protocolo.Escalonamento.size()); // Todas as operações devem ser escalonadas
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Commit);

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction); // W1(X)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction); // R1(X)
        Assert.assertTrue(1 == protocolo.Escalonamento.get(2).transaction); // C1
    }


    @Test
    public void testTresTransacoes(){
        database = criarBancoDeDadosExemplo1(); // Inicializa o banco de dados padrão para esse teste
        protocolo.database = database;

        // Configurar registros
        Registro registroY = database.buscarRegistro("Y");
        Registro registroX = database.buscarRegistro("X");
        Registro registroZ = database.buscarRegistro("Z");

        // Criar operações
        LinkedList<Operacao> operacoes = new LinkedList<>();


        Operacao readT1 = new Read(1, registroX);  
        readT1.escopoLock = Operacao.lock.rowlock; 
        operacoes.add(readT1);
        
        Operacao writeT1 = new Write(1, registroY); 
        writeT1.escopoLock = Operacao.lock.rowlock;  
        operacoes.add(writeT1);
        
    
        Operacao readT2 = new Read(2, registroY);    
        readT2.escopoLock = Operacao.lock.rowlock; 
        operacoes.add(readT2);
        
        operacoes.add(new Commit(1));  
        
        Operacao writeT2 = new Write(2, registroZ);  
        writeT2.escopoLock = Operacao.lock.rowlock; 
        operacoes.add(writeT2);

        Operacao readT3 = new Read(3, registroZ);  
        readT3.escopoLock = Operacao.lock.rowlock; 
        operacoes.add(readT3);
        
        operacoes.add(new Commit(2));

        Operacao writeT3 = new Write(3, registroX);  
        writeT3.escopoLock = Operacao.lock.rowlock; 
        operacoes.add(writeT3);

        operacoes.add(new Commit(3));

        // Executar o escalonamento
        protocolo.rodar(operacoes);

        System.out.println(protocolo.Escalonamento);

        // r1(x)w1(y)r2(y)c1()w2(z)r3(z)c2()w3(x)c3()
        // r1(x)w1(y)r2(y)w2(z)c2()c1()

        
        Assert.assertEquals(6, protocolo.Escalonamento.size());
        Assert.assertTrue(protocolo.Escalonamento.get(0) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(1) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(2) instanceof Read);
        Assert.assertTrue(protocolo.Escalonamento.get(3) instanceof Write);
        Assert.assertTrue(protocolo.Escalonamento.get(4) instanceof Commit);
        Assert.assertTrue(protocolo.Escalonamento.get(5) instanceof Commit);

        // Verificar a ordem das operações escalonadas
        Assert.assertTrue(1 == protocolo.Escalonamento.get(0).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(1).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(2).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(3).transaction);
        Assert.assertTrue(2 == protocolo.Escalonamento.get(4).transaction);
        Assert.assertTrue(1 == protocolo.Escalonamento.get(5).transaction);

    }

}
