import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    LinkedList<Operacao>      OperacoesEmOrdemCronologica = new LinkedList();

    //Detecção de conflitos -> grafo de serialização
    //Detecção de deadlock -> grafo de wait-for
    HashMap<Integer,Integer>  GrafoWaitFor;
    ArrayList<Integer>        OrdemInsercaoTransacoes = new ArrayList<>();
    ArrayList <Bloqueio>      BloqueiosAtivos;
    Boolean                   pararExecucaoEmDeadlock = false;

    //Parte da database e cópias de versão do 2v2pl
    Database                  database;
    HashMap<Integer,Database> datacopies;
    


    //Melhor segmentar em transações mesmo, quanto uma espera pelo bloqueio da outra skipar
    //Ou talvez verificar se está no grafo wait for

    //Observer para relatar eventos da execução do algoritmo

    public void rodar(LinkedList<Operacao> scheduleRecebido){

        OperacoesEmOrdemCronologica = scheduleRecebido;

        while(!OperacoesEmOrdemCronologica.isEmpty()){

            Operacao operacao = OperacoesEmOrdemCronologica.pop();

            Boolean  TransacaoEsperandoOutra = GrafoWaitFor.keySet().contains(operacao.transaction);

            //DETECÇÃO DE DEADLOCKS//
            if(detectarDeadlock()) {
                solucionarDeadlock(operacao);
            }

            //Verifica se está no grafo waitfor, esperando liberar outra transação, se estiver skippa
            else if(TransacaoEsperandoOutra){
                OperacoesRestantes.add(operacao);
                System.out.println("Tamanho: " + OperacoesRestantes.size() + "outro: " + OperacoesEmOrdemCronologica.size());
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
                }
            }

            //Se terminar as operações em ordem cronológica, 
            //mas ainda houver operações em espera,
            //Então elas vão ser as novas operações em ordem cronológica
            if(OperacoesEmOrdemCronologica.isEmpty()){
                OperacoesEmOrdemCronologica = OperacoesRestantes;

                Collections.reverse(OperacoesEmOrdemCronologica); //Pop agora funciona no início
            }

        }

    }

    private boolean rodarOperacao(Write write){

        Registro registro = write.registro;

        List<Bloqueio> bloqueios = BloqueiosAtivos.stream()
                                                  .filter(b -> b.data.igual(registro))
                                                  .toList();

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

            bloqueios
            .stream()
            .filter(bloqueio -> !TabelaConflitos.podeConcederBloqueio(Bloqueio.type.ESCRITA, bloqueio.tipo))
            .forEach(bloqueioIncompativel -> {

                //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
                Integer transaction = bloqueioIncompativel.getTransaction();
                
                //Senão botar transação em espera no grafo wait for 
                GrafoWaitFor.put(write.transaction, transaction);

            });
        }

        return false;
    }

    private boolean rodarOperacao(Read read){

        Registro registro = read.registro;

        List<Bloqueio> bloqueios = BloqueiosAtivos.stream()
                                                  .filter(b -> b.data.igual(registro))
                                                  .toList();

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

            bloqueios
            .stream()
            .filter(bloqueio -> !TabelaConflitos.podeConcederBloqueio(Bloqueio.type.LEITURA, bloqueio.tipo))
            .forEach(bloqueioIncompativel -> {

                //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
                Integer transaction = bloqueioIncompativel.getTransaction();
                
                //Senão botar transação em espera no grafo wait for 
                GrafoWaitFor.put(read.transaction, transaction);

            });
        }

        //Ver como fazer essa parte depois
        return false;
    }

    //Caso de commit ou abort
    private boolean rodarOperacao(Commit commit){

        //Todos os writelocks desse commit
        List<Bloqueio> writelock    = BloqueiosAtivos.stream()
                                                     .filter(bloqueio -> bloqueio.tipo == Bloqueio.type.ESCRITA)
                                                     .filter(bloqueio -> bloqueio.transaction == commit.transaction)
                                                     .toList();

        //Todos os readlocks existentes no BD
        List<Bloqueio> readlock     = BloqueiosAtivos.stream()
                                                     .filter(bloqueio -> bloqueio.tipo == Bloqueio.type.LEITURA)
                                                     .toList();

        //Tenta converter todos wlj em clj
        //Enquanto houver wlj(x) faça
        for(var wlj : writelock){

            Bloqueio rlk = readlock.stream()
                                   .filter(rli -> rli.data.igual(wlj.data) && rli.transaction != wlj.transaction)
                                   .findFirst()
                                   .orElse(null);

            //Se existir rlk(x), com 0<K<=n, k != j
            if(rlk != null){
                //Aguarda a concessão de clj(x)
                GrafoWaitFor.put(commit.transaction, rlk.transaction);

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

        //Se for a primeira operação da transação a ser escalonada, adiciona ela na ordem das transações
        //Útil para saber quem a transação mais nova a ser abortada no deadlock
        if(!OrdemInsercaoTransacoes.contains(operacao.transaction)) 
            OrdemInsercaoTransacoes.add(operacao.transaction);
    }

    //Lógica de solicitar bloqueio, solucionar conflito, resolver deadlock...

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

        System.out.println("Transação " + transaction + " abortada devido a deadlock.");

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

        for(Tabela tabela : DBCopia.tabelas){
            for(Pagina pagina : tabela.paginas){
                for(Registro registroCopia : pagina.registros){

                    Registro original = database.buscarRegistro(registroCopia.nome);

                    original.valor = registroCopia.valor;
                }
            }
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
}

//Implementar uma classe tabela de conflitos para os bloqueios
//Implementar uma classe registro, tabela, ... que contém os bloqueios