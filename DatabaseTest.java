import org.junit.*;

import java.util.ArrayList;

public class DatabaseTest {

    @Test
    public void testInstanciarRegistro() {
        // Creating a Registro instance
        Pagina pagina = new Pagina(new ArrayList<>(), null); // Empty pagina for now
        Registro registro = new Registro("TestName", 100, pagina);

        // Assert that properties are set correctly
        Assert.assertEquals("TestName", registro.nome);
        Assert.assertTrue(100 == registro.valor);
        Assert.assertEquals(pagina, registro.pagina);
    }

    @Test
    public void testClonarRegistro() {
        // Setup for cloning test
        Pagina pagina = new Pagina(new ArrayList<>(), null);
        Registro originalRegistro = new Registro("Original", 200, pagina);

        // Clone the Registro object
        Registro clonedRegistro = (Registro) originalRegistro.clonar();

        // Assert that the cloned object is a different instance
        Assert.assertNotSame(originalRegistro, clonedRegistro);

        // Assert that values are correctly copied
        Assert.assertEquals(originalRegistro.nome, clonedRegistro.nome);
        Assert.assertEquals(originalRegistro.valor, clonedRegistro.valor);

        // Assert that the cloned object points to the same page
        Assert.assertEquals(originalRegistro.pagina, clonedRegistro.pagina);
    }

    @Test
    public void testClonarPagina() {
        // Create a Registro and add it to the Pagina
        Pagina originalPagina = new Pagina(new ArrayList<>(), null);
        Registro registro = new Registro("Test", 100, originalPagina);
        originalPagina.registros.add(registro);

        // Clone the Pagina object
        Pagina clonedPagina = (Pagina) originalPagina.clonar();

        // Assert that the cloned object is a different instance
        Assert.assertNotSame(originalPagina, clonedPagina);

        // Assert that the cloned Pagina contains a cloned Registro
        Registro clonedRegistro = (Registro) clonedPagina.registros.get(0);
        Assert.assertNotSame(registro, clonedRegistro);

        // Assert that the values are correctly copied
        Assert.assertEquals(registro.nome, clonedRegistro.nome);
        Assert.assertEquals(registro.valor, clonedRegistro.valor);
    }

    @Test
    public void testClonarTabela() {

        ArrayList<Pagina> paginas = new ArrayList<>();
        Tabela tabela = new Tabela(paginas, null);
        // Criar uma Página e adicionar um Registro a ela
        ArrayList<Registro> registros = new ArrayList<>();
        Pagina pagina = new Pagina(registros, tabela);
        Registro registro = new Registro("Registro1", 100, pagina);
        registros.add(registro);

        // Criar uma Tabela e adicionar a Página a ela
        paginas.add(pagina);

        // Clonar a Tabela
        Tabela clonedTabela = (Tabela) tabela.clonar();

        // Assert que a Tabela clonada é uma instância diferente
        Assert.assertNotSame(tabela, clonedTabela);

        // Assert que a Tabela clonada contém uma Página clonada
        Pagina clonedPagina = clonedTabela.paginas.get(0);
        Assert.assertNotSame(pagina, clonedPagina);

        // Assert que o Registro na Página clonada é uma instância diferente
        Registro clonedRegistro = clonedPagina.registros.get(0);
        Assert.assertNotSame(registro, clonedRegistro);

        // Assert que os valores do Registro clonados foram copiados corretamente
        Assert.assertEquals(registro.nome, clonedRegistro.nome);
        Assert.assertEquals(registro.valor, clonedRegistro.valor);
    }


    @Test
    public void testClonarDatabase() {
        // Setup for a complex cloning test with Database -> Tabela -> Pagina -> Registro

        ArrayList<Tabela> tabelas = new ArrayList<>();
        Database database = new Database(tabelas);

        ArrayList<Pagina> paginas = new ArrayList<>();
        Tabela tabela = new Tabela(paginas, database);

        
        ArrayList<Registro> registros = new ArrayList<>();
        Pagina pagina = new Pagina(registros, tabela);

        
        registros.add(new Registro("Registro1", 100, pagina));

        paginas.add(pagina);

        tabelas.add(tabela);

        // Clone the Database object
        Database clonedDatabase = database.clonar();

        // Assert that the cloned database is a different instance
        Assert.assertNotSame(database, clonedDatabase);

        Assert.assertTrue(database.tabelas.size() != 0);
        Assert.assertTrue(clonedDatabase.tabelas.size() != 0);

        // Assert that the cloned database has the correct structure
        Tabela clonedTabela = (Tabela) clonedDatabase.tabelas.get(0);
        Pagina clonedPagina = (Pagina) clonedTabela.paginas.get(0);
        Registro clonedRegistro = (Registro) clonedPagina.registros.get(0);

        // Assert that the values are correctly copied
        Assert.assertEquals("Registro1", clonedRegistro.nome);
        Assert.assertTrue(100 == clonedRegistro.valor);
    }
}
