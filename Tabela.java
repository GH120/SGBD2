import java.util.ArrayList;

interface Data{
    boolean temBloqueio(Operacao operacao);
}

public class Tabela implements Data{

    ArrayList<Tupla> tuplas;
    Bloqueio         bloqueio;

    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    public void propagarBloqueio(Bloqueio bloqueio) {
        this.bloqueio = bloqueio;
    }
}

class Tupla implements Data{

    Tabela              tabela;
    ArrayList<Registro> registros;
    Bloqueio            bloqueio;


    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    public void propagarBloqueio(Bloqueio bloqueio) {
        this.bloqueio = bloqueio;
        tabela.propagarBloqueio(bloqueio);
    }
}


class Registro implements Data{
    String   Nome;
    Integer  valor;
    Tupla    tupla;
    Bloqueio bloqueio;

    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    //Propaga bloqueios intecionais;
    public void propagarBloqueio(Bloqueio bloqueio) {

        tupla.propagarBloqueio(bloqueio);
    }
}