import java.util.ArrayList;
import java.util.function.Predicate;

//TODO criar testes para verificar se clonagem está funcionando

abstract class Data {

    public Bloqueio bloqueio    = null;

    abstract boolean  temBloqueio(Operacao operacao);
    abstract boolean  propagarBloqueioIntencional(Bloqueio bloqueio);
    abstract Registro buscarRegistro(String nome);
    abstract Data     clonar(); 
    abstract Data     buscar(Predicate<Data> filtro);
    abstract Data     getPai();
    abstract void     setPai(Data data);
    abstract boolean  igual (Data data);

    //Get parent node e set parent node?
}

//Implementação do padrão composite, útil para modelar a árvore onde os nós folhas são os registros
abstract class Composite extends Data{

    private ArrayList<? extends Data> nodes; //nodes é uma maneira generalizada de chamar os filhos, se for database então seus filhos são tabelas, de tabelas páginas e assim em diante.

    Composite(ArrayList<? extends Data> nodes){
        this.nodes      = nodes;
    }

    public boolean temBloqueio(Operacao operacao) {
        return false;
    }

    public boolean propagarBloqueioIntencional(Bloqueio bloqueio) {
        return false;
    }

    public Data buscar(Predicate<Data> condition) {

        if (condition.test(this)) return this;

        else return  nodes.stream()
                          .map(node -> node.buscar(condition))
                          .filter(node -> node != null)
                          .findFirst()
                          .orElse(null);
    }

    public Registro buscarRegistro(String nome){

        Data registro = buscar(x -> x instanceof Registro && ((Registro) x).nome.equals(nome));

        return (Registro) registro;
    }

    //Extremamente ineficiente
    public boolean igual(Data data){

        Composite identico = (Composite) data;
        
        int i = 0;
        for(Data node : identico.nodes){
            if(!node.igual(this.nodes.get(i++))) 
                return false;
        }

        return true;
    }
}

//Composite é uma classe do padrão composite, útil para modelar a estrutura de árvore do BD
//Definida lá no final junto com os registros

class Database extends Composite {
    ArrayList<Tabela> tabelas;

    public Database(ArrayList<Tabela> tabelas) {
        super(tabelas);
        this.tabelas = tabelas;
    }

    public Database clonar() {
        Database copia = new Database(new ArrayList<>());

        for (Tabela tabela : tabelas) {

            Tabela cloneTabela = tabela.clonar();

            copia.tabelas.add(cloneTabela);

            cloneTabela.setPai(copia);
        }

        return copia;
    }

    public Data getPai(){
        return null;
    }

    public void setPai(Data data){
        return;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Database:\n");
        for (Tabela tabela : tabelas) {
            sb.append(tabela.toString()).append("\n");  // Chama o toString da classe Tabela
        }
        return sb.toString();
    }
}

class Tabela extends Composite {

    ArrayList<Pagina> paginas;
    Database        database;

    public Tabela(ArrayList<Pagina> paginas, Database database) {
        super(paginas);
        this.paginas = paginas;
        this.database = database;
    }

    public Tabela clonar() {
        Tabela copia = new Tabela(new ArrayList<>(), database);

        for (Pagina pagina : paginas) {

            Pagina paginaClone = pagina.clonar();

            copia.paginas.add(paginaClone);

            paginaClone.setPai(copia);
        }

        return copia;
    }

    public Data getPai(){
        return database;
    }

    public void setPai(Data database){
        this.database = (Database) database;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("  Tabela:\n");
        for (Pagina pagina : paginas) {
            sb.append(pagina.toString()).append("\n");  // Chama o toString da classe Pagina
        }
        return sb.toString();
    }

}

class Pagina extends Composite {

    ArrayList<Registro> registros;
    Tabela          tabela;

    public Pagina(ArrayList<Registro> registros, Tabela tabela) {
        super(registros);
        this.registros = registros;
        this.tabela    = tabela;
    }

    public Pagina clonar() {
        Pagina copia = new Pagina(new ArrayList<>(), tabela);

        for (Registro registro : registros) {

            Registro registroClone = registro.clonar();

            copia.registros.add(registroClone);

            registroClone.setPai(copia);
        }

        return copia;
    }

    public Tabela getPai(){
        return tabela;
    }

    public void setPai(Data tabela){
        this.tabela = (Tabela) tabela;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("      Pagina:\n");
        for (Registro registro : registros) {
            sb.append("    Registro: ").append(registro.nome)
              .append(", Valor: ").append(registro.valor).append("\n");
        }
        return sb.toString();
    }

}

class Registro extends Data {
    String   nome;
    Integer  valor;
    Pagina   pagina;

    public Registro(String nome, Integer valor, Pagina pagina) {
        this.nome       = nome;
        this.valor      = valor;
        this.pagina     = pagina;
    }

    public boolean temBloqueio(Operacao operacao) {
        return bloqueio != null;
    }

    public boolean propagarBloqueioIntencional(Bloqueio bloqueio) {
        if (this.bloqueio == null) {
            this.bloqueio = bloqueio;
            return true;
        }
        return false; // Logic for compatibility can be added
    }

    public Data buscar(Predicate<Data> condition) {
        if (condition.test(this)) return this;
        else                      return null;
    }

    public Registro buscarRegistro(String nome){
        if(this.nome == nome) return this;
        else                  return null;
    }

    public Registro clonar() {
        return new Registro(this.nome, this.valor, this.pagina); // Creates a new Registro with the same values
    }

    public boolean igual(Data data){

        Registro registroIgual = (Registro) data;

        return registroIgual.nome == nome;
    }

    public Pagina getPai(){
        return pagina;
    }

    public void setPai(Data pagina){
        this.pagina = (Pagina)pagina;
    }
}