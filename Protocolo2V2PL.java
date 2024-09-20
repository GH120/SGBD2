import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    //Detecção de conflitos -> grafo de serialização
    //Detecção de deadlock -> grafo de wait-for
    //Inserir uma operação: verificar conflitos, se não houver escalona e 

    LinkedList<Operacao>      Escalonamento;
    LinkedList<Operacao>      OperacoesRestantes;
    HashMap<Integer,Integer>  GrafoWaitFor;

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
            if(rodarOperacao(operacao)){
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

    //Casos de inserção das operações
    //Overloading de métodos para tratar cada caso (read,write,commit,abort)
    private boolean rodarOperacao(Operacao operacao){

        if(solicitarBloqueio(operacao)){
            return true;
        } 
        else {
            solucionarConflito(operacao);
            return false;
        }
    }

    //Caso de commit ou abort
    private boolean rodarOperacao(Commit commit){

        liberarBloqueios(commit.transaction);

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
    
        if(TabelaConflitos.podeConcederBloqueio(registro.bloqueio, operacao)){
            registro.bloqueio = TabelaConflitos.getBloqueio(operacao);

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
            Pagina    tupla    = registro.tupla;
            Tabela   tabela   = tupla.tabela;

            if(registro.bloqueio.getTransaction() == transaction){
                registro.bloqueio = null;
            }

            if(tupla.bloqueio.getTransaction() == transaction){
                tupla.bloqueio = null;
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