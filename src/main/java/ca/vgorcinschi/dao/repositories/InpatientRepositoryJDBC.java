package ca.vgorcinschi.dao.repositories;

import static ca.vgorcinschi.util.CommonUtil.localToSql;
import ca.vgorcinschi.model.Inpatient;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Inpatient implementation of GenericRepository and DetailRecordMethods
 *
 * @author vgorcinschi
 */
@Repository
@Qualifier("jdbc")
public class InpatientRepositoryJDBC implements InpatientRepository {

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

    //prepared statement that will be used by two methods
    private final String updatePreparedStatement = "UPDATE INPATIENT SET PATIENTID = ?,"
            + " DATEOFSTAY=?, ROOMNUMBER=?, DAILYRATE=?, SUPPLIES=? "
            + ",SERVICES =? WHERE ID=?";

    //injection point
    @Autowired
    public InpatientRepositoryJDBC(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public boolean add(Inpatient entity) {
        //our prepared statement
        String sql = "INSERT INTO INPATIENT (PATIENTID, DATEOFSTAY, ROOMNUMBER,"
                + "DAILYRATE, SUPPLIES, SERVICES) VALUES (?, ?, ?, ?, ?, ?)";
        //using try-catch to log two scenarions
        try {
            /**
             * this method does auto-conversion provided the sequence of the
             * arguments is the same -
             * @link http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html#update-java.lang.String-java.lang.Object...-
             */
            jdbcTemplate.update(sql, entity.getPatientId(), localToSql.apply(entity.getDateOfStay()),
                    entity.getRoomNumber(), entity.getDailyRate(), entity.getSupplies(),
                    entity.getServices());
            log.info("Inpatient " + entity + " was successfully saved to the DB.");
            return true;
        } catch (DataAccessException e) {
            //log error and return false
            log.error("SQL query for adding Inpatient " + entity + " failed. "
                    + e.getMostSpecificCause().toString());
            return false;
        }
    }

    @Override
    public boolean update(Inpatient entity) {
        if (entity.getId() > 0) {
            /*
             creating a new controlled transaction. obtain a callback function
             (new TransactionCallback<Boolean>(TransactionStatus)) from the 
             transaction template. This function takes only one argument - Transaction
             status which can rollback a transaction. Using lambda we avoid the
             explicit new TransactionCallback<Boolean>(TransactionStatus) declaration
             */
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                //query arguments, jdbcTemplate will automatically cast the types
                Object[] args = {entity.getPatientId(), localToSql.apply(entity.getDateOfStay()),
                    entity.getRoomNumber(), entity.getDailyRate(), entity.getSupplies(),
                    entity.getServices(), entity.getId()};
                //update the db, return the # of affected rows
                if (jdbcTemplate.update(updatePreparedStatement, args) == 1) {
                    log.info("Inpatient successfully updated in the database: " + entity);
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
                String deleteStatement = "DELETE FROM INPATIENT WHERE ID = ?";
                //update the db, return the # of deleted rows
                if (jdbcTemplate.update(deleteStatement, id) == 1) {
                    log.info("Inpatient with id " + id + " has been removed from "
                            + "the database");
                    return true;
                }
                transactionStatus.setRollbackOnly();
                log.error(id + " isn't a valid id. Deleting skipped.");
                return false;
            });
        } else {
            log.error("Cannot delete the inpatient: " + id + " is an invalid "
                    + "id");
            return false;
        }
    }

    @Override
    public boolean updateBatch(List<Inpatient> entities) {
        //if there's nothing to update - return false
        if (entities == null || entities.isEmpty()) {
            log.warn("updateBatch() was called on an empty Inpatients list.");
            return false;
        } else {
            //view comments for the block in lines 117-123
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                /**
                 * a couple of notes about batchUpdate() method. It executes the
                 *
                 * @param sql on every item in the entities
                 * @return an array of integers (where integers is number of
                 * rows affected) by each query
                 */
                int[] arrayOfaffectedRows = jdbcTemplate.batchUpdate(updatePreparedStatement, new BatchPreparedStatementSetter() {
                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Inpatient inpatient = entities.get(i);
                        ps.setInt(1, inpatient.getPatientId());
                        ps.setTimestamp(2, localToSql.apply(inpatient.getDateOfStay()));
                        ps.setString(3, inpatient.getRoomNumber());
                        ps.setBigDecimal(4, inpatient.getDailyRate());
                        ps.setBigDecimal(5, inpatient.getSupplies());
                        ps.setBigDecimal(6, inpatient.getServices());
                        ps.setInt(7, inpatient.getId());
                    }
                });
                /*
                 Our test is "Did every query update only one row in the DB?"
                 if yes - it is good, else we will rollback.
                 */
                if (Arrays.stream(arrayOfaffectedRows).allMatch(i -> i == 1)) {
                    log.info(entities.size() + " inpatient records for patient "
                            + "with id " + entities.get(0).getPatientId() + " have been successfully "
                            + "updated.");
                    return true;
                }
                transactionStatus.setRollbackOnly();
                log.error("Rolled back batch update of " + entities.size() + " records for patient "
                        + "with id " + entities.get(0).getPatientId() + " for the attempt "
                        + "to modify <>1 record(s) with a single update statement");
                return false;
            });
        }
    }

    @Override
    public List<Inpatient> getPatientDetails(int patientId) {
        //the prepared statement
        String sql = "SELECT * FROM INPATIENT WHERE PATIENTID = ?";
        //create an empty List of Inpatients
        List<Inpatient> inpatients = new ArrayList<>();
        //auto mapping the db columns to data bean properties: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/BeanPropertyRowMapper.html
        inpatients = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(Inpatient.class), patientId);
        return inpatients;
    }
}
