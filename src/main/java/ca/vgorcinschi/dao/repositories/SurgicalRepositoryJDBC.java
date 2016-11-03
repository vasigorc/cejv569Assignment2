package ca.vgorcinschi.dao.repositories;

import static ca.vgorcinschi.CommonUtil.localToSql;
import ca.vgorcinschi.model.Surgical;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Surgical implementation of GenericRepository and DetailRecordMethods NB:
 * Please note that I didn't write an abstract class AbstractRepository <E>
 * a) because all methods would have to be overwritten. The underlying generic E
 * is too different so property types and number that we will extract in each
 * method will vary too much b) in terms of spring beans -that would leave us to
 * either create another abstract class per Detail Record or make beans from
 * final classes which is not optimal for testing different profiles
 *
 * @author v_gorcin
 */
@Repository
@Qualifier("jdbc")
public class SurgicalRepositoryJDBC implements SurgicalRepository {

    /**
     * note we will only use this logger to log methods that return boolean. For
     * methods which declare 'throws' we will use logger one level up in the
     * service interface
     */
    private final Logger log
            = LoggerFactory.getLogger(this.getClass().getName());

    //configured in src.main.resources.application.yml
    private final JdbcTemplate jdbcTemplate;

    //Spring's transaction template is used to rollback the transaction
    private final TransactionTemplate transactionTemplate;

    //point of injection
    @Autowired
    public SurgicalRepositoryJDBC(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public boolean add(Surgical entity) {
        //our prepared statement
        String sql = "INSERT INTO SURGICAL (PATIENTID, DATEOFSURGERY, SURGERY,"
                + "ROOMFEE, SURGEONFEE, SUPPLIES) VALUES (?, ?, ?, ?, ?, ?)";
        //using try-catch to log two scenarions
        try {
            /**
             * this method does auto-conversion provided the sequence of the
             * arguments is the same -
             * @link http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html#update-java.lang.String-java.lang.Object...-
             */
            jdbcTemplate.update(sql, entity.getPatientId(), localToSql.apply(entity.getDateOfSurgery()),
                    entity.getSurgery(), entity.getRoomFee(), entity.getSurgeonFee(),
                    entity.getSupplies());
            log.info("Surgical " + entity + " was successfully saved to the DB.");
            return true;
        } catch (DataAccessException e) {
            //log error and return false
            log.error("SQL query for adding Inpatient " + entity + " failed. "
                    + e.getMostSpecificCause().toString());
            return false;
        }
    }

    @Override
    public boolean update(Surgical entity) {
        if (entity.getId() > 0) {
            /*
             creating a new controlled transaction. obtain a callback function
             (new TransactionCallback<Boolean>(TransactionStatus)) from the 
             transaction template. This function takes only one argument - Transaction
             status which can rollback a transaction. Using lambda we avoid the
             explicit new TransactionCallback<Boolean>(TransactionStatus) declaration
             */
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                //prepared statement
                String updateStatement = "UPDATE SURGICAL SET PATIENTID = ?,"
                        + " DATEOFSURGERY=?, SURGERY=?, ROOMFEE=?, SURGEONFEE=? "
                        + ",SUPPLIES =? WHERE ID=?";
                //query arguments, jdbcTemplate will automatically cast the types
                Object[] args = {entity.getPatientId(), localToSql.apply(entity.getDateOfSurgery()),
                    entity.getSurgery(), entity.getRoomFee(), entity.getSurgeonFee(),
                    entity.getSupplies(), entity.getId()};
                //update the db, return the # of affected rows
                if (jdbcTemplate.update(updateStatement, args) == 1) {
                    log.info("Surgical successfully updated in the database: " + entity);
                    return true;
                }
                //if other then one row was affected roll back transaction
                //and log error
                transactionStatus.setRollbackOnly();
                log.error("Couldn't update " + entity + ". Query tried to modify "
                        + "more/less then one row. Check db design.");
                return false;
            });
        } else {
            //log error and return false
            log.error(entity + " doesn't have a valid id. Persistance skipped.");
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        if (id > 0) {
            /*
             creating a new controlled transaction. obtain a callback function
             (new TransactionCallback<Boolean>(TransactionStatus)) from the 
             transaction template. This function takes only one argument - Transaction
             status which can rollback a transaction. Using lambda we avoid the
             explicit new TransactionCallback<Boolean>(TransactionStatus){} declaration
             */
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                String deleteStatement = "DELETE FROM SURGICAL WHERE ID = ?";
                //update the db, return the # of deleted rows
                if (jdbcTemplate.update(deleteStatement, id) == 1) {
                    log.info("Surgical with id " + id + " has been removed from "
                            + "the database");
                    return true;
                }
                transactionStatus.setRollbackOnly();
                log.error(id + " isn't a valid id. Deleting skipped.");
                return false;
            });
        } else {
            log.error("Cannot delete the surgical: " + id + " is an invalid "
                    + "id");
            return false;
        }
    }

    @Override
    public boolean updateBatch(List<Surgical> entities) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Surgical> getPatientDetails(int patientId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}