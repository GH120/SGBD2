import java.util.*;

public class Main{
    public static void main(String[] args) {

        Controle controle = new Controle("resources/dbs/database1.json","resources/ops/database1.json");

        controle.runEscalonamento(args[0]);
    }
}
