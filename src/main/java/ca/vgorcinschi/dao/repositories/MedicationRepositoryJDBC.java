package ca.vgorcinschi.dao.repositories;

import static ca.vgorcinschi.util.CommonUtil.localToSql;
import ca.vgorcinschi.dao.repositories.helpers.MedicationBatchPreparedStatementSetter;
import ca.vgorcinschi.dao.repositories.helpers.MedicationPreparedStatementSetter;
import ca.vgorcinschi.model.Medication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Medication implementation of GenericRepository and DetailRecordMethods NB:
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
public class MedicationRepositoryJDBC implements MedicationRepository {

    /**
     * we will only use this logger to log methods that return boolean. For
     * methods which declare 'throws' we will use logger one level up in the
     * service interface
     */
    private final Logger log
            = LoggerFactory.getLogger(this.getClass().getName());

    //configured in src.main.resources.application.yml
    private final JdbcTemplate jdbcTemplate;

    //Spring's transaction template is used to rollback the transaction
    private final TransactionTemplate transactionTemplate;

    //prepared statement
    private final String updatePreparedStatement = "UPDATE MEDICATION SET PATIENTID = ?,"
            + " DATEOFMED=?, MED=?, UNITCOST=?, UNITS=? WHERE ID=?";

    @Autowired
    public MedicationRepositoryJDBC(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public boolean add(Medication entity) {
        //our prepared statement
        String sql = "INSERT INTO MEDICATION (PATIENTID, DATEOFMED, MED,"
                + "UNITCOST, UNITS) VALUES (?, ?, ?, ?, ?)";
        try {
            /**
             * this method does auto-conversion provided the sequence of the
             * arguments is the same -
             * @link http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html#update-java.lang.String-java.lang.Object...-
             */
            jdbcTemplate.update(sql, entity.getPatientId(), localToSql.apply(entity.getDateOfMedication()),
                    entity.getMed(), entity.getUnitCost(), entity.getUnits());
            log.info("Medication " + entity + " was successfully saved to the DB.");
            return true;
        } catch (DataAccessException e) {
            //log error and return false
            log.error("SQL query for adding Medication " + entity + " failed. "
                    + e.getMostSpecificCause().toString());
            return false;
        }
    }

    @Override
    public boolean update(Medication entity) {
        if (entity.getId() > 0) {
            /*
             creating a new controlled transaction. obtain a callback function
             (new TransactionCallback<Boolean>(TransactionStatus)) from the 
             transaction template. This function takes only one argument - Transaction
             status which can rollback a transaction.
             */
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                //update the db if the # of affected rows is 1
                if (jdbcTemplate.update(updatePreparedStatement,
                        new MedicationPreparedStatementSetter(entity)) == 1) {
                    log.info("Medication successfully updated in the database: " + entity);
                    return true;
                }
                //else roll back transaction and log error
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
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                String deleteStatement = "DELETE FROM MEDICATION WHERE ID = ?";
                //update the db, return the # of deleted rows
                if (jdbcTemplate.update(deleteStatement, id) == 1) {
                    log.info("Medication with id " + id + " deleted from the database");
                    return true;
                }
                transactionStatus.setRollbackOnly();
                log.error(id + " is an invalid id. Deleting skipped.");
                return false;
            });
        } else {
            log.error("Cannot delete the Medication: " + id + " is an invalid "
                    + "id");
            return false;
        }
    }

    @Override
    public boolean updateBatch(List<Medication> entities) {
        //if there's nothing to update - return false
        if (entities == null || entities.isEmpty()) {
            log.warn("updateBatch() was called on an empty Medications' list.");
            return false;
        } else {
            //I think this is an optimal implementations of the use of TransactionTemplate 
            //with BatchPreparedStatementSetter - see also Inpatient & Surgical RepositoryJDBCs
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                int[] arrayOfaffectedRows = jdbcTemplate.batchUpdate(updatePreparedStatement, 
                        new MedicationBatchPreparedStatementSetter(entities));
                //Did every query update only one row in the DB?
                if (Arrays.stream(arrayOfaffectedRows).allMatch(i -> i == 1)) {
                    log.info(entities.size() + " surgical records for patient "
                            + "with id " + entities.get(0).getPatientId() + " have been successfully "
                            + "updated.");
                    return true;
                }
                //else - rollback
                transactionStatus.setRollbackOnly();
                log.error("Rolled back batch update of " + entities.size() + " records for patient "
                        + "with id " + entities.get(0).getPatientId() + " for the attempt "
                        + "to modify <>1 record(s) with a single update statement");
                return false;
            });
        }
    }

    @Override
    public List<Medication> getPatientDetails(int patientId) {
         //the prepared statement
        String sql = "SELECT * FROM MEDICATION WHERE PATIENTID = ?";
        //create an empty List of Surgical's
        List<Medication> medications = new ArrayList<>();
        //auto mapping the db columns to data bean properties: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/BeanPropertyRowMapper.html
        medications = jdbcTemplate.query(sql, 
                BeanPropertyRowMapper.newInstance(Medication.class), patientId);
        return medications;
    }
}