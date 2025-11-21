package com.libreria.data.dao;

import com.libreria.core.models.Configuracion;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(Configuracion.class)
public interface ConfiguracionDao {
    @SqlQuery("SELECT * FROM configuracion WHERE clave = ?")
    Optional<Configuracion> obtenerPorClave(String clave);

    @SqlQuery("SELECT * FROM configuracion")
    List<Configuracion> listarTodas();

    @SqlUpdate("UPDATE configuracion SET valor = ? WHERE clave = ?")
    void actualizar(String valor, String clave);
}
