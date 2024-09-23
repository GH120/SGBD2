import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

//Todo protocolo vai poder rodar uma lista de operações em ordem cronológica
//Retorna o escalonamento correto dessa lista, depois de todos os bloqueios concedidos e liberados
interface Protocolo {
    void rodar(LinkedList<Operacao> OperacoesEmOrdemCronologica);
}

class Protocolo2V2PL implements Protocolo {

    //Inserir uma operação: verificar conflitos, se não houver escalona, 
    //se houver coloca nas operações restantes
    LinkedList<Operacao>      Escalonamento;
    LinkedList<Operacao>      OperacoesRestantes;
    LinkedList<Operacao>      OperacoesEmOrdemCronologica;

    //Detecção de conflitos -> grafo de serialização
    //Detecção de deadlock -> grafo de wait-for
    HashMap<Integer,Integer>  GrafoWaitFor;
    ArrayList<Integer>        TransacoesEsperando;
    ArrayList<Integer>        OrdemInsercaoTransacoes;
    ArrayList <Bloqueio>      BloqueiosAtivos;
    Boolean                   pararExecucaoEmDeadlock = false;

    //Parte da database e cópias de versão do 2v2pl
    Database                  database;
    HashMap<Integer,Database> datacopies;


    // Construtor que recebe a database
    public Protocolo2V2PL() {
        this.Escalonamento = new LinkedList<>();           // Inicializa a lista de operações escalonadas
        this.OperacoesRestantes = new LinkedList<>();      // Inicializa a lista de operações restantes
        this.GrafoWaitFor = new HashMap<>();               // Inicializa o grafo de wait-for
        this.BloqueiosAtivos = new ArrayList<>();          // Inicializa a lista de bloqueios ativos
        this.datacopies = new HashMap<>();                 // Inicializa o mapa de cópias da database
        this.OperacoesEmOrdemCronologica = new LinkedList<>(); // Inicializa a lista de operações cronológicas
        this.OrdemInsercaoTransacoes = new ArrayList<>();  // Inicializa a lista de transações em ordem de inserção
        this.TransacoesEsperando  = new ArrayList<>();
    }
    
    


    //Melhor segmentar em transações mesmo, quanto uma espera pelo bloqueio da outra skipar
    //Ou talvez verificar se está no grafo wait for

    //Observer para relatar eventos da execução do algoritmo

    public void rodar(LinkedList<Operacao> scheduleRecebido){

        OperacoesEmOrdemCronologica = scheduleRecebido;

        int i = 0;
        while(i < 100000){

            //Retoma as transações que forem paradas
            retomarTransacoesParadas();

            Operacao operacao = OperacoesEmOrdemCronologica.pop();

            Boolean  TransacaoEsperandoOutra = GrafoWaitFor.keySet().contains(operacao.transaction);

            //Verifica se está no grafo waitfor, esperando liberar outra transação, se estiver skippa
            if(TransacaoEsperandoOutra){
                OperacoesRestantes.add(operacao);

                // if(commitsInexistentes()) return;

                // System.out.println("Tamanho: " + OperacoesRestantes.size() + "outro: " + OperacoesEmOrdemCronologica.size());
            }
            //TRATATAMENTO DE OPERAÇÕES//
            else{

                //Tenta rodar operação, se não for sucesso então coloca nas operações restantes   
                if     (operacao instanceof Write  && rodarOperacao((Write)operacao)){
                    escalonarOperacao(operacao);
                }
                else if(operacao instanceof Read   && rodarOperacao((Read)operacao)){
                    escalonarOperacao(operacao);
                }
                else if(operacao instanceof Update && rodarOperacao((Update)operacao)){
                    escalonarOperacao(operacao);
                }
                else if(operacao instanceof Commit && rodarOperacao((Commit)operacao)){

                    escalonarOperacao(operacao);

                    sincronizarBancosDeDados(operacao.transaction);

                    liberarBloqueios(operacao.transaction); //Libera os bloqueios do commit

                    //Enquanto estiver sincronizando os certify locks estão funcionando, 
                    //Outras operações que não afetem os dados sincronizados poderão ser efetivadas
                    CompletableFuture.runAsync(() -> {
                        
                        //Retirado daqui por causar comportamento imprevisível
                        //Se requisitado pelo professor, reinserir

                    });
                }
                else if(operacao instanceof Abort){
                    escalonarOperacao(operacao);

                    abortarTransaction(operacao.transaction);
                }
                else{
                    OperacoesRestantes.push(operacao);
                    System.out.println("Operação " + operacao.getNome() + " espera T" + GrafoWaitFor.get(operacao.transaction));
                }
            }

            //DETECÇÃO DE DEADLOCKS//
            if(detectarDeadlock()) {
                solucionarDeadlock(operacao);
            }

            //Se terminar as operações em ordem cronológica, 
            //mas ainda houver operações em espera,
            //Então elas vão ser as novas operações em ordem cronológica
            if(OperacoesEmOrdemCronologica.isEmpty()){

                if(OperacoesRestantes.isEmpty()) return; //Caso de parada, terminou execução

                OperacoesEmOrdemCronologica = new LinkedList<>(OperacoesRestantes);

                OperacoesRestantes.clear();

                Collections.reverse(OperacoesEmOrdemCronologica); //Pop agora funciona no início

            }

        }

        System.out.println("Loop Eterno");

    }

