import java.util.*;
import com.google.gson.Gson;

public class Controle {

    //create database {database: {pagina1:{...}, pagina2{...}}}

    //receber escalonamento r1(x with pagelock)w1(y with tablelock)w2(z)c2w3(y)r3(z)c1c3 -> retorna escalonamento no output
        //recebe string
        //Transforma em linkedList de operações 
        //roda o Protocolo.rodar(linkedList de operações)
        //Usa o escalonamento desse protocolo para criar uma string
        //Printa no output

    public static Database createDatabase(String path){
        JsonParser jsonParser = new JsonParser(false);
        Database database = jsonParser.jsonToDatabase(path);
        // System.out.println(database);
        return database;
    }

    public static LinkedList createOpsList(String path, Database database){
        JsonParser jsonParser = new JsonParser(true);
        LinkedList<Operacao> operacoes = jsonParser.jsonToOperacoes(path, database);
        return operacoes;
    }

    public static void runEscalonamento(LinkedList<Operacao> operacoes, Database db) {
        Protocolo2V2PL protocolo = new Protocolo2V2PL();
        protocolo.database = db;
        protocolo.rodar(operacoes);
        protocolo.Escalonamento.forEach(x -> System.out.print(x.getNome()));
    }

    void selecionarTransacao(){
        
    }
    
    void adicionarOperacao(){

    }

    void removerOperacao(){
        
    }

    void receberEscalonamento(){

    }

    void rodarAlgoritmo(){

    }
}
