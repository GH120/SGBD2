import java.util.*;

//Felipe Vieira Duarte 
//Thomaz Ângelo
//Guilherme de Menezes Furtado

//Funcionalidades implementadas 
    //Múltipla granulosidade     -> database, tabelas, páginas e tuplas
    //Granulosidade de bloqueios -> rowlock(padrão), pagelock, tablelock
    //Database em JSON           -> nos resources/dbs/database1.json, podem ter várias e customizáveis
    //Parser de Operações        -> pode inserir operações como strings para rodar o escalonamento, CUIDADO COM A GRAMÁTICA QUE PODE PASSAR ERROS DESPERCEBIDOS
    //Testes de Vários casos     -> ControleTest tem vários casos já implementados com saída

//Github: https://github.com/GH120/SGBD2

//ALGORITMO NO ARQUIVO PROTOCOLO2V2PL, MÉTODO RODAR(LikedList<Operacoes> OperacoesEmOrdemCronologica)

public class Main{
    public static void main(String[] args) {

        //ALGORITMO NO ARQUIVO PROTOCOLO2V2PL, MÉTODO RODAR(LikedList<Operacoes> OperacoesEmOrdemCronologica)

        //Escolhe a database e o arquivo onde vão ser inseridos o parsing das operações
        Controle controle = new Controle("resources/dbs/database2.json","resources/ops/database1.json");

        //Exemplos de Entradas e Saídas
        //Transações abortadas retiram operações do escalonamento final
        //"w1(x with tablelock) w2(z) c1 c2" -> W1(x)C1W2(z)C2 //Tablelock bloqueia W(z), que está na mesma tabela 
        //"w1(x with pagelock) w2(z) c1 c2"  -> W1(x)W2(z)C1C2 //Pagelock não bloqueia mais W(z), pois estão em páginas diferentes
        //"r1(x with pagelock) w1(y) r2(x with pagelock) c1 w2(z) r3(z) c2 w3(y) c3" -> R1(x)W1(y)R2(x)W2(z)C2C1
        //"r1(x) r2(x) w1(x) r1(z) c1 w2(y) r3(y) c2 w3(z) c3" -> R1(x)R2(x)W1(x)R1(z)W2(y)C2C1

        //Basta inserir as operações e rodar que funciona (CUIDADO COM A GRAMÁTICA, ERROS DE SINTAXE GERAM ERROS NO ALGORITMO)
        controle.runEscalonamento("r1(x) r2(x) w1(x) r1(z) c1 w2(y) r3(y) c2 w3(z) c3");
    }
}