    private boolean rodarOperacao(Write write){

        Registro registro = write.registro;

        List<Bloqueio> bloqueios = BloqueiosAtivos.stream()
                                                  .filter(b -> b.data.igual(registro))
                                                  .collect(Collectors.toList());

        if(TabelaConflitos.podeConcederBloqueio(write, bloqueios)){

            Bloqueio bloqueio = TabelaConflitos.obterBloqueio(write); //Talvez transformar em um setter dos registros

            BloqueiosAtivos.add(bloqueio);

            registro.propagarBloqueio(bloqueio); //Cria os bloqueios intencionais 

            //Se a transação Tj possua wlj(x), executou wj(xn)
            Database copiaBD = datacopies.get(write.transaction);

            if(copiaBD == null){ 
                //Criar cópia do banco de dados -> create copy
                this.criarCopia(write);
            }
            else{
                //Converte  rj(x) em rj(xn)
                write.registro = copiaBD.buscarRegistro(registro.nome);
            }

            //Escalona wj(xn) -> parte do 2v2PL
            return true;
        }
        else{

            ArrayList<Bloqueio> todosBloqueios = new ArrayList<Bloqueio>(bloqueios);

            //Intencionais e sobre tabelas
            todosBloqueios.addAll(write.registro.bloqueios);
            todosBloqueios.addAll(write.registro.pagina.bloqueios);
            todosBloqueios.addAll(write.registro.pagina.tabela.bloqueios);
            todosBloqueios.addAll(database.bloqueios);

            todosBloqueios
            .stream()
            .filter(bloqueio -> !TabelaConflitos.podeConcederBloqueio(bloqueio, write))
            .forEach(bloqueioIncompativel -> {

                //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
                Integer transaction = bloqueioIncompativel.getTransaction();
                
                //Senão botar transação em espera no grafo wait for 
                GrafoWaitFor.put(write.transaction, transaction);

                if(!TransacoesEsperando.contains(transaction)) TransacoesEsperando.add(write.transaction);

                //Coloca ela como transação em standby
            });
        }

        return false;
    }

