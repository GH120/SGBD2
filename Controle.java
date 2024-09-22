import java.util.*;
import com.google.gson.Gson;

public class Controle {

    String pathDatabase;
    String pathOperacoes;
    Database database;

    //create database {database: {pagina1:{...}, pagina2{...}}}

    //receber escalonamento r1(x with pagelock)w1(y with tablelock)w2(z)c2w3(y)r3(z)c1c3 -> retorna escalonamento no output
        //recebe string
        //Transforma em linkedList de operações 
        //roda o Protocolo.rodar(linkedList de operações)
        //Usa o escalonamento desse protocolo para criar uma string
        //Printa no output


    Controle(String pathDatabase, String pathOperacoes){
        this.pathDatabase  = pathDatabase;
        this.pathOperacoes = pathOperacoes;
    }

    public Database createDatabase(){
        JsonParser jsonParser = new JsonParser(false);
        Database database = jsonParser.jsonToDatabase(pathDatabase);
        // System.out.println(database);

        this.database = database;

        return database;
    }

    public LinkedList<Operacao> createOpsList(){
        JsonParser jsonParser = new JsonParser(false);
        LinkedList<Operacao> operacoes = jsonParser.jsonToOperacoes(pathOperacoes, database);
        return operacoes;
    }

    public void runEscalonamento(LinkedList<Operacao> operacoes, Database db) {
        Protocolo2V2PL protocolo = new Protocolo2V2PL();
        protocolo.database = db;
        protocolo.rodar(operacoes);
        protocolo.Escalonamento.forEach(x -> System.out.print(x.getNome()));
    }

    public void parserOperacoes(String operacoesEscritas){
        try{

            ParserOperacoes.escreverParaArquivo(
                ParserOperacoes.processarOperacoes(operacoesEscritas),
                pathOperacoes
            );

        }
        catch(Exception e){
            System.out.println("Erro no processamento de operações");
        }
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
