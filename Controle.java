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

    void createDatabase(String path){
        JsonParser jsonParser = new JsonParser(false);
        Database database = jsonParser.jsonToDatabase(path);
        System.out.println(database);
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