    private boolean rodarOperacao(Read read){

        Registro registro = read.registro;

        List<Bloqueio> bloqueios = BloqueiosAtivos.stream()
                                                  .filter(b -> b.data.igual(registro))
                                                  .collect(Collectors.toList());

        if(TabelaConflitos.podeConcederBloqueio(read, bloqueios)){
            
            Bloqueio bloqueio = TabelaConflitos.obterBloqueio(read); //Talvez transformar em um setter dos registros

            BloqueiosAtivos.add(bloqueio);

            registro.propagarBloqueio(bloqueio); //Cria os bloqueios intencionais

            //Verifica se pertence a transação com a cópia do banco de dados

            //Se a transação Tj possua wlj(x), executou wj(xn)
            Database copiaBD = datacopies.get(read.transaction);

            
            if(copiaBD == null){
                //Escalona rj(x)
                return true;
            }
            else{
                //Converte  rj(x) em rj(xn)
                read.registro = copiaBD.buscarRegistro(registro.nome);

                //Escalona rj(xn)
                return true;
            }
        }
        else{

            ArrayList<Bloqueio> todosBloqueios = new ArrayList<Bloqueio>(bloqueios);

            todosBloqueios.addAll(read.registro.bloqueios);
            todosBloqueios.addAll(read.registro.pagina.bloqueios);
            todosBloqueios.addAll(read.registro.pagina.tabela.bloqueios);
            todosBloqueios.addAll(database.bloqueios);

            todosBloqueios
            .forEach(bloqueioIncompativel -> {

                //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
                Integer transaction = bloqueioIncompativel.getTransaction();
                
                //Senão botar transação em espera no grafo wait for 
                GrafoWaitFor.put(read.transaction, transaction);

                if(!TransacoesEsperando.contains(transaction)) TransacoesEsperando.add(read.transaction);

            });
        }

        //Ver como fazer essa parte depois
        return false;
    }

    private boolean rodarOperacao(Update update){

        Registro registro = update.registro;

        List<Bloqueio> bloqueios = BloqueiosAtivos.stream()
                                                  .filter(b -> b.data.igual(registro))
                                                  .collect(Collectors.toList());

        if(TabelaConflitos.podeConcederBloqueio(update, bloqueios)){

            Bloqueio bloqueio = TabelaConflitos.obterBloqueio(update); //Talvez transformar em um setter dos registros

            BloqueiosAtivos.add(bloqueio);

            registro.propagarBloqueio(bloqueio); //Cria os bloqueios intencionais 

            //Escalona wj(xn) -> parte do 2v2PL
            return true;
        }
        else{
            //Refatorar depois
            ArrayList<Bloqueio> todosBloqueios = new ArrayList<Bloqueio>(bloqueios);

            //Bloqueios intencionais e concretos dos ascendentes
            todosBloqueios.addAll(update.registro.bloqueios);
            todosBloqueios.addAll(update.registro.pagina.bloqueios);
            todosBloqueios.addAll(update.registro.pagina.tabela.bloqueios);
            todosBloqueios.addAll(database.bloqueios);

            //Vê qual deles causou o problema
            todosBloqueios
            .stream()
            .filter(bloqueio -> !TabelaConflitos.podeConcederBloqueio(bloqueio, update))
            .forEach(bloqueioIncompativel -> {

                //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
                Integer transaction = bloqueioIncompativel.getTransaction();
                
                //Senão botar transação em espera no grafo wait for 
                GrafoWaitFor.put(update.transaction, transaction);

                if(!TransacoesEsperando.contains(transaction)) TransacoesEsperando.add(update.transaction);


            });

            return false;
        }

    }
    //Caso de commit ou abort
    private boolean rodarOperacao(Commit commit){

        //Todos os writelocks desse commit
        List<Bloqueio> writelock    = BloqueiosAtivos.stream()
                                                     .filter(bloqueio -> bloqueio.tipo == Bloqueio.type.ESCRITA)
                                                     .filter(bloqueio -> bloqueio.transaction == commit.transaction)
                                                     .collect(Collectors.toList());

        //Todos os readlocks existentes no BD
        List<Bloqueio> readlock     = BloqueiosAtivos.stream()
                                                     .filter(bloqueio -> bloqueio.tipo == Bloqueio.type.LEITURA)
                                                     .collect(Collectors.toList());

        //Tenta converter todos wlj em clj
        //Enquanto houver wlj(x) faça
        for(var wlj : writelock){

            Bloqueio rlk = null;

            //Maior gambiarra de todos os tempos
            //Poderia refatorar em um loop while usando bloqueios no escopo
            for(Bloqueio read : readlock){
                if(read.transaction == wlj.transaction) continue;

                boolean registroIgual = read.data.igual(wlj.data);
                boolean paginaIgual   = read.data.getPai().equals(wlj.data.getPai());
                boolean tabelaIgual   = read.data.getPai().getPai().equals(wlj.data.getPai().getPai());

                if(read.escopo == Operacao.lock.rowlock){
                    if(registroIgual){
                        rlk = read; 
                        break;
                    }
                }

                if(read.escopo == Operacao.lock.pagelock){
                    if(registroIgual || paginaIgual){
                        rlk = read; 
                        break;
                    }
                }

                if(read.escopo == Operacao.lock.tablelock){
                    if(registroIgual || paginaIgual || tabelaIgual){
                        rlk = read; 
                        break;
                    }
                }
            }

            //Se existir rlk(x), com 0<K<=n, k != j
            if(rlk != null){
                //Aguarda a concessão de clj(x)
                GrafoWaitFor.put(commit.transaction, rlk.transaction);

                if(!TransacoesEsperando.contains(commit.transaction)) TransacoesEsperando.add(commit.transaction);

                return false;
            }

            else{
                 //Concede o bloqueio clj(x)
                  Bloqueio bloqueio = TabelaConflitos.obterBloqueio(commit);

                  bloqueio.data   = wlj.data;

                  bloqueio.escopo = wlj.escopo;
                  
                  bloqueio.data.propagarBloqueio(bloqueio);

                  BloqueiosAtivos.add(bloqueio);
            }
        }
            
        //Escalona cj
        return true;
    }


