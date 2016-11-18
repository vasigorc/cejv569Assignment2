package ca.vgorcinschi.dao.repositories;

import static ca.vgorcinschi.util.CommonUtil.*;
import ca.vgorcinschi.model.Patient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * the JDBC implementation of PatientRepository
 *
 * @author vgorcinschi
 */
@Qualifier("jdbc")
@Repository
public class PatientRepositoryJDBC implements PatientRepository {

    /**
     * note we will only use this logger to log methods that return boolean. For
     * methods which declare 'throws' we will use logger one level up in the
     * service interface
     */
    private final Logger log
            = LoggerFactory.getLogger(this.getClass().getName());

    //general select for patient(s)
    private final String generalSelect = "SELECT * FROM PATIENT";

    //configured in src.main.resources.application.yml
    private final JdbcTemplate jdbcTemplate;

    //Spring's transaction template is used to rollback the transaction
    private final TransactionTemplate transactionTemplate;

    //point of injection
    @Autowired
    public PatientRepositoryJDBC(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * retrieving just one object by an unique id
     *
     * @param patientId - is the id of the searched employee
     * @return - a patient object
     * @throws DataAccessException - failed query, incl. no result
     * @throws IncorrectResultSizeDataAccessException - result set contained >1
     * row
     */
    @Override
    public Patient findById(int patientId) throws DataAccessException, 
            IncorrectResultSizeDataAccessException, EmptyResultDataAccessException {
        //appending the condition to the general query
        String byId = generalSelect + " WHERE PATIENTID = ?";
        try {
            
        } catch (EmptyResultDataAccessException e) {
        }
        return jdbcTemplate.queryForObject(byId, (rs, rowCount) -> {
            //Patient object to be filled
            Patient candidate = new Patient();
            //populate the candidate 
            candidate.setPatientId(patientId);
            candidate.setLastName(rs.getString("LASTNAME"));
            candidate.setFirstName(rs.getString("FIRSTNAME"));
            candidate.setDiagnosis(rs.getString("DIAGNOSIS"));
            candidate.setAdmissionDate(rs.getTimestamp("ADMISSIONDATE").toLocalDateTime());
            candidate.setReleaseDate(rs.getTimestamp("RELEASEDATE").toLocalDateTime());
            return candidate;
        }, patientId);
    }

    /**
     * return 1-n number of Patients matching
     *
     * @param lastName of an employee/employee(s)
     * @return a list of patient objects
     * @throws DataAccessException query failed
     */
    @Override
    public List<Patient> findByLastName(String lastName) throws DataAccessException {
        //appending the condition to the general query
        String byLastName = generalSelect + " WHERE LASTNAME = ?";
        //the list that we will return
        List<Patient> patients = new ArrayList<>();
        //a list of all rows matching the query
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(byLastName, lastName);
        //iterate through the result set rows
        rows.stream().forEach((row) -> {
            //create a patient
            Patient p = new Patient((int) row.get("PATIENTID"), (String) row.get("LASTNAME"),
                    (String) row.get("FIRSTNAME"), (String) row.get("DIAGNOSIS"),
                    ((Timestamp) row.get("ADMISSIONDATE")).toLocalDateTime(),
                    ((Timestamp) row.get("RELEASEDATE")).toLocalDateTime());
            patients.add(p);
        });
        return patients;
    }

    /**
     * similar to the previous query, but there is no filter. Return all
     * patients.
     *
     * @return list of all patients
     * @throws DataAccessException
     */
    @Override
    public List<Patient> getAll() throws DataAccessException {
        //the list that we will return
        List<Patient> patients = new ArrayList<>();
        //a list of all rows
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(generalSelect);
        //iterate through the result set rows
        rows.stream().forEach((row) -> {
            //create a patient
            Patient p = new Patient((int) row.get("PATIENTID"), (String) row.get("LASTNAME"),
                    (String) row.get("FIRSTNAME"), (String) row.get("DIAGNOSIS"),
                    ((Timestamp) row.get("ADMISSIONDATE")).toLocalDateTime(),
                    ((Timestamp) row.get("RELEASEDATE")).toLocalDateTime());
            patients.add(p);
        });
        return patients;
    }

    /**
     * serialize the patient and save to the db
     *
     * @param entity - a Patient object
     * @return 
     */
    @Override
    public boolean add(Patient entity) {
        //our prepared statement
        String sql = "INSERT INTO PATIENT (LASTNAME, FIRSTNAME, DIAGNOSIS,"
                + "ADMISSIONDATE, RELEASEDATE) VALUES (?, ?, ?, ?, ?)";
        //using try-catch to log two scenarions
        try {
            /**
             * this method does auto-conversion provided the sequence of the
             * arguments is the same -
             * @link http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html#update-java.lang.String-java.lang.Object...-
             */
            jdbcTemplate.update(sql, entity.getLastName(), entity.getFirstName(),
                    entity.getDiagnosis(), localToSql.apply(entity.getAdmissionDate()),
                    localToSql.apply(entity.getReleaseDate()));
            log.info("Patient " + entity + " was successfully saved to the DB.");
            return true;
        } catch (DataAccessException e) {
            //log error and return false
            log.error("SQL query for adding Patient " + entity + " failed. "
                    + e.getMostSpecificCause().toString());
            return false;
        }
    }

    /**
     * update one Patient record
     *
     * @param entity - a patient object
     * @return true if we updated one and only one db record, false otherwise
     */
    @Override
    public boolean update(Patient entity) {
        if (entity.getPatientId() > 0) {
            /*
             creating a new controlled transaction. obtain a callback function
             (new TransactionCallback<Boolean>(TransactionStatus)) from the 
             transaction template. This function takes only one argument - Transaction
             status which can rollback a transaction. Using lambda we avoid the
             explicit new TransactionCallback<Boolean>(TransactionStatus) declaration
             */
            return transactionTemplate.execute((TransactionStatus transactionStatus) -> {
                //prepared statement
                String updateStatement = "UPDATE PATIENT SET LASTNAME = ?,"
                        + " FIRSTNAME=?, DIAGNOSIS=?, ADMISSIONDATE=?, RELEASEDATE=? "
                        + "WHERE PATIENTID=?";
                //query arguments, jdbcTemplate will automatically cast the types
                Object[] args = {entity.getLastName(), entity.getFirstName(), entity.getDiagnosis(),
                    localToSql.apply(entity.getAdmissionDate()), localToSql.apply(entity.getReleaseDate()),
                    entity.getPatientId()};
                //update the db, return the # of affected rows
                if (jdbcTemplate.update(updateStatement, args) == 1) {
                    log.info("Patient successfully updated in the database: "+entity);
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

    /**
     * this is the only method for which we don't have to also update detail
     * records. All details records have CASCADE DELETE
     *
     * @param entity - patient that we want to delete
     * @return boolean if we managed to delete the patient or not
     */
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
                String deleteStatement = "DELETE FROM PATIENT WHERE PATIENTID = ?";
                //update the db, return the # of deleted rows
                if (jdbcTemplate.update(deleteStatement, id) == 1) {
                    log.info("Patient with id "+id+" has been removed from "
                            + "the database");
                    return true;
                }
                log.error(id + " isn't a valid id. Deleting skipped.");
                return false;
            });
        } else {
            log.error("Cannot delete a patient: " + id + " is an invalid "
                    + "id");
            return false;
        }
    }
}
