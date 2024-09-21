import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

//Todo protocolo vai poder rodar uma lista de operações em ordem cronológica
//Retorna o escalonamento correto dessa lista, depois de todos os bloqueios concedidos e liberados
interface Protocolo {
    void rodar(LinkedList<Operacao> OperacoesEmOrdemCronologica);
}

/**
 * 1. Scheduler recebe wj(x)
    Tenta obter wlj(x)
    Se existe wlk(x) ou clk(x), então
        Aguarda a concessão de wlj(x) e não
        escalona wj(x)
    Senão
        Concede o bloqueio wlj(x)
        Converte wj(x) em wj(xn)
        Escalona wj(xn)

2. Scheduler recebe rj(x)
    Tenta obter rlj(x)
    Se existe clk(x), então
        Aguarda a concessão de rlj(x) e não
        escalona rj(x)
    Senão
        Concede o bloqueio rlj(x)
    Se Tj possuía wlj(x) // executou wj(xn)
        Converte rj(x) em rj(xn)
        Escalona rj(xn)
    Senão
        Escalona rj(x)

 3. Scheduler recebe a operação cj
// Tenta Converter todos wlj em clj
    Enquanto houver wlj(x), faça
        Se existir rlk(x), com 0<K≤n. k ≠ j
            Aguarda a concessão de clj(x)
        Senão
        Concede o bloqueio clj(x)
    Fim-enquanto
    Escalona cj
**/
class Protocolo2V2PL implements Protocolo {

    //Inserir uma operação: verificar conflitos, se não houver escalona, 
    //se houver coloca nas operações restantes
    LinkedList<Operacao>      Escalonamento;
    LinkedList<Operacao>      OperacoesRestantes;

    //Detecção de conflitos -> grafo de serialização
    //Detecção de deadlock -> grafo de wait-for
    HashMap<Integer,Integer>  GrafoWaitFor;
    ArrayList <Bloqueio>      BloqueiosAtivos;

    //Parte da database e cópias de versão do 2v2pl
    Database                  database;
    HashMap<Integer,Data>     datacopies;
    


    //Melhor segmentar em transações mesmo, quanto uma espera pelo bloqueio da outra skipar
    //Ou talvez verificar se está no grafo wait for

    //Observer para relatar eventos da execução do algoritmo