    private void escalonarOperacao(Operacao operacao){
        Escalonamento.add(operacao);

        System.out.println("Operação " + operacao.getNome() + " escalonada");

        //Se for a primeira operação da transação a ser escalonada, adiciona ela na ordem das transações
        //Útil para saber quem a transação mais nova a ser abortada no deadlock
        if(!OrdemInsercaoTransacoes.contains(operacao.transaction)) 
            OrdemInsercaoTransacoes.add(operacao.transaction);
    }

    //Lógica de solicitar bloqueio, solucionar conflito, resolver deadlock...


    private void retomarTransacoesParadas(){

        Iterator<Integer> iterator = TransacoesEsperando.iterator();

        //Pega todas as transações em espera e coloca elas na ordem cronológica
        while(iterator.hasNext()){

            Integer transaction = iterator.next();

            Boolean transactionAtiva = !GrafoWaitFor.keySet().contains(transaction);

            if(transactionAtiva){
                OperacoesRestantes.stream()
                                  .filter(o -> o.transaction == transaction)
                                  .forEach(o -> OperacoesEmOrdemCronologica.push(o));
                OperacoesRestantes.removeIf(o -> o.transaction == transaction);

                iterator.remove();
            }
        }
    }

    private boolean solucionarDeadlock(Operacao operacao){

        //Obter as transações em Deadlock
        Integer transaction      = operacao.transaction;
        Integer otherTransaction = GrafoWaitFor.get(transaction);

        //Abortar a transação mais recente
        Integer transacaoAbortada = (OrdemInsercaoTransacoes.indexOf(transaction) > 
                                     OrdemInsercaoTransacoes.indexOf(otherTransaction))? 
                                     transaction : otherTransaction;

        abortarTransaction(transacaoAbortada);

        //Tenta realizar a operação de novo se não for a transação abortada
        if(transacaoAbortada != transaction) 
            OperacoesRestantes.add(operacao);

        return false;
    }

    //Se receber comando para abortar, ou se houver um deadlock
    private void abortarTransaction(Integer transaction){

        //Reverter as alterações da transação -> não precisa, pois não foram comitadas, podemos até retirar a copia do BD
        datacopies.remove(transaction);

        // liberar seus bloqueios
        liberarBloqueios(transaction);

        // Remover todas as dependências no grafo wait-for
        GrafoWaitFor.entrySet().removeIf(entry -> entry.getKey().equals(transaction) || entry.getValue().equals(transaction));

        // Retirar todas as Operações da Transação
        OperacoesRestantes         .removeIf(operacao -> operacao.transaction == transaction);
        OperacoesEmOrdemCronologica.removeIf(operacao -> operacao.transaction == transaction);
        Escalonamento.removeIf(operacao -> operacao.transaction == transaction);

        System.out.println("Transação " + transaction + " abortada");

    }

