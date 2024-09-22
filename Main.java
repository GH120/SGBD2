import java.util.*;

public class Main{
    public static void main(String[] args) {

        Controle controle = new Controle("resources/dbs/database1.json","resources/ops/database1.json");

        String Operacoes = "w1(Registro1 with tablelock) r1(Registro1) u1(Registro2) c2 a3";

        Database database = controle.createDatabase();
        
        controle.parserOperacoes(Operacoes); //Parser nas strings

        LinkedList<Operacao> operacoes = controle.createOpsList();

        controle.runEscalonamento(operacoes, database);
    }
}
