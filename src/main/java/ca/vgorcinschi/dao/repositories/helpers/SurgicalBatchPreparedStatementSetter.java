package ca.vgorcinschi.dao.repositories.helpers;

import static ca.vgorcinschi.util.CommonUtil.localToSql;
import ca.vgorcinschi.model.Surgical;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 * SurgicalBatchPreparedStatementSetter is a template based on Spring's
 * BatchPreparedStatementSetter. It will be used to generate an update prepared
 * statement for each node of the passed list of detail record.
 *
 * @author vgorcinschi
 */
public class SurgicalBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<Surgical> surgicals;

    public SurgicalBatchPreparedStatementSetter(List<Surgical> surgicals) {
        this.surgicals = surgicals;
    }

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Surgical surgical = surgicals.get(i);
        ps.setInt(1, surgical.getPatientId());
        ps.setTimestamp(2, localToSql.apply(surgical.getDateOfSurgery()));
        ps.setString(3, surgical.getSurgery());
        ps.setBigDecimal(4, surgical.getRoomFee());
        ps.setBigDecimal(5, surgical.getSurgeonFee());
        ps.setBigDecimal(6, surgical.getSupplies());
        ps.setInt(7, surgical.getId());
    }

    @Override
    public int getBatchSize() {
        return surgicals.size();
    }

    public List<Surgical> getSurgicals() {
        return surgicals;
    }
}