    private void liberarBloqueios(Integer transaction){
        // liberar bloqueios da transação

        // retira os intencionais
        BloqueiosAtivos.stream()
                       .filter(bloqueio  -> bloqueio.transaction == transaction)
                       .forEach(bloqueio -> bloqueio.data.removerBloqueios(transaction));

        // retira os bloqueios dos ativos
        BloqueiosAtivos.removeIf(bloqueio -> bloqueio.transaction == transaction);

        // Remove aresta do grafo wait for com essa transação
        GrafoWaitFor.entrySet()
                    .removeIf(
                    entry -> entry.getKey().equals(transaction) || 
                                entry.getValue().equals(transaction)
                );
    }

    private void criarCopia(Operacao operacao){

        Integer transaction = operacao.transaction;


        //Cria uma cópia do banco de dados, melhor copiar ele todo pois é mais fácil de implementar
        //Antes tentei fazer algo mais eficiente, mas é trabai dimar
        switch(operacao.escopoLock){
            case rowlock: {
                Database data = (Database) database.clonar();
                
                datacopies.put(transaction, data);

                break;
            }

            case pagelock: {
                Database data = (Database) database.clonar();

                datacopies.put(transaction, data);

                break;
            }

            case tablelock: {
                Database data = (Database) database.clonar();

                datacopies.put(transaction, data);

                break;
            } 
        }
    }

    private void sincronizarBancosDeDados(Integer transaction){

        Database DBCopia = datacopies.get(transaction);

        if(DBCopia != null){
            
            for(Tabela tabela : DBCopia.tabelas){
                for(Pagina pagina : tabela.paginas){
                    for(Registro registroCopia : pagina.registros){
                        
                        Registro original = database.buscarRegistro(registroCopia.nome);
                        
                        original.valor = registroCopia.valor;
                    }
                }
            }

        }
        else{

            Boolean  apenasUpdates = Escalonamento.stream()
                                              .filter(o -> o.transaction == transaction)
                                              .allMatch(o -> !(o instanceof Write));

            if(apenasUpdates) System.out.println("Apenas Updates, nenhuma sincronização");
            else System.out.println("Erro na Sincronização");
        }
    }
    

    //Detecção de deadlock

    private boolean detectarDeadlock() {
        // Realiza uma busca no grafo wait-for para detectar ciclos
        Map<Integer, Boolean> visitados = new HashMap<>();
        
        for (Integer transacao : GrafoWaitFor.keySet()) {
            if (detectaCiclo(transacao, visitados)) {
                return true; // Deadlock detectado
            }
        }
        
        return false;
    }
    
    private boolean detectaCiclo(Integer transacaoAtual, Map<Integer, Boolean> visitados) {
        if (visitados.containsKey(transacaoAtual) && visitados.get(transacaoAtual)) {
            return true; // Encontrou um ciclo
        }
    
        visitados.put(transacaoAtual, true); // Marca como visitado
    
        Integer transacaoDependente = GrafoWaitFor.get(transacaoAtual);
        if (transacaoDependente != null && detectaCiclo(transacaoDependente, visitados)) {
            return true; // Deadlock detectado
        }
    
        visitados.put(transacaoAtual, false); // Marca como processado
        return false;
    }

    // //Se não foram inseridos commits como operações
    // private void tratarCommitsInexistentes(){

    //     //Para todas as operações restantes, supondo que são todas as que faltam
    //     //Se houver alguma operação sem commit que esteja causando um deadlock
    //     for(Operacao operacao : OperacoesRestantes){

           

    //         if(semCommits) return true;
    //     }

    //     return false;
    // }
}

//Implementar uma classe tabela de conflitos para os bloqueios
//Implementar uma classe registro, tabela, ... que contém os bloqueios