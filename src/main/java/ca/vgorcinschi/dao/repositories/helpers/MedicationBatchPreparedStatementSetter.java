package ca.vgorcinschi.dao.repositories.helpers;

import static ca.vgorcinschi.util.CommonUtil.localToSql;
import ca.vgorcinschi.model.Medication;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 *
 * @author vgorcinschi
 */
public class MedicationBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<Medication> medications;

    public MedicationBatchPreparedStatementSetter(List<Medication> surgicals) {
        this.medications = surgicals;
    }

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Medication medication = medications.get(i);
        ps.setInt(1, medication.getPatientId());
        ps.setTimestamp(2, localToSql.apply(medication.getDateOfMedication()));
        ps.setString(3, medication.getMed());
        ps.setBigDecimal(4, medication.getUnitCost());
        ps.setBigDecimal(5, medication.getUnits());
        ps.setInt(6, medication.getId());
    }

    @Override
    public int getBatchSize() {
        return medications.size();
    }

    public List<Medication> getMedication() {
        return medications;
    }
}