package space.cubicworld.core;

import lombok.Getter;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.model.CoreTeamInvitation;
import space.cubicworld.core.model.CoreTeamMember;

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
                .addAnnotatedClass(CoreTeam.class)
                .addAnnotatedClass(CoreTeamMember.class)
                .addAnnotatedClass(CoreTeamInvitation.class)
                .setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQLDialect")
                .setProperty(AvailableSettings.URL, "jdbc:mysql://%s/%s".formatted(mysqlHost, mysqlDatabase))
                .setProperty(AvailableSettings.USER, mysqlUsername)
                .setProperty(AvailableSettings.PASS, mysqlPassword)
                .setProperty(AvailableSettings.HBM2DDL_AUTO, "update")
                .setProperty(
                        AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS,
                        "org.hibernate.context.internal.ThreadLocalSessionContext"
                )
                .buildSessionFactory();
    }

    public Transaction beginTransaction() {
        Transaction transaction = hibernateSessionFactory
                .getCurrentSession()
                .getTransaction();
        if (!transaction.isActive()) transaction.begin();
        return transaction;
    }

    public Transaction commitTransaction() {
        Transaction transaction = hibernateSessionFactory
                .getCurrentSession()
                .getTransaction();
        if (transaction.isActive()) transaction.commit();
        return transaction;
    }

}
