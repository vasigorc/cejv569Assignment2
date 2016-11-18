package ca.vgorcinschi.dao.repositories.helpers;

import static ca.vgorcinschi.util.CommonUtil.localToSql;
import ca.vgorcinschi.model.Medication;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementSetter;

/**
 * MedicationPreparedStatementSetter is a template based on Spring's
 * PreparedStatementSetter. It will be used to generate an update prepared
 * statement for a single Medication record.
 *
 * @author vgorcinschi
 */
public class MedicationPreparedStatementSetter implements PreparedStatementSetter {

    private final Medication medication;

    public MedicationPreparedStatementSetter(Medication medication) {
        this.medication = medication;
    }

    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
        ps.setInt(1, medication.getPatientId());
        ps.setTimestamp(2, localToSql.apply(medication.getDateOfMedication()));
        ps.setString(3, medication.getMed());
        ps.setBigDecimal(4, medication.getUnitCost());
        ps.setBigDecimal(5, medication.getUnits());
        ps.setInt(6, medication.getId());
    }
}