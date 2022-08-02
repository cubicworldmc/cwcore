package space.cubicworld;

import lombok.Getter;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import space.cubicworld.model.CorePlayer;

@Getter
public class CorePlugin {

    private final SessionFactory hibernateSessionFactory;

    public CorePlugin(
            String mysqlHost,
            String mysqlUsername,
            String mysqlPassword,
            String mysqlDatabase
    ) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        hibernateSessionFactory = new Configuration()
                .addAnnotatedClass(CorePlayer.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect")
                .setProperty("hibernate.connection.url", "jdbc:mysql://%s/%s".formatted(mysqlHost, mysqlDatabase))
                .setProperty("hibernate.connection.username", mysqlUsername)
                .setProperty("hibernate.connection.password", mysqlPassword)
                .buildSessionFactory();
    }

}
