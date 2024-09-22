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

    void createDatabase(){

        JsonParser jsonParser = new JsonParser(true);
        jsonParser.jsonToDatabase("resources/database1.json");
        
        ArrayList<Tabela> tabelas = new ArrayList<>();
        
        Database database = new Database(tabelas);

        ArrayList<Pagina> paginas = new ArrayList<>();
        
        Tabela tabela = new Tabela(paginas, database);
        tabelas.add(tabela);

        ArrayList<Registro> registros = new ArrayList<>();
        
        Pagina pagina = new Pagina(registros, tabela);
        paginas.add(pagina);

        Registro registro = new Registro("Registro1", 100, pagina);
        registros.add(registro);
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