    public void rodar(LinkedList<Operacao> OperacoesEmOrdemCronologica){

        //Reverte a ordem para o pop funcionar retirando elementos do início
        Collections.reverse(OperacoesEmOrdemCronologica);

        while(!OperacoesEmOrdemCronologica.isEmpty()){

            Operacao operacao = OperacoesEmOrdemCronologica.pop();

            Boolean  TransacaoEsperandoOutra = GrafoWaitFor.keySet().contains(operacao.transaction);

            //Verifica se está no grafo waitfor, se estiver skippa
            if(TransacaoEsperandoOutra) continue;

            //Tenta rodar operação, se não for sucesso então coloca nas operações restantes   
            if     (operacao instanceof Write  && rodarOperacao((Write)operacao)){
                Escalonamento.push(operacao);
            }
            else if(operacao instanceof Read   && rodarOperacao((Read)operacao)){
                Escalonamento.push(operacao);
            }
            else if(operacao instanceof Commit && rodarOperacao((Commit)operacao)){
                Escalonamento.push(operacao);
            }
            else if(operacao instanceof Abort  && rodarOperacao((Abort)operacao)){
                Escalonamento.push(operacao);
            }
            else{
                OperacoesRestantes.push(operacao);
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

        Bloqueio bloqueioExistente = write.registro.bloqueio;

        if(TabelaConflitos.podeConcederBloqueio(write)){
            write.registro.bloqueio = TabelaConflitos.obterBloqueio(write); //Talvez transformar em um setter dos registros

            //Criar cópia do banco de dados -> create copy
            this.criarCopia(write);

            //Escalona wj(xn) -> parte do 2v2PL
            return true;
        }
        else{
            //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
            Integer transaction = bloqueioExistente.getTransaction();
            
            //Senão botar transação em espera no grafo wait for 
            GrafoWaitFor.put(write.transaction, transaction);

            //Detectar Deadlock
            // if(detectarDeadlock()) abortarTransaction(write.transaction);
        }

        return false;
    }

    private boolean rodarOperacao(Read read){

        Registro registro = read.registro;

        Bloqueio bloqueioExistente = registro.bloqueio;

        if(TabelaConflitos.podeConcederBloqueio(read)){
            registro.bloqueio = TabelaConflitos.obterBloqueio(read); //Talvez transformar em um setter dos registros

            //Escalona wj(xn) -> parte do 2v2PL

            //Verifica se pertence a transação com a cópia do banco de dados

            //Se a transação Tj possua wlj(x), executou wj(xn)
            Data copia = datacopies.get(read.transaction);

            
            if(copia == null){
                //Escalona rj(x)
                Escalonamento.add(read);
            }
            else{
                //Converte  rj(x) em rj(xn)
                read.registro = copia.buscarRegistro(registro.nome);

                //Escalona rj(xn)
                return true;
            }
        }
        else{
            //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
            Integer transaction = bloqueioExistente.getTransaction();
            
            //Senão botar transação em espera no grafo wait for 
            GrafoWaitFor.put(read.transaction, transaction);

            //Detectar Deadlock
            // if(detectarDeadlock()) abortarTransaction(write.transaction);
        }

        //Ver como fazer essa parte depois
        return false;
    }

    //Caso de commit ou abort
    private boolean rodarOperacao(Commit commit){

        List<Bloqueio> writelock    = BloqueiosAtivos.stream()
                                                     .filter(bloqueio -> bloqueio.tipo == Bloqueio.type.ESCRITA && 
                                                                         bloqueio.transaction == commit.transaction)
                                                     .toList();

        List<Bloqueio> readlock     = BloqueiosAtivos.stream().filter(bloqueio -> bloqueio.tipo == Bloqueio.type.LEITURA).toList();
        // List<Bloqueio> certifylock  = BloqueiosAtivos.stream().filter(bloqueio -> bloqueio.tipo == Bloqueio.type.CERTIFY).toList();

        //Tenta converter todos wlj em clj
        //Enquanto houver wlj(x) faça
        for(var wlj : writelock){

            Bloqueio rlk = readlock.stream()
                                   .filter(rli -> rli.transaction != wlj.transaction && rli.registro.nome == wlj.registro.nome)
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
                  wlj.registro.bloqueio = TabelaConflitos.obterBloqueio(commit);
            }
        }
            
        //Escalona cj
        return true;
    }

    private boolean rodarOperacao(Abort abort){

        // liberar bloqueios da operação
        // Reverter todas as modificações
        liberarBloqueios(abort.transaction);
        abortarTransaction(abort.transaction);

        return true;

    }


    //Lógica de solicitar bloqueio, solucionar conflito, resolver deadlock...

    private boolean solicitarBloqueio(Operacao operacao){

        //Pegar registro referido da operação
        Registro registro = operacao.registro;
    
        if(TabelaConflitos.podeConcederBloqueio(operacao)){
            registro.bloqueio = TabelaConflitos.obterBloqueio(operacao);

            return true;
        }
        //Se ele tiver, ou ele é compartilhado usando a tabela de liberação de bloqueios ou não existe
        Integer transaction = registro.bloqueio.getTransaction();
        
        //Senão botar transação em espera no grafo wait for 
        GrafoWaitFor.put(operacao.transaction, transaction);

        //Detectar Deadlock
        if(detectarDeadlock()) abortarTransaction(operacao.transaction);

        return false;
    }

    private boolean solucionarConflito(Operacao operacao){

        return false;
    }

    //Se receber comando para abortar, ou se houver um deadlock
    private void abortarTransaction(Integer transaction){

        // Reverter as alterações da transação e liberar seus bloqueios
        liberarBloqueios(transaction);

        // Remover todas as dependências no grafo wait-for
        GrafoWaitFor.entrySet().removeIf(entry -> entry.getKey().equals(transaction) || entry.getValue().equals(transaction));

        System.out.println("Transação " + transaction + " abortada devido a deadlock.");

    }

    private void liberarBloqueios(Integer transaction){
        // liberar bloqueios da transação
        List<Operacao> Transaction = Escalonamento
                                         .stream()
                                         .filter(o -> o.transaction == transaction && !(o instanceof Abort || o instanceof Commit))
                                         .toList();

        for(Operacao operacao : Transaction){

            Registro registro = operacao.registro;
            Pagina   pagina   = registro.pagina;
            Tabela   tabela   = pagina.tabela;

            if(registro.bloqueio.getTransaction() == transaction){
                registro.bloqueio = null;
            }

            if(pagina.bloqueio.getTransaction() == transaction){
                pagina.bloqueio = null;
            }

            if(tabela.bloqueio.getTransaction() == transaction){
                tabela.bloqueio = null;
            }
         }

         // Remove aresta do grafo wait for com essa transação
         GrafoWaitFor.entrySet()
                     .removeIf(
                        entry -> entry.getKey().equals(transaction) || 
                                 entry.getValue().equals(transaction)
                    );
    }

    private void criarCopia(Operacao operacao){

        Integer transaction = operacao.transaction;


        switch(operacao.escopoLock){
            case rowlock: {
                Data data = operacao.registro.clonar();
                
                datacopies.put(transaction, data);

                break;
            }

            case pagelock: {
                Data data = operacao.registro.pagina.clonar();

                datacopies.put(transaction, data);

                break;
            }

            case tablelock: {
                Data data = operacao.registro.pagina.tabela.clonar();

                datacopies.put(transaction, data);

                break;
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